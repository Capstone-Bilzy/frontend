package com.android.bilzy.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.bilzy.data.demo.DemoData
import com.android.bilzy.domain.model.Settlement
import com.android.bilzy.domain.repository.OcrRepository
import com.android.bilzy.domain.repository.SettlementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 정산내역 상세 화면. settlement_id로 상세(GET /settlements/{id})를 불러온다. */
@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    private val settlementRepository: SettlementRepository,
    private val ocrRepository: OcrRepository
) : ViewModel() {

    private val _settlement = MutableStateFlow<Settlement?>(null)
    val settlement = _settlement.asStateFlow()

    private var loadedId: String? = null

    fun load(id: String) {
        if (id == loadedId && _settlement.value != null) return
        loadedId = id
        if (id == DemoData.DEMO_ID) {
            _settlement.value = DemoData.settlement
            return
        }
        viewModelScope.launch {
            runCatching { settlementRepository.getSettlement(id) }
                .onSuccess { _settlement.value = it }
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
