package com.android.favorie

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.android.favorie.databinding.FragmentMyPageBinding
import com.android.favorie.network.RetrofitClient
import com.android.favorie.network.model.NicknameUpdateRequest
import kotlinx.coroutines.launch

class MyPageFragment : Fragment() {

    private var _binding: FragmentMyPageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadProfile()

        binding.btnLogout.setOnClickListener {
            TokenManager.accessToken = null
            TokenManager.refreshToken = null
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }

        binding.btnDeleteAccount.setOnClickListener {
            val dialog = WithdrawDialog {
                requireActivity().lifecycleScope.launch {
                    try { RetrofitClient.api.deleteAccount() } catch (_: Exception) {}
                    TokenManager.clear()
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    requireActivity().finish()
                }
            }
            dialog.show(parentFragmentManager, "WithdrawDialog")
        }

        binding.tvDisplayName.setOnClickListener {
            val etNickname = EditText(requireContext()).apply {
                setText(binding.tvDisplayName.text)
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
                    updateNickname(newNickname)
                }
                .setNegativeButton("취소", null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadProfile()
    }

    private fun loadProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getMyProfile()
                if (response.isSuccessful) {
                    val body = response.body()!!
                    binding.tvDisplayName.text = body.nickname
                    binding.tvLoginEmail.text  = body.email
                } else {
                    Log.e("MyPageFragment", "프로필 조회 실패: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("MyPageFragment", "프로필 조회 예외", e)
            }
        }
    }

    private fun updateNickname(nickname: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.updateNickname(NicknameUpdateRequest(nickname))
                if (response.isSuccessful) {
                    binding.tvDisplayName.text = response.body()!!.nickname
                    Toast.makeText(requireContext(), "닉네임이 변경되었습니다", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "변경 실패 (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("MyPageFragment", "닉네임 수정 예외", e)
                Toast.makeText(requireContext(), "네트워크 오류", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}