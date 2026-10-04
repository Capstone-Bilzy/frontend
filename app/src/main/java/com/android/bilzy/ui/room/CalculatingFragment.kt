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
import android.widget.ImageView
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
    private var advanced = false

    /** 이 화면에서 ready 플래그 전송이 한 번이라도 성공했는지(AmountAdjust에서 실패했을 때의 재전송용). */
    private var readySent = false

    /** 방장의 마지막 계산 시도 시각(실패 시 재시도 간격 조절, /calculate rate limit 10/min 고려). */
    private var lastCalcAttemptAt = 0L
    private var calcFailToastShown = false

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
     * 방장·게스트 모두 주기적으로 상세를 다시 불러와 체크 표시(ready)와 상태를 갱신한다.
     * - 누구든: 상태가 calculated/done이 되면 결과 화면으로 이동(observeRoom).
     * - 방장만: 모든 멤버가 특이사항 입력을 마치면(ready) 계산(POST /calculate, 백엔드가 방장만 허용)을
     *   호출하고, 실패하면 이 화면에 머문 채 잠시 뒤 자동으로 다시 시도한다.
     * 예전엔 방장이 화면 진입 즉시 한 번만 계산을 호출하고 폴링도 하지 않아, 계산이 한 번 실패하거나
     * 다른 멤버가 아직 입력 중이면 체크 표시도 안 바뀌고 다음 화면으로도 못 넘어갔다.
     */
    private fun startFlow() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (!advanced) {
                    if (!readySent) readySent = roomViewModel.submitReady()
                    roomViewModel.refresh()
                    calculateIfOwnerAndAllReady()
                    delay(POLL_INTERVAL_MS)
                }
            }
        }
    }

    private suspend fun calculateIfOwnerAndAllReady() {
        if (advanced) return
        val settlement = roomViewModel.settlement.value ?: return
        if (settlement.status == SettlementStatus.CALCULATED || settlement.status == SettlementStatus.DONE) return
        if (settlement.members.isEmpty() || settlement.members.any { !it.ready }) return
        if (!roomViewModel.isOwner()) return
        val now = SystemClock.elapsedRealtime()
        if (lastCalcAttemptAt != 0L && now - lastCalcAttemptAt < CALC_RETRY_INTERVAL_MS) return
        lastCalcAttemptAt = now

        val ok = roomViewModel.calculate()
        if (!isAdded || _binding == null) return
        if (ok) {
            advance()
        } else if (!calcFailToastShown) {
            calcFailToastShown = true
            Toast.makeText(requireContext(), "정산 계산에 실패했어요. 잠시 후 다시 시도해주세요", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeRoom() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                roomViewModel.settlement.collect { settlement ->
                    settlement ?: return@collect
                    renderSettlement(settlement)
                    // 계산이 끝나 상태가 바뀌면 결과 화면으로 이동(방장도 포함 — 계산 응답을 못 받았어도
                    // 서버에서는 끝났을 수 있다).
                    val isCalculated = settlement.status == SettlementStatus.CALCULATED ||
                        settlement.status == SettlementStatus.DONE
                    if (isCalculated) advance()
                }
            }
        }
    }

    private fun advance() {
        if (advanced || _binding == null) return
        advanced = true
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
        val weighted = joined > 6
        row.weightSum = if (weighted) joined.toFloat() else -1f
        val myNick = roomViewModel.myNickname.value
        members.forEach { member ->
            // 특이사항 입력(정산 준비 완료)을 마친 멤버만 밝은 스타일 — 피그마 "계산중".
            row.addView(
                buildAvatarTile(
                    requireContext(),
                    initial = member.nickname.take(1),
                    label = if (member.nickname == myNick) "${member.nickname}(나)" else member.nickname,
                    active = member.ready,
                    weighted = weighted
                )
            )
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

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val POLL_INTERVAL_MS = 1800L
        const val CALC_RETRY_INTERVAL_MS = 8000L
    }
}
