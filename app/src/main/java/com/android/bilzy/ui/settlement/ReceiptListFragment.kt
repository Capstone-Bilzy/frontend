package com.android.bilzy.ui.settlement

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
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentReceiptListBinding
import com.android.bilzy.domain.model.Receipt
import com.android.bilzy.ui.scan.ScanFlowViewModel
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
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_history_card)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
        }

        row.addView(TextView(ctx).apply {
            text = "${receipt.round}차"
            setTextColor(Color.parseColor("#7DE87D"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setBackgroundResource(R.drawable.bg_chip_round_outline_green)
            setPadding(dp(10), dp(4), dp(10), dp(4))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dp(10) }
        })

        row.addView(TextView(ctx).apply {
            text = receipt.storeName?.ifBlank { "이름 없는 영수증" } ?: "이름 없는 영수증"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            )
        })

        row.addView(TextView(ctx).apply {
            text = won(receipt.totalAmount)
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        })

        return row
    }

    private fun won(value: Long) = nf.format(value) + "원"

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
