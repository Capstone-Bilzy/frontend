package com.android.favorie

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment

class WithdrawDialog(val onConfirm: () -> Unit) : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_withdraw, container, false)

        // 배경을 투명하게 해야 우리가 만든 둥근 모서리가 보입니다.
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        view.findViewById<View>(R.id.btn_withdraw_confirm).setOnClickListener {
            onConfirm() // 탈퇴 처리 로직 실행
            dismiss()   // 팝업 닫기
        }

        return view
    }
}