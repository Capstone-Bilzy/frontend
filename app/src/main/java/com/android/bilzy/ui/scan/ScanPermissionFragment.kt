package com.android.bilzy.ui.scan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentScanPermissionBinding

class ScanPermissionFragment : Fragment() {

    private var _binding: FragmentScanPermissionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanPermissionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnCamera.setOnClickListener {
            findNavController().navigate(R.id.action_scanPermission_to_scanCamera)
        }

        binding.btnManual.setOnClickListener {
            findNavController().navigate(R.id.action_scanPermission_to_manualInput)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
