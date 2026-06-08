package com.android.bilzy.ui.scan

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentReceiptSaveBinding
import com.android.bilzy.ui.receipt.SavedReceiptViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 보관함 저장 확인 화면. 선택한 영수증을 미리보고, 독립 OCR로 금액을 프리필한 뒤
 * 사용자가 가게명·금액을 확인/수정해 저장한다.
 */
@AndroidEntryPoint
class ReceiptSaveFragment : Fragment() {

    private var _binding: FragmentReceiptSaveBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SavedReceiptViewModel by hiltNavGraphViewModels(R.id.nav_graph)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReceiptSaveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showReceiptPreview()
        viewModel.runScan()  // 진입 시 독립 OCR로 금액 추정

        binding.btnRescan.setOnClickListener {
            // 다시 고르기 — 보관함 목록으로 돌아가 갤러리 재선택
            findNavController().navigateUp()
        }

        binding.btnSave.setOnClickListener { save() }

        observeScan()
        observeSave()
    }

    private fun showReceiptPreview() {
        val bytes = viewModel.pendingImage ?: return
        runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
            .getOrNull()?.let { binding.ivReceipt.setImageBitmap(it) }
    }

    private fun observeScan() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.scanState.collect { state ->
                when (state) {
                    SavedReceiptViewModel.ScanState.Loading ->
                        binding.tvScanStatus.text = "금액을 인식하는 중…"
                    is SavedReceiptViewModel.ScanState.Success -> {
                        binding.tvScanStatus.text = "인식 완료 — 금액을 확인해주세요"
                        // 사용자가 아직 직접 입력하지 않았을 때만 프리필
                        if (binding.etAmount.text.isNullOrBlank() && state.suggestedTotal > 0) {
                            binding.etAmount.setText(state.suggestedTotal.toString())
                        }
                    }
                    is SavedReceiptViewModel.ScanState.Error ->
                        binding.tvScanStatus.text = "금액 자동인식 실패 — 직접 입력해주세요"
                    SavedReceiptViewModel.ScanState.Idle -> Unit
                }
            }
        }
    }

    private fun save() {
        val store = binding.etStore.text?.toString()?.trim().orEmpty()
        if (store.isEmpty()) {
            Toast.makeText(requireContext(), "가게명을 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }
        val amount = binding.etAmount.text?.toString()?.trim()?.toLongOrNull() ?: 0L
        viewModel.save(store, amount)
    }

    private fun observeSave() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.saveState.collect { state ->
                when (state) {
                    SavedReceiptViewModel.SaveState.Loading ->
                        binding.btnSave.isEnabled = false
                    SavedReceiptViewModel.SaveState.Success -> {
                        viewModel.consumeSaveState()
                        findNavController().navigate(R.id.action_receiptSave_to_saved)
                    }
                    is SavedReceiptViewModel.SaveState.Error -> {
                        binding.btnSave.isEnabled = true
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        viewModel.consumeSaveState()
                    }
                    SavedReceiptViewModel.SaveState.Idle ->
                        binding.btnSave.isEnabled = true
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
