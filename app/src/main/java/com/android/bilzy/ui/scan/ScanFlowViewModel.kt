package com.android.bilzy.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.bilzy.domain.model.ReceiptItemDraft
import com.android.bilzy.domain.model.ScannedReceipt
import com.android.bilzy.domain.model.Settlement
import com.android.bilzy.domain.model.SettlementStatus
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

    /** 현재 진행 중인 정산방 id (스캔/확정 시 지연 생성되어 채워짐) */
    var settlementId: String? = null
        private set
    /** 사용자가 OCR 결과 화면에서 정한 모임 이름. 정해지기 전엔 비어 있음. */
    var settlementTitle: String = ""
        private set

    /**
     * OCR 결과 화면의 모임 이름 입력값(서버 confirm 여부와 무관하게 유지).
     * "추가 스캔하기"는 confirm()을 호출하지 않아 settlementTitle이 안 채워지므로,
     * 라운드가 넘어가도 화면 프리필이 유지되도록 별도로 들고 있는다.
     */
    var pendingGroupName: String = ""

    /** /ocr/scan 결과(사용자 확인·수정 대상) */
    var scannedReceipt: ScannedReceipt? = null
        private set

    /** 현재 스캔/확정 대상 라운드(영수증). "추가 스캔하기"로 넘어갈 때마다 1씩 증가한다. */
    var currentRound: Int = 1
        private set

    // ── 정산방 지연 생성 ──────────────────────────────────
    /**
     * 정산방이 아직 없으면 임시 제목으로 생성하고 id를 반환한다.
     * 백엔드는 스캔/확정 전에 settlement_id를 요구하지만, 제목 입력 화면을 없애
     * 모임 이름은 OCR 결과 화면에서 받으므로 우선 임시 제목으로 만들고 확정 시 갱신한다.
     */
    private suspend fun ensureSettlementId(): String {
        settlementId?.let { return it }
        val created = settlementRepository.createSettlement(PLACEHOLDER_TITLE)
        settlementId = created.id
        return created.id
    }

    // ── 영수증 스캔(OCR 업로드) ────────────────────────────
    /** ScanCamera에서 확보한 이미지. Recognizing 화면이 소비해 업로드한다. */
    private var pendingImage: Pair<ByteArray, String>? = null

    /** 마지막으로 촬영/선택한 영수증 이미지(저장 확인 화면 미리보기용). 업로드 후에도 유지된다. */
    var capturedImage: ByteArray? = null
        private set

    fun setPendingImage(bytes: ByteArray, mimeType: String) {
        pendingImage = bytes to mimeType
        capturedImage = bytes
    }

    /**
     * 영수증 저장 화면에서 '다시 찍기'를 누르면 호출. 스캔 때 정산건에 올라간 영수증 이미지를 서버에서 지운다.
     * 실패해도 흐름은 막지 않는다(재촬영 시 새 이미지가 덮어씀).
     */
    fun discardReceiptImage() {
        val id = settlementId ?: return
        viewModelScope.launch { runCatching { settlementRepository.deleteReceiptImage(id, currentRound) } }
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
        val image = pendingImage
        if (image == null) {
            _scanState.value = ScanState.Error("스캔할 이미지가 없습니다")
            return
        }
        viewModelScope.launch {
            _scanState.value = ScanState.Loading
            runCatching {
                val sid = ensureSettlementId()
                ocrRepository.scan(sid, currentRound, image.first, image.second)
            }
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

    /** 직접 입력 화면의 인라인 편집(품목명/수량/가격)을 반영한다. */
    fun updateItem(index: Int, item: ReceiptItemDraft) {
        _items.value = _items.value.toMutableList().also { if (index in it.indices) it[index] = item }
    }

    sealed interface ConfirmState {
        data object Idle : ConfirmState
        data object Loading : ConfirmState
        data object Success : ConfirmState
        data class Error(val message: String) : ConfirmState
    }

    private val _confirmState = MutableStateFlow<ConfirmState>(ConfirmState.Idle)
    val confirmState = _confirmState.asStateFlow()

    /** 마지막 confirm() 호출이 최종 라운드였는지 — 화면이 성공 후 분기(라운드 이동 vs 목록 이동)에 사용. */
    var lastConfirmWasFinalRound: Boolean = true
        private set

    /**
     * 수정된 항목을 /ocr/confirm으로 이 라운드(receipt)에 확정한다. 다른 라운드는 건드리지 않는다.
     * title이 비어있지 않으면 정산방 제목으로 저장(PATCH)한다.
     * isFinalRound=true면 확정 후 정산방 상태를 waiting으로 넘겨 스캔 단계를 마감한다
     * (/ocr/confirm은 더 이상 자동으로 status를 바꾸지 않으므로 여러 라운드를 계속 스캔할 수 있다).
     */
    fun confirm(title: String = "", storeName: String = "", isFinalRound: Boolean = true) {
        if (_confirmState.value == ConfirmState.Loading) return
        if (_items.value.isEmpty()) {
            _confirmState.value = ConfirmState.Error("항목이 최소 1개는 필요해요")
            return
        }
        viewModelScope.launch {
            _confirmState.value = ConfirmState.Loading
            lastConfirmWasFinalRound = isFinalRound
            runCatching {
                val sid = ensureSettlementId()
                val cleanTitle = title.trim()
                if (cleanTitle.isNotEmpty() && cleanTitle != settlementTitle) {
                    settlementRepository.updateTitle(sid, cleanTitle)
                    settlementTitle = cleanTitle
                }
                ocrRepository.confirm(sid, currentRound, storeName.trim(), _items.value)
                if (isFinalRound) {
                    settlementRepository.updateStatus(sid, SettlementStatus.WAITING)
                }
            }
                .onSuccess { _confirmState.value = ConfirmState.Success }
                .onFailure { _confirmState.value = ConfirmState.Error(it.message ?: "확정에 실패했습니다") }
        }
    }

    fun consumeConfirmState() {
        _confirmState.value = ConfirmState.Idle
    }

    /** 다음 영수증을 스캔하기 전 현재 편집 화면 상태를 비운다. */
    fun resetForNextScan() {
        _items.value = emptyList()
        scannedReceipt = null
    }

    /** "추가 스캔하기": 이번 라운드는 이미 confirm()으로 서버에 반영됐다는 전제하에 다음 라운드로 넘어간다. */
    fun advanceToNextRound() {
        currentRound += 1
        resetForNextScan()
    }

    // ── 다차 정산(n차) 영수증 목록 화면용 ────────────────────
    private val _settlement = MutableStateFlow<Settlement?>(null)
    val settlement = _settlement.asStateFlow()

    /** 다차 정산 영수증 목록 화면의 "영수증 저장하기" 완료 여부(서버 호출 없는 순수 표시 상태). */
    var receiptListSaved: Boolean = false

    /** ReceiptListFragment 진입 시 정산방 상세(receipts 포함)를 다시 불러온다. */
    fun loadSettlement() {
        val id = settlementId ?: return
        viewModelScope.launch {
            runCatching { settlementRepository.getSettlement(id) }
                .onSuccess { _settlement.value = it }
        }
    }

    /**
     * 정산 완료 후 홈으로 돌아가거나 로그아웃할 때 호출해 이 ViewModel 전체를 초기 상태로 되돌린다.
     * nav_graph 스코프(Activity 생명주기 동안 유지)라 리셋하지 않으면 settlementId 등 이전
     * (완료됐거나 다른 사용자의) 정산방 상태가 다음 스캔에 그대로 재사용될 위험이 있다.
     */
    fun reset() {
        settlementId = null
        settlementTitle = ""
        pendingGroupName = ""
        scannedReceipt = null
        currentRound = 1
        pendingImage = null
        capturedImage = null
        _scanState.value = ScanState.Idle
        _items.value = emptyList()
        _confirmState.value = ConfirmState.Idle
        lastConfirmWasFinalRound = true
        _settlement.value = null
        receiptListSaved = false
    }

    private companion object {
        /** 모임 이름을 정하기 전 정산방 생성에 쓰는 임시 제목. 확정 시 사용자 입력으로 교체됨. */
        const val PLACEHOLDER_TITLE = "정산"
    }
}
