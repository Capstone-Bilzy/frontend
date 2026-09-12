package com.android.bilzy.ui.room

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.setMargins
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.android.bilzy.R
import com.android.bilzy.databinding.FragmentCalculatingBinding
import com.android.bilzy.domain.model.Settlement
import com.android.bilzy.domain.model.SettlementMember
import com.android.bilzy.domain.model.SettlementStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CalculatingFragment : Fragment() {

    private var _binding: FragmentCalculatingBinding? = null
    private val binding get() = _binding!!

    private val roomViewModel: RoomViewModel by hiltNavGraphViewModels(R.id.nav_graph)
    private val handler = Handler(Looper.getMainLooper())

    private var advanced = false

    /** null = 아직 방장 여부 확인 전. */
    private var isOwnerFlow: Boolean? = null

    /** 게스트 폴링: 방장이 계산을 끝낼 때까지 상세를 다시 불러온다(MemberWaitingFragment와 동일 패턴). */
    private val pollTick = object : Runnable {
        override fun run() {
            if (_binding == null || advanced) return
            roomViewModel.load()
            handler.postDelayed(this, 1800L)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalculatingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        roomViewModel.settlement.value?.let { renderSettlement(it) }
        observeRoom()
        startFlow()
    }

    /**
     * 방장만 실제 계산(POST /calculate)을 호출할 수 있다(백엔드 403) — 게스트는 호출하지 않고
     * 방장이 계산을 끝내 상태가 바뀔 때까지 폴링만 한다.
     */
    private fun startFlow() {
        viewLifecycleOwner.lifecycleScope.launch {
            val owner = roomViewModel.isOwner()
            isOwnerFlow = owner
            if (owner) {
                runCalculate()
            } else {
                handler.post(pollTick)
            }
        }
    }

    /** 계산 실패 시(예: 아직 게스트 판정 오류 등) 무조건 다음 화면으로 넘기지 않고 이 화면에 머문다. */
    private suspend fun runCalculate() {
        val start = SystemClock.elapsedRealtime()
        val ok = roomViewModel.calculate()
        val elapsed = SystemClock.elapsedRealtime() - start
        if (elapsed < 1500L) delay(1500L - elapsed)
        if (!isAdded || _binding == null) return
        if (ok) {
            advance()
        } else {
            Toast.makeText(requireContext(), "정산 계산에 실패했어요. 잠시 후 다시 시도해주세요", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeRoom() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                roomViewModel.settlement.collect { settlement ->
                    settlement ?: return@collect
                    renderSettlement(settlement)
                    // 게스트: 방장이 계산을 끝내 상태가 바뀌면 결과 화면으로 이동
                    val isCalculated = settlement.status == SettlementStatus.CALCULATED ||
                        settlement.status == SettlementStatus.DONE
                    if (isOwnerFlow == false && isCalculated) {
                        advance()
                    }
                }
            }
        }
    }

    private fun advance() {
        if (advanced || _binding == null) return
        advanced = true
        handler.removeCallbacks(pollTick)
        findNavController().navigate(R.id.action_calculating_to_settlementResult)
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

        renderPending(members)
    }

    /** ready==false인 멤버 이름을 나열해 "OOO님이 아직 특이사항을 입력하지 않았어요" 형태로 보여준다(프로토타입 CalcWait). */
    private fun renderPending(members: List<SettlementMember>) {
        val container = binding.pendingContainer
        container.removeAllViews()
        val pendingNames = members.filterNot { it.ready }.map { it.nickname }
        if (pendingNames.isEmpty()) {
            container.visibility = View.GONE
            return
        }
        container.visibility = View.VISIBLE
        container.addView(
            pendingText("${pendingNames.joinToString(", ")}님이 아직 특이사항을 입력하지 않았어요", color = "#BEBEF7")
        )
        container.addView(
            pendingText("모두 완료되면 자동으로 정산이 시작돼요", color = "#8888BB").apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = dp(4)
            }
        )
    }

    private fun pendingText(text: String, color: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            setTextColor(Color.parseColor(color))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
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
        handler.removeCallbacksAndMessages(null)
        super.onDestroyView()
        _binding = null
    }
}
