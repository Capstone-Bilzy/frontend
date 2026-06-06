package com.android.bilzy.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentSignupBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 회원가입 진입 화면. "카카오로 회원가입"을 누르면 **카카오 OAuth를 먼저** 수행해
 * 실제 닉네임/프로필을 확보한 뒤 약관 동의로 진행한다(동의 화면에 실제 정보 표시).
 */
@AndroidEntryPoint
class SignupFragment : Fragment() {

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!

    // Signup → Terms → Info 가 공유하는 nav_graph 스코프 ViewModel
    private val viewModel: SignupViewModel by hiltNavGraphViewModels(R.id.nav_graph)

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
            viewModel.startKakaoSignup(requireContext())
        }

        binding.btnNaver.setOnClickListener {
            Toast.makeText(requireContext(), "네이버 로그인은 준비 중이에요", Toast.LENGTH_SHORT).show()
        }

        observePrepare()
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
                            findNavController().navigate(R.id.action_signup_to_signupKakaoTerms)
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
        binding.btnKakao.isEnabled = !loading
        binding.btnNaver.isEnabled = !loading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
