package com.android.favorie

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.android.favorie.network.RetrofitClient
import com.android.favorie.network.model.RecommendItemMeta
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class RecommendDetailFragment : Fragment(R.layout.fragment_recommend_detail) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewPager = view.findViewById<ViewPager2>(R.id.viewPager_recommend)
        val selectedCategory = arguments?.getString("category_type") ?: "MUSIC"

        val cached = sessionCache
        if (cached != null) {
            bindViewPager(viewPager, cached, selectedCategory)
        } else {
            loadRecommendations(viewPager, selectedCategory)
        }
    }

    private fun loadRecommendations(viewPager: ViewPager2, selectedCategory: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // /category로 일일 추천 목록을 받고, 각 itemId마다 /recommend/items/{itemId}로 메타를 채운다.
                val recResp = RetrofitClient.api.getCategoryRecommendations()
                if (!recResp.isSuccessful) {
                    Log.e("RecommendDetail", "추천 조회 실패: ${recResp.code()}")
                    showError("추천 데이터를 불러오지 못했습니다.")
                    return@launch
                }

                val results = recResp.body()?.results ?: emptyList()
                if (results.isEmpty()) {
                    showError("아직 추천 데이터가 없습니다.")
                    return@launch
                }

                val items = results.map { result ->
                    async {
                        val meta = try {
                            val detailResp = RetrofitClient.api.getRecommendedItemDetail(result.itemId)
                            if (detailResp.isSuccessful) detailResp.body()?.meta else null
                        } catch (e: Exception) {
                            Log.w("RecommendDetail", "상세 조회 실패 itemId=${result.itemId}", e)
                            null
                        }
                        toRecommendItem(result.recommendationId, result.category, result.itemId, meta)
                    }
                }.awaitAll()

                if (items.isEmpty()) {
                    showError("추천 아이템을 불러오지 못했습니다.")
                    return@launch
                }

                sessionCache = items
                bindViewPager(viewPager, items, selectedCategory)

            } catch (e: Exception) {
                Log.e("RecommendDetail", "추천 로드 예외", e)
                showError("네트워크 오류가 발생했습니다.")
            }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun bindViewPager(
        viewPager: ViewPager2,
        items: List<RecommendItem>,
        selectedCategory: String
    ) {
        val adapter = RecommendAdapter(items) { item -> navigateToItemDetail(item) }
        viewPager.adapter = adapter

        val startIndex = items.indexOfFirst { it.categoryName == selectedCategory }
        if (startIndex != -1) viewPager.setCurrentItem(startIndex, false)
    }

    private fun toRecommendItem(
        recommendationId: Long,
        rawCategory: String,
        itemId: Long,
        meta: RecommendItemMeta?
    ): RecommendItem {
        val colorMap = mapOf(
            "MUSIC"   to "#EBCB00",
            "MOVIE"   to "#FF4B4B",
            "BOOK"    to "#00C853",
            "PLACE"   to "#FF8C00",
            "FASHION" to "#2196F3",
            "MOOD"    to "#9C27B0"
        )
        val displayName = mapOf("PLACE" to "SPACE", "MOOD" to "VIBE")
        val category = rawCategory.uppercase()

        val subtitle = when (category) {
            "MOVIE" -> listOfNotNull(
                meta?.director,
                meta?.releaseYear?.toString()
            ).joinToString(" · ").ifBlank { "추천 영화" }
            "BOOK"  -> listOfNotNull(
                meta?.author,
                meta?.genre
            ).joinToString(" · ").ifBlank { "추천 도서" }
            "MUSIC" -> listOfNotNull(
                meta?.artist,
                meta?.album
            ).joinToString(" · ").ifBlank { "추천 음악" }
            "PLACE" -> meta?.address?.ifBlank { "추천 장소" } ?: "추천 장소"
            else    -> "추천 아이템"
        }

        val description = when (category) {
            "MOVIE" -> meta?.genre ?: ""
            "BOOK"  -> meta?.genre ?: ""
            "MUSIC" -> meta?.album ?: ""
            "PLACE" -> meta?.address ?: ""
            else    -> ""
        }

        return RecommendItem(
            categoryName     = displayName[category] ?: category,
            categoryColor    = colorMap[category] ?: "#FFFFFF",
            moodText         = description,
            imageRes         = R.drawable.img_sample,
            itemTitle        = meta?.title ?: subtitle,
            imageUrl         = meta?.imageUrl,
            itemId           = itemId,
            recommendationId = recommendationId,
            apiCategory      = category,
            meta             = meta
        )
    }

    private fun navigateToItemDetail(item: RecommendItem) {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.nav_host_fragment, RecommendItemDetailFragment.newInstance(item))
            .addToBackStack(null)
            .commit()
    }

    companion object {
        // 세션 캐시: 앱 실행 중 동일 결과 재사용, 새 아이템 저장 시 무효화
        var sessionCache: List<RecommendItem>? = null
    }
}
