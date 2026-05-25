package com.android.bilzy.ui.room

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentAmountAdjustBinding

class AmountAdjustFragment : Fragment() {

    private var _binding: FragmentAmountAdjustBinding? = null
    private val binding get() = _binding!!

    private val selectedChips = mutableSetOf<Int>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAmountAdjustBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        val chips = listOf(
            binding.chip1, binding.chip2, binding.chip3, binding.chip4,
            binding.chip5, binding.chip6, binding.chip7, binding.chip8
        )

        chips.forEachIndexed { index, chip ->
            chip.setOnClickListener {
                if (selectedChips.contains(index)) {
                    selectedChips.remove(index)
                    chip.alpha = 1f
                } else {
                    selectedChips.add(index)
                    chip.alpha = 0.5f
                }
            }
        }

        binding.btnSettle.setOnClickListener {
            findNavController().navigate(R.id.action_amountAdjust_to_calculating)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
