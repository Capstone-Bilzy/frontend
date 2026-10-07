package com.android.bilzy.ui.history

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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentHistoryDetailBinding
import com.android.bilzy.domain.model.MemberRoundAmount
import com.android.bilzy.domain.model.Receipt
import com.android.bilzy.domain.model.Settlement
import com.android.bilzy.ui.room.payerLine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat

/**
 * 정산 완료된 방의 상세 내역. Figma "정산내역_회사 점심 약속" 실측 기준으로
 * 정산 결과 요약 화면(SettlementResultFragment)과 동일한 요약카드/참여자카드 구조를 쓴다
 * (라운드별 합계+결제자 안내, 참여자별 전체 라운드 표시+제외항목 칩).
 */
@AndroidEntryPoint
class HistoryDetailFragment : Fragment() {

    private var _binding: FragmentHistoryDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryDetailViewModel by viewModels()
    private val nf = NumberFormat.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.navHome.setOnClickListener {
            findNavController().navigate(R.id.action_historyDetail_to_home)
        }
        binding.navScan.setOnClickListener {
            findNavController().navigate(R.id.action_historyDetail_to_scanHub)
        }
        binding.navHistory.setOnClickListener {
            findNavController().navigate(R.id.action_historyDetail_to_historyList)
        }
        binding.navMyPage.setOnClickListener {
            findNavController().navigate(R.id.action_historyDetail_to_myPage)
        }

