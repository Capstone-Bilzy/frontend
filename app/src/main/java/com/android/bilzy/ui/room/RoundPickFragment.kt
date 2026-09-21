package com.android.bilzy.ui.room

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
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
import java.text.NumberFormat

/**
 * 정산방 참여 확정 직후(호스트: QR 초대 화면에서 "입장하기", 게스트: QR 스캔/딥링크 참여 직후),
 * EnteringRoom 진입 전에 거치는 "나의 정산 목록" 화면.
 * 프로토타입 .bz-pick-list: 라운드(영수증)마다 차수 칩+가게이름+금액이 담긴 카드를 세로로 나열하고,
 * 카드를 눌러 참여 여부를 토글한다.
 */
@AndroidEntryPoint
class RoundPickFragment : Fragment() {

    private var _binding: FragmentRoundPickBinding? = null
    private val binding get() = _binding!!

    private val roomViewModel: RoomViewModel by hiltNavGraphViewModels(R.id.nav_graph)
    private val nf = NumberFormat.getInstance()

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
                    renderCards()
                }
            }
        }
    }

    private fun observePickedRounds() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                roomViewModel.pickedRounds.collect { picked ->
                    renderCards()
                    binding.btnNext.isEnabled = picked.isNotEmpty()
                    binding.btnNext.alpha = if (picked.isNotEmpty()) 1f else 0.5f
                }
            }
        }
    }

    private fun renderCards() {
        val list = binding.roundPickList
        list.removeAllViews()
        val picked = roomViewModel.pickedRounds.value
        receipts.forEach { receipt ->
            list.addView(roundCard(receipt, receipt.round in picked))
        }
    }

    private fun roundCard(receipt: Receipt, selected: Boolean): View {
        val ctx = requireContext()
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(
                if (selected) R.drawable.bg_round_pick_card_selected else R.drawable.bg_round_pick_card
            )
            setPadding(dp(16), dp(20), dp(16), dp(20))
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
            setOnClickListener { roomViewModel.togglePickedRound(receipt.round) }
        }

        card.addView(TextView(ctx).apply {
            text = "${receipt.round}차"
            setTextColor(Color.parseColor("#7DE87D"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(typeface, Typeface.BOLD)
            setBackgroundResource(R.drawable.bg_chip_round_outline_green)
            setPadding(dp(11), dp(4), dp(11), dp(4))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dp(14) }
        })

        card.addView(TextView(ctx).apply {
            text = receipt.storeName?.ifBlank { "이름 없는 영수증" } ?: "이름 없는 영수증"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })

        card.addView(TextView(ctx).apply {
            text = won(receipt.totalAmount)
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, Typeface.BOLD)
        })

        return card
    }

    private fun won(value: Long) = nf.format(value) + "원"

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
