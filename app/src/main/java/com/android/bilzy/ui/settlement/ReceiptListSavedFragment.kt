package com.android.bilzy.ui.settlement

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentReceiptListSavedBinding
import com.android.bilzy.ui.common.playMotionOnce

/**
 * 다차 정산 영수증 목록의 "저장하기" 결과 화면. 버튼 없이 저장 모션(약 3초)이 끝나면 자동으로 목록으로 돌아간다.
 * 영상이 끝 이벤트를 못 주는 경우를 대비해 4초 뒤에도 돌아간다.
 */
class ReceiptListSavedFragment : Fragment() {

    private var _binding: FragmentReceiptListSavedBinding? = null
    private val binding get() = _binding!!
    private val handler = Handler(Looper.getMainLooper())
    private var releaseMotion: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReceiptListSavedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        releaseMotion = binding.motionView.playMotionOnce(R.raw.receipt_saved) { goBack() }
        handler.postDelayed({ goBack() }, 4000L)
    }

    private fun goBack() {
        handler.removeCallbacksAndMessages(null)
        if (isAdded && _binding != null) {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        handler.removeCallbacksAndMessages(null)
        releaseMotion?.invoke()
        releaseMotion = null
        super.onDestroyView()
        _binding = null
    }
}
