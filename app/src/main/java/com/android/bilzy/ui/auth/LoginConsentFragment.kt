package com.android.bilzy.ui.auth

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentConsentBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 로그인 흐름의 "동의 항목 안내" 화면(provider 선택 직후, 실제 OAuth 호출 전).
 * fragment-scope LoginViewModel을 직접 갖는다(LoginFragment와 별개 인스턴스) —
 * 로그인은 원래 fragment-scope 규칙을 따르는 화면이라 여기서도 그대로 유지한다.
 */
@AndroidEntryPoint
class LoginConsentFragment : Fragment() {

    private var _binding: FragmentConsentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()

    private val isKakao: Boolean by lazy { arguments?.getString(ARG_PROVIDER) != "naver" }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConsentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        applyProviderStyle()

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnConfirm.setOnClickListener {
            if (isKakao) viewModel.loginWithKakao(requireContext())
            else viewModel.loginWithNaver(requireContext())
        }

        observeState()
    }

    private fun applyProviderStyle() {
        if (isKakao) {
            binding.tvTitle.text = "카카오"
            binding.tvHeadline.text = "카카오로 간편하게 시작하세요"
            binding.tvNameSub.text = "Kakao 계정 정보 활용 동의"
            binding.tvConfirmText.text = "동의하고 계속하기"
            binding.tvConfirmText.setTextColor(Color.parseColor("#191919"))
            binding.btnConfirm.setBackgroundResource(R.drawable.bg_button_kakao)
            binding.ivConfirmIcon.setImageResource(R.drawable.ic_kakao_logo)
        } else {
            binding.tvTitle.text = "네이버"
            binding.tvHeadline.text = "네이버로 간편하게 시작하세요"
            binding.tvNameSub.text = "Naver 계정 정보 활용 동의"
            binding.tvConfirmText.text = "네이버로 시작하기"
            binding.tvConfirmText.setTextColor(Color.WHITE)
            binding.btnConfirm.setBackgroundResource(R.drawable.bg_button_naver)
            binding.ivConfirmIcon.setImageResource(R.drawable.ic_naver_logo)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        is LoginViewModel.LoginState.Loading -> setLoading(true)
                        is LoginViewModel.LoginState.Success -> {
                            setLoading(false)
                            findNavController().navigate(R.id.action_loginConsent_to_loginLoading)
                            viewModel.consumeState()
                        }
                        is LoginViewModel.LoginState.Error -> {
                            setLoading(false)
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            viewModel.consumeState()
                        }
                        is LoginViewModel.LoginState.Idle -> setLoading(false)
                    }
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnConfirm.isEnabled = !loading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_PROVIDER = "provider"
    }
}
