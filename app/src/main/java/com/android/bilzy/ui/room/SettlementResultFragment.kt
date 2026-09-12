package com.android.bilzy.ui.room

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentSettlementResultBinding
import com.android.bilzy.domain.model.MemberRoundAmount
import com.android.bilzy.domain.model.Settlement
import com.android.bilzy.domain.model.SettlementMember
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat

@AndroidEntryPoint
class SettlementResultFragment : Fragment() {

    private var _binding: FragmentSettlementResultBinding? = null
    private val binding get() = _binding!!

    private val roomViewModel: RoomViewModel by hiltNavGraphViewModels(R.id.nav_graph)
    private val nf = NumberFormat.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettlementResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnConfirm.setOnClickListener {
            // 정산 완료 처리 후 완료 화면으로 (실패해도 화면은 진행)
            viewLifecycleOwner.lifecycleScope.launch {
                roomViewModel.markDone()
                if (isAdded && _binding != null) {
                    findNavController().navigate(R.id.action_settlementResult_to_complete)
                }
            }
        }

        observeRoom()
    }

    private fun observeRoom() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                roomViewModel.settlement.collect { settlement ->
                    settlement ?: return@collect
                    render(settlement)
                }
            }
        }
    }

    private fun render(settlement: Settlement) {
        val members = settlement.members
        val n = members.size.coerceAtLeast(1)
        val total = if (settlement.totalAmount > 0) settlement.totalAmount
        else settlement.items.sumOf { it.price * it.quantity }

        binding.tvMemberChip.text = "${members.size}명 참여"
        binding.tvRoomTitle.text = settlement.title.ifBlank { "정산" }
        binding.tvTotalAmount.text = "${nf.format(total)}원"
        binding.tvDate.text = formatDate(settlement.createdAt)
        renderAvatars(members)

        // 저장된 금액이 있으면 사용, 없으면 엔빵
        val hasStored = members.any { it.amount > 0 }
        val shares = RoomViewModel.evenSplit(total, n)
        val myNick = roomViewModel.myNickname.value

        val container = binding.personsContainer
        container.removeAllViews()
        members.forEachIndexed { i, m ->
            val amount = if (hasStored) m.amount else shares.getOrElse(i) { 0L }
            val reason = m.reason?.takeIf { hasStored && it.isNotBlank() }
            val roundAmounts = if (hasStored) m.rounds else emptyList()
            container.addView(personCard(m.nickname, amount, m.nickname == myNick, reason, roundAmounts))
        }
    }

    private fun renderAvatars(members: List<SettlementMember>) {
        val row = binding.avatarRow
        row.removeAllViews()
        val visible = members.take(3)
        val overflow = members.size - visible.size
        visible.forEachIndexed { i, member ->
            val circle = FrameLayout(requireContext()).apply {
                setBackgroundResource(R.drawable.bg_role_chip)
                val size = dp(28)
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    if (i > 0) marginStart = dp(-8)
                }
            }
            circle.addView(TextView(requireContext()).apply {
                text = member.nickname.take(1)
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
            })
            val url = member.profileImageUrl
            if (!url.isNullOrBlank()) {
                val avatarImage = ImageView(requireContext()).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }
                circle.addView(avatarImage)
                // 이니셜을 아래 레이어로 남겨두고, 이미지 로드 실패 시 이 뷰만 숨겨 자연스럽게 폴백한다.
                avatarImage.load(url) {
                    crossfade(true)
                    transformations(CircleCropTransformation())
                    listener(onError = { _, _ -> avatarImage.visibility = View.GONE })
                }
            }
            row.addView(circle)
        }
        if (overflow > 0) {
            row.addView(TextView(requireContext()).apply {
                text = "+$overflow"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                gravity = Gravity.CENTER
                setBackgroundResource(R.drawable.bg_avatar_circle)
                val size = dp(28)
                layoutParams = LinearLayout.LayoutParams(size, size).apply { marginStart = dp(-8) }
            })
        }
    }

    private fun personCard(
        name: String,
        amount: Long,
        isMe: Boolean,
        reason: String?,
        roundAmounts: List<MemberRoundAmount> = emptyList()
    ): View {
        val ctx = requireContext()
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_person_card)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
        }
        val row = RelativeLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        // 이름 (+ 나 배지)
        val nameWrap = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { addRule(RelativeLayout.ALIGN_PARENT_START) }
        }
        nameWrap.addView(TextView(ctx).apply {
            text = name
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
        })
        if (isMe) {
            nameWrap.addView(TextView(ctx).apply {
                text = "나"
                setTextColor(Color.parseColor("#BEBEF7"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setBackgroundResource(R.drawable.bg_chip_purple)
                setPadding(dp(6), dp(2), dp(6), dp(2))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { marginStart = dp(6) }
            })
        }
        row.addView(nameWrap)
        // 금액
        row.addView(TextView(ctx).apply {
            text = "${nf.format(amount)}원"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_END)
                addRule(RelativeLayout.CENTER_VERTICAL)
            }
        })
        card.addView(row)
        val rounds: List<Triple<String, Long, String>> = if (roundAmounts.isNotEmpty()) {
            roundAmounts.map { Triple("${it.round}차", it.amount, it.reason?.takeIf(String::isNotBlank) ?: "1/N 정산") }
        } else {
            listOf(Triple("1차", amount, reason ?: "1/N 정산"))
        }
        rounds.forEach { (round, roundAmount, tag) -> card.addView(roundTagRow(round, roundAmount, tag)) }
        return card
    }

    /** 라운드별 "N차 - 금액" 텍스트 + 사유 태그 칩 한 행. */
    private fun roundTagRow(round: String, amount: Long, tag: String): View {
        val ctx = requireContext()
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        }
        row.addView(TextView(ctx).apply {
            text = "$round - ${nf.format(amount)}원"
            setTextColor(Color.parseColor("#BEBEF7"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            )
        })
        row.addView(TextView(ctx).apply {
            text = tag
            setTextColor(Color.parseColor("#7CE7A0"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setBackgroundResource(R.drawable.bg_chip_green_outline)
            setPadding(dp(10), dp(4), dp(10), dp(4))
        })
        return row
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    /** ISO 8601(2026-06-08T...) → "2026.06.08". 없으면 빈 문자열. */
    private fun formatDate(iso: String?): String {
        iso ?: return ""
        return iso.substringBefore('T').replace('-', '.')
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
