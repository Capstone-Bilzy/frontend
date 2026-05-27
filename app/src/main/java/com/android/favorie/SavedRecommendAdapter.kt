package com.android.favorie

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class SavedRecommendAdapter(
    private val items: List<RecommendItem>,
    private val onItemClick: (RecommendItem) -> Unit
) : RecyclerView.Adapter<SavedRecommendAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivImage: ImageView     = view.findViewById(R.id.iv_saved_recommend_image)
        val tvCategory: TextView   = view.findViewById(R.id.tv_saved_recommend_category)
        val tvTitle: TextView      = view.findViewById(R.id.tv_saved_recommend_title)
        val viewAccent: View       = view.findViewById(R.id.view_saved_recommend_accent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_saved_recommend, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val accentColor = Color.parseColor(item.categoryColor)

        Glide.with(holder.ivImage)
            .load(item.imageUrl)
            .placeholder(item.imageRes)
            .error(item.imageRes)
            .centerCrop()
            .into(holder.ivImage)

        holder.tvCategory.text = item.categoryName
        holder.tvCategory.setTextColor(accentColor)
        holder.tvTitle.text = item.itemTitle
        holder.viewAccent.setBackgroundColor(accentColor)

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size
}
