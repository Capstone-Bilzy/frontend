package com.android.favorie

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.android.favorie.databinding.FragmentCompatibilityResultBinding
import com.android.favorie.network.model.CategorySimilarity
import com.android.favorie.network.model.CommonTag
import com.android.favorie.network.model.CompareResponse
import com.google.android.flexbox.FlexboxLayout

class CompatibilityResultFragment : Fragment() {

    private var _binding: FragmentCompatibilityResultBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCompatibilityResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        @Suppress("DEPRECATION")
        val data = arguments?.getSerializable(ARG_RESULT) as? CompareResponse ?: return
        renderResult(data)
    }

    @SuppressLint("SetTextI18n")
    private fun renderResult(data: CompareResponse) {
        binding.tvNameMe.text = data.me.nickname
        binding.tvNameOther.text = data.opponent.nickname

        val overallPct = (data.overallSimilarity * 100).toInt()
        binding.tvCompPercent.text = "$overallPct%"

        setupCompBars(data.categorySimilarities)
        setupCommonTags(data.commonTags)
    }

    private fun setupCompBars(categories: List<CategorySimilarity>) {
        val colorMap = mapOf(
            "MUSIC"   to "#FFDD57",
            "MOVIE"   to "#FF4B4B",
            "BOOK"    to "#3DBA6F",
            "SPACE"   to "#FF8C00",
            "PLACE"   to "#FF8C00",
            "FASHION" to "#4B9EFF",
            "VIBE"    to "#BB3FBD",
            "MOOD"    to "#BB3FBD"
        )

        val container = binding.layoutCompBars
        container.removeAllViews()

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels - (40 * displayMetrics.density).toInt()

        categories.forEach { item ->
            val itemView = layoutInflater.inflate(R.layout.item_comp_bar, container, false)

            val barBg      = itemView.findViewById<View>(R.id.view_bar_bg)
            val barFill    = itemView.findViewById<View>(R.id.view_bar_fill)
            val tvCategory = itemView.findViewById<TextView>(R.id.tv_bar_category)
            val tvPercent  = itemView.findViewById<TextView>(R.id.tv_bar_percent)

            val hex = colorMap[item.category.uppercase()] ?: "#FFFFFF"
            val itemColor = Color.parseColor(hex)
            val pct = (item.similarity * 100).toInt()

            tvCategory.text = item.category
            tvPercent.text = "$pct%"

            barBg.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 80f
                setColor(Color.parseColor("#2A2A2A"))
            }

            barFill.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 80f
                setColor(itemColor)
            }

            barFill.post {
                val params = barFill.layoutParams
                params.width = (screenWidth * (pct / 100f)).toInt()
                barFill.layoutParams = params
            }

            container.addView(itemView)
        }
    }

    private fun setupCommonTags(tags: List<CommonTag>) {
        val flexbox = binding.flexboxTags
        flexbox.removeAllViews()

        tags.forEach { tag ->
            val tv = TextView(requireContext()).apply {
                text = "#${tag.name}"
                textSize = 14f
                setTextColor(Color.WHITE)
                setPadding(36, 18, 36, 18)
                val lp = FlexboxLayout.LayoutParams(
                    FlexboxLayout.LayoutParams.WRAP_CONTENT,
                    FlexboxLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 12, 12) }
                layoutParams = lp
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 60f
                    setColor(Color.parseColor("#2A2A2A"))
                    setStroke(2, Color.parseColor("#666666"))
                }
            }
            flexbox.addView(tv)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_RESULT = "compare_result"

        fun newInstance(result: CompareResponse) = CompatibilityResultFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_RESULT, result)
            }
        }
    }
}