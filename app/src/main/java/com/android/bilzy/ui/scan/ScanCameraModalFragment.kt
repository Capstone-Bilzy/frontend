package com.android.bilzy.ui.scan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentScanCameraModalBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ScanCameraModalFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentScanCameraModalBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanCameraModalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnPhotoLibrary.setOnClickListener {
            findNavController().navigate(R.id.action_modal_to_recognizing)
        }

        binding.btnFile.setOnClickListener {
            findNavController().navigate(R.id.action_modal_to_recognizing)
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
