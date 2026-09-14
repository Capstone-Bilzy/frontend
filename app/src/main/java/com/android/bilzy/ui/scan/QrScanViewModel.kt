package com.android.bilzy.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.bilzy.data.local.TokenStore
import com.android.bilzy.domain.repository.SettlementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import javax.inject.Inject

/** QR로 인식한 정산방에 참여(join)하는 로직. */
@HiltViewModel
class QrScanViewModel @Inject constructor(
    private val settlementRepository: SettlementRepository,
    private val tokenStore: TokenStore,
    private val json: Json
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

    /** 카카오 닉네임 미동의 등으로 기본값 닉네임만 있어 이름 입력 화면을 거쳐야 하는지 여부. */
    suspend fun needsNicknamePrompt(): Boolean {
        val nickname = tokenStore.currentNickname()?.trim()
        return nickname.isNullOrBlank() || nickname == "사용자" || nickname == "참여자"
    }

    /**
     * QR/딥링크에서 추출한 settlement_id로 참여를 시도한다. inviteToken은 신규 참여자만 필요.
     * [nickname]이 주어지면(직접 입력받은 이름) 그 값을 저장 후 사용하고, 없으면 기존 저장된 닉네임을 쓴다.
     */
    fun join(settlementId: String, inviteToken: String? = null, nickname: String? = null) {
        if (_joinState.value == JoinState.Loading) return
        if (settlementId == joinedSettlementId) return // 같은 QR 중복 인식 무시
        joinedSettlementId = settlementId
        viewModelScope.launch {
            _joinState.value = JoinState.Loading
            runCatching {
                val resolvedNickname = if (nickname != null) {
                    tokenStore.saveNickname(nickname)
                    nickname
                } else {
                    tokenStore.currentNickname()?.takeIf { it.isNotBlank() } ?: "참여자"
                }
                settlementRepository.joinByQr(settlementId, resolvedNickname, inviteToken)
            }
                .onSuccess { _joinState.value = JoinState.Success(settlementId) }
                .onFailure { e ->
                    val errorCode = (e as? HttpException)?.errorCode()
                    // 409 = 이미 참여 중 → 정상 입장으로 처리(호스트가 직접 입장하는 시나리오 포함).
                    // 단, SETTLEMENT_DONE도 409라서 error_code가 없을 때만 이 케이스로 본다.
                    if (e is HttpException && e.code() == 409 && errorCode == null) {
                        _joinState.value = JoinState.Success(settlementId)
                    } else {
                        joinedSettlementId = null // 재시도 허용
                        _joinState.value = JoinState.Error(e.toJoinErrorMessage(errorCode))
                    }
                }
        }
    }

    fun consumeState() {
        _joinState.value = JoinState.Idle
    }

    /** join 실패 응답의 `error_code`를 사용자 문구로 매핑. 파싱 실패 시 일반 에러로 폴백. */
    private fun Throwable.toJoinErrorMessage(errorCode: String?): String = when (errorCode) {
        "INVITE_TOKEN_EXPIRED" -> "초대 링크가 만료됐어요. 방장에게 새 링크를 요청해주세요"
        "INVITE_TOKEN_MISSING", "INVITE_TOKEN_INVALID" -> "유효하지 않은 초대 링크예요"
        "SETTLEMENT_DONE" -> "이미 완료된 정산방이에요"
        else -> message ?: "정산방 참여에 실패했어요"
    }

    /** `{"detail": {"error_code": "...", "message": "..."}}` 형식 에러 바디에서 error_code만 뽑는다. */
    private fun HttpException.errorCode(): String? {
        val body = response()?.errorBody()?.string() ?: return null
        return runCatching { json.decodeFromString<JoinErrorResponse>(body) }
            .getOrNull()?.detail?.errorCode
    }

    @Serializable
    private data class JoinErrorResponse(val detail: JoinErrorDetail? = null)

    @Serializable
    private data class JoinErrorDetail(
        @SerialName("error_code") val errorCode: String? = null,
        val message: String? = null
    )
}
