package com.android.bilzy.ui.settlement

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.bilzy.databinding.ItemOcrBinding

class OcrItemAdapter(private val items: List<Pair<String, String>>) :
    RecyclerView.Adapter<OcrItemAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemOcrBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOcrBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (name, price) = items[position]
        holder.binding.tvName.text = name
        holder.binding.tvPrice.text = price
    }

    override fun getItemCount() = items.size
}
