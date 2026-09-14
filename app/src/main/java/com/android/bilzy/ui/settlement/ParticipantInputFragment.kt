package com.android.bilzy.ui.settlement

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentParticipantInputBinding
import com.android.bilzy.ui.room.RoomViewModel
import com.android.bilzy.ui.scan.QrScanViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ParticipantInputFragment : Fragment() {

    private var _binding: FragmentParticipantInputBinding? = null
    private val binding get() = _binding!!

    private val roomViewModel: RoomViewModel by hiltNavGraphViewModels(R.id.nav_graph)
    private val qrScanViewModel: QrScanViewModel by viewModels()

    // 게스트(QR/딥링크) 흐름에서만 채워짐. null이면 기존 호스트 흐름 그대로 동작.
    private val pendingSettlementId: String? get() = arguments?.getString("pendingSettlementId")
    private val pendingToken: String? get() = arguments?.getString("pendingToken")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentParticipantInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnClearName.setOnClickListener { binding.etName.text?.clear() }

        // 이미 정한 표시 이름이 있으면 미리 채워 둔다("사용자" 폴백은 제외).
        roomViewModel.loadSuggestedName()
        viewLifecycleOwner.lifecycleScope.launch {
            roomViewModel.suggestedName.collect { name ->
                if (!name.isNullOrBlank() && binding.etName.text.isNullOrBlank()) {
                    binding.etName.setText(name)
                }
            }
        }

        observeJoin()

        binding.btnNext.setOnClickListener {
            val name = binding.etName.text?.toString()?.trim().orEmpty()
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "이름을 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // 입력한 이름을 표시 이름으로 저장(이후 입장에도 재사용됨)
            roomViewModel.setMyName(name)
            val pendingId = pendingSettlementId
            if (pendingId != null) {
                // 게스트 흐름: 이 화면에서 입력받은 이름으로 곧바로 join API 호출
                binding.btnNext.isEnabled = false
                qrScanViewModel.join(pendingId, pendingToken, nickname = name)
            } else {
                findNavController().navigate(R.id.action_participantInput_to_qrInvite)
            }
        }
    }

    private fun observeJoin() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                qrScanViewModel.joinState.collect { state ->
                    when (state) {
                        is QrScanViewModel.JoinState.Success -> {
                            roomViewModel.expectedCount = 0 // 게스트: 인원 게이팅 없음
                            roomViewModel.setRoom(state.settlementId)
                            qrScanViewModel.consumeState()
                            findNavController().navigate(R.id.action_participantInput_to_roundPick)
                        }
                        is QrScanViewModel.JoinState.Error -> {
                            binding.btnNext.isEnabled = true
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            qrScanViewModel.consumeState()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
