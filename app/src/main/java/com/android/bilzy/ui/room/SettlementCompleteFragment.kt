package com.android.bilzy.ui.room

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentSettlementCompleteBinding
import com.android.bilzy.domain.model.Settlement
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat

@AndroidEntryPoint
class SettlementCompleteFragment : Fragment() {

    private var _binding: FragmentSettlementCompleteBinding? = null
    private val binding get() = _binding!!

    private val roomViewModel: RoomViewModel by hiltNavGraphViewModels(R.id.nav_graph)
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
            findNavController().navigate(R.id.action_settlementComplete_to_home)
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

        binding.tvTotalAmount.text = "${nf.format(total)}원"
        binding.tvSubtitle.text = "${settlement.title.ifBlank { "정산" }} · ${members.size}명"

        // 내 금액: 저장된 금액 있으면 사용, 없으면 엔빵 내 몫
        val myNick = roomViewModel.myNickname.value
        val shares = RoomViewModel.evenSplit(total, n)
        val myIndex = members.indexOfFirst { it.nickname == myNick }.takeIf { it >= 0 } ?: 0
        val stored = members.getOrNull(myIndex)?.amount ?: 0L
        val myAmount = if (stored > 0) stored else shares.getOrElse(myIndex) { 0L }
        binding.tvMyAmount.text = "${nf.format(myAmount)}원"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
