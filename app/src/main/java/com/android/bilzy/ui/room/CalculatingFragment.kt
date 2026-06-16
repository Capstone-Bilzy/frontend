package com.android.bilzy.ui.room

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.SystemClock
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setMargins
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentCalculatingBinding
import com.android.bilzy.domain.model.Settlement
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CalculatingFragment : Fragment() {

    private var _binding: FragmentCalculatingBinding? = null
    private val binding get() = _binding!!

    private val roomViewModel: RoomViewModel by hiltNavGraphViewModels(R.id.nav_graph)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalculatingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        roomViewModel.settlement.value?.let { renderSettlement(it) }

        viewLifecycleOwner.lifecycleScope.launch {
            val start = SystemClock.elapsedRealtime()
            roomViewModel.calculate()
            val elapsed = SystemClock.elapsedRealtime() - start
            if (elapsed < 1500L) delay(1500L - elapsed)
            if (isAdded && _binding != null) {
                findNavController().navigate(R.id.action_calculating_to_settlementResult)
            }
        }
    }

    private fun renderSettlement(settlement: Settlement) {
        val members = settlement.members
        if (members.isEmpty()) return

        val target = roomViewModel.expectedCount.coerceAtLeast(members.size)
        val joined = members.size

        binding.tvStatus.text = "$joined / ${target}명"
        binding.progressBar.progress = (joined * 100 / target).coerceIn(0, 100)

        val row = binding.avatarRow
        row.removeAllViews()
        row.weightSum = joined.toFloat()
        val myNick = roomViewModel.myNickname.value
        members.forEach { member ->
            row.addView(avatarTile(member.nickname, member.nickname == myNick))
        }
    }

    private fun avatarTile(name: String, isMe: Boolean): View {
        val ctx = requireContext()
        val tile = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val circle = FrameLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44)).apply { gravity = Gravity.CENTER_HORIZONTAL }
            setBackgroundResource(R.drawable.bg_avatar_done)
        }
        circle.addView(TextView(ctx).apply {
            text = name.take(1)
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER
            )
        })
        val label = TextView(ctx).apply {
            text = if (isMe) "$name(나)" else name
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(dp(4)) }
        }
        tile.addView(circle)
        tile.addView(label)
        return tile
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
