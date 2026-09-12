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
    private lateinit var adapter: OcrItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManualInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = OcrItemAdapter(
            onDelete = { index -> viewModel.removeItem(index) },
            onChange = { index, item -> viewModel.updateItem(index, item) }
        )
        binding.rvItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvItems.adapter = adapter

        if (viewModel.settlementTitle.isNotEmpty()) {
            binding.etGroupName.setText(viewModel.settlementTitle)
        }

        binding.tvRoundBadge.text = "${viewModel.currentRound}차"

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnAddItem.setOnClickListener { viewModel.addItem("", 0L, 1) }

        binding.btnComplete.setOnClickListener { onSubmit(isFinalRound = true) }
        binding.btnMore.setOnClickListener { onSubmit(isFinalRound = false) }

        observeItems()
        observeConfirm()
    }

    private fun onSubmit(isFinalRound: Boolean) {
        val title = binding.etGroupName.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "모임 이름을 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }
        val items = viewModel.items.value
        if (items.isEmpty() || items.any { it.name.isBlank() || it.price <= 0L }) {
            Toast.makeText(requireContext(), "모든 항목의 이름과 가격을 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }
        val storeName = binding.etStoreName.text.toString().trim()
        viewModel.confirm(title, storeName, isFinalRound)
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
                        is ScanFlowViewModel.ConfirmState.Loading -> setButtonsEnabled(false)
                        is ScanFlowViewModel.ConfirmState.Success -> {
                            setButtonsEnabled(true)
                            viewModel.consumeConfirmState()
                            if (viewModel.lastConfirmWasFinalRound) {
                                findNavController().navigate(R.id.action_manualInput_to_receiptList)
                            } else {
                                // 프로토타입과 동일: 직접입력은 다음 라운드로 넘어갈 때 화면 이동 없이
                                // 같은 화면에서 입력값만 비운다(카메라로 돌아갈 필요가 없음).
                                viewModel.advanceToNextRound()
                                binding.etStoreName.text?.clear()
                                binding.tvRoundBadge.text = "${viewModel.currentRound}차"
                            }
                        }
                        is ScanFlowViewModel.ConfirmState.Error -> {
                            setButtonsEnabled(true)
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            viewModel.consumeConfirmState()
                        }
                        is ScanFlowViewModel.ConfirmState.Idle -> setButtonsEnabled(true)
                    }
                }
            }
        }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        binding.btnComplete.isEnabled = enabled
        binding.btnMore.isEnabled = enabled
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
