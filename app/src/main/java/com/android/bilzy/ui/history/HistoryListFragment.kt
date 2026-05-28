package com.android.bilzy.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentHistoryListBinding

class HistoryListFragment : Fragment() {

    private var _binding: FragmentHistoryListBinding? = null
    private val binding get() = _binding!!

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

        val currentItems = listOf(
            HistoryItem("회사 점심 약속", "5. 24. 2026 · 12:30 PM", "6명", "57,500원"),
            HistoryItem("동기 모임", "5. 15. 2026 · 07:45 PM", "7명", "112,000원"),
            HistoryItem("팀 빌딩 워크숍", "5. 20. 2026 · 06:45 PM", "10명", "230,000원"),
            HistoryItem("프로젝트 발표회", "5. 25. 2026 · 02:45 PM", "15명", "312,600원"),
            HistoryItem("정기 회의", "5. 30. 2026 · 10:45 AM", "8명", "92,000원"),
        )

        val pastItems = listOf(
            HistoryItem("생일 파티", "4. 28. 2026 · 05:30 PM", "6명", "103,500원"),
        )

        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = HistoryAdapter(currentItems) {
            findNavController().navigate(R.id.action_historyList_to_historyDetail)
        }

        binding.rvPastHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPastHistory.adapter = HistoryAdapter(pastItems) {
            findNavController().navigate(R.id.action_historyList_to_historyDetail)
        }

        binding.navHome.setOnClickListener {
            findNavController().navigate(R.id.action_historyList_to_home)
        }

        binding.navScan.setOnClickListener {
            findNavController().navigate(R.id.action_historyList_to_scanHub)
        }

        binding.navMyPage.setOnClickListener {
            findNavController().navigate(R.id.action_historyList_to_myPage)
        }

        binding.tvLogo.setOnLongClickListener {
            findNavController().navigate(R.id.action_historyList_to_historyEmpty)
            true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
