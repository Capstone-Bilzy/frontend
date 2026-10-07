package com.android.bilzy.ui.common

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import com.android.bilzy.databinding.DialogConfirmBinding

/**
 * 앱 톤(어두운 카드 + 글래스 버튼)에 맞춘 확인 팝업. 시스템 기본 AlertDialog는 흰 배경이라 화면과 따로 놀았다.
 * 왼쪽이 취소, 오른쪽이 실행 버튼이다(하단 버튼 쌍과 같은 배치).
 */
fun showConfirmDialog(
    context: Context,
    title: String,
    message: String,
    confirmText: String,
    cancelText: String = "취소",
    onConfirm: () -> Unit
) {
    val dialog = Dialog(context)
    val binding = DialogConfirmBinding.inflate(LayoutInflater.from(context))
    binding.tvConfirmTitle.text = title
    binding.tvConfirmMessage.text = message
    binding.btnConfirmCancel.text = cancelText
    binding.btnConfirmOk.text = confirmText
    binding.btnConfirmCancel.setOnClickListener { dialog.dismiss() }
    binding.btnConfirmOk.setOnClickListener {
        dialog.dismiss()
        onConfirm()
    }
    dialog.setContentView(binding.root)
    dialog.window?.apply {
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setLayout(
            (context.resources.displayMetrics.widthPixels * 0.86f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
    dialog.show()
}
