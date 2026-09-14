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
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentAmountAdjustBinding
import com.android.bilzy.domain.model.Receipt
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

    /** 참여한 라운드(영수증) 목록(round 오름차순). 정상 경로에서는 호스트/게스트 모두 RoundPick에서
     * 최소 1개 라운드를 골라야 다음으로 넘어갈 수 있다(빈 값이면 버튼 비활성화). 그럼에도 선택이 비어 있으면
     * 방어적으로 전체 라운드를 참여한 것으로 간주한다. */
    private var pickedReceipts: List<Receipt> = emptyList()
    private var memberCount = 1

    // 현재 보고 있는 라운드(pickedReceipts[roomViewModel.adjIdx]) 관련 상태
    private var currentItems: List<ReceiptItem> = emptyList()
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

        binding.btnBack.setOnClickListener { handleBack() }
        binding.btnSettle.setOnClickListener { onSettleClicked() }
        // 하드웨어/제스처 뒤로가기도 같은 규칙을 따르게 한다 — 방장은 RoundPick을 안 거치므로
        // 뒤로가기로 라운드를 건너뛰어 이탈하면 그 라운드에서 조용히 0원 처리되는 문제가 있었다(완주 강제).
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { handleBack() }

        observeRoom()
    }

    /** 라운드 도중이면 화면을 나가지 않고 이전 라운드로만 돌아간다(완주 강제) — 첫 라운드에서만 실제로 나간다. */
    private fun handleBack() {
        if (roomViewModel.adjIdx > 0) {
            roomViewModel.adjIdx -= 1
            selectedChips.clear()
            renderRound()
        } else {
            findNavController().navigateUp()
        }
    }

    private fun onSettleClicked() {
        val receipt = pickedReceipts.getOrNull(roomViewModel.adjIdx) ?: return
        val excludedNames = selectedChips.mapNotNull { currentItems.getOrNull(it)?.name }
        val isLastRound = roomViewModel.adjIdx >= pickedReceipts.size - 1

        binding.btnSettle.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            val ok = roomViewModel.submitRoundAdjustment(receipt.round, excludedNames)
            if (!isAdded || _binding == null) return@launch
            binding.btnSettle.isEnabled = true
            if (!ok) {
                Toast.makeText(requireContext(), "금액 조정 저장에 실패했어요", Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (isLastRound) {
                roomViewModel.submitReady() // 실패해도 계산 대기 화면 진입은 그대로 진행
                findNavController().navigate(R.id.action_amountAdjust_to_calculating)
            } else {
                roomViewModel.adjIdx += 1
                selectedChips.clear()
                renderRound()
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
        memberCount = settlement.members.size.coerceAtLeast(1)
        val picked = roomViewModel.pickedRounds.value
        val allReceipts = settlement.receipts.sortedBy { it.round }
        // 정상 경로에서는 RoundPick에서 최소 1개를 골라야 넘어올 수 있지만, 방어적으로 비어 있으면 전체 라운드를 참여한 것으로 본다.
        pickedReceipts = if (picked.isEmpty()) allReceipts else allReceipts.filter { it.round in picked }
        if (pickedReceipts.isEmpty() && settlement.items.isNotEmpty()) {
            // receipts가 아직 안 내려온 과거 데이터 방어: 평면 items로라도 표시
            pickedReceipts = listOf(
                Receipt(
                    id = "", settlementId = settlement.id, round = 1, storeName = null,
                    receiptImageUrl = null, totalAmount = settlement.totalAmount, items = settlement.items
                )
            )
        }
        renderRound()
    }

    private fun renderRound() {
        val receipt = pickedReceipts.getOrNull(roomViewModel.adjIdx) ?: return
        val items = receipt.items
        currentItems = items
        val total = if (receipt.totalAmount > 0) receipt.totalAmount else items.sumOf { it.price * it.quantity }

        renderReceiptTable(items, total)

        baseShare = RoomViewModel.evenSplit(total, memberCount).firstOrNull() ?: 0L
        binding.tvSplitLabel.text = "기본 1/N (${memberCount}명)"

        renderChips(items)
        recompute()

        val isLastRound = roomViewModel.adjIdx >= pickedReceipts.size - 1
        binding.btnSettle.text = if (isLastRound) {
            "정산 시작하기"
        } else {
            val nextRound = pickedReceipts.getOrNull(roomViewModel.adjIdx + 1)?.round ?: (receipt.round + 1)
            "${nextRound}차로 넘어가기"
        }
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
