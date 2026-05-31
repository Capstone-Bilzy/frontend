package com.android.bilzy.ui.settlement

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentManualInputBinding
import com.android.bilzy.ui.scan.ScanFlowViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** 영수증 없이 항목을 직접 입력하는 경로. OCR 결과와 동일하게 confirm으로 확정한다. */
@AndroidEntryPoint
class ManualInputFragment : Fragment() {

    private var _binding: FragmentManualInputBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScanFlowViewModel by hiltNavGraphViewModels(R.id.nav_graph)
    private lateinit var adapter: OcrItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManualInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = OcrItemAdapter(onDelete = { index -> viewModel.removeItem(index) })
        binding.rvItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvItems.adapter = adapter

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnAddItem.setOnClickListener {
            val name = binding.etItemName.text.toString().trim()
            val price = binding.etItemPrice.text.toString().trim().toLongOrNull() ?: 0L
            if (name.isEmpty() || price <= 0L) {
                Toast.makeText(requireContext(), "항목명과 가격을 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.addItem(name, price, 1)
            binding.etItemName.text?.clear()
            binding.etItemPrice.text?.clear()
        }

        binding.btnStart.setOnClickListener { viewModel.confirm() }

        observeItems()
        observeConfirm()
    }

    private fun observeItems() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.items.collect { adapter.submit(it) }
            }
        }
    }

    private fun observeConfirm() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.confirmState.collect { state ->
                    when (state) {
                        is ScanFlowViewModel.ConfirmState.Loading -> binding.btnStart.isEnabled = false
                        is ScanFlowViewModel.ConfirmState.Success -> {
                            binding.btnStart.isEnabled = true
                            viewModel.consumeConfirmState()
                            findNavController().navigate(R.id.action_manualInput_to_peopleCount)
                        }
                        is ScanFlowViewModel.ConfirmState.Error -> {
                            binding.btnStart.isEnabled = true
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            viewModel.consumeConfirmState()
                        }
                        is ScanFlowViewModel.ConfirmState.Idle -> binding.btnStart.isEnabled = true
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
