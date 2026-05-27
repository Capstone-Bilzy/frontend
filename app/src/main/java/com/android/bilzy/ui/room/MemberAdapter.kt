package com.android.bilzy.ui.room

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.bilzy.databinding.ItemMemberBinding

class MemberAdapter(private val members: List<Pair<String, Boolean>>) :
    RecyclerView.Adapter<MemberAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemMemberBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (name, joined) = members[position]
        holder.binding.tvName.text = name
        holder.binding.ivStatus.alpha = if (joined) 1f else 0.3f
    }

    override fun getItemCount() = members.size
}
