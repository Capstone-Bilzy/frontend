package com.android.bilzy.ui.room

import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentRoundPickBinding
import com.android.bilzy.domain.model.Receipt
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 정산방 참여 확정 직후(호스트: QR 초대 화면에서 "입장하기", 게스트: QR 스캔/딥링크 참여 직후),
 * EnteringRoom 진입 전에 거치는 "나의 정산 목록" 화면.
 * 정산방의 실제 라운드(receipts) 개수만큼 토글 칩을 동적으로 만든다.
 */
@AndroidEntryPoint
class RoundPickFragment : Fragment() {

    private var _binding: FragmentRoundPickBinding? = null
    private val binding get() = _binding!!

    private val roomViewModel: RoomViewModel by hiltNavGraphViewModels(R.id.nav_graph)

    private var receipts: List<Receipt> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoundPickBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnNext.setOnClickListener {
            binding.btnNext.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                val ok = roomViewModel.submitPickedRounds()
                if (isAdded && _binding != null) {
                    binding.btnNext.isEnabled = true
                    if (ok) {
                        findNavController().navigate(R.id.action_roundPick_to_enteringRoom)
                    } else {
                        Toast.makeText(requireContext(), "차수 선택 저장에 실패했어요", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        observeSettlement()
        observePickedRounds()
        roomViewModel.load()
    }

    private fun observeSettlement() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                roomViewModel.settlement.collect { settlement ->
                    receipts = settlement?.receipts.orEmpty()
                    renderChips()
                }
            }
        }
    }

    private fun observePickedRounds() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                roomViewModel.pickedRounds.collect { picked ->
                    renderChips()
                    binding.btnNext.isEnabled = picked.isNotEmpty()
                    binding.btnNext.alpha = if (picked.isNotEmpty()) 1f else 0.5f
                }
            }
        }
    }

    private fun renderChips() {
        val row = binding.roundToggleRow
        row.removeAllViews()
        val picked = roomViewModel.pickedRounds.value
        receipts.forEach { receipt ->
            val chip = TextView(requireContext()).apply {
                text = "${receipt.round}차"
                setTextColor(android.graphics.Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                gravity = android.view.Gravity.CENTER
                minWidth = dp(96)
                setPadding(dp(16), 0, dp(16), 0)
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(44)
                ).apply { marginEnd = dp(10) }
                isClickable = true
                isFocusable = true
            }
            styleChip(chip, receipt.round in picked)
            chip.setOnClickListener {
                roomViewModel.togglePickedRound(receipt.round)
            }
            row.addView(chip)
        }
    }

    private fun styleChip(chip: TextView, selected: Boolean) {
        chip.setBackgroundResource(
            if (selected) R.drawable.bg_role_chip else R.drawable.bg_chip_outline
        )
        chip.setTypeface(chip.typeface, if (selected) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
