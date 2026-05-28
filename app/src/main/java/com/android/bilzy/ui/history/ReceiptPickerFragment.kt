package com.android.bilzy.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentReceiptPickerBinding

class ReceiptPickerFragment : Fragment() {

    private var _binding: FragmentReceiptPickerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReceiptPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val receipts = listOf(
            SavedReceiptItem("버거킹 강남점", "57,500원 · 2026.05.04"),
            SavedReceiptItem("정담 한식당", "70,000원 · 2026.05.03"),
            SavedReceiptItem("스타벅스 강남역점", "23,800원 · 2026.04.28"),
            SavedReceiptItem("교촌치킨 역삼점", "42,000원 · 2026.04.20"),
            SavedReceiptItem("올리브영 강남본점", "18,200원 · 2026.04.15"),
        )

        binding.rvReceipts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvReceipts.adapter = SavedReceiptAdapter(receipts) {
            findNavController().navigate(R.id.action_receiptPicker_to_historyDetailWithReceipt)
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnNewScan.setOnClickListener {
            findNavController().navigate(R.id.action_receiptPicker_to_scanHub)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
