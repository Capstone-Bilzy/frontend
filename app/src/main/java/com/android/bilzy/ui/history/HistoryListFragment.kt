package com.android.bilzy.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentHistoryListBinding
import com.android.bilzy.domain.model.SettlementHistory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat

@AndroidEntryPoint
class HistoryListFragment : Fragment() {

    private var _binding: FragmentHistoryListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPastHistory.layoutManager = LinearLayoutManager(requireContext())

        binding.navHome.setOnClickListener {
            findNavController().navigate(R.id.action_historyList_to_home)
        }
        binding.navScan.setOnClickListener {
            findNavController().navigate(R.id.action_historyList_to_scanHub)
        }
        binding.navMyPage.setOnClickListener {
            findNavController().navigate(R.id.action_historyList_to_myPage)
        }

        observeHistory()
        viewModel.load()
    }

    private fun observeHistory() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.history.collect { list ->
                    list ?: return@collect
                    render(list)
                }
            }
        }
    }

    private fun render(list: List<SettlementHistory>) {
        val nowYm = currentYearMonth()
        val current = list.filter { yearMonth(it.createdAt) == nowYm }
        val past = list.filter { yearMonth(it.createdAt) != nowYm }

        // 이번 달 요약 카드
        val monthTotal = current.sumOf { it.totalAmount }
        binding.tvTotalAmount.text = NumberFormat.getInstance().format(monthTotal) + "원"
        binding.tvCount.text = "${current.size}회"

        binding.rvHistory.adapter = HistoryAdapter(current.map(::toItem)) { openDetail(it) }
        binding.rvPastHistory.adapter = HistoryAdapter(past.map(::toItem)) { openDetail(it) }
    }

    private fun openDetail(item: HistoryItem) {
        findNavController().navigate(
            R.id.action_historyList_to_historyDetail,
            bundleOf("settlementId" to item.settlementId)
        )
    }

    private fun toItem(h: SettlementHistory) = HistoryItem(
        settlementId = h.settlementId,
        name = h.title.ifBlank { "정산" },
        date = formatDate(h.createdAt),
        peopleCount = if (h.memberCount > 0) "${h.memberCount}명" else "정산 완료",
        amount = NumberFormat.getInstance().format(h.totalAmount) + "원"
    )

    /** "2026-05-24T12:30:..." → "5. 24. 2026" */
    private fun formatDate(iso: String?): String {
        iso ?: return ""
        val parts = iso.substringBefore('T').split('-')
        if (parts.size < 3) return ""
        val y = parts[0]
        val m = parts[1].toIntOrNull() ?: return ""
        val d = parts[2].toIntOrNull() ?: return ""
        return "$m. $d. $y"
    }

    private fun yearMonth(iso: String?): String =
        iso?.substringBefore('T')?.substringBeforeLast('-') ?: ""

    private fun currentYearMonth(): String {
        val now = java.time.LocalDate.now()
        return "%04d-%02d".format(now.year, now.monthValue)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
