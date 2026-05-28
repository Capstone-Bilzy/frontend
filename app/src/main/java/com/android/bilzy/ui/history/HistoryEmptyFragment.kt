package com.android.bilzy.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentHistoryEmptyBinding

class HistoryEmptyFragment : Fragment() {

    private var _binding: FragmentHistoryEmptyBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryEmptyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pastItems = listOf(
            HistoryItem("생일 파티", "4. 28. 2026 · 05:30 PM", "6명", "103,500원"),
        )

        binding.rvPastHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPastHistory.adapter = HistoryAdapter(pastItems) {
            findNavController().navigate(R.id.action_historyEmpty_to_historyDetail)
        }

        binding.navHome.setOnClickListener {
            findNavController().navigate(R.id.action_historyEmpty_to_home)
        }
        binding.navScan.setOnClickListener {
            findNavController().navigate(R.id.action_historyEmpty_to_scanHub)
        }
        binding.navMyPage.setOnClickListener {
            findNavController().navigate(R.id.action_historyEmpty_to_myPage)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
