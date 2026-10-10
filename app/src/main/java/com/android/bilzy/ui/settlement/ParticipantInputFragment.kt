package com.android.bilzy.ui.settlement

import com.android.bilzy.ui.common.hideWhileKeyboardShown
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.android.bilzy.ui.common.loading
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

        // 이름이 있어야 "입력 완료"가 켜지고, 지우기(✕)도 이름이 있을 때만 보인다(프로토타입 NameInput).
        binding.etName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = updateNextEnabled()
        })
        updateNextEnabled()
        // 키보드의 완료 키 = "입력 완료"
        binding.etName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE && binding.btnNext.isEnabled) {
                binding.btnNext.performClick()
                true
            } else false
        }
        // 키보드가 올라와 있는 동안에는 하단 버튼을 숨긴다(키보드 위로 떠올라 입력 칸을 가리지 않게).
        hideWhileKeyboardShown(binding.btnNext)

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
                submitting = true
                updateNextEnabled()
                loading.show()
                // 키보드를 내린다(완료 키로 제출한 경우 버튼이 다시 보이도록)
                hideKeyboard()
                qrScanViewModel.join(pendingId, pendingToken, nickname = name)
            } else {
                // 호스트 흐름(QR초대 화면에서 넘어옴): 방금 확정한 이름으로 멤버십을 생성한다.
                submitting = true
                updateNextEnabled()
                loading.show()
                hideKeyboard()
                viewLifecycleOwner.lifecycleScope.launch {
                    val ok = roomViewModel.ensureMyMembershipAndAwait()
                    loading.hide()
                    if (!isAdded || _binding == null) return@launch
                    submitting = false
                    updateNextEnabled()
                    if (ok) {
                        findNavController().navigate(R.id.action_participantInput_to_roundPick)
                    } else {
                        Toast.makeText(requireContext(), "정산방 입장에 실패했어요. 다시 시도해주세요", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private var submitting = false

    private fun updateNextEnabled() {
        val b = _binding ?: return
        val hasName = !b.etName.text?.toString().isNullOrBlank()
        b.btnNext.isEnabled = hasName && !submitting
        b.btnClearName.visibility = if (hasName) View.VISIBLE else View.INVISIBLE
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
                            loading.hide()
                            submitting = false
                            updateNextEnabled()
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            qrScanViewModel.consumeState()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etName.windowToken, 0)
    }

    override fun onDestroyView() {
        submitting = false
        super.onDestroyView()
        _binding = null
    }
}
