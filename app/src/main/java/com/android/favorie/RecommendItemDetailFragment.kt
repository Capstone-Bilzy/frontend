package com.android.favorie

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.android.favorie.network.RetrofitClient
import com.bumptech.glide.Glide
import com.google.gson.Gson
import kotlinx.coroutines.launch

class RecommendItemDetailFragment : Fragment(R.layout.fragment_recommend_item_detail) {

    companion object {
        private const val ARG_ITEM = "recommend_item_json"

        fun newInstance(item: RecommendItem) = RecommendItemDetailFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_ITEM, Gson().toJson(item))
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val itemJson = arguments?.getString(ARG_ITEM) ?: return
        val item = Gson().fromJson(itemJson, RecommendItem::class.java) ?: return

        val ivImage      = view.findViewById<ImageView>(R.id.iv_recommend_detail_image)
        val tvCategory   = view.findViewById<TextView>(R.id.tv_recommend_detail_category)
        val tvTitle      = view.findViewById<TextView>(R.id.tv_recommend_detail_title)
        val tvDesc       = view.findViewById<TextView>(R.id.tv_recommend_detail_description)
        val btnAdd       = view.findViewById<Button>(R.id.btn_recommend_add_item)
        val viewAccent   = view.findViewById<View>(R.id.view_recommend_accent)

        val accentColor = Color.parseColor(item.categoryColor)

        Glide.with(this)
            .load(item.imageUrl)
            .placeholder(item.imageRes)
            .error(item.imageRes)
            .centerCrop()
            .into(ivImage)

        tvCategory.text = item.categoryName
        tvCategory.setTextColor(accentColor)
        tvTitle.text = item.itemTitle
        tvDesc.text  = item.moodText
        viewAccent.setBackgroundColor(accentColor)
        btnAdd.backgroundTintList = ColorStateList.valueOf(accentColor)

        btnAdd.setOnClickListener {
            btnAdd.isEnabled = false
            saveRecommendation(item) { btnAdd.isEnabled = true }
        }
    }

    // 추천 아이템 스크랩: PATCH /recommend/{id}/scrap.
    // 스크랩된 항목은 이후 GET /recommend/my 에 노출된다.
    private fun saveRecommendation(item: RecommendItem, onFinally: () -> Unit) {
        val recommendationId = item.recommendationId
        if (recommendationId <= 0L) {
            Toast.makeText(requireContext(), "잘못된 추천 항목입니다", Toast.LENGTH_SHORT).show()
            onFinally()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resp = RetrofitClient.api.scrapRecommendation(recommendationId)
                if (resp.isSuccessful) {
                    Toast.makeText(requireContext(), "스크랩되었습니다!", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                } else {
                    Log.e("RecommendItemDetail", "스크랩 실패: ${resp.code()}")
                    Toast.makeText(requireContext(), "스크랩 실패 (${resp.code()})", Toast.LENGTH_SHORT).show()
                    onFinally()
                }
            } catch (e: Exception) {
                Log.e("RecommendItemDetail", "스크랩 예외", e)
                Toast.makeText(requireContext(), "네트워크 오류", Toast.LENGTH_SHORT).show()
                onFinally()
            }
        }
    }
}
