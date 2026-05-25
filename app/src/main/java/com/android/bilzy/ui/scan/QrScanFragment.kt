package com.android.bilzy.ui.scan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentQrScanBinding

class QrScanFragment : Fragment() {

    private var _binding: FragmentQrScanBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQrScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigate(R.id.action_qrScan_to_home)
        }

        binding.btnShutter.setOnClickListener {
            findNavController().navigate(R.id.action_qrScan_to_enteringRoom)
        }

        binding.tabReceipt.setOnClickListener {
            findNavController().navigate(R.id.action_qrScan_to_scanCamera)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
