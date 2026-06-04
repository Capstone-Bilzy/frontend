package com.android.bilzy.ui.room

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentEnteringRoomBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EnteringRoomFragment : Fragment() {

    private var _binding: FragmentEnteringRoomBinding? = null
    private val binding get() = _binding!!
    private val handler = Handler(Looper.getMainLooper())

    private val roomViewModel: RoomViewModel by hiltNavGraphViewModels(R.id.nav_graph)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEnteringRoomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 본인 멤버십 보장 후 정산방 상세(멤버·항목·총액)를 불러온다 → 멤버 대기 화면으로
        roomViewModel.ensureMyMembership()
        handler.postDelayed({
            if (_binding != null) {
                findNavController().navigate(R.id.action_enteringRoom_to_memberWaiting)
            }
        }, 1500L)
    }

    override fun onDestroyView() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroyView()
        _binding = null
    }
}
