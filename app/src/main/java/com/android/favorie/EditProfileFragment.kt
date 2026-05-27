package com.android.favorie

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.android.favorie.network.RetrofitClient
import com.android.favorie.network.model.NicknameUpdateRequest
import kotlinx.coroutines.launch

class EditProfileFragment : Fragment() {

    private var currentNickname: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvEditName = view.findViewById<TextView>(R.id.tv_edit_name)
        val tvEditId   = view.findViewById<TextView>(R.id.tv_edit_id)
        val btnWithdraw = view.findViewById<TextView>(R.id.btn_withdraw)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getMyProfile()
                if (response.isSuccessful) {
                    val body = response.body()!!
                    currentNickname = body.nickname
                    tvEditName.text = body.nickname
                    tvEditId.text   = body.email
                }
            } catch (e: Exception) {
                Log.e("EditProfileFragment", "프로필 조회 예외", e)
            }
        }

        tvEditName.setOnClickListener {
            val etNickname = EditText(requireContext()).apply {
                setText(currentNickname)
                hint = "새 닉네임 입력"
            }
            AlertDialog.Builder(requireContext())
                .setTitle("닉네임 수정")
                .setView(etNickname)
                .setPositiveButton("확인") { _, _ ->
                    val newNickname = etNickname.text.toString().trim()
                    if (newNickname.isEmpty()) {
                        Toast.makeText(requireContext(), "닉네임을 입력해주세요", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    updateNickname(newNickname, tvEditName)
                }
                .setNegativeButton("취소", null)
                .show()
        }

        btnWithdraw.setOnClickListener {
            WithdrawDialog {
                requireActivity().lifecycleScope.launch {
                    try { RetrofitClient.api.deleteAccount() } catch (_: Exception) {}
                    TokenManager.clear()
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    requireActivity().finish()
                }
            }.show(parentFragmentManager, "WithdrawDialog")
        }
    }

    private fun updateNickname(nickname: String, tvEditName: TextView) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.updateNickname(NicknameUpdateRequest(nickname))
                if (response.isSuccessful) {
                    currentNickname = response.body()!!.nickname
                    tvEditName.text = currentNickname
                    Toast.makeText(requireContext(), "닉네임이 변경되었습니다", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "변경 실패 (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("EditProfileFragment", "닉네임 수정 예외", e)
                Toast.makeText(requireContext(), "네트워크 오류", Toast.LENGTH_SHORT).show()
            }
        }
    }
}