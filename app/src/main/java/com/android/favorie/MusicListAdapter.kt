package com.android.favorie

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class MusicListAdapter(
    private val items: List<MusicItem>,
    private val itemClick: (MusicItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_LIST = 0 // 음악용
        private const val TYPE_GRID = 1 // 영화/책 등 그리드용
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position].mainTab == "MUSIC") TYPE_LIST else TYPE_GRID
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_LIST) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_music_list, parent, false)
            MusicViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_grid_thumbnail, parent, false)
            GridViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is MusicViewHolder -> holder.bind(item)
            is GridViewHolder -> holder.bind(item)
        }
        holder.itemView.setOnClickListener { itemClick(item) }
    }

    override fun getItemCount(): Int = items.size

    // ✅ 음악용 뷰홀더 (보내주신 XML ID에 맞게 수정됨)
    inner class MusicViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivAlbumArt: ImageView = view.findViewById(R.id.iv_album_art)
        private val tvMusicTitle: TextView = view.findViewById(R.id.tv_music_title)
        private val tvArtistName: TextView = view.findViewById(R.id.tv_artist_name)

        fun bind(item: MusicItem) {
            tvMusicTitle.text = item.title
            tvArtistName.text = item.artist
            Glide.with(itemView.context)
                .load(item.imageUrl)
                .centerCrop()
                .into(ivAlbumArt)
        }
    }

    // ✅ 그리드용 뷰홀더 (인스타 피드 스타일 - 썸네일만)
    inner class GridViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivThumb: ImageView = view.findViewById(R.id.iv_grid_thumb)

        fun bind(item: MusicItem) {
            Glide.with(itemView.context)
                .load(item.imageUrl)
                .centerCrop()
                .into(ivThumb)
        }
    }
}