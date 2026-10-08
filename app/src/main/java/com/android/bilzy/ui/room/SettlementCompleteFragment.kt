package com.android.bilzy.ui.room

import android.graphics.Color
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentSettlementCompleteBinding
import com.android.bilzy.domain.model.BankAccount
import com.android.bilzy.domain.model.MemberRoundAmount
import com.android.bilzy.domain.model.Settlement
import com.android.bilzy.domain.model.SettlementStatus
import com.android.bilzy.ui.scan.ScanFlowViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat

@AndroidEntryPoint
class SettlementCompleteFragment : Fragment() {

    private var _binding: FragmentSettlementCompleteBinding? = null
    private val binding get() = _binding!!

    private val roomViewModel: RoomViewModel by hiltNavGraphViewModels(R.id.nav_graph)
    private val scanFlowViewModel: ScanFlowViewModel by hiltNavGraphViewModels(R.id.nav_graph)

    private var myAccount: BankAccount? = null
    /** 화면에 보이는 계좌 문구(복사 대상). 계좌가 없으면 null. */
    private var accountText: String? = null
    private var iAmOwner = false
    private val nf = NumberFormat.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettlementCompleteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnHome.setOnClickListener {
            // 완료된 정산방/스캔 상태가 다음 정산에 재사용되지 않도록 nav_graph 스코프 ViewModel을 초기화한다.
            roomViewModel.reset()
            scanFlowViewModel.reset()
            findNavController().navigate(R.id.action_settlementComplete_to_home)
        }

        binding.accountRow.setOnClickListener { copyAccount() }

