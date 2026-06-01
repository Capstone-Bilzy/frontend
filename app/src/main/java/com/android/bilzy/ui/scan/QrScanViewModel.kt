package com.android.bilzy.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.bilzy.data.local.TokenStore
import com.android.bilzy.domain.repository.SettlementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** QR로 인식한 정산방에 참여(join)하는 로직. */
@HiltViewModel
class QrScanViewModel @Inject constructor(
    private val settlementRepository: SettlementRepository,
    private val tokenStore: TokenStore
) : ViewModel() {

    /** 인식·참여 중인 정산방 id. 중복 처리 방지용. */
    var joinedSettlementId: String? = null
        private set

    sealed interface JoinState {
        data object Idle : JoinState
        data object Loading : JoinState
        data class Success(val settlementId: String) : JoinState
        data class Error(val message: String) : JoinState
    }

    private val _joinState = MutableStateFlow<JoinState>(JoinState.Idle)
    val joinState = _joinState.asStateFlow()

    /** QR에서 추출한 settlement_id로 참여를 시도한다. */
    fun join(settlementId: String) {
        if (_joinState.value == JoinState.Loading) return
        if (settlementId == joinedSettlementId) return // 같은 QR 중복 인식 무시
        joinedSettlementId = settlementId
        viewModelScope.launch {
            _joinState.value = JoinState.Loading
            runCatching {
                val nickname = tokenStore.currentNickname()?.takeIf { it.isNotBlank() } ?: "참여자"
                settlementRepository.joinByQr(settlementId, nickname)
            }
                .onSuccess { _joinState.value = JoinState.Success(settlementId) }
                .onFailure {
                    joinedSettlementId = null // 재시도 허용
                    _joinState.value = JoinState.Error(it.message ?: "정산방 참여에 실패했어요")
                }
        }
    }

    fun consumeState() {
        _joinState.value = JoinState.Idle
    }
}
