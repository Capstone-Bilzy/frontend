package com.android.bilzy.ui.settlement

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.bilzy.databinding.ItemOcrBinding
import com.android.bilzy.domain.model.ReceiptItemDraft

/**
 * OCR 결과/직접입력 화면 공용 인라인 편집 어댑터(프로토타입 .bz-items li와 동일하게 품목명/수량/가격을
 * 그 자리에서 바로 고칠 수 있다). 편집 중 매 키 입력마다 리스트를 다시 그리면(EditText 포커스가 날아가)
 * 타이핑이 끊기므로, 항목 개수가 그대로인 업데이트(=글자 수정)는 notifyDataSetChanged를 생략한다.
 */
class OcrItemAdapter(
    private val onDelete: (Int) -> Unit,
    private val onChange: (Int, ReceiptItemDraft) -> Unit
) : RecyclerView.Adapter<OcrItemAdapter.ViewHolder>() {

    private var items: List<ReceiptItemDraft> = emptyList()

    fun submit(newItems: List<ReceiptItemDraft>) {
        val sizeChanged = newItems.size != items.size
        items = newItems
        if (sizeChanged) notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemOcrBinding) : RecyclerView.ViewHolder(binding.root) {
        var nameWatcher: TextWatcher? = null
        var qtyWatcher: TextWatcher? = null
        var priceWatcher: TextWatcher? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOcrBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val binding = holder.binding

        holder.nameWatcher?.let { binding.etName.removeTextChangedListener(it) }
        holder.qtyWatcher?.let { binding.etQty.removeTextChangedListener(it) }
        holder.priceWatcher?.let { binding.etPrice.removeTextChangedListener(it) }

        if (binding.etName.text.toString() != item.name) binding.etName.setText(item.name)
        val qtyText = item.quantity.toString()
        if (binding.etQty.text.toString() != qtyText) binding.etQty.setText(qtyText)
        val priceText = if (item.price == 0L) "" else item.price.toString()
        if (binding.etPrice.text.toString() != priceText) binding.etPrice.setText(priceText)

        // 워처는 holder.bindingAdapterPosition으로 매 호출 시점의 현재 항목을 다시 읽는다.
        // submit()이 필드 편집만으로는 notifyDataSetChanged를 생략(포커스 유지)하므로,
        // onBindViewHolder 시점에 캡처한 item을 클로저로 물면 낡은 스냅샷을 계속 덮어쓰게 된다.
        holder.nameWatcher = simpleWatcher { text ->
            emitChange(holder) { it.copy(name = text) }
        }.also { binding.etName.addTextChangedListener(it) }

        holder.qtyWatcher = simpleWatcher { text ->
            emitChange(holder) { it.copy(quantity = (text.toIntOrNull() ?: 1).coerceAtLeast(1)) }
        }.also { binding.etQty.addTextChangedListener(it) }

        holder.priceWatcher = simpleWatcher { text ->
            emitChange(holder) { it.copy(price = (text.toLongOrNull() ?: 0L).coerceAtLeast(0L)) }
        }.also { binding.etPrice.addTextChangedListener(it) }

        binding.btnDelete.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onDelete(pos)
        }
    }

    private fun emitChange(holder: ViewHolder, transform: (ReceiptItemDraft) -> ReceiptItemDraft) {
        val pos = holder.bindingAdapterPosition
        if (pos == RecyclerView.NO_POSITION || pos !in items.indices) return
        onChange(pos, transform(items[pos]))
    }

    private fun simpleWatcher(onText: (String) -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        override fun afterTextChanged(s: Editable?) {
            onText(s?.toString().orEmpty())
        }
    }

    override fun getItemCount() = items.size
}
