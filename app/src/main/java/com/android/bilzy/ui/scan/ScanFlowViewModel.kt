package com.android.bilzy.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.bilzy.domain.model.ReceiptItemDraft
import com.android.bilzy.domain.model.ScannedReceipt
import com.android.bilzy.domain.repository.OcrRepository
import com.android.bilzy.domain.repository.SettlementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 영수증 스캔 → OCR → 확정에 걸친 화면들이 공유하는 ViewModel.
 * nav_graph 스코프(hiltNavGraphViewModels(R.id.nav_graph))로 주입해 settlement_id·스캔결과를 공유한다.
 */
@HiltViewModel
class ScanFlowViewModel @Inject constructor(
    private val settlementRepository: SettlementRepository,
    private val ocrRepository: OcrRepository
) : ViewModel() {

    /** 현재 진행 중인 정산방 id (createSettlement 성공 후 채워짐) */
    var settlementId: String? = null
        private set
    var settlementTitle: String = ""
        private set

    /** /ocr/scan 결과(사용자 확인·수정 대상) */
    var scannedReceipt: ScannedReceipt? = null
        private set

    // ── 정산방 생성 ───────────────────────────────────────
    sealed interface CreateState {
        data object Idle : CreateState
        data object Loading : CreateState
        data class Created(val id: String) : CreateState
        data class Error(val message: String) : CreateState
    }

    private val _createState = MutableStateFlow<CreateState>(CreateState.Idle)
    val createState = _createState.asStateFlow()

    fun createSettlement(title: String) {
        if (_createState.value == CreateState.Loading) return
        viewModelScope.launch {
            _createState.value = CreateState.Loading
            runCatching { settlementRepository.createSettlement(title) }
                .onSuccess {
                    settlementId = it.id
                    settlementTitle = it.title
                    _createState.value = CreateState.Created(it.id)
                }
                .onFailure { _createState.value = CreateState.Error(it.message ?: "정산방 생성에 실패했습니다") }
        }
    }

    fun consumeCreateState() {
        _createState.value = CreateState.Idle
    }

    // ── 영수증 스캔(OCR 업로드) ────────────────────────────
    /** ScanCamera에서 확보한 이미지. Recognizing 화면이 소비해 업로드한다. */
    private var pendingImage: Pair<ByteArray, String>? = null

    fun setPendingImage(bytes: ByteArray, mimeType: String) {
        pendingImage = bytes to mimeType
    }

    sealed interface ScanState {
        data object Idle : ScanState
        data object Loading : ScanState
        data object Success : ScanState
        data class Error(val message: String) : ScanState
    }

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState = _scanState.asStateFlow()

    /** 보관된 이미지를 /ocr/scan에 업로드한다. 성공 시 scannedReceipt가 채워진다. */
    fun runScan() {
        if (_scanState.value == ScanState.Loading) return
        val sid = settlementId
        val image = pendingImage
        if (sid == null || image == null) {
            _scanState.value = ScanState.Error("스캔할 이미지가 없습니다")
            return
        }
        viewModelScope.launch {
            _scanState.value = ScanState.Loading
            runCatching { ocrRepository.scan(sid, image.first, image.second) }
                .onSuccess {
                    scannedReceipt = it
                    _items.value = it.items
                    pendingImage = null
                    _scanState.value = ScanState.Success
                }
                .onFailure { _scanState.value = ScanState.Error(it.message ?: "영수증 인식에 실패했습니다") }
        }
    }

    fun consumeScanState() {
        _scanState.value = ScanState.Idle
    }

    // ── OCR 결과 항목 편집 + 확정 ──────────────────────────
    private val _items = MutableStateFlow<List<ReceiptItemDraft>>(emptyList())
    val items = _items.asStateFlow()

    /** 화면에 보이는 항목들의 총액(단가×수량 합) — 백엔드 confirm 계산과 동일. */
    fun currentTotal(): Long = _items.value.sumOf { it.subtotal }

    fun addItem(name: String, price: Long, quantity: Int) {
        _items.value = _items.value + ReceiptItemDraft(name, price, quantity)
    }

    fun removeItem(index: Int) {
        _items.value = _items.value.toMutableList().also { if (index in it.indices) it.removeAt(index) }
    }

    sealed interface ConfirmState {
        data object Idle : ConfirmState
        data object Loading : ConfirmState
        data object Success : ConfirmState
        data class Error(val message: String) : ConfirmState
    }

    private val _confirmState = MutableStateFlow<ConfirmState>(ConfirmState.Idle)
    val confirmState = _confirmState.asStateFlow()

    /** 수정된 항목을 /ocr/confirm으로 확정한다(status=waiting). */
    fun confirm() {
        if (_confirmState.value == ConfirmState.Loading) return
        val sid = settlementId
        if (sid == null) {
            _confirmState.value = ConfirmState.Error("정산방 정보가 없습니다")
            return
        }
        if (_items.value.isEmpty()) {
            _confirmState.value = ConfirmState.Error("항목이 최소 1개는 필요해요")
            return
        }
        viewModelScope.launch {
            _confirmState.value = ConfirmState.Loading
            runCatching { ocrRepository.confirm(sid, _items.value) }
                .onSuccess { _confirmState.value = ConfirmState.Success }
                .onFailure { _confirmState.value = ConfirmState.Error(it.message ?: "확정에 실패했습니다") }
        }
    }

    fun consumeConfirmState() {
        _confirmState.value = ConfirmState.Idle
    }
}
