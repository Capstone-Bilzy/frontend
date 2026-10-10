package com.android.bilzy.ui.common

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doAfterTextChanged
import com.android.bilzy.databinding.DialogTextInputBinding

/**
 * 짧은 글자 한 줄을 입력받는 팝업(확인 팝업과 같은 톤). 비어 있으면 확인 버튼이 눌리지 않는다.
 * onConfirm에는 앞뒤 공백을 뗀 값이 넘어간다.
 */
fun showTextInputDialog(
    context: Context,
    title: String,
    initial: String,
    hint: String,
    maxLength: Int,
    confirmText: String,
    onConfirm: (String) -> Unit
) {
    val dialog = Dialog(context)
    val binding = DialogTextInputBinding.inflate(LayoutInflater.from(context))
    binding.tvInputTitle.text = title
    binding.btnInputOk.text = confirmText
    binding.etInput.hint = hint
    binding.etInput.filters = arrayOf(InputFilter.LengthFilter(maxLength))
    binding.etInput.setText(initial.take(maxLength))
    binding.etInput.setSelection(binding.etInput.text.length)

    fun refresh() {
        val ok = binding.etInput.text.toString().isNotBlank()
        binding.btnInputOk.isEnabled = ok
    }
    fun submit() {
        val value = binding.etInput.text.toString().trim()
        if (value.isEmpty()) return
        dialog.dismiss()
        onConfirm(value)
    }
    refresh()
    binding.etInput.doAfterTextChanged { refresh() }
    binding.etInput.setOnEditorActionListener { _, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_DONE) { submit(); true } else false
    }
    binding.btnInputCancel.setOnClickListener { dialog.dismiss() }
    binding.btnInputOk.setOnClickListener { submit() }

    dialog.setContentView(binding.root)
    dialog.window?.apply {
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setLayout(
            (context.resources.displayMetrics.widthPixels * 0.86f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }
    dialog.show()
    binding.etInput.requestFocus()
}
