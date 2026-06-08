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
import com.android.bilzy.databinding.FragmentParticipantInputBinding
import com.android.bilzy.ui.room.RoomViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ParticipantInputFragment : Fragment() {

    private var _binding: FragmentParticipantInputBinding? = null
    private val binding get() = _binding!!

    private val roomViewModel: RoomViewModel by hiltNavGraphViewModels(R.id.nav_graph)

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

        binding.btnNext.setOnClickListener {
            val name = binding.etName.text?.toString()?.trim().orEmpty()
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "이름을 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // 입력한 이름을 표시 이름으로 저장(이후 입장에도 재사용됨)
            roomViewModel.setMyName(name)
            findNavController().navigate(R.id.action_participantInput_to_qrInvite)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
