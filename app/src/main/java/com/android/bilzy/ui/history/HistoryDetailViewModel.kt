package com.android.bilzy.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.bilzy.domain.model.Settlement
import com.android.bilzy.domain.repository.OcrRepository
import com.android.bilzy.domain.repository.SettlementRepository
import com.android.bilzy.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 정산내역 상세 화면. settlement_id로 상세(GET /settlements/{id})를 불러온다. */
@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    private val settlementRepository: SettlementRepository,
    private val ocrRepository: OcrRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    /** 지금 로그인한 유저 id — 요약 줄을 결제자/참여자 중 누구 기준으로 보여줄지 정하는 데 쓴다. */
    private val _myUserId = MutableStateFlow(userRepository.cachedProfile()?.id)
    val myUserId = _myUserId.asStateFlow()

    init {
        if (_myUserId.value == null) {
            viewModelScope.launch {
                _myUserId.value = runCatching { userRepository.getMyProfile() }.getOrNull()?.id
            }
        }
    }

    private val _settlement = MutableStateFlow<Settlement?>(null)
    val settlement = _settlement.asStateFlow()

    /** 상세를 불러오지 못했을 때 한 번 울린다(화면이 로딩 표시를 내리고 토스트로 알림). */
    private val _loadFailed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val loadFailed = _loadFailed.asSharedFlow()

    private var loadedId: String? = null

    fun load(id: String) {
        if (id == loadedId && _settlement.value != null) return
        loadedId = id
        viewModelScope.launch {
            runCatching { settlementRepository.getSettlement(id) }
                .onSuccess { _settlement.value = it }
                .onFailure { _loadFailed.tryEmit(Unit) }
        }
    }

    /** 완료된 정산방에도 쓸 수 있는 순수 사진 첨부(OCR·금액 계산 없음). 성공하면 상세를 다시 불러온다. */
    suspend fun attachPhoto(imageBytes: ByteArray, mimeType: String): Boolean {
        val id = loadedId ?: return false
        return runCatching { ocrRepository.attachPhoto(id, imageBytes, mimeType) }
            .onSuccess {
                runCatching { settlementRepository.getSettlement(id) }.onSuccess { _settlement.value = it }
            }
            .isSuccess
    }
}
