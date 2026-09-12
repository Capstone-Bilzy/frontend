package com.android.bilzy.ui.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.bilzy.data.local.TokenStore
import com.android.bilzy.domain.model.BankAccount
import com.android.bilzy.domain.model.Settlement
import com.android.bilzy.domain.repository.AccountRepository
import com.android.bilzy.domain.repository.SettlementRepository
import com.android.bilzy.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 정산방 입장 이후 화면들(입장→멤버대기→금액조정→계산→결과→완료)이 공유하는 ViewModel.
 * nav_graph 스코프(hiltNavGraphViewModels(R.id.nav_graph))로 주입해 settlement_id와 조회 결과를 공유한다.
 * 진입 경로(호스트: QrInvite, 게스트: QrScan) 양쪽에서 setRoom()으로 id를 넣는다.
 */
@HiltViewModel
class RoomViewModel @Inject constructor(
    private val settlementRepository: SettlementRepository,
    private val accountRepository: AccountRepository,
    private val userRepository: UserRepository,
    private val tokenStore: TokenStore
) : ViewModel() {

    /** 현재 보고 있는 정산방 id. */
    var settlementId: String? = null
        private set

    /** 정산 인원 설정 화면에서 고른 인원수(호스트). 멤버 대기 게이팅에 사용. 0이면 미설정(게스트 등). */
    var expectedCount: Int = 0

    private val _settlement = MutableStateFlow<Settlement?>(null)
    val settlement = _settlement.asStateFlow()

    /** 내 닉네임(멤버 목록에서 '나' 식별용). */
    private val _myNickname = MutableStateFlow<String?>(null)
    val myNickname = _myNickname.asStateFlow()

    /** 내 대표 계좌(정산 완료 화면의 '송금 계좌' 표시용). null = 아직 로딩 전 또는 미설정. */
    private val _myAccount = MutableStateFlow<BankAccount?>(null)
    val myAccount = _myAccount.asStateFlow()

    /** 참여자 입력 화면에서 직접 입력한 표시 이름. 있으면 join·식별에 로그인 닉네임보다 우선. */
    private var myNameOverride: String? = null

    /** 참여자 입력 화면에서 호출. 입력한 이름은 TokenStore에 저장해 이후 입장(호스트·게스트)에 재사용한다. */
    fun setMyName(name: String) {
        val clean = name.trim().takeIf { it.isNotBlank() }
        myNameOverride = clean
        clean?.let {
            _myNickname.value = it
            viewModelScope.launch { tokenStore.saveNickname(it) }
        }
    }

    /** 참여자 입력 화면 프리필용 — 이미 정한 표시 이름(일반 폴백 "사용자"/"참여자"는 제외). */
    private val _suggestedName = MutableStateFlow<String?>(null)
    val suggestedName = _suggestedName.asStateFlow()

    fun loadSuggestedName() {
        viewModelScope.launch {
            val name = myNameOverride ?: tokenStore.currentNickname()
            _suggestedName.value = name?.takeIf { it.isNotBlank() && it != "사용자" && it != "참여자" }
        }
    }

    /** 금액 조정 화면에서 만든 특이사항(칩 선택 등) → AI 계산에 전달. */
    var aiNote: String = ""

    // ── 다차 정산(n차): 내가 참여한 라운드 선택/조정 상태 ─────────────
    private val _pickedRounds = MutableStateFlow<Set<Int>>(emptySet())
    val pickedRounds = _pickedRounds.asStateFlow()

    fun togglePickedRound(round: Int) {
        _pickedRounds.value = if (round in _pickedRounds.value) _pickedRounds.value - round else _pickedRounds.value + round
    }

    /** RoundPick "선택 완료": 고른 라운드 집합을 서버에 반영하고 상세를 다시 불러온다. */
    suspend fun submitPickedRounds(): Boolean {
        val id = settlementId ?: return false
        return runCatching { settlementRepository.setMyRounds(id, _pickedRounds.value.toList()) }
            .onSuccess { load() }
            .isSuccess
    }

    /** AmountAdjust 화면에서 지금 보고 있는 라운드 인덱스(pickedReceipts 기준). 라운드 넘어갈 때마다 증가. */
    var adjIdx: Int = 0

    /** AmountAdjust "다음"/"정산 시작하기": 이 라운드에서 안 먹은 항목을 서버에 반영한다. */
    suspend fun submitRoundAdjustment(round: Int, excludedItemNames: List<String>): Boolean {
        val id = settlementId ?: return false
        return runCatching { settlementRepository.setMyRoundAdjustment(id, round, excludedItemNames) }
            .isSuccess
    }

    /** AmountAdjust 마지막 라운드 제출 성공 시 호출 — 실패해도 흐름은 막지 않는다(runCatching). */
    suspend fun submitReady(): Boolean {
        val id = settlementId ?: return false
        return runCatching { settlementRepository.markReady(id) }.isSuccess
    }

    /** 현재 로그인한 유저가 이 정산방의 방장(생성자)인지. 프로필 조회 실패 시 false로 안전하게 처리. */
    suspend fun isOwner(): Boolean {
        val createdBy = _settlement.value?.createdBy ?: return false
        val myId = userRepository.cachedProfile()?.id
            ?: runCatching { userRepository.getMyProfile() }.getOrNull()?.id
            ?: return false
        return myId == createdBy
    }

    /** true면 AI 계산이 적용된 멤버 금액(저장값), false면 클라이언트 엔빵으로 표시. */
    var aiApplied: Boolean = false
        private set

    fun setRoom(id: String?) {
        if (id.isNullOrBlank()) return
        if (settlementId != id) {
            settlementId = id
            _settlement.value = null
            // 다른 정산방으로 전환되는 경우 이전 방의 라운드 선택/AI 특이사항이 새 방에 새어들지 않도록 초기화
            _pickedRounds.value = emptySet()
            aiNote = ""
            aiApplied = false
            adjIdx = 0
        }
        viewModelScope.launch {
            _myNickname.value = myNameOverride ?: tokenStore.currentNickname()
        }
    }

    /**
     * 내가 이 방의 멤버가 아니면 참여시킨다(이미 멤버면 409가 나도 무시).
     * 호스트 흐름(방을 만든 사람)도 멤버 목록·정산 결과에 본인이 보이도록 한다.
     */
    fun ensureMyMembership() {
        val id = settlementId ?: return
        viewModelScope.launch {
            val nick = myNameOverride
                ?: tokenStore.currentNickname()?.takeIf { it.isNotBlank() }
                ?: "참여자"
            runCatching { settlementRepository.joinByQr(id, nick) } // 이미 참여 중이면 무시
            runCatching { settlementRepository.getSettlement(id) }
                .onSuccess { _settlement.value = it }
        }
    }

    /** 정산방 상세를 다시 불러온다(멤버·항목·총액 포함). 멤버 대기 폴링에도 사용. */
    fun load() {
        val id = settlementId ?: return
        viewModelScope.launch {
            runCatching { settlementRepository.getSettlement(id) }
                .onSuccess { _settlement.value = it }
        }
    }

    /**
     * AI(Gemini) 정산 계산. 성공 시 멤버별 금액·사유가 반영된 상세로 갱신하고 aiApplied=true.
     * 실패하면 false(호출 측은 엔빵으로 폴백).
     */
    suspend fun calculate(): Boolean {
        val id = settlementId ?: return false
        return runCatching { settlementRepository.calculate(id, aiNote) }
            .onSuccess {
                _settlement.value = it
                aiApplied = it.members.any { m -> m.amount > 0 }
            }
            .isSuccess
    }

    /** 정산 완료 화면 진입 시 내 계좌를 로드. 이미 로드됐으면 스킵. */
    fun loadMyAccount() {
        if (_myAccount.value != null) return
        viewModelScope.launch {
            runCatching { accountRepository.getMyAccount() }
                .onSuccess { if (!it.isEmpty) _myAccount.value = it }
        }
    }

    /** 정산 완료 처리. 성공 시 히스토리 캐시를 클리어해 홈 복귀 시 최신 내역이 표시되도록 한다. */
    suspend fun markDone(): Boolean {
        val id = settlementId ?: return false
        return runCatching { settlementRepository.markDone(id) }
            .onSuccess {
                _settlement.value = it
                userRepository.clearCache()
            }
            .isSuccess
    }

    /**
     * 정산 완료 후 홈으로 돌아가거나 로그아웃할 때 호출해 이 ViewModel 전체를 초기 상태로 되돌린다.
     * nav_graph 스코프라 리셋하지 않으면 다음 정산방 입장 시 이전(완료됐거나 다른 사용자의)
     * settlementId·멤버 상태가 그대로 재사용될 위험이 있다.
     */
    fun reset() {
        settlementId = null
        expectedCount = 0
        _settlement.value = null
        _myNickname.value = null
        _myAccount.value = null
        myNameOverride = null
        _suggestedName.value = null
        aiNote = ""
        _pickedRounds.value = emptySet()
        aiApplied = false
        adjIdx = 0
    }

    companion object {
        /**
         * 총액을 n명에게 엔빵(원 단위). 나머지는 앞사람부터 1원씩 더해 합이 총액과 정확히 일치한다.
         * 예) 100,000원 / 3명 → [33,334, 33,333, 33,333]
         */
        fun evenSplit(total: Long, n: Int): List<Long> {
            if (n <= 0) return emptyList()
            val base = total / n
            val remainder = (total - base * n).toInt()
            return List(n) { i -> base + if (i < remainder) 1 else 0 }
        }
    }
}
