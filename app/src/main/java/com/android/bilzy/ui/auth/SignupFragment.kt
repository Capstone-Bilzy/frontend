package com.android.bilzy.ui.auth

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
import com.android.bilzy.databinding.FragmentSignupBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 회원가입 진입 화면. 소셜 로그인은 백엔드가 upsert(없으면 생성)라 로그인과 동일 동작이므로
 * LoginViewModel을 그대로 재사용한다.
 */
@AndroidEntryPoint
class SignupFragment : Fragment() {

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()

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
            viewModel.loginWithKakao(requireContext())
        }

        binding.btnNaver.setOnClickListener {
            Toast.makeText(requireContext(), "네이버 로그인은 준비 중이에요", Toast.LENGTH_SHORT).show()
        }

        observeState()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        is LoginViewModel.LoginState.Loading -> setLoading(true)
                        is LoginViewModel.LoginState.Success -> {
                            setLoading(false)
                            findNavController().navigate(R.id.loginLoadingFragment)
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
        binding.btnKakao.isEnabled = !loading
        binding.btnNaver.isEnabled = !loading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
