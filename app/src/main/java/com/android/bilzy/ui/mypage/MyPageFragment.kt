package com.android.bilzy.ui.mypage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentMyPageBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MyPageFragment : Fragment() {

    private var _binding: FragmentMyPageBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MyPageViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeProfile()
        observeLogout()

        binding.menuAccount.setOnClickListener {
            findNavController().navigate(R.id.action_myPage_to_myPageAccount)
        }

        binding.menuLogout.setOnClickListener {
            // 토큰 클리어가 끝나면 observeLogout()에서 온보딩으로 이동한다.
            viewModel.logout()
        }

        binding.navHome.setOnClickListener {
            findNavController().navigate(R.id.action_myPage_to_home)
        }

        binding.navScan.setOnClickListener {
            findNavController().navigate(R.id.action_myPage_to_scanHub)
        }

        binding.navHistory.setOnClickListener {
            findNavController().navigate(R.id.action_myPage_to_historyList)
        }
    }

    private fun observeProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.profile.collect { p ->
                    p ?: return@collect
                    binding.tvUserName.text = p.nickname
                    if (p.loginType.isNotBlank()) binding.tvLoginType.text = p.loginType
                }
            }
        }
    }

    private fun observeLogout() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loggedOut.collect {
                    findNavController().navigate(R.id.action_myPage_to_onboarding)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
