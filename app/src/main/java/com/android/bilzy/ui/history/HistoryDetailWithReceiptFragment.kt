package com.android.bilzy.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentHistoryDetailWithReceiptBinding

class HistoryDetailWithReceiptFragment : Fragment() {

    private var _binding: FragmentHistoryDetailWithReceiptBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryDetailWithReceiptBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val participants = listOf(
            ParticipantItem("정민지 (나)", "햄버거, 감자튀김, 음료", "10,875원", "핫도그 -3,500원"),
            ParticipantItem("이재연", "햄버거, 핫도그, 음료", "11,875원", "감자튀김 -2,500원"),
            ParticipantItem("한정우", "햄버거, 감자튀김, 핫도그", "12,375원", "음료 -2,000원"),
            ParticipantItem("김지우", "햄버거, 감자튀김, 음료", "10,875원", "핫도그 -3,500원"),
        )

        binding.rvParticipants.layoutManager = LinearLayoutManager(requireContext())
        binding.rvParticipants.adapter = HistoryParticipantAdapter(participants)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnAddReceipt.setOnClickListener {
            findNavController().navigate(R.id.action_historyDetailWithReceipt_to_receiptPicker)
        }

        binding.navHome.setOnClickListener {
            findNavController().navigate(R.id.action_historyDetailWithReceipt_to_home)
        }
        binding.navScan.setOnClickListener {
            findNavController().navigate(R.id.action_historyDetailWithReceipt_to_scanHub)
        }
        binding.navHistory.setOnClickListener {
            findNavController().navigate(R.id.action_historyDetailWithReceipt_to_historyList)
        }
        binding.navMyPage.setOnClickListener {
            findNavController().navigate(R.id.action_historyDetailWithReceipt_to_myPage)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
