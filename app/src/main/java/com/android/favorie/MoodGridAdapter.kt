package com.android.favorie

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class MoodGridAdapter(
    private val items: List<MusicItem>,
    private val onClick: (MusicItem) -> Unit
) : RecyclerView.Adapter<MoodGridAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivMood: ImageView = view.findViewById(R.id.iv_mood_bg)
        val tvMood: TextView = view.findViewById(R.id.tv_mood_title)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mood_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvMood.text = item.category

        // ⭐ 여기 수정
        holder.itemView.setOnClickListener {
            onClick(item)
        }

        Glide.with(holder.itemView.context)
            .load(item.imageUrl)
            .centerCrop()
            .placeholder(android.R.color.darker_gray)
            .into(holder.ivMood)
    }

    override fun getItemCount(): Int = items.size
}