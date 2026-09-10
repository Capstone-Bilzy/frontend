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
import java.text.NumberFormat

/** 영수증 없이 항목을 직접 입력하는 경로. OCR 결과와 동일하게 confirm으로 확정한다. */
@AndroidEntryPoint
class ManualInputFragment : Fragment() {

    private var _binding: FragmentManualInputBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScanFlowViewModel by hiltNavGraphViewModels(R.id.nav_graph)
    private lateinit var adapter: ManualItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManualInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ManualItemAdapter(
            onDelete = { index -> viewModel.removeItem(index) },
            onChange = { index, item -> viewModel.updateItem(index, item) }
        )
        binding.rvItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvItems.adapter = adapter

        if (viewModel.settlementTitle.isNotEmpty()) {
            binding.etGroupName.setText(viewModel.settlementTitle)
        }

        // TODO: 가게 이름(etStoreName)은 현재 클라이언트에서만 표시되고 서버로 전송되지 않음
        // (OcrResultFragment와 동일) — 도메인 모델/백엔드 계약에 필드 추가 필요.

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnAddItem.setOnClickListener { viewModel.addItem("", 0L, 1) }

        binding.btnStart.setOnClickListener {
            val title = binding.etGroupName.text.toString().trim()
            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "모임 이름을 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val items = viewModel.items.value
            if (items.isEmpty() || items.any { it.name.isBlank() || it.price <= 0L }) {
                Toast.makeText(requireContext(), "모든 항목의 이름과 가격을 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.confirm(title)
        }

        observeItems()
        observeConfirm()
    }

    private fun observeItems() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.items.collect { items ->
                    adapter.submit(items)
                    binding.tvTotal.text = won(items.sumOf { it.subtotal })
                }
            }
        }
    }

    private fun won(value: Long) = NumberFormat.getInstance().format(value) + "원"

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
