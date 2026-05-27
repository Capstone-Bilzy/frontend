package com.android.favorie

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.favorie.network.RetrofitClient
import kotlinx.coroutines.launch

class SavedRecommendFragment : Fragment(R.layout.fragment_saved_recommend) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_saved_recommends)
        val tvEmpty      = view.findViewById<TextView>(R.id.tv_saved_empty)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resp = RetrofitClient.api.getMyRecommendations()
                if (!resp.isSuccessful || resp.body() == null) {
                    showEmpty(recyclerView, tvEmpty)
                    return@launch
                }

                val results = resp.body()!!.results
                if (results.isEmpty()) {
                    showEmpty(recyclerView, tvEmpty)
                    return@launch
                }

                val colorMap = mapOf(
                    "MUSIC"   to "#EBCB00",
                    "MOVIE"   to "#FF4B4B",
                    "BOOK"    to "#00C853",
                    "PLACE"   to "#FF8C00",
                    "FASHION" to "#2196F3",
                    "MOOD"    to "#9C27B0"
                )
                val displayName = mapOf("PLACE" to "SPACE", "MOOD" to "VIBE")

                val items = results.map { result ->
                    val category = result.category.uppercase()
                    val meta = result.meta
                    RecommendItem(
                        categoryName     = displayName[category] ?: category,
                        categoryColor    = colorMap[category] ?: "#FFFFFF",
                        moodText         = "",
                        imageRes         = R.drawable.img_sample,
                        itemTitle        = meta?.title ?: "",
                        imageUrl         = meta?.imageUrl,
                        itemId           = result.itemId,
                        recommendationId = result.recommendationId,
                        apiCategory      = category,
                        meta             = meta
                    )
                }

                tvEmpty.visibility      = View.GONE
                recyclerView.visibility = View.VISIBLE
                recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
                recyclerView.adapter = SavedRecommendAdapter(items) { item ->
                    navigateToDetail(item)
                }

            } catch (e: Exception) {
                Log.e("SavedRecommend", "추천 목록 로드 실패", e)
                showEmpty(recyclerView, tvEmpty)
            }
        }
    }

    private fun showEmpty(recyclerView: RecyclerView, tvEmpty: TextView) {
        tvEmpty.visibility      = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun navigateToDetail(item: RecommendItem) {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.nav_host_fragment, RecommendItemDetailFragment.newInstance(item))
            .addToBackStack(null)
            .commit()
    }
}
