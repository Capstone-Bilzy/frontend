package com.android.bilzy.ui.room

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.bilzy.databinding.ItemSettlementPersonBinding

class SettlementPersonAdapter(private val items: List<Pair<String, String>>) :
    RecyclerView.Adapter<SettlementPersonAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemSettlementPersonBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSettlementPersonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (name, amount) = items[position]
        holder.binding.tvName.text = name
        holder.binding.tvAmount.text = amount
    }

    override fun getItemCount() = items.size
}
