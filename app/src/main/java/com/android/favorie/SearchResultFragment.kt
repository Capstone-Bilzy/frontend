package com.android.favorie

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.android.favorie.network.RetrofitClient
import com.android.favorie.network.model.*
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch

class SearchResultFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val query            = arguments?.getString("search_query")   ?: ""
        var comment          = arguments?.getString("user_comment")   ?: ""
        val category         = arguments?.getString("category")       ?: "MUSIC"
        val externalId       = arguments?.getString("external_id")
        val imageUrl         = arguments?.getString("image_url")
        val address          = arguments?.getString("address")
        val latitude         = if (arguments?.containsKey("latitude") == true) arguments?.getDouble("latitude") else null
        val longitude        = if (arguments?.containsKey("longitude") == true) arguments?.getDouble("longitude") else null
        val recommendationId = if (arguments?.containsKey("recommendation_id") == true) arguments?.getLong("recommendation_id") ?: -1L else -1L

        val tvMovieMeta  = view.findViewById<TextView>(R.id.tv_movie_meta)
        val tvUserComment = view.findViewById<TextView>(R.id.tv_user_comment)
        val btnConfirm   = view.findViewById<Button>(R.id.btn_confirm_add)

        view.findViewById<TextView>(R.id.tv_song_info).text = query

        fun refreshCommentUI() {
            if (comment.isEmpty()) {
                tvUserComment.text = "코멘트를 입력해주세요"
                tvUserComment.setTextColor(android.graphics.Color.parseColor("#888888"))
                btnConfirm.text = "코멘트 입력하기"
            } else {
                tvUserComment.text = comment
                tvUserComment.setTextColor(android.graphics.Color.WHITE)
                btnConfirm.text = "+ ADD ITEM"
            }
        }
        refreshCommentUI()

        if (category == "MOVIE" && externalId != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val resp = RetrofitClient.api.getMovieDetail(externalId.toLong())
                    if (resp.isSuccessful) {
                        val detail = resp.body()!!
                        val meta = listOfNotNull(
                            detail.director?.let { "감독 $it" },
                            detail.genre,
                            detail.releaseYear?.toString()
                        ).joinToString(" · ")
                        if (meta.isNotEmpty()) {
                            tvMovieMeta.text = meta
                            tvMovieMeta.visibility = View.VISIBLE
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        Glide.with(this)
            .load(imageUrl)
            .centerCrop()
            .placeholder(R.drawable.ic_add)
            .into(view.findViewById<ImageView>(R.id.iv_album_art))

        btnConfirm.setOnClickListener {
            if (comment.length < 10) {
                // 코멘트가 없거나 짧으면 입력 다이얼로그 표시
                val etComment = EditText(requireContext()).apply {
                    hint = "10자 이상 입력해주세요"
                    setText(comment)
                }
                AlertDialog.Builder(requireContext())
                    .setTitle("나만의 코멘트")
                    .setView(etComment)
                    .setPositiveButton("확인") { _, _ ->
                        val input = etComment.text.toString().trim()
                        if (input.length < 10) {
                            Toast.makeText(requireContext(), "코멘트를 10자 이상 입력해주세요", Toast.LENGTH_SHORT).show()
                        } else {
                            comment = input
                            refreshCommentUI()
                        }
                    }
                    .setNegativeButton("취소", null)
                    .show()
            } else {
                btnConfirm.isEnabled = false
                saveItem(category, query, comment, externalId, imageUrl, address, latitude, longitude, recommendationId) {
                    btnConfirm.isEnabled = true
                }
            }
        }
    }

    private fun navigateToCategory(category: String) {
        val target = when (category) {
            "MOVIE"   -> MovieMainFragment()
            "BOOK"    -> BookMainFragment()
            "SPACE"   -> SpaceMainFragment()
            "FASHION" -> FashionMainFragment()
            "MOOD"    -> VibeMainFragment()
            else      -> MusicMainFragment()
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, target)
            .addToBackStack(null)
            .commit()
    }

    private fun saveItem(
        category: String,
        title: String,
        userText: String,
        externalId: String?,
        imageUrl: String?,
        address: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        recommendationId: Long = -1L,
        onFinally: (() -> Unit)? = null
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = when (category) {
                    "MOVIE"   -> RetrofitClient.api.recordMovie(
                        MovieItemRequest(tmdbId = externalId?.toIntOrNull() ?: 0, userText = userText, imageUrl = imageUrl)
                    )
                    "BOOK"    -> RetrofitClient.api.recordBook(
                        BookItemRequest(bookExternalId = externalId ?: title, userText = userText, imageUrl = imageUrl)
                    )
                    "FASHION" -> RetrofitClient.api.recordFashion(
                        FashionItemRequest(title = title, userText = userText, imageUrl = imageUrl)
                    )
                    "MOOD"    -> RetrofitClient.api.recordMood(
                        MoodItemRequest(title = title, userText = userText, imageUrl = imageUrl)
                    )
                    "SPACE"   -> RetrofitClient.api.recordPlace(
                        PlaceItemRequest(
                            kakaoPlaceId = externalId,
                            placeName    = title,
                            address      = address,
                            latitude     = latitude,
                            longitude    = longitude,
                            userText     = userText,
                            imageUrl     = imageUrl
                        )
                    )
                    else      -> RetrofitClient.api.recordMusic(
                        MusicItemRequest(spotifyId = externalId, title = title, userText = userText, imageUrl = imageUrl)
                    )
                }

                if (response.isSuccessful) {
                    val itemId = response.body()?.id
                    val initialStatus = response.body()?.vectorStatus
                    Log.d("SaveItem", "저장 성공: id=$itemId status=$initialStatus")

                    // 추천 아이템에서 저장한 경우 추천 목록에서 제거
                    if (recommendationId > 0L) {
                        try {
                            RetrofitClient.api.deleteRecommendation(recommendationId)
                            RecommendDetailFragment.sessionCache = null
                        } catch (e: Exception) {
                            Log.w("SaveItem", "추천 삭제 실패 id=$recommendationId", e)
                        }
                    }

                    // 태그 생성은 백그라운드에서 진행. 상세 화면이 vectorStatus를 폴링해 인라인으로 표시한다.
                    Toast.makeText(requireContext(), "저장되었습니다!", Toast.LENGTH_SHORT).show()
                    navigateToCategory(category)
                } else {
                    val errBody = response.errorBody()?.string() ?: ""
                    Log.e("SaveItem", "저장 실패: ${response.code()} ${response.message()} | $errBody")
                    Toast.makeText(requireContext(), "저장 실패 (${response.code()}): $errBody", Toast.LENGTH_LONG).show()
                    onFinally?.invoke()
                }
            } catch (e: Exception) {
                Log.e("SaveItem", "저장 예외", e)
                Toast.makeText(requireContext(), "네트워크 오류", Toast.LENGTH_SHORT).show()
                onFinally?.invoke()
            }
        }
    }
}