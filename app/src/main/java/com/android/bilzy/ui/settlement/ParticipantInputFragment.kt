package com.android.bilzy.ui.settlement

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentParticipantInputBinding

class ParticipantInputFragment : Fragment() {

    private var _binding: FragmentParticipantInputBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentParticipantInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnNext.setOnClickListener {
            findNavController().navigate(R.id.action_participantInput_to_qrInvite)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
