package com.android.bilzy.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentReceiptPickerBinding
import com.android.bilzy.domain.model.SavedReceipt
import com.android.bilzy.ui.receipt.SavedReceiptViewModel
import com.android.bilzy.util.ImageCompressor
import androidx.appcompat.app.AlertDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** 저장 영수증 보관함 목록. 갤러리에서 새 영수증을 골라 저장 화면으로 보내고, 항목을 보거나 삭제한다. */
@AndroidEntryPoint
class ReceiptPickerFragment : Fragment() {

    private var _binding: FragmentReceiptPickerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SavedReceiptViewModel by hiltNavGraphViewModels(R.id.nav_graph)
    private lateinit var adapter: SavedReceiptAdapter

    /** 갤러리에서 영수증 이미지 선택 → 바이트 읽어 저장 화면으로. */
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@registerForActivityResult
        val bytes = runCatching {
            requireContext().contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
        if (bytes == null || bytes.isEmpty()) {
            Toast.makeText(requireContext(), "이미지를 불러오지 못했어요", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        val mime = requireContext().contentResolver.getType(uri) ?: "image/jpeg"
        viewLifecycleOwner.lifecycleScope.launch {
            val compressed = ImageCompressor.compress(bytes, mime)
            viewModel.setPendingImage(compressed.bytes, compressed.mime)
            findNavController().navigate(R.id.action_receiptPicker_to_receiptSave)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReceiptPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SavedReceiptAdapter { showReceiptDialog(it) }
        binding.rvReceipts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvReceipts.adapter = adapter

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnNewScan.setOnClickListener { pickImage.launch("image/*") }

        observeList()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadReceipts()
    }

    private fun observeList() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.listState.collect { state ->
                when (state) {
                    is SavedReceiptViewModel.ListState.Loaded -> {
                        adapter.submit(state.receipts)
                        binding.tvEmpty.visibility = if (state.receipts.isEmpty()) View.VISIBLE else View.GONE
                    }
                    is SavedReceiptViewModel.ListState.Error -> {
                        binding.tvEmpty.visibility = View.GONE
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                    SavedReceiptViewModel.ListState.Loading -> Unit
                }
            }
        }
    }

    /** 영수증 크게보기 + 삭제. */
    private fun showReceiptDialog(receipt: SavedReceipt) {
        val image = ImageView(requireContext()).apply {
            adjustViewBounds = true
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, 0)
            if (!receipt.imageUrl.isNullOrBlank()) {
                load(receipt.imageUrl) {
                    placeholder(R.drawable.bg_receipt_thumb)
                    error(R.drawable.bg_receipt_thumb)
                }
            } else {
                setImageResource(R.drawable.ic_receipt)
            }
        }
        AlertDialog.Builder(requireContext())
            .setTitle(receipt.storeName.ifBlank { "이름 없는 영수증" })
            .setView(image)
            .setNegativeButton("삭제") { _, _ -> confirmDelete(receipt) }
            .setPositiveButton("닫기", null)
            .show()
    }

    private fun confirmDelete(receipt: SavedReceipt) {
        AlertDialog.Builder(requireContext())
            .setMessage("이 영수증을 삭제할까요?")
            .setNegativeButton("취소", null)
            .setPositiveButton("삭제") { _, _ ->
                viewModel.delete(receipt.id) { ok ->
                    val msg = if (ok) "삭제했어요" else "삭제에 실패했어요"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
