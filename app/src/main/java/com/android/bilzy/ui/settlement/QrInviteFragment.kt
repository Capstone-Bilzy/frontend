package com.android.bilzy.ui.settlement

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentQrInviteBinding

class QrInviteFragment : Fragment() {

    private var _binding: FragmentQrInviteBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQrInviteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnEnter.setOnClickListener {
            findNavController().navigate(R.id.action_qrInvite_to_enteringRoom)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
