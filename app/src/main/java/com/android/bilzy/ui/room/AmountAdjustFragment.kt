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
import com.android.bilzy.databinding.FragmentAmountAdjustBinding
import com.android.bilzy.domain.model.ReceiptItem
import com.android.bilzy.domain.model.Settlement
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat

@AndroidEntryPoint
class AmountAdjustFragment : Fragment() {

    private var _binding: FragmentAmountAdjustBinding? = null
    private val binding get() = _binding!!

    private val roomViewModel: RoomViewModel by hiltNavGraphViewModels(R.id.nav_graph)

    private val selectedChips = mutableSetOf<Int>()
    private val nf = NumberFormat.getInstance()

    // 차감 계산용 상태
    private var currentItems: List<ReceiptItem> = emptyList()
    private var memberCount = 1
    private var baseShare = 0L

    /** 주류는 차감 대상에서 제외(프로토타입 "주류제외" 규칙). */
    private val alcoholKeywords = listOf("소주", "맥주", "막걸리", "와인", "위스키", "하이볼", "양주", "보드카", "사케", "청하", "고량주", "주류")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAmountAdjustBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnSettle.setOnClickListener {
            // 칩에서 고른 '안 먹은 항목'을 특이사항 문자열로 만들어 AI 계산에 전달
            roomViewModel.aiNote = buildAiNote()
            findNavController().navigate(R.id.action_amountAdjust_to_calculating)
        }

        observeRoom()
        observePickedRounds()
    }

    /**
     * 다차 정산(n차) UI 뼈대: 선택한 차수 수에 따라 버튼 문구만 바꾼다.
     * TODO(다차 정산): 실제 차수 반복 네비게이션은 다음 단계 — 지금은 항상 1차뿐이라 분기만 존재.
     */
    private fun observePickedRounds() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                roomViewModel.pickedRounds.collect { picked ->
                    binding.btnSettle.text = if (picked.size <= 1) "정산 시작하기" else "2차로 넘어가기"
                }
            }
        }
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
        val items = settlement.items
        currentItems = items
        memberCount = settlement.members.size.coerceAtLeast(1)
        val total = if (settlement.totalAmount > 0) settlement.totalAmount
        else items.sumOf { it.price * it.quantity }

        renderReceiptTable(items, total)

        // 1/N: 내 몫(앞사람부터 1원 더하는 엔빵의 첫 번째 몫과 동일 수준)
        baseShare = RoomViewModel.evenSplit(total, memberCount).firstOrNull() ?: 0L
        binding.tvSplitLabel.text = "기본 1/N (${memberCount}명)"

        renderChips(items)
        recompute()
    }

    /**
     * 선택한(안 먹은) 항목의 1인분 가격을 내 몫에서 차감한다(주류 제외).
     * 차감 = Σ (항목 합계 / 인원수). 예) 냉면 45,000/6 + 공기밥 6,000/6 = 8,500원
     */
    private fun recompute() {
        val deduction = selectedChips.sumOf { idx ->
            val item = currentItems.getOrNull(idx) ?: return@sumOf 0L
            if (isAlcohol(item.name)) 0L
            else (item.price * item.quantity) / memberCount
        }
        val myAmount = (baseShare - deduction).coerceAtLeast(0L)
        binding.tvAmount.text = "${nf.format(myAmount)} 원"
        binding.tvDeduction.text = "-${nf.format(deduction)}원"
    }

    private fun isAlcohol(name: String): Boolean =
        alcoholKeywords.any { name.contains(it) }

    /** 선택한(안 먹은) 항목을 AI 계산용 특이사항 문장으로 변환. */
    private fun buildAiNote(): String {
        val me = roomViewModel.myNickname.value?.takeIf { it.isNotBlank() } ?: "나"
        val excluded = selectedChips.mapNotNull { currentItems.getOrNull(it)?.name }
        if (excluded.isEmpty()) {
            return "특이사항 없음. 전원이 균등하게(주류 포함) 나눠 주세요."
        }
        return "'$me'님은 다음 항목을 먹지 않았으니 '$me'님 몫에서 제외해 주세요: " +
            "${excluded.joinToString(", ")}. 주류는 전원이 공평하게 나누고, 나머지 항목은 균등 분배해 주세요."
    }

    /** 영수증 항목 테이블을 실제 OCR 항목으로 다시 그린다. */
    private fun renderReceiptTable(items: List<ReceiptItem>, total: Long) {
        val table = binding.receiptTable
        table.removeAllViews()
        items.forEach { item ->
            table.addView(itemRow(item.name, item.quantity.toString(), "${nf.format(item.price * item.quantity)}원"))
        }
        table.addView(divider())
        table.addView(totalRow(total))
    }

    private fun itemRow(name: String, qty: String, price: String): View {
        val ctx = requireContext()
        val rl = RelativeLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(6) }
        }
        rl.addView(cell(name, Color.WHITE, false).apply {
            (layoutParams as RelativeLayout.LayoutParams).addRule(RelativeLayout.ALIGN_PARENT_START)
        })
        rl.addView(cell(qty, Color.parseColor("#BEBEF7"), false).apply {
            (layoutParams as RelativeLayout.LayoutParams).addRule(RelativeLayout.CENTER_HORIZONTAL)
        })
        rl.addView(cell(price, Color.WHITE, false).apply {
            (layoutParams as RelativeLayout.LayoutParams).addRule(RelativeLayout.ALIGN_PARENT_END)
        })
        return rl
    }

    private fun totalRow(total: Long): View {
        val ctx = requireContext()
        val rl = RelativeLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        rl.addView(cell("합계", Color.WHITE, true).apply {
            (layoutParams as RelativeLayout.LayoutParams).addRule(RelativeLayout.ALIGN_PARENT_START)
        })
        rl.addView(cell("${nf.format(total)}원", Color.parseColor("#A5A6F6"), true).apply {
            (layoutParams as RelativeLayout.LayoutParams).addRule(RelativeLayout.ALIGN_PARENT_END)
        })
        return rl
    }

    private fun cell(text: String, color: Int, bold: Boolean): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            setTextColor(color)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, if (bold) 14f else 13f)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            layoutParams = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun divider(): View {
        return View(requireContext()).apply {
            setBackgroundColor(Color.parseColor("#33FFFFFF"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)
            ).apply { topMargin = dp(2); bottomMargin = dp(8) }
        }
    }

    /** 차감 선택 칩을 실제 항목명으로 채운다(항목 수 제한 없음, 가로 스크롤). */
    private fun renderChips(items: List<ReceiptItem>) {
        val row = binding.chipRow
        row.removeAllViews()
        items.forEachIndexed { index, item ->
            val chip = TextView(requireContext()).apply {
                text = item.name
                setTextColor(android.graphics.Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                gravity = Gravity.CENTER
                setPadding(dp(16), 0, dp(16), 0)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(38)
                ).apply { marginEnd = dp(8) }
                isClickable = true
                isFocusable = true
            }
            styleChip(chip, selectedChips.contains(index))
            chip.setOnClickListener {
                if (selectedChips.contains(index)) selectedChips.remove(index)
                else selectedChips.add(index)
                styleChip(chip, selectedChips.contains(index))
                recompute()
            }
            row.addView(chip)
        }
    }

    /** 선택된 칩은 보라색 채움, 미선택은 외곽선. */
    private fun styleChip(chip: android.widget.TextView, selected: Boolean) {
        chip.setBackgroundResource(
            if (selected) R.drawable.bg_role_chip else R.drawable.bg_chip_outline
        )
        chip.setTypeface(chip.typeface, if (selected) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
