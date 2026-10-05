package com.android.bilzy.ui.settlement

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentPeopleCountBinding
import com.android.bilzy.ui.room.RoomViewModel
import com.android.bilzy.ui.scan.ScanFlowViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PeopleCountFragment : Fragment() {

    private var _binding: FragmentPeopleCountBinding? = null
    private val binding get() = _binding!!
    private var count = 2

    private val roomViewModel: RoomViewModel by hiltNavGraphViewModels(R.id.nav_graph)
    private val scanViewModel: ScanFlowViewModel by hiltNavGraphViewModels(R.id.nav_graph)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPeopleCountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateCount()

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnMinus.setOnClickListener {
            if (count > 2) { count--; updateCount() }
        }

        binding.btnPlus.setOnClickListener {
            if (count < 20) { count++; updateCount() }
        }

        binding.btnNext.setOnClickListener {
            roomViewModel.expectedCount = count
            // 정원 저장은 RoomViewModel이 정산방 id를 알아야 하는데, 예전엔 id를 다음 화면(초대 QR)에서야
            // 넘겨줘서 여기서는 항상 id가 없어 요청도 못 보내고 "저장 실패"가 떴다. 먼저 id를 넘긴다.
            roomViewModel.setRoom(scanViewModel.settlementId)
            viewLifecycleOwner.lifecycleScope.launch {
                if (!roomViewModel.saveMemberCapacity(count)) {
                    Toast.makeText(requireContext(), "정원 설정 저장에 실패했어요. 정원 제한 없이 진행돼요", Toast.LENGTH_SHORT).show()
                }
                findNavController().navigate(R.id.action_peopleCount_to_qrInvite)
            }
        }
    }

    private fun updateCount() {
        binding.tvCount.text = count.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
