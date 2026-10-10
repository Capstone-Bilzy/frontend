package com.android.bilzy.ui.room

import com.android.bilzy.util.setFontWeight
import android.text.TextUtils
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
import com.android.bilzy.ui.common.loading
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentAmountAdjustBinding
import com.android.bilzy.domain.model.Receipt
import com.android.bilzy.domain.model.ReceiptItem
import com.android.bilzy.domain.model.RoundSplit
import com.android.bilzy.domain.model.Settlement
import com.android.bilzy.domain.model.SettlementMember
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private var currentSettlement: Settlement? = null

    /** 지금 보고 있는 차수를 나누는 인원 — 그 차수를 고른 멤버 수(아직 아무도 안 골랐으면 방 전체 인원). */
    private var roundCount = 1

    // 현재 보고 있는 라운드(pickedReceipts[roomViewModel.adjIdx]) 관련 상태
    private var currentItems: List<ReceiptItem> = emptyList()
    private var baseShare = 0L

    /** 내 사용자 id(멤버 목록에서 나를 찾는 데 쓴다). 못 구하면 닉네임으로 찾는다. */
    private var myUserId: String? = null

    /** 칩 선택을 서버 저장값으로 채워 둔 차수 — 차수가 바뀔 때 한 번만 채우고, 그 뒤로는 화면의 선택이 기준이다. */
    private var chipsLoadedForRound: Int? = null

    /** 칩을 누를 때마다 바로 저장하는 작업(연달아 누르면 마지막 것만 보낸다). */
    private var liveSaveJob: Job? = null

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

        viewLifecycleOwner.lifecycleScope.launch {
            myUserId = roomViewModel.myUserId()
            if (_binding != null) recompute()
        }
        observeRoom()
        keepRoomFresh()
    }

    /** 다른 멤버가 뒤늦게 차수를 고르면 나누는 인원이 달라지므로, 화면이 보이는 동안 가끔 다시 불러온다. */
    private fun keepRoomFresh() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    roomViewModel.refresh()
                    delay(REFRESH_INTERVAL_MS)
                }
            }
        }
    }

    /** 라운드 도중이면 화면을 나가지 않고 이전 라운드로만 돌아간다(완주 강제) — 첫 라운드에서만 실제로 나간다. */
    private fun handleBack() {
        if (roomViewModel.adjIdx > 0) {
            roomViewModel.adjIdx -= 1
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
        loading.show()
        liveSaveJob?.cancel() // 아래에서 같은 내용을 확실히 저장한다
        viewLifecycleOwner.lifecycleScope.launch {
            val ok = roomViewModel.submitRoundAdjustment(receipt.round, excludedNames)
            loading.hide()
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
        currentSettlement = settlement
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
        val total = if (receipt.totalAmount > 0) receipt.totalAmount else items.sumOf { it.total }

        renderReceiptTable(items, total)

        // 방 전체 인원이 아니라 이 차수에 참여한 사람끼리 나눈다(서버 계산과 동일).
        // 예전엔 혼자 간 2차 9,000원이 "2명 · 4,500원"으로 보였다.
        roundCount = currentSettlement?.roundParticipantCount(receipt.round)?.takeIf { it > 0 } ?: memberCount
        baseShare = RoomViewModel.evenSplit(total, roundCount).firstOrNull() ?: 0L
        binding.tvSplitLabel.text = "기본 1/N (${roundCount}명)"

        // 이 차수에 처음 들어왔으면(또는 앞 차수로 되돌아왔으면) 서버에 저장돼 있는 내 선택으로 칩을 채운다.
        if (chipsLoadedForRound != receipt.round) {
            chipsLoadedForRound = receipt.round
            selectedChips.clear()
            val saved = myMember()?.rounds?.firstOrNull { it.round == receipt.round }?.excludedItemNames.orEmpty()
            items.forEachIndexed { index, item -> if (item.name in saved) selectedChips.add(index) }
        }

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
     * 이 차수에서 내가 낼 금액을 서버 규칙 계산과 같은 방법으로 미리 보여준다(RoundSplit).
     * 내 선택은 화면의 칩을, 다른 사람의 선택은 3초마다 다시 불러오는 서버 값을 쓴다 — 누가 메뉴를 제외하면
     * 몇 초 안에 내 금액에도 반영된다. 오른쪽 알약은 기본 1/N과의 차이(다른 사람이 제외해 내 몫이 늘면 +).
     */
    private fun recompute() {
        val receipt = pickedReceipts.getOrNull(roomViewModel.adjIdx) ?: return
        val me = myMember()
        val myExcluded = selectedChips.mapNotNull { currentItems.getOrNull(it)?.name }.toSet()
        val participants = currentSettlement?.members.orEmpty()
            .filter { m -> m.id == me?.id || m.rounds.any { it.round == receipt.round } }
            .map { m ->
                val excluded = if (m.id == me?.id) myExcluded
                else m.rounds.firstOrNull { it.round == receipt.round }?.excludedItemNames.orEmpty().toSet()
                RoundSplit.Participant(m.id, excluded)
            }
        val split = RoundSplit.split(currentItems, participants)
        // 기준 금액은 아무도 메뉴를 빼지 않았을 때의 내 몫(1원 나머지를 누가 받는지까지 같은 규칙으로 계산).
        val base = me?.let { RoundSplit.split(currentItems, participants.map { p -> p.copy(excludedItemNames = emptySet()) })[it.id] }
            ?: baseShare
        val myAmount = me?.let { split[it.id] }
            // 멤버 목록에서 나를 아직 못 찾았으면(첫 응답 전) 예전처럼 내 선택만 빼서 보여준다.
            ?: (baseShare - myExcluded.sumOf { name ->
                (currentItems.firstOrNull { it.name == name }?.total ?: 0L) / roundCount
            }).coerceAtLeast(0L)
        val diff = myAmount - base
        binding.tvAmount.text = nf.format(myAmount)
        binding.tvDeduction.text = if (diff > 0) "+${nf.format(diff)}원" else "-${nf.format(-diff)}원"
    }

    /** 멤버 목록에서 나. id를 아직 모르면 표시 이름으로 찾는다. */
    private fun myMember(): SettlementMember? {
        val members = currentSettlement?.members.orEmpty()
        return members.firstOrNull { myUserId != null && it.userId == myUserId }
            ?: members.firstOrNull { it.nickname == roomViewModel.myNickname.value }
    }

    /** 칩 선택을 바로 서버에 저장한다 — 다른 사람 화면의 미리보기에 반영되게. 실패해도 조용히 넘어간다(버튼을 누를 때 다시 저장). */
    private fun saveSelectionSoon() {
        val receipt = pickedReceipts.getOrNull(roomViewModel.adjIdx) ?: return
        val names = selectedChips.mapNotNull { currentItems.getOrNull(it)?.name }
        liveSaveJob?.cancel()
        liveSaveJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(LIVE_SAVE_DELAY_MS)
            roomViewModel.submitRoundAdjustment(receipt.round, names)
        }
    }

    /** 영수증 항목 테이블을 실제 OCR 항목으로 다시 그린다. */
    private fun renderReceiptTable(items: List<ReceiptItem>, total: Long) {
        val table = binding.receiptTable
        table.removeAllViews()
        items.forEach { item ->
            table.addView(itemRow(item.name, item.quantity.toString(), "${nf.format(item.total)}원"))
        }
        table.addView(divider())
        table.addView(totalRow(total))
    }

    // 피그마 "금액조정" 영수증 내역 표: 품목 줄 16sp(이름·수량 400, 금액 500), 줄 높이 24 + 간격 11,
    // 합계 줄 18sp 600(금액 #BFAFFF). 줄은 카드 안쪽 여백에서 16dp 더 들어가 있고 구분선만 여백 끝까지 간다.
    private fun itemRow(name: String, qty: String, price: String): View {
        val row = tableRow(bottomMargin = 11)
        row.addView(cell(name, 16f, 400, Color.WHITE).apply {
            layoutParams = LinearLayout.LayoutParams(0, dp(24), 1f)
            ellipsize = TextUtils.TruncateAt.END
        })
        row.addView(cell(qty, 16f, 400, Color.WHITE).apply {
            layoutParams = LinearLayout.LayoutParams(dp(30), dp(24))
            gravity = Gravity.CENTER
        })
        row.addView(cell(price, 16f, 500, Color.WHITE).apply {
            layoutParams = LinearLayout.LayoutParams(dp(95), dp(24))
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        })
        return row
    }

    private fun totalRow(total: Long): View {
        val row = tableRow(bottomMargin = 0)
        row.addView(cell("합계", 18f, 600, Color.WHITE).apply {
            layoutParams = LinearLayout.LayoutParams(0, dp(24), 1f)
        })
        row.addView(cell("${nf.format(total)}원", 18f, 600, Color.parseColor("#BFAFFF")).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(24))
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        })
        return row
    }

    private fun tableRow(bottomMargin: Int): LinearLayout =
        LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), 0, dp(16), 0)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { this.bottomMargin = dp(bottomMargin) }
        }

    private fun cell(text: String, sizeSp: Float, weight: Int, color: Int): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            setTextColor(color)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
            setFontWeight(weight)
            maxLines = 1
            includeFontPadding = false
            gravity = Gravity.CENTER_VERTICAL
        }
    }

    private fun divider(): View {
        return View(requireContext()).apply {
            setBackgroundColor(Color.parseColor("#504C91"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)
            ).apply { bottomMargin = dp(10) }
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
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                includeFontPadding = false
                maxLines = 1
                gravity = Gravity.CENTER
                setPadding(dp(20), 0, dp(20), 0)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(40)
                )
                isClickable = true
                isFocusable = true
            }
            styleChip(chip, selectedChips.contains(index))
            chip.setOnClickListener {
                if (selectedChips.contains(index)) selectedChips.remove(index)
                else selectedChips.add(index)
                styleChip(chip, selectedChips.contains(index))
                recompute()
                saveSelectionSoon()
            }
            row.addView(chip)
        }
    }

    /** 선택된 칩은 보라색 채움 + 흰 굵은 글자, 미선택은 연보라 외곽선(피그마 값). */
    private fun styleChip(chip: android.widget.TextView, selected: Boolean) {
        chip.setBackgroundResource(
            if (selected) R.drawable.bg_item_chip_selected else R.drawable.bg_chip_outline
        )
        chip.setTextColor(if (selected) Color.WHITE else Color.parseColor("#D4C7FF"))
        chip.setFontWeight(if (selected) 600 else 400)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val REFRESH_INTERVAL_MS = 3000L
        const val LIVE_SAVE_DELAY_MS = 400L
    }
}
