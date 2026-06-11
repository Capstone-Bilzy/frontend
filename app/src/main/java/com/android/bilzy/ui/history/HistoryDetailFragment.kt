package com.android.bilzy.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentHistoryDetailBinding
import com.android.bilzy.domain.model.Settlement
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat

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

        binding.rvParticipants.layoutManager = LinearLayoutManager(requireContext())

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

    private fun render(s: Settlement) {
        binding.tvTitle.text = s.title.ifBlank { "정산" }
        binding.tvSettlementName.text = s.title.ifBlank { "정산" }
        binding.tvTotalAmount.text = nf.format(s.totalAmount) + "원"
        binding.tvPeopleChip.text = "👥 ${s.members.size}명 참여"
        binding.tvDate.text = formatDate(s.createdAt)

        val participants = s.members.map { m ->
            ParticipantItem(
                name = m.nickname,
                items = "",                       // 멤버별 주문 항목은 상세 응답에 없음 → 숨김
                amount = nf.format(m.amount) + "원",
                adjustment = m.reason?.takeIf { it.isNotBlank() }
            )
        }
        binding.rvParticipants.adapter = HistoryParticipantAdapter(participants)
    }

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
