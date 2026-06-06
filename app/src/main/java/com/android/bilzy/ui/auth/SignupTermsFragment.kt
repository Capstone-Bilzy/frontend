package com.android.bilzy.ui.auth

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentSignupTermsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 서비스 이용약관·개인정보 동의 화면(카카오 동의항목과는 별개 — 우리 서비스 약관 동의).
 * 각 항목을 탭하면 체크가 토글되고, **필수 약관에 동의해야만** "동의하고 계속하기"가 진행된다.
 * 동의 시 (OAuth에서 이미 확보한 토큰으로) 백엔드 가입을 완료하고 loginLoading으로 이동한다.
 *
 * 참고: 실제 이용약관/개인정보처리방침 '문서' 표시는 추후 추가 예정(현재는 동의 게이트만).
 */
@AndroidEntryPoint
class SignupTermsFragment : Fragment() {

    private var _binding: FragmentSignupTermsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SignupViewModel by hiltNavGraphViewModels(R.id.nav_graph)

    // 동의 상태 (초기엔 모두 미동의 — 사용자가 직접 동의해야 함)
    private var agreeTerms = false       // 이용약관 (필수)
    private var agreePrivacy = false     // 개인정보 수집·이용 (필수)
    private var agreeLocation = false    // 위치기반 (선택)
    private var agreeMarketing = false   // 마케팅 (선택)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
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

        renderChecks()

        binding.itemAgreeAll.setOnClickListener {
            val next = !allAgreed()
            agreeTerms = next; agreePrivacy = next; agreeLocation = next; agreeMarketing = next
            renderChecks()
        }
        binding.itemTerms1.setOnClickListener { agreeTerms = !agreeTerms; renderChecks() }
        binding.itemTerms2.setOnClickListener { agreePrivacy = !agreePrivacy; renderChecks() }
        binding.itemTerms3.setOnClickListener { agreeLocation = !agreeLocation; renderChecks() }
        binding.itemTerms4.setOnClickListener { agreeMarketing = !agreeMarketing; renderChecks() }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnConfirm.setOnClickListener {
            if (!agreeTerms || !agreePrivacy) {
                Toast.makeText(requireContext(), "필수 약관에 동의해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // OAuth에서 확보한 토큰으로 백엔드 가입 완료
            viewModel.completeSignup()
        }

        observeComplete()
    }

    private fun observeComplete() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.completeState.collect { state ->
                    when (state) {
                        is SignupViewModel.CompleteState.Loading -> binding.btnConfirm.isEnabled = false
                        is SignupViewModel.CompleteState.Success -> {
                            binding.btnConfirm.isEnabled = true
                            viewModel.consumeCompleteState()
                            findNavController().navigate(R.id.action_signupKakaoTerms_to_loginLoading)
                        }
                        is SignupViewModel.CompleteState.Error -> {
                            binding.btnConfirm.isEnabled = true
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            viewModel.consumeCompleteState()
                        }
                        is SignupViewModel.CompleteState.Idle -> binding.btnConfirm.isEnabled = true
                    }
                }
            }
        }
    }

    private fun allAgreed() = agreeTerms && agreePrivacy && agreeLocation && agreeMarketing

    private fun renderChecks() {
        setCheck(binding.icCheck1, agreeTerms)
        setCheck(binding.icCheck2, agreePrivacy)
        setCheck(binding.icCheck3, agreeLocation)
        setCheck(binding.icCheck4, agreeMarketing)
        setCheck(binding.icCheckAll, allAgreed())
    }

    /** 체크 on=진보라(#7C7AED), off=흐린 보라(30%). */
    private fun setCheck(icon: ImageView, on: Boolean) {
        val color = if (on) Color.parseColor("#7C7AED") else Color.parseColor("#4D7C7AED")
        icon.imageTintList = ColorStateList.valueOf(color)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_IS_KAKAO = "is_kakao"
    }
}