        roomViewModel.loadMyAccount()
        observeRoom()
        observeAccount()
        viewLifecycleOwner.lifecycleScope.launch {
            iAmOwner = roomViewModel.isOwner()
            if (_binding != null) {
                roomViewModel.settlement.value?.let { render(it) }
                renderAccount()
            }
        }
    }

    private fun observeRoom() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                roomViewModel.settlement.collect { settlement ->
                    settlement ?: return@collect
                    render(settlement)
                    renderAccount()
                }
            }
        }
    }

    private fun observeAccount() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                roomViewModel.myAccount.collect { account ->
                    myAccount = account
                    renderAccount()
                }
            }
        }
    }

    /**
     * "송금 계좌"는 돈을 받을 사람, 즉 결제자(방장)의 계좌다. 예전엔 누구에게나 본인 계좌를 보여줘서
     * 참여자는 보낼 계좌를 볼 수 없었다(본인 계좌가 없으면 "계좌 정보 없음").
     * 서버가 내려준 결제자 계좌를 쓰고, 그 값이 없을 때 방장 본인에게만 자기 계좌를 대신 보여준다.
     */
    private fun renderAccount() {
        val account = roomViewModel.settlement.value?.payerAccount ?: myAccount.takeIf { iAmOwner }
        val hasAccount = account != null && !account.isEmpty
        accountText = if (hasAccount) {
            "${account!!.bankName} ${account.accountNumber} ${account.accountHolder}".trim()
        } else {
            null
        }
        binding.tvAccount.text = accountText ?: "계좌 정보 없음"
        // 복사할 계좌가 없으면 복사 아이콘과 안내 문구를 감춘다.
        binding.icCopyAccount.visibility = if (hasAccount) View.VISIBLE else View.GONE
        binding.tvCopyHint.visibility = if (hasAccount) View.VISIBLE else View.GONE
        fitCardToViewport()
    }

    /**
     * 화면이 짧거나 하단 내비게이션 바(3버튼)가 있는 기기에서는 카드가 다 안 들어가 송금 계좌 줄이
     * 아래 버튼에 바짝 붙거나 가려졌다. 카드가 보이는 영역보다 크면 위쪽 그림을 그만큼 줄여 한 화면에 맞춘다
     * (그래도 안 들어가면 스크롤).
     */
    private fun fitCardToViewport() {
        val b = _binding ?: return
        b.scrollContent.post {
            val bind = _binding ?: return@post
            val icon = bind.successIcon
            val full = dp(ICON_FULL_DP)
            val min = dp(ICON_MIN_DP)
            val content = bind.scrollContent.getChildAt(0) ?: return@post
            val lp = content.layoutParams as ViewGroup.MarginLayoutParams
            val available = bind.scrollContent.height - bind.scrollContent.paddingTop - bind.scrollContent.paddingBottom
            // 그림을 원래 크기로 뒀을 때 필요한 높이를 기준으로 계산한다(이미 줄여 둔 상태에서 다시 불려도 같은 결과).
            val needed = content.height + lp.topMargin + lp.bottomMargin + (full - icon.layoutParams.height)
            val target = (full - (needed - available).coerceAtLeast(0)).coerceIn(min, full)
            if (icon.layoutParams.height != target) {
                icon.layoutParams = icon.layoutParams.apply { height = target }
            }
        }
    }

    /** 송금 계좌 줄을 누르면 "은행 계좌번호 예금주"를 클립보드에 복사한다(프로토타입 .bz-account). */
    private fun copyAccount() {
        val text = accountText ?: return
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("송금 계좌", text))
        Toast.makeText(requireContext(), "송금 계좌를 복사했어요", Toast.LENGTH_SHORT).show()
    }

    private fun render(settlement: Settlement) {
        val members = settlement.members
        val n = members.size.coerceAtLeast(1)
        val total = if (settlement.totalAmount > 0) settlement.totalAmount
        else settlement.items.sumOf { it.total }

        binding.tvTotalAmount.text = "${nf.format(total)}원"
        binding.tvSubtitle.text = "${settlement.title.ifBlank { "정산" }} · ${members.size}명"

        // 계산 완료 여부는 금액 값(0원일 수도 있는 정상 결과)이 아니라 정산방 상태로 판단해야 한다 —
        // 특정 라운드에 안 왔거나 그 라운드 항목을 전부 "안 먹음" 처리하면 정당하게 0원이 나올 수 있는데,
        // 예전 로직(stored > 0)은 이걸 "아직 계산 안 됨"으로 오인해 엔빵 금액을 잘못 보여줬다.
        val calculated = settlement.status == SettlementStatus.CALCULATED || settlement.status == SettlementStatus.DONE

        // 내 금액: 계산 완료면 저장된 금액(0원이어도 신뢰), 아니면 엔빵 내 몫
        val myNick = roomViewModel.myNickname.value
        val shares = RoomViewModel.evenSplit(total, n)
        val myIndex = members.indexOfFirst { it.nickname == myNick }.takeIf { it >= 0 } ?: 0
        val myMember = members.getOrNull(myIndex)
        val myAmount = if (calculated) (myMember?.amount ?: 0L) else shares.getOrElse(myIndex) { 0L }
        // 방장은 이미 전액을 결제한 사람이라 낼 돈이 아니라 받을 돈(총액 − 내 몫)을 보여준다. 참여자는 그대로 낼 금액.
        if (iAmOwner) {
            binding.tvMyAmountLabel.text = "받을 금액"
            binding.tvMyAmount.text = "${nf.format((total - myAmount).coerceAtLeast(0L))}원"
        } else {
            binding.tvMyAmountLabel.text = "내가 낼 금액"
            binding.tvMyAmount.text = "${nf.format(myAmount)}원"
        }

        val myRoundAmounts = if (calculated) myMember?.rounds.orEmpty() else emptyList()
        val rounds: List<Pair<String, String>> = if (myRoundAmounts.isNotEmpty()) {
            myRoundAmounts.map { "${it.round}차" to roundTag(settlement, it) }
        } else {
            val myReason = myMember?.reason?.takeIf { it.isNotBlank() }
            listOf("1차" to (myReason ?: "1/N 정산"))
        }
        binding.roundsContainer.removeAllViews()
        rounds.forEach { (round, tag) -> binding.roundsContainer.addView(roundBadge(round, tag)) }
        fitCardToViewport()
    }

    /**
     * 차수 옆 초록 칩 문구 — 결과·정산내역 화면과 같은 규칙: 안 먹은 메뉴가 있으면 그것만("맥주 -5,000원"),
     * 없으면 먹은 메뉴 이름만("피자, 맥주"). AI가 쓴 계산 설명 문장은 쓰지 않는다.
     */
    private fun roundTag(settlement: Settlement, ra: MemberRoundAmount): String {
        val receipt = settlement.receipts.firstOrNull { it.round == ra.round } ?: return "1/N 정산"
        val n = settlement.roundParticipantCount(ra.round).coerceAtLeast(1)
        val excluded = ra.excludedItemNames.mapNotNull { name ->
            receipt.items.find { it.name == name }?.let { "$name -${nf.format(it.total / n)}원" }
        }
        val tags = excluded.ifEmpty { receipt.items.map { it.name }.filter(String::isNotBlank).distinct() }
        return tags.joinToString(", ").ifEmpty { "1/N 정산" }
    }

    private fun roundBadge(round: String, tag: String): View {
        val ctx = requireContext()
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
        }
        row.addView(TextView(ctx).apply {
            text = round
            setTextColor(Color.parseColor("#BEBEF7"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, dp(4), dp(8), dp(4))
        })
        row.addView(TextView(ctx).apply {
            text = tag
            setTextColor(Color.parseColor("#67F874"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setBackgroundResource(R.drawable.bg_chip_green_outline)
            setPadding(dp(10), dp(4), dp(10), dp(4))
        })
        return row
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        /** 완료 그림의 원래 높이(레이아웃 값)와 줄일 수 있는 최소 높이(dp). */
        const val ICON_FULL_DP = 173
        const val ICON_MIN_DP = 96
    }
}