        observeSettlement()
        observeMyUserId()
        arguments?.getString("settlementId")?.let { viewModel.load(it) }
    }

    private fun observeSettlement() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.settlement.collect { settlement ->
                    settlement ?: return@collect
                    render(settlement)
                }
            }
        }
    }

    /** 내 id를 뒤늦게 알게 되면(프로필 캐시가 없던 경우) 요약 줄을 다시 그린다. */
    private fun observeMyUserId() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.myUserId.collect { viewModel.settlement.value?.let { s -> renderPayerLine(s) } }
            }
        }
    }

    private fun render(s: Settlement) {
        binding.tvTitle.text = s.title.ifBlank { "정산" }
        val date = formatDate(s.createdAt)
        binding.tvSettlementName.text = s.title.ifBlank { "정산" } + (if (date.isNotBlank()) " · $date" else "")
        binding.tvTotalAmount.text = bigAmountSpan(s.totalAmount)
        renderRounds(s)
        renderPayerLine(s)

        val container = binding.personsContainer
        container.removeAllViews()
        roundParticipants = s.receipts.associate { it.round to s.roundParticipantCount(it.round) }
        s.members.forEach { m ->
            container.addView(personCard(m.nickname, m.amount, m.rounds, s.receipts))
        }
    }

    /** 라운드(영수증)별 "N차 · 참여인원" 칩 + 합계. 라운드 사이 세로 구분선 포함. */
    private fun renderRounds(settlement: Settlement) {
        val container = binding.roundsContainer
        container.removeAllViews()
        val receipts = settlement.receipts.sortedBy { it.round }
        binding.roundsDivider.visibility = if (receipts.isEmpty()) View.GONE else View.VISIBLE
        receipts.forEachIndexed { index, receipt ->
            if (index > 0) {
                container.addView(View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(1), dp(48)).apply {
                        marginStart = dp(4); marginEnd = dp(4)
                        gravity = Gravity.CENTER_VERTICAL
                    }
                    setBackgroundColor(Color.parseColor("#33FFFFFF"))
                })
            }
            val participants = settlement.members.count { m -> m.rounds.any { it.round == receipt.round } }
            container.addView(roundColumn(receipt.round, participants, receipt.totalAmount))
        }
    }

    private fun roundColumn(round: Int, participants: Int, amount: Long): View {
        val ctx = requireContext()
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        col.addView(TextView(ctx).apply {
            text = "${round}차 · ${participants}명"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setBackgroundResource(R.drawable.bg_round_chip)
            setPadding(dp(12), dp(4), dp(12), dp(4))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        })
        col.addView(TextView(ctx).apply {
            text = "${nf.format(amount)}원"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        })
        return col
    }

    /** "{결제자} 전액 결제 · 받을 금액/내가 낼 금액 {금액}". 결제자를 찾을 수 없으면 숨김. */
    private fun renderPayerLine(settlement: Settlement) {
        // 결제자가 보면 "받을 금액", 참여자가 보면 "내가 낼 금액"(ui/room/PayerLine.kt)
        val line = payerLine(settlement, settlement.totalAmount, viewModel.myUserId.value)
        if (line == null) {
            binding.payerRow.visibility = View.GONE
            return
        }
        binding.payerRow.visibility = View.VISIBLE
        binding.tvPayerPrefix.text = line.first
        binding.tvPayerAmount.text = "${nf.format(line.second)}원"
    }

    /** 차수별 참여 인원. 제외 항목 칩에 품목 전체 금액이 아니라 내 몫에서 실제로 빠진 금액(품목÷인원)을 보여주는 데 쓴다. */
    private var roundParticipants: Map<Int, Int> = emptyMap()

    private fun personCard(
        name: String,
        amount: Long,
        roundAmounts: List<MemberRoundAmount>,
        receipts: List<Receipt>
    ): View {
        val ctx = requireContext()
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_participant_card)
            setPadding(dp(17), dp(22), dp(16), dp(22))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
        }
        val row = RelativeLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        row.addView(TextView(ctx).apply {
            text = name
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { addRule(RelativeLayout.ALIGN_PARENT_START) }
        })
        row.addView(TextView(ctx).apply {
            text = "${nf.format(amount)}원"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_END)
                addRule(RelativeLayout.CENTER_VERTICAL)
            }
        })
        card.addView(row)

        // 정산방의 모든 라운드를 다 보여준다 — 참여 안 한 라운드는 "N차 - 0원" + "미참여".
        val roundsByNumber = roundAmounts.associateBy { it.round }
        data class RoundRow(val label: String, val amount: Long, val tags: List<String>)
        val rows: List<RoundRow> = if (receipts.isNotEmpty()) {
            receipts.sortedBy { it.round }.map { receipt ->
                val ra = roundsByNumber[receipt.round]
                if (ra != null) {
                    val itemTags = ra.excludedItemNames.mapNotNull { itemName ->
                        receipt.items.find { it.name == itemName }
                            ?.let { "$itemName -${nf.format(it.total / (roundParticipants[receipt.round] ?: 1).coerceAtLeast(1))}원" }
                    }
                    // 안 먹은 메뉴가 있으면 그것만("맥주 -5,000원"), 없으면 먹은 메뉴 이름만("피자", "맥주") 칩으로 보여준다.
                    // 예전엔 AI가 쓴 계산 설명 문장("피자(20000원/2명) 10000원 + …")이 통째로 들어갔다.
                    val tags = itemTags.ifEmpty {
                        receipt.items.map { it.name }.filter(String::isNotBlank).distinct()
                            .ifEmpty { listOf("1/N 정산") }
                    }
                    RoundRow("${receipt.round}차", ra.amount, tags)
                } else {
                    RoundRow("${receipt.round}차", 0L, listOf("미참여"))
                }
            }
        } else if (roundAmounts.isNotEmpty()) {
            roundAmounts.map {
                RoundRow("${it.round}차", it.amount, listOf(it.reason?.takeIf(String::isNotBlank) ?: "1/N 정산"))
            }
        } else {
            listOf(RoundRow("1차", amount, listOf("1/N 정산")))
        }
        rows.forEachIndexed { index, r ->
            if (index > 0) {
                card.addView(View(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(1)
                    ).apply { topMargin = dp(18) }
                    setBackgroundColor(Color.parseColor("#33FFFFFF"))
                })
            }
            card.addView(roundTagRow(r.label, r.amount, r.tags))
        }
        return card
    }

    /** 라운드별 "N차 - 금액" 텍스트 + 태그 칩들(제외 항목 개수만큼, 줄바꿈 가능). */
    private fun roundTagRow(round: String, amount: Long, tags: List<String>): View {
        val ctx = requireContext()
        val block = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(18) }
        }
        block.addView(TextView(ctx).apply {
            text = "$round - ${nf.format(amount)}원"
            setTextColor(Color.parseColor("#AAB2FF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        })
        block.addView(
            com.google.android.material.chip.ChipGroup(ctx).apply {
                isSingleLine = false
                chipSpacingHorizontal = dp(8)
                chipSpacingVertical = dp(6)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(8) }
                tags.forEach { tag ->
                    addView(TextView(ctx).apply {
                        text = tag
                        setTextColor(Color.parseColor("#67F874"))
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                        setBackgroundResource(R.drawable.bg_chip_green_outline)
                        setPadding(dp(12), dp(4), dp(12), dp(4))
                    })
                }
            }
        )
        return block
    }

    /** Figma 실측: 총액 숫자는 34sp, "원"은 22sp로 크기가 다르다. */
    private fun bigAmountSpan(amount: Long): android.text.SpannableString {
        val number = nf.format(amount)
        val full = "${number}원"
        val px22sp = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, 22f, resources.displayMetrics
        ).toInt()
        return android.text.SpannableString(full).apply {
            setSpan(
                android.text.style.AbsoluteSizeSpan(px22sp, false),
                number.length, full.length,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    /** "2026-05-04T..." → "2026.05.04" */
    private fun formatDate(iso: String?): String {
        iso ?: return ""
        return iso.substringBefore('T').replace('-', '.')
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
