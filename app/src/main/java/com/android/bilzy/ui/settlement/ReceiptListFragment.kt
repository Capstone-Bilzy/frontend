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
import com.android.bilzy.ui.scan.ScanFlowViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat

/**
 * 다차 정산(n차) UI 뼈대: 스캔된 영수증들을 표시하는 목록 화면.
 * ⚠️ TODO(다차 정산): 여기 쌓이는 목록은 [ScanFlowViewModel.receiptDrafts]의 클라이언트 인메모리
 * 상태일 뿐, 서버는 정산방당 영수증 1장만 지원한다(/ocr/confirm이 항목을 통째로 덮어씀).
 * 실제로 서버에 반영되는 건 마지막에 "완료하기"를 눌러 confirm된 한 건뿐이다.
 */
@AndroidEntryPoint
class ReceiptListFragment : Fragment() {

    private var _binding: FragmentReceiptListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScanFlowViewModel by hiltNavGraphViewModels(R.id.nav_graph)
    private val nf = NumberFormat.getInstance()

    /** 영수증 이미지 보관 기능과 무관한 순수 표시용 토글(서버 호출 없음). */
    private var savedToggled = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReceiptListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnSaveToggle.setOnClickListener {
            savedToggled = !savedToggled
            binding.btnSaveToggle.text = if (savedToggled) "확인 완료" else "목록 확인했어요"
        }

        binding.btnPeople.setOnClickListener {
            findNavController().navigate(R.id.action_receiptList_to_peopleCount)
        }

        observeDrafts()
    }

    private fun observeDrafts() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.receiptDrafts.collect { drafts -> render(drafts) }
            }
        }
    }

    private fun render(drafts: List<ScanFlowViewModel.ReceiptListEntry>) {
        binding.receiptListContainer.removeAllViews()
        drafts.forEach { entry ->
            binding.receiptListContainer.addView(receiptRow(entry))
        }
        binding.tvCountLabel.text = "영수증 총 ${drafts.size}건"
        binding.tvGrandTotal.text = won(drafts.sumOf { it.total })
    }

    private fun receiptRow(entry: ScanFlowViewModel.ReceiptListEntry): View {
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
            text = "${entry.round}차"
            setTextColor(Color.parseColor("#BEBEF7"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setBackgroundResource(R.drawable.bg_chip_purple)
            setPadding(dp(10), dp(4), dp(10), dp(4))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dp(10) }
        })

        row.addView(TextView(ctx).apply {
            text = entry.store.ifBlank { "이름 없는 영수증" }
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            )
        })

        row.addView(TextView(ctx).apply {
            text = won(entry.total)
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dp(10) }
        })

        row.addView(TextView(ctx).apply {
            text = "✕"
            setTextColor(Color.parseColor("#B0B0C4"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(dp(28), dp(28))
            setOnClickListener { viewModel.removeDraftFromList(entry.round) }
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
