package com.android.bilzy.ui.room

import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentCalculatingBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CalculatingFragment : Fragment() {

    private var _binding: FragmentCalculatingBinding? = null
    private val binding get() = _binding!!

    private val roomViewModel: RoomViewModel by hiltNavGraphViewModels(R.id.nav_graph)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalculatingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 실제 AI(Gemini) 정산 호출. 실패하면 결과 화면에서 엔빵으로 폴백.
        viewLifecycleOwner.lifecycleScope.launch {
            val start = SystemClock.elapsedRealtime()
            roomViewModel.calculate()
            // 로딩 애니메이션이 너무 빨리 사라지지 않도록 최소 표시 시간 보장
            val elapsed = SystemClock.elapsedRealtime() - start
            if (elapsed < 1500L) delay(1500L - elapsed)
            if (isAdded && _binding != null) {
                findNavController().navigate(R.id.action_calculating_to_settlementResult)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
