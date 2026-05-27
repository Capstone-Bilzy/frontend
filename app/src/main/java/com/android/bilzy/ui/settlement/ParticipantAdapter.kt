package com.android.bilzy.ui.settlement

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.bilzy.databinding.ItemParticipantBinding

class ParticipantAdapter(
    private val participants: MutableList<String>,
    private val onRemove: (Int) -> Unit
) : RecyclerView.Adapter<ParticipantAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemParticipantBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemParticipantBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.tvName.text = participants[position]
        holder.binding.btnRemove.setOnClickListener { onRemove(position) }
    }

    override fun getItemCount() = participants.size
}
