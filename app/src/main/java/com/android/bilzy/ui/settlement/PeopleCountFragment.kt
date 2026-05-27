package com.android.bilzy.ui.settlement

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentPeopleCountBinding

class PeopleCountFragment : Fragment() {

    private var _binding: FragmentPeopleCountBinding? = null
    private val binding get() = _binding!!
    private var count = 2

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPeopleCountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateCount()

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnMinus.setOnClickListener {
            if (count > 2) { count--; updateCount() }
        }

        binding.btnPlus.setOnClickListener {
            if (count < 20) { count++; updateCount() }
        }

        binding.btnNext.setOnClickListener {
            findNavController().navigate(R.id.action_peopleCount_to_participantInput)
        }
    }

    private fun updateCount() {
        binding.tvCount.text = count.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
