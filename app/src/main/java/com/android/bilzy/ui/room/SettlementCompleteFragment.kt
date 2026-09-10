package com.android.bilzy.ui.room

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentSettlementCompleteBinding
import com.android.bilzy.domain.model.Settlement
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

        roomViewModel.loadMyAccount()
        observeRoom()
        observeAccount()
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

    private fun observeAccount() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                roomViewModel.myAccount.collect { account ->
                    if (account != null && !account.isEmpty) {
                        binding.tvAccount.text =
                            "${account.bankName} ${account.accountNumber} ${account.accountHolder}".trim()
                    } else {
                        binding.tvAccount.text = "계좌 정보 없음"
                    }
                }
            }
        }
    }

    private fun render(settlement: Settlement) {
        val members = settlement.members
        val n = members.size.coerceAtLeast(1)
        val total = if (settlement.totalAmount > 0) settlement.totalAmount
        else settlement.items.sumOf { it.price * it.quantity }

        binding.tvTotalAmount.text = "${nf.format(total)}원"
        binding.tvSubtitle.text = "${settlement.title.ifBlank { "정산" }} · ${members.size}명"

        // 내 금액: 저장된 금액 있으면 사용, 없으면 엔빵 내 몫
        val myNick = roomViewModel.myNickname.value
        val shares = RoomViewModel.evenSplit(total, n)
        val myIndex = members.indexOfFirst { it.nickname == myNick }.takeIf { it >= 0 } ?: 0
        val stored = members.getOrNull(myIndex)?.amount ?: 0L
        val myAmount = if (stored > 0) stored else shares.getOrElse(myIndex) { 0L }
        binding.tvMyAmount.text = "${nf.format(myAmount)}원"

        // 다차 정산(n차) UI 뼈대: "1개짜리 리스트를 순회"하는 구조로 만들어 향후 실제 다차
        // 데이터로 교체할 수 있게 한다. TODO(다차 정산): 지금은 항상 1차 데이터뿐이다.
        val myReason = members.getOrNull(myIndex)?.reason?.takeIf { it.isNotBlank() }
        val rounds = listOf("1차" to (myReason ?: "N분의 1 적용"))
        binding.roundsContainer.removeAllViews()
        rounds.forEach { (round, tag) -> binding.roundsContainer.addView(roundBadge(round, tag)) }
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
            setTextColor(Color.parseColor("#7CE7A0"))
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
}
