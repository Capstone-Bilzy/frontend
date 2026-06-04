package com.android.bilzy.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.bilzy.databinding.ItemHomeHistoryBinding
import com.android.bilzy.domain.model.SettlementHistory
import java.text.NumberFormat

/** 홈 "최근 정산 내역" 리스트 어댑터. */
class HomeHistoryAdapter(
    private val onClick: (SettlementHistory) -> Unit
) : RecyclerView.Adapter<HomeHistoryAdapter.VH>() {

    private val items = mutableListOf<SettlementHistory>()

    fun submit(list: List<SettlementHistory>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class VH(val binding: ItemHomeHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemHomeHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.tvTitle.text = item.title.ifBlank { "정산" }
        holder.binding.tvSubtitle.text = subtitle(item)
        holder.binding.tvAmount.text = NumberFormat.getInstance().format(item.totalAmount) + "원"
        holder.binding.root.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size

    /** "5.29 · 4명 참여" — 인원수가 있으면 인원, 없으면 정산 완료로 표기. */
    private fun subtitle(item: SettlementHistory): String {
        val date = formatDate(item.createdAt)
        val detail = if (item.memberCount > 0) "${item.memberCount}명 참여" else "정산 완료"
        return if (date.isNotEmpty()) "$date · $detail" else detail
    }

    /** "2026-05-29T08:36:49..." → "5.29" */
    private fun formatDate(iso: String?): String {
        iso ?: return ""
        val datePart = iso.substringBefore('T')          // 2026-05-29
        val parts = datePart.split('-')
        if (parts.size < 3) return ""
        val month = parts[1].toIntOrNull() ?: return ""
        val day = parts[2].toIntOrNull() ?: return ""
        return "$month.$day"
    }
}
