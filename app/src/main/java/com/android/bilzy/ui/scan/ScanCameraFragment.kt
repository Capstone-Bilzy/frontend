package com.android.bilzy.ui.scan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentScanCameraBinding

class ScanCameraFragment : Fragment() {

    private var _binding: FragmentScanCameraBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnShutter.setOnClickListener {
            findNavController().navigate(R.id.action_scanCamera_to_modal)
        }

        binding.btnGallery.setOnClickListener {
            findNavController().navigate(R.id.action_scanCamera_to_modal)
        }

        binding.tabQr.setOnClickListener {
            findNavController().navigate(R.id.action_scanCamera_to_qrScan)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
