package com.android.bilzy.ui.room

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentSettlementResultBinding
import com.android.bilzy.domain.model.Settlement
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

        // 저장된 금액이 있으면 사용, 없으면 엔빵
        val hasStored = members.any { it.amount > 0 }
        val shares = RoomViewModel.evenSplit(total, n)
        val myNick = roomViewModel.myNickname.value

        val container = binding.personsContainer
        container.removeAllViews()
        members.forEachIndexed { i, m ->
            val amount = if (hasStored) m.amount else shares.getOrElse(i) { 0L }
            val reason = m.reason?.takeIf { hasStored && it.isNotBlank() }
            container.addView(personCard(m.nickname, amount, m.nickname == myNick, reason))
        }
    }

    private fun personCard(name: String, amount: Long, isMe: Boolean, reason: String?): View {
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
        // AI 계산 사유(있을 때만) — 초록 아웃라인 칩
        if (!reason.isNullOrBlank()) {
            card.addView(TextView(ctx).apply {
                text = reason
                setTextColor(Color.parseColor("#7CE7A0"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setBackgroundResource(R.drawable.bg_chip_green_outline)
                setPadding(dp(10), dp(4), dp(10), dp(4))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(8) }
            })
        }
        return card
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
