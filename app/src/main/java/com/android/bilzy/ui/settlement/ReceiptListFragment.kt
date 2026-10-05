package com.android.bilzy.ui.settlement

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
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentReceiptListBinding
import com.android.bilzy.domain.model.Receipt
import com.android.bilzy.ui.scan.ScanFlowViewModel
import com.android.bilzy.util.setFontWeight
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat

/** 다차 정산(n차): 지금까지 확정된 라운드(영수증)들을 서버 데이터([ScanFlowViewModel.settlement.receipts])로 표시한다. */
@AndroidEntryPoint
class ReceiptListFragment : Fragment() {

    private var _binding: FragmentReceiptListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScanFlowViewModel by hiltNavGraphViewModels(R.id.nav_graph)
    private val nf = NumberFormat.getInstance()

    /** 삭제 요청 진행 중(연타로 중복 삭제 방지 — 번호가 당겨지므로 두 번 누르면 다른 차수가 지워진다). */
    private var deleting = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReceiptListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnSave.setOnClickListener {
            viewModel.receiptListSaved = true
            findNavController().navigate(R.id.action_receiptList_to_saved)
        }

        binding.btnPeople.setOnClickListener {
            findNavController().navigate(R.id.action_receiptList_to_peopleCount)
        }

        renderSaveButton()
        observeSettlement()
        viewModel.loadSettlement()
    }

    /** "저장 완료" 화면에서 자동 복귀한 뒤에도 버튼 상태가 유지되도록 매번 다시 그린다. */
    private fun renderSaveButton() {
        binding.btnSave.text = if (viewModel.receiptListSaved) "저장 완료" else "영수증 저장하기"
    }

    private fun observeSettlement() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.settlement.collect { settlement ->
                    render(settlement?.receipts.orEmpty())
                }
            }
        }
    }

    private fun render(receipts: List<Receipt>) {
        binding.receiptListContainer.removeAllViews()
        receipts.forEach { receipt ->
            binding.receiptListContainer.addView(receiptRow(receipt))
        }
        binding.tvCountLabel.text = "영수증 총 ${receipts.size}건"
        binding.tvGrandTotal.text = nf.format(receipts.sumOf { it.totalAmount })
    }

    private fun receiptRow(receipt: Receipt): View {
        val ctx = requireContext()
        // 피그마 영수증 목록 행: 344x84, r20, 좌측 여백 18, 행 간격 12
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_receipt_row)
            setPadding(dp(18), 0, dp(8), 0)
            minimumHeight = dp(84)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
            // 차수를 누르면 그 차수의 스캔 내역(+다시 찍기) 화면으로 이동
            isClickable = true
            isFocusable = true
            setOnClickListener {
                findNavController().navigate(
                    R.id.action_receiptList_to_roundDetail,
                    bundleOf(OcrResultFragment.ARG_REVIEW_ROUND to receipt.round)
                )
            }
        }

        row.addView(TextView(ctx).apply {
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

        row.addView(TextView(ctx).apply {
            text = receipt.storeName?.ifBlank { "이름 없는 영수증" } ?: "이름 없는 영수증"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setFontWeight(400)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            )
        })

        row.addView(TextView(ctx).apply {
            text = won(receipt.totalAmount)
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setFontWeight(600)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        })

        // 프로토타입 .bz-del-r: 차수 삭제(✕). 확인 없이 바로 지운다(프로토타입과 동일).
        row.addView(TextView(ctx).apply {
            text = "✕"
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#80FFFFFF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            layoutParams = LinearLayout.LayoutParams(dp(32), dp(32)).apply { marginStart = dp(6) }
            isClickable = true
            isFocusable = true
            contentDescription = "삭제"
            setOnClickListener { deleteRound(receipt.round) }
        })

        return row
    }

    private fun deleteRound(round: Int) {
        if (deleting) return
        deleting = true
        viewLifecycleOwner.lifecycleScope.launch {
            val remaining = viewModel.deleteRound(round)
            deleting = false
            if (!isAdded || _binding == null) return@launch
            when (remaining) {
                null -> Toast.makeText(requireContext(), "영수증 삭제에 실패했어요", Toast.LENGTH_SHORT).show()
                0 -> {
                    // 남은 영수증이 없으면 정산을 이어갈 수 없으므로 1차 스캔으로 돌려보낸다.
                    findNavController().navigate(
                        R.id.scanCameraFragment,
                        null,
                        NavOptions.Builder().setPopUpTo(R.id.receiptListFragment, true).build()
                    )
                }
                else -> renderSaveButton()
            }
        }
    }

    private fun won(value: Long) = nf.format(value) + "원"

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
