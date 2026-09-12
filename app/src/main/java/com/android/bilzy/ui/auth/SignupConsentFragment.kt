package com.android.bilzy.ui.auth

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentConsentBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 회원가입 흐름의 "동의 항목 안내" 화면(provider 선택 직후, 실제 OAuth 호출 전).
 * Signup→Terms→Info가 공유하는 nav_graph 스코프 SignupViewModel을 그대로 재사용한다
 * (SignupFragment와 같은 인스턴스라 여기서 startXxxSignup()을 호출해도 상태가 이어진다).
 */
@AndroidEntryPoint
class SignupConsentFragment : Fragment() {

    private var _binding: FragmentConsentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SignupViewModel by hiltNavGraphViewModels(R.id.nav_graph)

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
            if (isKakao) viewModel.startKakaoSignup(requireContext())
            else viewModel.startNaverSignup(requireContext())
        }

        observePrepare()
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

    private fun observePrepare() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.prepareState.collect { state ->
                    when (state) {
                        is SignupViewModel.PrepareState.Loading -> setLoading(true)
                        is SignupViewModel.PrepareState.Ready -> {
                            setLoading(false)
                            viewModel.consumePrepareState()
                            findNavController().navigate(
                                R.id.action_signupConsent_to_signupKakaoTerms,
                                bundleOf(SignupTermsFragment.ARG_IS_KAKAO to viewModel.isKakao)
                            )
                        }
                        is SignupViewModel.PrepareState.Error -> {
                            setLoading(false)
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            viewModel.consumePrepareState()
                        }
                        is SignupViewModel.PrepareState.Idle -> setLoading(false)
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
