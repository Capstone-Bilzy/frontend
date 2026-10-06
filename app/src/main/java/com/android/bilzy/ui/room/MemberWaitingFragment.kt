package com.android.bilzy.ui.room

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.view.setMargins
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentMemberWaitingBinding
import com.android.bilzy.domain.model.Settlement
import com.android.bilzy.domain.model.SettlementMember
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MemberWaitingFragment : Fragment() {

    private var _binding: FragmentMemberWaitingBinding? = null
    private val binding get() = _binding!!
    private val handler = Handler(Looper.getMainLooper())

    private val roomViewModel: RoomViewModel by hiltNavGraphViewModels(R.id.nav_graph)

    private var advanced = false
    private var fallbackScheduled = false

    /**
     * 멤버 합류 폴링(서버 재조회). 응답을 받은 뒤에 다음 요청까지 쉬므로 서버가 느려도 요청이 겹쳐 쌓이지 않고,
     * 화면이 보이는 동안(STARTED)에만 돈다 — 예전엔 1.5초마다 무조건 쏴서 응답이 2~4초 걸리면 요청이 겹쳤고
     * 홈 버튼으로 나가 있어도 계속 돌았다.
     */
    private fun startPolling() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (!advanced) {
                    roomViewModel.refresh()
                    delay(POLL_INTERVAL_MS)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMemberWaitingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 방장은 입장 후에도 뒤로가기로 초대 QR 화면으로 돌아갈 수 있다. 기본 뒤로가기는 바로 앞의
        // "입장 중" 화면으로 가는데, 그 화면은 곧장 이 화면으로 다시 넘어와 버려 뒤로가기가 먹지 않았다.
        // QR 화면을 거치지 않은 참여자(게스트)는 스택에 QR 화면이 없으므로 기존 동작 그대로 둔다.
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val nav = findNavController()
                    if (!nav.popBackStack(R.id.qrInviteFragment, false)) {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )

        observeRoom()
        startPolling()
    }

    /**
     * 모여야 하는 인원. 방장은 인원 설정 화면에서 고른 값을, 참여자는 서버에 저장된 정원을 쓴다.
     * 예전엔 참여자가 정원을 몰라(0) 혼자여도 곧장 금액 조정으로 넘어갔고, 그 화면이 "1명" 기준으로 계산됐다.
     */
    private fun targetCount(settlement: Settlement?): Int =
        roomViewModel.expectedCount.takeIf { it > 0 } ?: settlement?.memberCapacity ?: 0

    private fun observeRoom() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                roomViewModel.settlement.collect { settlement ->
                    settlement ?: return@collect
                    val members = settlement.members
                    val target = targetCount(settlement)
                    renderMembers(members, target)
                    // 정원이 없는 방(구버전 데이터 등)에서만 안전장치로 일정 시간 뒤 진행
                    if (target <= 0 && !fallbackScheduled) {
                        fallbackScheduled = true
                        handler.postDelayed({ advance() }, 7000L)
                    }
                    binding.tvStatus.text =
                        if (target > 0) "${members.size} / ${target}명" else "${members.size}명"
                    binding.progressBar.progress = when {
                        target > 0 -> (members.size * 100 / target).coerceIn(0, 100)
                        members.isNotEmpty() -> 100
                        else -> 10
                    }

                    val myNick = roomViewModel.myNickname.value
                    val iAmIn = myNick != null && members.any { it.nickname == myNick }
                    val ready = if (target > 0) members.size >= target else iAmIn || members.isNotEmpty()
                    if (ready) {
                        handler.postDelayed({ advance() }, 1200L)
                    }
                }
            }
        }
    }

    private fun advance() {
        if (advanced || _binding == null) return
        advanced = true
        findNavController().navigate(R.id.action_memberWaiting_to_amountAdjust)
    }

    /**
     * avatarRow를 다시 그린다. 정원(expectedCount)을 아는 경우(호스트)는 화면 진입 시점부터
     * 정원만큼 빈 슬롯을 전부 보여주고, 실제로 합류한 멤버 수만큼 앞에서부터 채운다.
     * 정원을 모르는 경우(게스트 — expectedCount<=0)는 기존처럼 합류한 멤버만 표시한다.
     */
    private fun renderMembers(members: List<SettlementMember>, target: Int) {
        val row = binding.avatarRow
        row.removeAllViews()
        val myNick = roomViewModel.myNickname.value
        val ctx = requireContext()

        val slots = if (target > 0) maxOf(target, members.size) else members.size
        if (slots == 0) return
        // 피그마는 6칸까지 고정 폭으로 왼쪽부터 놓는다. 그보다 많으면 한 줄에 들어가도록 균등 분배.
        val weighted = slots > 6
        row.weightSum = if (weighted) slots.toFloat() else -1f
        for (i in 0 until slots) {
            val member = members.getOrNull(i)
            row.addView(
                if (member != null) {
                    // 이 행에 렌더되는 멤버는 이미 합류를 마친 사람들뿐이라 항상 "합류완료" 스타일.
                    buildAvatarTile(
                        ctx,
                        initial = member.nickname.take(1),
                        label = if (member.nickname == myNick) "${member.nickname}(나)" else member.nickname,
                        active = true,
                        profileImageUrl = member.profileImageUrl,
                        weighted = weighted
                    )
                } else {
                    // 아직 합류하지 않은 정원 슬롯
                    buildAvatarTile(ctx, initial = "", label = "...", active = false, weighted = weighted)
                }
            )
        }
    }

    private companion object {
        const val POLL_INTERVAL_MS = 1500L
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroyView()
        _binding = null
    }
}
