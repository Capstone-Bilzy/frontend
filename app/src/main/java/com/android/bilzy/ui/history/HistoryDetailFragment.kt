package com.android.bilzy.ui.history

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
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

        val demoItems = if (s.id == com.android.bilzy.data.demo.DemoData.DEMO_ID)
            com.android.bilzy.data.demo.DemoData.memberItems else emptyMap()
        val participants = s.members.map { m ->
            ParticipantItem(
                name = m.nickname,
                items = demoItems[m.id] ?: "",
                amount = nf.format(m.amount) + "원",
                adjustment = m.reason?.takeIf { it.isNotBlank() }
            )
        }
        binding.rvParticipants.adapter = HistoryParticipantAdapter(participants)

        renderAvatars(s.members.size)
    }

    private fun renderAvatars(count: Int) {
        val row = binding.avatarRow
        row.removeAllViews()
        if (count == 0) return

        val maxVisible = 4
        val visible = minOf(count, maxVisible)
        val dp32 = dp(32)
        val dpNeg8 = dp(-8)

        repeat(visible) { i ->
            val isLast = i == visible - 1 && count <= maxVisible
            val circle = View(requireContext()).apply {
                setBackgroundResource(R.drawable.bg_avatar_circle)
                layoutParams = LinearLayout.LayoutParams(dp32, dp32).apply {
                    marginEnd = if (isLast) 0 else dpNeg8
                }
            }
            row.addView(circle)
        }

        // 초과 인원 "+N" 뱃지
        if (count > maxVisible) {
            val extra = count - maxVisible
            val badge = FrameLayout(requireContext()).apply {
                setBackgroundResource(R.drawable.bg_avatar_circle)
                layoutParams = LinearLayout.LayoutParams(dp32, dp32)
            }
            badge.addView(TextView(requireContext()).apply {
                text = "+$extra"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            })
            row.addView(badge)
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
