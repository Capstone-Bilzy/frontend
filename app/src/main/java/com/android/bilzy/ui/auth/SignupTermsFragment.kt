package com.android.bilzy.ui.auth

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentSignupTermsBinding

class SignupTermsFragment : Fragment() {

    private var _binding: FragmentSignupTermsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupTermsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isKakao = arguments?.getBoolean(ARG_IS_KAKAO, true) ?: true

        if (isKakao) {
            binding.tvTitle.text = "카카오"
            binding.tvConfirmText.text = "💬  동의하고 계속하기"
            binding.tvConfirmText.setTextColor(Color.parseColor("#191919"))
            binding.btnConfirm.setBackgroundResource(R.drawable.bg_button_kakao)
        } else {
            binding.tvTitle.text = "네이버"
            binding.tvConfirmText.text = "N  네이버로 시작하기"
            binding.tvConfirmText.setTextColor(Color.WHITE)
            binding.btnConfirm.setBackgroundResource(R.drawable.bg_button_naver)
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnConfirm.setOnClickListener {
            if (isKakao) {
                findNavController().navigate(R.id.action_signupKakaoTerms_to_signupKakaoInfo)
            } else {
                findNavController().navigate(R.id.action_signupNaverTerms_to_signupNaverInfo)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_IS_KAKAO = "is_kakao"
    }
}
