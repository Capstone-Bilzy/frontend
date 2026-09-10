package com.android.bilzy.ui.room

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentRoundPickBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 다차 정산(n차) UI 뼈대: QR 스캔/딥링크 참여 성공 직후, EnteringRoom 진입 전에 삽입되는
 * "나의 정산 목록" 화면. 실제 다차 데이터가 없으므로 현재 정산 1건을 "1차" 토글 1개로 표시한다.
 * TODO(다차 정산): 서버가 정산방당 여러 라운드를 지원하게 되면 실제 목록으로 교체한다.
 */
@AndroidEntryPoint
class RoundPickFragment : Fragment() {

    private var _binding: FragmentRoundPickBinding? = null
    private val binding get() = _binding!!

    private val roomViewModel: RoomViewModel by hiltNavGraphViewModels(R.id.nav_graph)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoundPickBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.chipRound1.setOnClickListener { roomViewModel.togglePickedRound(1) }
        binding.btnNext.setOnClickListener {
            findNavController().navigate(R.id.action_roundPick_to_enteringRoom)
        }

        observePickedRounds()
    }

    private fun observePickedRounds() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                roomViewModel.pickedRounds.collect { picked ->
                    val selected = 1 in picked
                    styleChip(selected)
                    binding.btnNext.isEnabled = picked.isNotEmpty()
                    binding.btnNext.alpha = if (picked.isNotEmpty()) 1f else 0.5f
                }
            }
        }
    }

    private fun styleChip(selected: Boolean) {
        binding.chipRound1.setBackgroundResource(
            if (selected) R.drawable.bg_role_chip else R.drawable.bg_chip_outline
        )
        binding.chipRound1.setTypeface(binding.chipRound1.typeface, if (selected) Typeface.BOLD else Typeface.NORMAL)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
