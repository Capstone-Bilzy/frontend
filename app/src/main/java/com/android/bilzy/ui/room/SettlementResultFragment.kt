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
        else settlement.items.sumOf { it.total }

        binding.tvMemberChip.text = "${members.size}명 참여"
        val date = formatDate(settlement.createdAt)
        binding.tvRoomTitle.text = settlement.title.ifBlank { "정산" } + (if (date.isNotBlank()) " · $date" else "")
        binding.tvTotalAmount.text = bigAmountSpan(total)
        renderAvatars(members)
        renderRounds(settlement)
        renderPayerLine(settlement, total)

        // 저장된 금액이 있으면 사용, 없으면 엔빵
        val hasStored = members.any { it.amount > 0 }
        val shares = RoomViewModel.evenSplit(total, n)
        val myNick = roomViewModel.myNickname.value

        val container = binding.personsContainer
        container.removeAllViews()
        roundParticipants = settlement.receipts.associate { it.round to settlement.roundParticipantCount(it.round) }
        val receipts = settlement.receipts
        members.forEachIndexed { i, m ->
            val amount = if (hasStored) m.amount else shares.getOrElse(i) { 0L }
            val reason = m.reason?.takeIf { hasStored && it.isNotBlank() }
            val roundAmounts = if (hasStored) m.rounds else emptyList()
            container.addView(personCard(m.nickname, amount, m.nickname == myNick, reason, roundAmounts, receipts))
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

    /** 라운드(영수증)별 "N차 · 참여인원" 칩 + 합계(프로토타입 .bz-res-rounds). 라운드 사이 세로 구분선 포함. */
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

    /** "{결제자} 전액 결제 · 받을 금액 {금액}"(프로토타입 .bz-res-payer). 결제자를 찾을 수 없으면 숨김. */
    private fun renderPayerLine(settlement: Settlement, total: Long) {
        val payer = settlement.members.find { it.userId == settlement.createdBy }
        if (payer == null) {
            binding.payerRow.visibility = View.GONE
            return
        }
        binding.payerRow.visibility = View.VISIBLE
        val receive = (total - payer.amount).coerceAtLeast(0)
        binding.tvPayerPrefix.text = "${payer.nickname} 전액 결제 · 받을 금액 "
        binding.tvPayerAmount.text = "${nf.format(receive)}원"
    }

    /** 차수별 참여 인원. 제외 항목 칩에 품목 전체 금액이 아니라 내 몫에서 실제로 빠진 금액(품목÷인원)을 보여주는 데 쓴다. */
    private var roundParticipants: Map<Int, Int> = emptyMap()

    private fun personCard(
        name: String,
        amount: Long,
        isMe: Boolean,
        reason: String?,
        roundAmounts: List<MemberRoundAmount> = emptyList(),
        receipts: List<com.android.bilzy.domain.model.Receipt> = emptyList()
    ): View {
        val ctx = requireContext()
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_person_card)
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
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTypeface(typeface, Typeface.BOLD)
        })
        if (isMe) {
            nameWrap.addView(TextView(ctx).apply {
                text = "나"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTypeface(typeface, Typeface.BOLD)
                setBackgroundResource(R.drawable.bg_me_badge)
                setPadding(dp(8), dp(3), dp(8), dp(3))
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

        // Figma 실제 디자인(정산 결과 요약 화면) 기준: 참여자 카드는 정산방의 모든 라운드를 다 보여준다 —
        // 참여한 라운드는 금액+태그(제외 항목별로 각각 칩, 없으면 "1/N 정산"), 참여 안 한 라운드는
        // "N차 - 0원" + "미참여" 칩으로 표시한다. receipts가 없으면(구버전 데이터) 기존처럼 단일 라운드로 폴백.
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
            listOf(RoundRow("1차", amount, listOf(reason ?: "1/N 정산")))
        }
        rows.forEachIndexed { index, r ->
            // 프로토타입 .bz-person-row: 첫 행 제외 위쪽 구분선
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

    /** 라운드별 "N차 - 금액" 텍스트 + 태그 칩들(제외 항목 개수만큼, 잘리지 않게 줄바꿈)이 있는 블록. */
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
