package com.android.bilzy.ui.mypage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentMyPageBinding
import com.android.bilzy.ui.room.RoomViewModel
import com.android.bilzy.ui.scan.ScanFlowViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MyPageFragment : Fragment() {

    private var _binding: FragmentMyPageBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MyPageViewModel by viewModels()
    // MyPageFragment도 nav_graph 안의 destination이라 nav_graph 스코프 ViewModel에 접근 가능하다.
    // 로그아웃 시 다른 계정의 정산 상태가 남지 않도록 함께 초기화한다.
    private val scanFlowViewModel: ScanFlowViewModel by hiltNavGraphViewModels(R.id.nav_graph)
    private val roomViewModel: RoomViewModel by hiltNavGraphViewModels(R.id.nav_graph)

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
                    renderAvatar(p.profileImageUrl)
                }
            }
        }
    }

    /** 프로필 이미지가 있으면 원형으로 채워 로드, 없으면(또는 로드 실패 시) 기존 캐릭터 아이콘 플레이스홀더 유지. */
    private fun renderAvatar(url: String) {
        if (url.isBlank()) {
            resetAvatarToPlaceholder()
            return
        }
        binding.ivProfileAvatar.scaleType = ImageView.ScaleType.CENTER_CROP
        binding.ivProfileAvatar.setPadding(0, 0, 0, 0)
        binding.ivProfileAvatar.load(url) {
            crossfade(true)
            transformations(CircleCropTransformation())
            placeholder(R.drawable.character_smile)
            listener(onError = { _, _ -> resetAvatarToPlaceholder() })
        }
    }

    private fun resetAvatarToPlaceholder() {
        binding.ivProfileAvatar.scaleType = ImageView.ScaleType.FIT_CENTER
        val padding = (6 * resources.displayMetrics.density).toInt()
        binding.ivProfileAvatar.setPadding(padding, padding, padding, padding)
        binding.ivProfileAvatar.setImageResource(R.drawable.character_smile)
    }

    private fun observeLogout() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loggedOut.collect {
                    scanFlowViewModel.reset()
                    roomViewModel.reset()
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
