package com.android.bilzy.ui.settlement

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.bilzy.databinding.FragmentReceiptListSavedBinding

/**
 * 다차 정산 영수증 목록의 "저장하기" 결과 화면. 버튼 없이 2.4초 후 자동으로 목록으로 돌아간다
 * (프로토타입 `Saved` 컴포넌트와 동일한 타이밍).
 */
class ReceiptListSavedFragment : Fragment() {

    private var _binding: FragmentReceiptListSavedBinding? = null
    private val binding get() = _binding!!
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReceiptListSavedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handler.postDelayed({
            if (isAdded) {
                findNavController().navigateUp()
            }
        }, 2400L)
    }

    override fun onDestroyView() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroyView()
        _binding = null
    }
}
