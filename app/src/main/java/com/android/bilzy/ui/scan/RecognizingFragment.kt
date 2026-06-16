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
        Pair("내역 확인이 끝났어요", "결과를 보여드릴게요")
    )
    private var currentStep = 0
    private var completing = false

    /** 실제 완료 전까지 머무는 단계(정보 추출). 마지막 '내역 확인'은 스캔 성공 시에만 점등. */
    private val waitingCap = 1

    /** 대기 중에는 cap 단계까지만 전진하고, 그 단계 아이콘을 주기적으로 펄스시켜 진행 중임을 보여준다. */
    private val animTick = object : Runnable {
        override fun run() {
            if (_binding == null || completing) return
            if (currentStep < waitingCap) {
                currentStep++
                updateStep(currentStep)
            }
            pulseStep(currentStep)
            handler.postDelayed(this, 1100L)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecognizingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateStep(0)
        pulseStep(0)
        handler.postDelayed(animTick, 1100L)
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
                            onScanComplete()
                        }
                        is ScanFlowViewModel.ScanState.Error -> {
                            handler.removeCallbacks(animTick)
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

    /** 실제 스캔 완료 → 마지막 '완료' 단계를 점등하고 잠깐 보여준 뒤 결과로 이동. */
    private fun onScanComplete() {
        if (completing) return
        completing = true
        handler.removeCallbacks(animTick)
        currentStep = 2
        updateStep(2)
        pulseStep(2)
        handler.postDelayed({
            if (isAdded && _binding != null) {
                // 인식 완료 → 영수증 저장 확인(9) → 저장 완료(10) → OCR 결과 순으로 이동
                findNavController().navigate(R.id.action_recognizing_to_receiptSave)
            }
        }, 700L)
    }

    private fun pulseStep(step: Int) {
        val frame = listOf(binding.step1Icon, binding.step2Icon, binding.step3Icon)[step]
        frame.animate().scaleX(1.18f).scaleY(1.18f).setDuration(300)
            .withEndAction { frame.animate().scaleX(1f).scaleY(1f).setDuration(300).start() }
            .start()
    }

    private fun updateStep(step: Int) {
        val (title, subtitle) = stepMessages[step]
        binding.tvStatus.text = title
        binding.tvSubStatus.text = subtitle

        val frames = listOf(binding.step1Icon, binding.step2Icon, binding.step3Icon)
        val icons = listOf(binding.ivStep1, binding.ivStep2, binding.ivStep3)
        val labels = listOf(binding.tvStep1, binding.tvStep2, binding.tvStep3)

        frames.forEachIndexed { i, frame ->
            val active = i <= step
            // 활성 단계는 보라색 칩 + 흰색, 대기 단계는 흐린 원 + 회색으로 색이 또렷하게 바뀐다.
            frame.setBackgroundResource(if (active) R.drawable.bg_step_active else R.drawable.bg_step_idle)
            icons[i].setColorFilter(if (active) ACTIVE_COLOR else IDLE_COLOR)
            labels[i].setTextColor(if (active) ACTIVE_COLOR else IDLE_COLOR)
        }
    }

    override fun onDestroyView() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val ACTIVE_COLOR = 0xFFFFFFFF.toInt()
        const val IDLE_COLOR = 0xFF8888BB.toInt()
    }
}
