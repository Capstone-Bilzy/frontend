package com.android.favorie

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

// 1. 여기서 <CommonGridAdapter.ViewHolder> 라고 정확히 지칭해야 합니다.
class CommonGridAdapter(
    private val items: List<CategoryItem>,
    private val onClick: (CategoryItem) -> Unit
) : RecyclerView.Adapter<CommonGridAdapter.ViewHolder>() {

    // 2. 내부 클래스 이름을 ViewHolder로 맞춥니다.
    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val ivBg: ImageView = v.findViewById(R.id.iv_grid_bg)
        val tvTitle: TextView = v.findViewById(R.id.tv_grid_title)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_common_grid, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.title

        Glide.with(holder.itemView.context)
            .load(item.imageUrl)
            .placeholder(R.color.black)
            .into(holder.ivBg)

        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size
}