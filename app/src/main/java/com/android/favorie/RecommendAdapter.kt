package com.android.favorie

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class RecommendAdapter(
    private val items: List<RecommendItem>,
    private val onItemClick: (RecommendItem) -> Unit
) : RecyclerView.Adapter<RecommendAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAlbumArt: ImageView   = view.findViewById(R.id.iv_detail_album_art)
        val tvTitle: TextView       = view.findViewById(R.id.tv_detail_title)
        val tvArtist: TextView      = view.findViewById(R.id.tv_detail_artist)
        val tvMemo: TextView        = view.findViewById(R.id.tv_detail_memo)
        val ivBookmark: ImageView   = view.findViewById(R.id.iv_detail_bookmark)
        val viewAccentBar: View     = view.findViewById(R.id.view_accent_bar)
        val chipGroupTags: ChipGroup = view.findViewById(R.id.chip_group_tags)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_music_detail_page, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val accentColor = Color.parseColor(item.categoryColor)

        if (!item.imageUrl.isNullOrBlank()) {
            Glide.with(holder.ivAlbumArt)
                .load(item.imageUrl)
                .placeholder(item.imageRes)
                .error(item.imageRes)
                .centerCrop()
                .into(holder.ivAlbumArt)
        } else {
            holder.ivAlbumArt.setImageResource(item.imageRes)
        }

        holder.tvTitle.text = item.itemTitle
        holder.tvArtist.text = item.categoryName
        holder.tvMemo.text = item.moodText

        holder.viewAccentBar.setBackgroundColor(accentColor)

        holder.chipGroupTags.removeAllViews()
        item.tags.forEach { tag ->
            val chip = Chip(holder.chipGroupTags.context).apply {
                text = "#$tag"
                setTextColor(Color.WHITE)
                chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#222222"))
                chipStrokeColor = ColorStateList.valueOf(Color.parseColor("#444444"))
                chipStrokeWidth = 1f
                isClickable = false
            }
            holder.chipGroupTags.addView(chip)
        }

        holder.ivBookmark.visibility = View.VISIBLE
        holder.ivBookmark.imageTintList = ColorStateList.valueOf(Color.WHITE)

        holder.ivBookmark.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size
}