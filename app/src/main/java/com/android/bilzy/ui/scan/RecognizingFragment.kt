package com.android.bilzy.ui.scan

import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.android.bilzy.databinding.FragmentRecognizingBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RecognizingFragment : Fragment() {

    private var _binding: FragmentRecognizingBinding? = null
    private val binding get() = _binding!!
    private val handler = Handler(Looper.getMainLooper())

    private val viewModel: ScanFlowViewModel by hiltNavGraphViewModels(R.id.nav_graph)

    private val stepMessages = listOf(
        Pair("영수증을 인식하는 중이에요", "잠시만 기다려 주세요"),
        Pair("정보를 추출하는 중이에요", "거의 다 됐어요"),
        Pair("내역을 확인하는 중이에요", "마지막 단계에요")
    )
    private var currentStep = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecognizingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateStep(0)
        scheduleNext()
        observeScan()
        // 화면 진입 시 업로드 시작 (Loading 가드로 중복 방지)
        if (viewModel.scanState.value !is ScanFlowViewModel.ScanState.Success) {
            viewModel.runScan()
        }
    }

    private fun observeScan() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.scanState.collect { state ->
                    when (state) {
                        is ScanFlowViewModel.ScanState.Success -> {
                            viewModel.consumeScanState()
                            findNavController().navigate(R.id.recognizing_to_ocrResult)
                        }
                        is ScanFlowViewModel.ScanState.Error -> {
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            viewModel.consumeScanState()
                            findNavController().navigateUp()
                        }
                        else -> Unit // Idle/Loading: 애니메이션 유지
                    }
                }
            }
        }
    }

    /** 업로드가 끝날 때까지 인식 단계 애니메이션을 순환시킨다. */
    private fun scheduleNext() {
        handler.postDelayed({
            if (_binding == null) return@postDelayed
            currentStep = (currentStep + 1) % stepMessages.size
            updateStep(currentStep)
            scheduleNext()
        }, 1200L)
    }

    private fun updateStep(step: Int) {
        val (title, subtitle) = stepMessages[step]
        binding.tvStatus.text = title
        binding.tvSubStatus.text = subtitle
        binding.step1Icon.alpha = if (step >= 0) 1f else 0.4f
        binding.step2Icon.alpha = if (step >= 1) 1f else 0.4f
        binding.step3Icon.alpha = if (step >= 2) 1f else 0.4f
    }

    override fun onDestroyView() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroyView()
        _binding = null
    }
}
