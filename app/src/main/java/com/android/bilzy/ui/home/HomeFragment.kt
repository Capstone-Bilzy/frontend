package com.android.bilzy.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardPayer.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_settlementTitle)
        }

        binding.cardParticipant.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_qrScan)
        }

        binding.navScan.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_scanHub)
        }

        binding.navHistory.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_historyList)
        }

        binding.navMyPage.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_myPage)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
