package com.android.bilzy.ui.settlement

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.bilzy.databinding.ItemOcrBinding
import com.android.bilzy.domain.model.ReceiptItemDraft
import java.text.NumberFormat

class OcrItemAdapter(
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<OcrItemAdapter.ViewHolder>() {

    private var items: List<ReceiptItemDraft> = emptyList()

    fun submit(newItems: List<ReceiptItemDraft>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemOcrBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOcrBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvName.text = item.name.ifBlank { "이름 없음" }
        holder.binding.tvQty.text = "${won(item.price)} × ${item.quantity}"
        holder.binding.tvPrice.text = won(item.subtotal)
        holder.binding.btnDelete.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onDelete(pos)
        }
    }

    override fun getItemCount() = items.size

    private fun won(value: Long) = NumberFormat.getInstance().format(value) + "원"
}
