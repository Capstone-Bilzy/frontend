package com.android.bilzy.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentSignupBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * 회원가입 진입 화면. provider를 고르면 동의 항목 안내(Consent) 화면으로 이동하고,
 * 거기서 "동의하고 계속하기"를 눌러야 **실제 OAuth**가 시작된다(동의 화면에서 확보한
 * 프로필로 다음 약관 동의 화면을 채운다).
 */
@AndroidEntryPoint
class SignupFragment : Fragment() {

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnKakao.setOnClickListener {
            findNavController().navigate(
                R.id.action_signup_to_signupConsent,
                bundleOf(SignupConsentFragment.ARG_PROVIDER to "kakao")
            )
        }

        binding.btnNaver.setOnClickListener {
            findNavController().navigate(
                R.id.action_signup_to_signupConsent,
                bundleOf(SignupConsentFragment.ARG_PROVIDER to "naver")
            )
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
