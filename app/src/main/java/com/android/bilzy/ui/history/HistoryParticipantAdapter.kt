package com.android.bilzy.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.bilzy.R

data class ParticipantItem(
    val name: String,
    val items: String,
    val amount: String,
    val adjustment: String?
)

class HistoryParticipantAdapter(
    private val participants: List<ParticipantItem>
) : RecyclerView.Adapter<HistoryParticipantAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvItems: TextView = view.findViewById(R.id.tvItems)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        val tvAdjustment: TextView = view.findViewById(R.id.tvAdjustment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_participant, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = participants[position]
        holder.tvName.text = item.name
        holder.tvItems.text = item.items
        holder.tvAmount.text = item.amount
        if (item.adjustment != null) {
            holder.tvAdjustment.visibility = View.VISIBLE
            holder.tvAdjustment.text = item.adjustment
        } else {
            holder.tvAdjustment.visibility = View.GONE
        }
    }

    override fun getItemCount() = participants.size
}
