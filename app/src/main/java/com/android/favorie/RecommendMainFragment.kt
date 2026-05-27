package com.android.favorie

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.android.favorie.network.RetrofitClient
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch

class RecommendMainFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_recommend_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 추천은 정형 데이터(MUSIC/MOVIE/BOOK/SPACE) 4종만 지원
        val cardMusic = view.findViewById<ConstraintLayout>(R.id.card_music)
        val cardMovie = view.findViewById<ConstraintLayout>(R.id.card_movie)
        val cardBook  = view.findViewById<ConstraintLayout>(R.id.card_book)
        val cardSpace = view.findViewById<ConstraintLayout>(R.id.card_space)

        cardMusic.setOnClickListener { navigateToDetail("MUSIC") }
        cardMovie.setOnClickListener { navigateToDetail("MOVIE") }
        cardBook.setOnClickListener  { navigateToDetail("BOOK") }
        cardSpace.setOnClickListener { navigateToDetail("SPACE") }

        loadRecommendThumbnails(view)
    }

    private fun loadRecommendThumbnails(view: View) {
        val ivMusic = view.findViewById<ImageView>(R.id.iv_music_img)
        val ivMovie = view.findViewById<ImageView>(R.id.iv_movie_img)
        val ivBook  = view.findViewById<ImageView>(R.id.iv_book_img)
        val ivSpace = view.findViewById<ImageView>(R.id.iv_space_img)

        val imageViewMap = mapOf(
            "MUSIC" to ivMusic,
            "MOVIE" to ivMovie,
            "BOOK"  to ivBook,
            "PLACE" to ivSpace
        )

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // /category가 일일 추천 목록을 반환한다 (당일 최초 호출 시 생성, 이후 같은 결과).
                // 응답에는 메타가 없으므로 itemId마다 /recommend/items/{itemId}로 imageUrl을 가져온다.
                val categoryResp = RetrofitClient.api.getCategoryRecommendations()
                if (!categoryResp.isSuccessful || categoryResp.body() == null) {
                    Log.e("RecommendMain", "category 호출 실패 code=${categoryResp.code()}")
                    return@launch
                }

                categoryResp.body()!!.results.forEach { result ->
                    launch {
                        try {
                            val detailResp = RetrofitClient.api.getRecommendedItemDetail(result.itemId)
                            if (!detailResp.isSuccessful) {
                                Log.w("RecommendMain", "아이템 상세 실패 itemId=${result.itemId} code=${detailResp.code()}")
                                return@launch
                            }
                            val imageUrl = detailResp.body()?.meta?.imageUrl ?: return@launch
                            imageViewMap[result.category.uppercase()]?.let { iv ->
                                Glide.with(this@RecommendMainFragment)
                                    .load(imageUrl)
                                    .centerCrop()
                                    .into(iv)
                            }
                        } catch (e: Exception) {
                            Log.e("RecommendMain", "아이템 상세 로드 예외 itemId=${result.itemId}", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("RecommendMain", "추천 목록 로드 실패", e)
            }
        }
    }

    private fun navigateToDetail(category: String) {
        val detailFragment = RecommendDetailFragment().apply {
            arguments = Bundle().apply { putString("category_type", category) }
        }
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.nav_host_fragment, detailFragment)
            .addToBackStack(null)
            .commit()
    }
}
