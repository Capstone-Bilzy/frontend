package com.android.bilzy.ui.scan

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentReceiptSaveBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReceiptSaveFragment : Fragment() {

    private var _binding: FragmentReceiptSaveBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScanFlowViewModel by hiltNavGraphViewModels(R.id.nav_graph)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReceiptSaveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showReceiptPreview()

        binding.btnRescan.setOnClickListener {
            findNavController().navigate(R.id.action_receiptSave_to_scanCamera)
        }

        binding.btnSave.setOnClickListener {
            findNavController().navigate(R.id.action_receiptSave_to_saved)
        }
    }

    /** 촬영/선택한 영수증 이미지를 미리보기로 표시. */
    private fun showReceiptPreview() {
        val bytes = viewModel.capturedImage ?: return
        val bitmap = runCatching {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull() ?: return
        binding.ivReceipt.setImageBitmap(bitmap)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
