package com.android.bilzy.ui.scan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentScanHubBinding

class ScanHubFragment : Fragment() {

    private var _binding: FragmentScanHubBinding? = null
    private val binding get() = _binding!!
    private var isReceiptMode = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanHubBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigate(R.id.action_scanHub_to_home)
        }

        binding.tabLeft.setOnClickListener { setMode(!isReceiptMode) }
        binding.tabCenter.setOnClickListener { /* 이미 활성 탭 */ }

        binding.btnShutter.setOnClickListener {
            if (isReceiptMode) {
                findNavController().navigate(R.id.action_scanHub_to_scanCameraModal)
            } else {
                findNavController().navigate(R.id.action_scanHub_to_enteringRoom)
            }
        }

        setMode(true)
    }

    private fun setMode(receipt: Boolean) {
        isReceiptMode = receipt
        if (receipt) {
            binding.tabLeft.text = "QR 스캔"
            binding.tabCenter.text = "영수증 스캔"
            binding.guideReceipt.visibility = View.VISIBLE
            binding.guideQr.visibility = View.GONE
        } else {
            binding.tabLeft.text = "영수증 스캔"
            binding.tabCenter.text = "QR 스캔"
            binding.guideReceipt.visibility = View.GONE
            binding.guideQr.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
