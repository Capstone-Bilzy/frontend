package com.android.bilzy.ui.auth

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentSignupInfoBinding

class SignupInfoFragment : Fragment() {

    private var _binding: FragmentSignupInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isKakao = arguments?.getBoolean(SignupTermsFragment.ARG_IS_KAKAO, true) ?: true

        if (isKakao) {
            binding.tvTitle.text = "카카오"
            binding.tvSubtitle.text = "카카오로 간편하게 시작하세요"
            binding.tvAccountType.text = "Kakao 계정 정보 활용 동의"
            binding.tvConfirmText.text = "💬  동의하고 계속하기"
            binding.tvConfirmText.setTextColor(Color.parseColor("#191919"))
            binding.btnConfirm.setBackgroundResource(R.drawable.bg_button_kakao)
        } else {
            binding.tvTitle.text = "네이버"
            binding.tvSubtitle.text = "네이버로 간편하게 시작하세요"
            binding.tvAccountType.text = "Naver 계정 정보 활용 동의"
            binding.tvConfirmText.text = "N  네이버로 시작하기"
            binding.tvConfirmText.setTextColor(Color.WHITE)
            binding.btnConfirm.setBackgroundResource(R.drawable.bg_button_naver)
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnConfirm.setOnClickListener {
            val actionId = if (isKakao) {
                R.id.action_signupKakaoInfo_to_loginLoading
            } else {
                R.id.action_signupNaverInfo_to_loginLoading
            }
            findNavController().navigate(actionId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
