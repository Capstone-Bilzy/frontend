package com.android.bilzy.ui.scan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentReceiptSaveBinding

class ReceiptSaveFragment : Fragment() {

    private var _binding: FragmentReceiptSaveBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReceiptSaveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnRescan.setOnClickListener {
            findNavController().navigate(R.id.action_receiptSave_to_scanCamera)
        }

        binding.btnSave.setOnClickListener {
            findNavController().navigate(R.id.action_receiptSave_to_saved)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
