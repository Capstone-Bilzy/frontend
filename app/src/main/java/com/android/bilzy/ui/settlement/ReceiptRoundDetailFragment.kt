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
import com.android.bilzy.databinding.FragmentReceiptRoundDetailBinding
import com.android.bilzy.domain.model.Receipt
import com.android.bilzy.ui.scan.ScanFlowViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat

/**
 * 영수증 목록에서 차수(라운드)를 눌렀을 때 그 차수에 스캔·확정된 항목을 다시 보여준다(읽기 전용).
 * 하단 "다시 찍기"는 같은 차수를 다시 스캔하는 흐름으로 보낸다 — 확정하면 그 차수만 새 내역으로 교체된다.
 */
@AndroidEntryPoint
class ReceiptRoundDetailFragment : Fragment() {

    private var _binding: FragmentReceiptRoundDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScanFlowViewModel by hiltNavGraphViewModels(R.id.nav_graph)
    private val nf = NumberFormat.getInstance()

    private val round: Int get() = requireArguments().getInt(ARG_ROUND)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReceiptRoundDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvRoundBadge.text = "${round}차"
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnRetake.setOnClickListener {
            viewModel.startRescan(round)
            findNavController().navigate(R.id.action_receiptRoundDetail_to_scanCamera)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.settlement.collect { settlement ->
                    settlement?.receipts?.firstOrNull { it.round == round }?.let(::render)
                }
            }
        }
    }

    private fun render(receipt: Receipt) {
        binding.tvStoreName.text = receipt.storeName?.ifBlank { null } ?: "이름 없는 영수증"
        val table = binding.itemTable
        table.removeAllViews()
        receipt.items.forEach { item ->
            table.addView(row(item.name, item.quantity.toString(), won(item.price * item.quantity), bold = false))
        }
        table.addView(View(requireContext()).apply {
            setBackgroundColor(Color.parseColor("#33FFFFFF"))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)).apply {
                topMargin = dp(6)
                bottomMargin = dp(12)
            }
        })
        val total = if (receipt.totalAmount > 0) receipt.totalAmount
        else receipt.items.sumOf { it.price * it.quantity }
        table.addView(row("합계", "", won(total), bold = true))
    }

    /** 품목명(남는 폭) / 수량 / 금액 한 줄. */
    private fun row(name: String, qty: String, amount: String, bold: Boolean): View {
        val ctx = requireContext()
        val line = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
        }
        fun cell(text: String, color: Int) = TextView(ctx).apply {
            this.text = text
            setTextColor(color)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }
        line.addView(cell(name, Color.WHITE).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        line.addView(cell(qty, Color.parseColor("#BEBEF7")).apply {
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(40), ViewGroup.LayoutParams.WRAP_CONTENT)
        })
        line.addView(cell(amount, Color.WHITE).apply {
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = dp(8) }
        })
        return line
    }

    private fun won(value: Long) = nf.format(value) + "원"

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_ROUND = "round"
    }
}
