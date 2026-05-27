package com.android.favorie

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class MusicMoodAdapter(
    private val moodList: List<MoodItem>,
    private val onMoodClick: (MoodItem) -> Unit
) : RecyclerView.Adapter<MusicMoodAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivBg: ImageView = view.findViewById(R.id.iv_grid_bg)
        val tvTitle: TextView = view.findViewById(R.id.tv_grid_title)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_common_grid, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = moodList[position]
        holder.tvTitle.text = item.title

        Glide.with(holder.itemView.context)
            .load(item.recentImageUrl)
            .centerCrop()
            .placeholder(R.color.black)
            .into(holder.ivBg)

        holder.itemView.setOnClickListener { onMoodClick(item) }
    }

    override fun getItemCount(): Int = moodList.size
}