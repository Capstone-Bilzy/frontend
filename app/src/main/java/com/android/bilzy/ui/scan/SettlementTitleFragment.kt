package com.android.bilzy.ui.scan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentSettlementTitleBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** 스캔 진입 첫 화면: 정산방 이름 입력 → createSettlement → 스캔 권한 화면으로. */
@AndroidEntryPoint
class SettlementTitleFragment : Fragment() {

    private var _binding: FragmentSettlementTitleBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScanFlowViewModel by hiltNavGraphViewModels(R.id.nav_graph)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettlementTitleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnNext.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "정산방 이름을 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.createSettlement(title)
        }

        observeState()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.createState.collect { state ->
                    when (state) {
                        is ScanFlowViewModel.CreateState.Loading -> setLoading(true)
                        is ScanFlowViewModel.CreateState.Created -> {
                            setLoading(false)
                            viewModel.consumeCreateState()
                            findNavController().navigate(R.id.action_settlementTitle_to_scanPermission)
                        }
                        is ScanFlowViewModel.CreateState.Error -> {
                            setLoading(false)
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            viewModel.consumeCreateState()
                        }
                        is ScanFlowViewModel.CreateState.Idle -> setLoading(false)
                    }
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnNext.isEnabled = !loading
        binding.btnNext.text = if (loading) "" else "다음"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
