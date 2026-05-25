package com.android.bilzy.ui.scan

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentRecognizingBinding

class RecognizingFragment : Fragment() {

    private var _binding: FragmentRecognizingBinding? = null
    private val binding get() = _binding!!
    private val handler = Handler(Looper.getMainLooper())

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
    }

    private fun scheduleNext() {
        handler.postDelayed({
            if (_binding == null) return@postDelayed
            currentStep++
            if (currentStep < stepMessages.size) {
                updateStep(currentStep)
                scheduleNext()
            } else {
                findNavController().navigate(R.id.action_recognizing_to_receiptSave)
            }
        }, 1500L)
    }

    private fun updateStep(step: Int) {
        val (title, subtitle) = stepMessages[step]
        binding.tvStatus.text = title
        binding.tvSubStatus.text = subtitle

        when (step) {
            0 -> {
                binding.step1Icon.alpha = 1f
                binding.step2Icon.alpha = 0.4f
                binding.step3Icon.alpha = 0.4f
            }
            1 -> {
                binding.step1Icon.alpha = 1f
                binding.step2Icon.alpha = 1f
                binding.step3Icon.alpha = 0.4f
            }
            2 -> {
                binding.step1Icon.alpha = 1f
                binding.step2Icon.alpha = 1f
                binding.step3Icon.alpha = 1f
            }
        }
    }

    override fun onDestroyView() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroyView()
        _binding = null
    }
}
