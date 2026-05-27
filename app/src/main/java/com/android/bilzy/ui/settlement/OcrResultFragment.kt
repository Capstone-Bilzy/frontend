package com.android.bilzy.ui.settlement

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentOcrResultBinding

class OcrResultFragment : Fragment() {

    private var _binding: FragmentOcrResultBinding? = null
    private val binding get() = _binding!!

    private val sampleItems = listOf(
        Pair("삼겹살 2인분", "28,000원"),
        Pair("된장찌개", "9,000원"),
        Pair("공기밥 x3", "3,000원"),
        Pair("음료수", "5,000원"),
        Pair("서비스 요금", "4,500원"),
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOcrResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvItems.adapter = OcrItemAdapter(sampleItems)

        binding.tvTotal.text = "49,500원"

        binding.btnBack.setOnClickListener {
            findNavController().navigate(R.id.action_ocrResult_to_home)
        }

        binding.btnAddItem.setOnClickListener {
            findNavController().navigate(R.id.action_ocrResult_to_addItem)
        }

        binding.btnStart.setOnClickListener {
            findNavController().navigate(R.id.action_ocrResult_to_peopleCount)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
