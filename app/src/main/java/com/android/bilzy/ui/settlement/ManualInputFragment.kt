package com.android.bilzy.ui.settlement

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentManualInputBinding

class ManualInputFragment : Fragment() {

    private var _binding: FragmentManualInputBinding? = null
    private val binding get() = _binding!!

    private val items = mutableListOf<Pair<String, String>>()
    private lateinit var adapter: OcrItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManualInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = OcrItemAdapter(items)
        binding.rvItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvItems.adapter = adapter

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnAddItem.setOnClickListener {
            val name = binding.etItemName.text.toString().trim()
            val price = binding.etItemPrice.text.toString().trim()
            if (name.isNotEmpty() && price.isNotEmpty()) {
                items.add(Pair(name, "${price}원"))
                adapter.notifyItemInserted(items.size - 1)
                binding.etItemName.text?.clear()
                binding.etItemPrice.text?.clear()
            }
        }

        binding.btnStart.setOnClickListener {
            findNavController().navigate(R.id.action_manualInput_to_peopleCount)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
