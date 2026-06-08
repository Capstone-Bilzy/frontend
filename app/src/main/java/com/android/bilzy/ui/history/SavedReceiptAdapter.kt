package com.android.bilzy.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.android.bilzy.R
import com.android.bilzy.domain.model.SavedReceipt
import java.text.NumberFormat

class SavedReceiptAdapter(
    private val onClick: (SavedReceipt) -> Unit
) : RecyclerView.Adapter<SavedReceiptAdapter.ViewHolder>() {

    private val items = mutableListOf<SavedReceipt>()

    fun submit(receipts: List<SavedReceipt>) {
        items.clear()
        items.addAll(receipts)
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivThumb: ImageView = view.findViewById(R.id.ivThumb)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvMeta: TextView = view.findViewById(R.id.tvMeta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_saved_receipt, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.storeName.ifBlank { "이름 없는 영수증" }
        holder.tvMeta.text = buildMeta(item)
        if (!item.imageUrl.isNullOrBlank()) {
            holder.ivThumb.imageTintList = null
            holder.ivThumb.load(item.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_receipt)
                error(R.drawable.ic_receipt)
            }
        } else {
            holder.ivThumb.setImageResource(R.drawable.ic_receipt)
        }
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size

    /** "57,500원 · 2026.05.04" — 총액 없으면 날짜만. */
    private fun buildMeta(item: SavedReceipt): String {
        val date = item.createdAt.take(10).replace("-", ".")
        val amount = item.totalAmount?.let {
            "${NumberFormat.getNumberInstance().format(it)}원 · "
        } ?: ""
        return "$amount$date"
    }
}
