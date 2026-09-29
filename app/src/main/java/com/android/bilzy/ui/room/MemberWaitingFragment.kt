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
import com.android.bilzy.domain.model.SettlementMember
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MemberWaitingFragment : Fragment() {

    private var _binding: FragmentMemberWaitingBinding? = null
    private val binding get() = _binding!!
    private val handler = Handler(Looper.getMainLooper())

    private val roomViewModel: RoomViewModel by hiltNavGraphViewModels(R.id.nav_graph)

    private var advanced = false

    /** 멤버 합류 폴링(서버 재조회). 내가 멤버에 포함되면 진행. */
    private val pollTick = object : Runnable {
        override fun run() {
            if (_binding == null || advanced) return
            roomViewModel.load()
            handler.postDelayed(this, 1500L)
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

        observeRoom()
        handler.post(pollTick)
        // 인원수를 설정하지 않은 경우(게스트 등)에만 안전장치로 일정 시간 뒤 진행
        if (roomViewModel.expectedCount <= 0) {
            handler.postDelayed({ advance() }, 7000L)
        }
    }

    private fun observeRoom() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                roomViewModel.settlement.collect { settlement ->
                    settlement ?: return@collect
                    val members = settlement.members
                    val target = roomViewModel.expectedCount
                    renderMembers(members)
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
        handler.removeCallbacks(pollTick)
        findNavController().navigate(R.id.action_memberWaiting_to_amountAdjust)
    }

    /**
     * avatarRow를 다시 그린다. 정원(expectedCount)을 아는 경우(호스트)는 화면 진입 시점부터
     * 정원만큼 빈 슬롯을 전부 보여주고, 실제로 합류한 멤버 수만큼 앞에서부터 채운다.
     * 정원을 모르는 경우(게스트 — expectedCount<=0)는 기존처럼 합류한 멤버만 표시한다.
     */
    private fun renderMembers(members: List<SettlementMember>) {
        val row = binding.avatarRow
        row.removeAllViews()
        val target = roomViewModel.expectedCount
        val myNick = roomViewModel.myNickname.value

        if (target > 0) {
            row.weightSum = target.toFloat()
            for (i in 0 until target) {
                val member = members.getOrNull(i)
                row.addView(
                    if (member != null) {
                        avatarTile(member.nickname, member.nickname == myNick, member.profileImageUrl)
                    } else {
                        emptySlotTile()
                    }
                )
            }
        } else {
            if (members.isEmpty()) return
            row.weightSum = members.size.toFloat()
            members.forEach { member ->
                row.addView(avatarTile(member.nickname, member.nickname == myNick, member.profileImageUrl))
            }
        }
    }

    /** 아직 합류하지 않은 정원 슬롯: 빈 원 + 체크 배지 없음. */
    private fun emptySlotTile(): View {
        val ctx = requireContext()
        val tile = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val circle = FrameLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
            setBackgroundResource(R.drawable.bg_avatar_pending)
        }
        val label = TextView(ctx).apply {
            text = ""
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(dp(4)) }
        }
        tile.addView(circle)
        tile.addView(label)
        return tile
    }

    private fun avatarTile(name: String, isMe: Boolean, profileImageUrl: String?): View {
        val ctx = requireContext()
        val tile = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val circle = FrameLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
            setBackgroundResource(R.drawable.bg_avatar_done)
            clipChildren = false
            clipToPadding = false
        }
        val initial = TextView(ctx).apply {
            text = name.take(1)
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER
            )
        }
        circle.addView(initial)
        if (!profileImageUrl.isNullOrBlank()) {
            val avatarImage = ImageView(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            circle.addView(avatarImage)
            // 이니셜을 아래 레이어로 남겨두고, 이미지 로드 실패 시 이 뷰만 숨겨 자연스럽게 폴백한다.
            avatarImage.load(profileImageUrl) {
                crossfade(true)
                transformations(CircleCropTransformation())
                listener(onError = { _, _ -> avatarImage.visibility = View.GONE })
            }
        }
        // 아바타 우측 하단 체크 배지 — 이 행에 렌더되는 멤버는 이미 합류를 마친 사람들뿐이라 항상 "합류완료" 상태.
        val checkBadge = FrameLayout(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(dp(16), dp(16), Gravity.BOTTOM or Gravity.END).apply {
                bottomMargin = -dp(1)
                marginEnd = -dp(1)
            }
            setBackgroundResource(R.drawable.bg_avatar_check_badge)
        }
        checkBadge.addView(ImageView(ctx).apply {
            setImageResource(R.drawable.ic_check)
            imageTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            layoutParams = FrameLayout.LayoutParams(dp(9), dp(9), Gravity.CENTER)
        })
        circle.addView(checkBadge)
        val label = TextView(ctx).apply {
            text = if (isMe) "$name(나)" else name
            setTextColor(Color.parseColor("#CCFFFFFF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(dp(4)) }
        }
        tile.addView(circle)
        tile.addView(label)
        return tile
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroyView()
        _binding = null
    }
}
