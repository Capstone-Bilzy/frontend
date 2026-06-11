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

/**
 * 09 영수증 저장 — 정산내역에 영수증 사진을 함께 보관할지 묻는 화면.
 * 이미지는 스캔(/ocr/scan) 때 이미 서버(정산건)에 올라가 있다.
 *  · 저장하기 = 그대로 보관 → 저장완료(10)로 이동
 *  · 다시 찍기 = 서버의 영수증 이미지 삭제 후 카메라로 재촬영
 */
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
            // 보관 안 함 — 서버의 영수증 이미지 삭제 후 다시 촬영
            viewModel.discardReceiptImage()
            findNavController().navigate(R.id.action_receiptSave_to_scanCamera)
        }
        binding.btnSave.setOnClickListener {
            // 정산내역에 영수증 사진 보관(이미 업로드됨) → 저장 완료
            findNavController().navigate(R.id.action_receiptSave_to_saved)
        }
    }

    private fun showReceiptPreview() {
        val bytes = viewModel.capturedImage ?: return
        runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
            .getOrNull()?.let { binding.ivReceipt.setImageBitmap(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
