package com.android.bilzy.ui.settlement

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
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
            commitTypedCount()
            if (count > MIN_COUNT) { count--; updateCount() }
        }

        binding.btnPlus.setOnClickListener {
            commitTypedCount()
            if (count < MAX_COUNT) { count++; updateCount() }
        }

        // "직접 숫자 입력하기": 가운데 숫자 칸에 커서를 켜고 키보드를 올린다(숫자 칸을 직접 눌러도 같다).
        binding.tvDirectInput.setOnClickListener { startTyping() }
        binding.tvCount.setOnClickListener { startTyping() }
        binding.tvCount.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.tvCount.isCursorVisible = true else commitTypedCount()
        }
        binding.tvCount.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                commitTypedCount()
                binding.tvCount.clearFocus()
                imm().hideSoftInputFromWindow(binding.tvCount.windowToken, 0)
                true
            } else false
        }

        binding.btnNext.setOnClickListener {
            commitTypedCount()
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
        val text = count.toString()
        if (binding.tvCount.text.toString() != text) binding.tvCount.setText(text)
        binding.tvCount.setSelection(binding.tvCount.text.length)
    }

    private fun startTyping() {
        binding.tvCount.isCursorVisible = true
        binding.tvCount.requestFocus()
        binding.tvCount.setSelection(binding.tvCount.text.length)
        imm().showSoftInput(binding.tvCount, InputMethodManager.SHOW_IMPLICIT)
    }

    /** 숫자 칸에 직접 쓴 값을 인원수로 반영한다. 비었거나 범위를 벗어나면 허용 범위(2~20명)로 맞춘다. */
    private fun commitTypedCount() {
        val typed = binding.tvCount.text.toString().toIntOrNull() ?: count
        count = typed.coerceIn(MIN_COUNT, MAX_COUNT)
        updateCount()
        binding.tvCount.isCursorVisible = false
    }

    private fun imm() =
        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    private companion object {
        const val MIN_COUNT = 2
        const val MAX_COUNT = 20
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
