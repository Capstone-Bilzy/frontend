package com.android.favorie

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.android.favorie.network.RetrofitClient
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MusicDetailFragment : Fragment(R.layout.fragment_music_detail) {

    private val categoryColors = mapOf(
        "MUSIC"   to "#EBCB00",
        "MOVIE"   to "#FF4B4B",
        "BOOK"    to "#00C853",
        "SPACE"   to "#FF8C00",
        "FASHION" to "#2196F3",
        "VIBE"    to "#9C27B0"
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mainTab      = arguments?.getString("mainTab") ?: "MUSIC"
        val category     = arguments?.getString("category")
        val startPosition = arguments?.getInt("startPosition", 0) ?: 0
        val targetItemId  = arguments?.getLong("targetItemId", -1L) ?: -1L
        val accentColor  = Color.parseColor(categoryColors[mainTab] ?: "#EBCB00")

        val viewPager = view.findViewById<ViewPager2>(R.id.viewPager_detail)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val apiCategory = when (mainTab) {
                    "SPACE" -> "PLACE"
                    "VIBE"  -> "MOOD"
                    else    -> mainTab
                }
                val response = RetrofitClient.api.getItems(category = apiCategory)
                if (response.isSuccessful) {
                    val all = response.body()?.items?.map { item ->
                        MusicItem(
                            id         = item.id.toString(),
                            title      = item.title,
                            artist     = "",
                            imageUrl   = item.imageUrl ?: "",
                            userText   = item.userText,
                            category   = item.category,
                            mainTab    = mainTab,
                            globalTags = item.globalTags ?: emptyList(),
                            localTags  = item.localTags ?: emptyList()
                        )
                    } ?: emptyList()

                    val items = if (category != null) all.filter { it.category == category } else all

                    val position = if (targetItemId != -1L)
                        items.indexOfFirst { it.id == targetItemId.toString() }.coerceAtLeast(0)
                    else
                        startPosition

                    viewPager.adapter = DetailPagerAdapter(items, accentColor)
                    viewPager.setCurrentItem(position, false)
                } else {
                    Toast.makeText(context, "목록을 불러오지 못했습니다", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "네트워크 오류", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun formatMeta(category: String, meta: Map<String, Any?>?): String {
        if (meta == null) return ""
        return when (category) {
            "MOVIE" -> listOfNotNull(
                meta["director"] as? String,
                meta["genre"] as? String,
                (meta["releaseYear"] as? Double)?.toInt()?.toString()
                    ?: (meta["releaseYear"] as? Int)?.toString()
            ).joinToString(" · ")
            "BOOK" -> listOfNotNull(
                meta["author"] as? String,
                meta["genre"] as? String,
                meta["pubDate"] as? String
            ).joinToString(" · ")
            "MUSIC" -> listOfNotNull(
                meta["artist"] as? String,
                meta["album"] as? String
            ).joinToString(" · ")
            "PLACE" -> listOfNotNull(
                meta["category"] as? String,
                meta["address"] as? String
            ).joinToString(" · ")
            else -> ""
        }
    }

    private fun deleteItem(itemId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.deleteItem(itemId.toLong())
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "삭제되었습니다", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                } else {
                    Toast.makeText(requireContext(), "삭제 실패 (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "네트워크 오류", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private inner class DetailPagerAdapter(
        private val items: List<MusicItem>,
        private val accentColor: Int
    ) : RecyclerView.Adapter<DetailPagerAdapter.PageHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_music_detail_page, parent, false)
            return PageHolder(v)
        }

        override fun onBindViewHolder(holder: PageHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        inner class PageHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val ivAlbumArt: ImageView  = view.findViewById(R.id.iv_detail_album_art)
            private val tvTitle: TextView      = view.findViewById(R.id.tv_detail_title)
            private val tvArtist: TextView     = view.findViewById(R.id.tv_detail_artist)
            private val tvMemo: TextView       = view.findViewById(R.id.tv_detail_memo)
            private val ivBookmark: ImageView  = view.findViewById(R.id.iv_detail_bookmark)
            private val viewAccentBar: View    = view.findViewById(R.id.view_accent_bar)
            private val chipGroup: ChipGroup   = view.findViewById(R.id.chip_group_tags)
            private val tvTagLoading: TextView = view.findViewById(R.id.tv_tag_loading)

            fun bind(item: MusicItem) {
                tvTitle.text  = item.title
                tvArtist.text = ""
                tvArtist.visibility = View.GONE
                tvMemo.text   = item.userText ?: ""

                setTags(item.globalTags + item.localTags)

                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val resp = RetrofitClient.api.getItemDetail(item.id.toLong())
                        if (resp.isSuccessful) {
                            val detail = resp.body()!!
                            val metaText = formatMeta(detail.category, detail.meta)
                            if (metaText.isNotEmpty()) {
                                tvArtist.text = metaText
                                tvArtist.visibility = View.VISIBLE
                            }
                            val detailTags = (detail.globalTags ?: emptyList()) + (detail.localTags ?: emptyList())
                            if (detailTags.isNotEmpty()) {
                                setTags(detailTags)
                                return@launch
                            }
                        }
                    } catch (_: Exception) {}
                    // 태그가 없으면 생성 완료될 때까지 폴링
                    showTagLoading()
                    pollForTags(item.id.toLong())
                }

                val params = ivAlbumArt.layoutParams as ConstraintLayout.LayoutParams
                if (item.mainTab == "MUSIC") {
                    params.dimensionRatio = "1:1"
                    params.height = 0
                    ivAlbumArt.scaleType = ImageView.ScaleType.CENTER_CROP
                    ivAlbumArt.adjustViewBounds = false
                    ivAlbumArt.layoutParams = params
                    Glide.with(itemView.context)
                        .load(item.imageUrl)
                        .centerCrop()
                        .into(ivAlbumArt)
                } else {
                    params.dimensionRatio = null
                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    ivAlbumArt.scaleType = ImageView.ScaleType.FIT_CENTER
                    ivAlbumArt.adjustViewBounds = true
                    ivAlbumArt.layoutParams = params
                    Glide.with(itemView.context)
                        .load(item.imageUrl)
                        .fitCenter()
                        .into(ivAlbumArt)
                }

                viewAccentBar.setBackgroundColor(accentColor)
                ivBookmark.visibility = View.GONE
            }

            private fun showTagLoading() {
                chipGroup.removeAllViews()
                chipGroup.visibility = View.GONE
                tvTagLoading.visibility = View.VISIBLE
            }

            private suspend fun pollForTags(itemId: Long) {
                repeat(10) { attempt ->
                    delay(3000)
                    try {
                        val statusResp = RetrofitClient.api.getItemStatus(itemId)
                        val status = statusResp.body()?.vectorStatus
                        android.util.Log.d("PollTags", "id=$itemId attempt=$attempt status=$status code=${statusResp.code()}")
                        if (statusResp.isSuccessful) {
                            when (status) {
                                "COMPLETED" -> {
                                    val detailResp = RetrofitClient.api.getItemDetail(itemId)
                                    val body = detailResp.body()
                                    val tags = (body?.globalTags ?: emptyList()) + (body?.localTags ?: emptyList())
                                    android.util.Log.d("PollTags", "COMPLETED tags=$tags")
                                    setTags(tags)
                                    return
                                }
                                "FAILED" -> {
                                    android.util.Log.w("PollTags", "id=$itemId 태그 생성 실패")
                                    setTags(emptyList())
                                    return
                                }
                            }
                        } else {
                            android.util.Log.e("PollTags", "status API 실패 code=${statusResp.code()}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("PollTags", "폴링 예외", e)
                    }
                }
                android.util.Log.w("PollTags", "id=$itemId 30초 내 COMPLETED 미도달")
                setTags(emptyList())
            }

            private fun setTags(tags: List<String>) {
                tvTagLoading.visibility = View.GONE
                chipGroup.removeAllViews()
                if (tags.isEmpty()) {
                    chipGroup.visibility = View.GONE
                    return
                }
                chipGroup.visibility = View.VISIBLE
                tags.forEach { tag ->
                    chipGroup.addView(Chip(itemView.context).apply {
                        text = "#$tag"
                        setTextColor(0xFFFFFFFF.toInt())
                        chipBackgroundColor = android.content.res.ColorStateList.valueOf(0xFF222222.toInt())
                        chipStrokeColor = android.content.res.ColorStateList.valueOf(0xFF444444.toInt())
                        chipStrokeWidth = itemView.resources.displayMetrics.density
                        isClickable = false
                    })
                }
            }
        }
    }
}