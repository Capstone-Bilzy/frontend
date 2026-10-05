package com.android.bilzy.ui.room

import android.graphics.Color
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
import com.android.bilzy.util.setFontWeight
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
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

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

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
        // 차수 목록을 못 불러오면(서버 일시 오류 등) 고를 카드가 없어 이 화면에서 막히므로, 뜰 때까지 다시 불러온다.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                roomViewModel.refresh()
                while (receipts.isEmpty()) {
                    delay(2000L)
                    roomViewModel.refresh()
                }
            }
        }
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
            // 피그마 정산방 목록 행: 344x84, r20, 좌우 여백 18/23
            setPadding(dp(18), 0, dp(23), 0)
            minimumHeight = dp(84)
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
            setOnClickListener { roomViewModel.togglePickedRound(receipt.round) }
        }

        card.addView(TextView(ctx).apply {
            text = "${receipt.round}차"
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#6ADB74"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setFontWeight(600)
            setBackgroundResource(R.drawable.bg_chip_round_outline_green)
            minWidth = dp(51)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(26)
            ).apply { marginEnd = dp(21) }
        })

        card.addView(TextView(ctx).apply {
            text = receipt.storeName?.ifBlank { "이름 없는 영수증" } ?: "이름 없는 영수증"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setFontWeight(400)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })

        card.addView(TextView(ctx).apply {
            text = won(receipt.totalAmount)
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setFontWeight(600)
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
