package com.android.bilzy.ui.mypage

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import com.android.bilzy.ui.common.loading
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import com.android.bilzy.util.stableCacheKey
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentMyPageBinding
import com.android.bilzy.ui.common.showConfirmDialog
import com.android.bilzy.ui.common.showTextInputDialog
import androidx.activity.result.contract.ActivityResultContracts
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

    /** 프로필 사진으로 쓸 이미지 선택 → ViewModel이 압축 후 서버로 보낸다(화면을 벗어나도 끊기지 않게). */
    private val pickProfilePhoto =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri ?: return@registerForActivityResult
            val resolver = requireContext().contentResolver
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes == null || bytes.isEmpty()) {
                Toast.makeText(requireContext(), "이미지를 불러오지 못했어요", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            viewModel.updateProfileImage(bytes, resolver.getType(uri) ?: "image/jpeg")
        }

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

        binding.menuEmail.setOnClickListener { openEmailInquiry() }

        binding.btnEditPhoto.setOnClickListener { pickProfilePhoto.launch("image/*") }
        binding.btnEditName.setOnClickListener {
            val current = viewModel.profile.value?.nickname.orEmpty()
            showTextInputDialog(
                requireContext(),
                title = "이름 변경",
                initial = current,
                hint = "이름",
                maxLength = 20,  // 서버 닉네임 길이 제한
                confirmText = "변경"
            ) { name -> if (name != current) viewModel.updateNickname(name) }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.saving.collect { loading.set(it) } }
                viewModel.editFailed.collect {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.menuLogout.setOnClickListener {
            // 잘못 눌러 바로 로그아웃되지 않도록 한 번 확인한다.
            showConfirmDialog(
                requireContext(),
                title = "로그아웃",
                message = "로그아웃할까요?",
                confirmText = "로그아웃"
            ) {
                // 토큰 클리어가 끝나면 observeLogout()에서 온보딩으로 이동한다.
                viewModel.logout()
            }
        }

        binding.menuWithdraw.setOnClickListener {
            showConfirmDialog(
                requireContext(),
                title = "회원 탈퇴",
                message = "탈퇴하면 이름·프로필 사진·계좌 정보가 삭제되고\n정산 기록에는 '탈퇴한 사용자'로 남아요\n되돌릴 수 없어요",
                confirmText = "탈퇴하기"
            ) {
                loading.show()
                viewModel.withdraw()
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.withdrawFailed.collect {
                    loading.hide()
                    Toast.makeText(requireContext(), "탈퇴에 실패했어요. 잠시 후 다시 시도해주세요", Toast.LENGTH_SHORT).show()
                }
            }
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
        // 원형 변환이 적용되기 전(첫 로드·전환 중)에 사진이 잠깐 네모로 보이지 않도록 뷰 자체를
        // 원형 배경 외곽선으로 잘라 둔다. 테두리 링은 foreground(fg_profile_ring)로 사진 위에 그려진다.
        binding.ivProfileAvatar.clipToOutline = true
        binding.ivProfileAvatar.setPadding(0, 0, 0, 0)
        // 비트맵을 CircleCrop으로 자르면 그림이 한쪽으로 밀려 보였다 — 원형은 위의 clipToOutline이 맡는다.
        // 캐시 키를 고정해, 화면에 다시 들어오거나 새 서명 URL을 받아도 같은 사진이면 기본 캐릭터를 거치지 않고 바로 그린다.
        binding.ivProfileAvatar.load(url) {
            stableCacheKey(url)
            crossfade(true)
            listener(onError = { _, _ -> resetAvatarToPlaceholder() })
        }
    }

    private fun resetAvatarToPlaceholder() {
        binding.ivProfileAvatar.scaleType = ImageView.ScaleType.FIT_CENTER
        val padding = (13 * resources.displayMetrics.density).toInt()
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

    /** 문의 메일 작성 화면을 연다(받는 사람이 채워진 상태). 메일 앱이 없으면 주소를 알려준다. */
    private fun openEmailInquiry() {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$INQUIRY_EMAIL"))
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "메일 앱이 없어요. $INQUIRY_EMAIL 로 보내주세요", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val INQUIRY_EMAIL = "bilzyapp2026@gmail.com"
    }
}
