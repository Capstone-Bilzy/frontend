package com.android.bilzy.ui.receipt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.bilzy.domain.model.SavedReceipt
import com.android.bilzy.domain.repository.SavedReceiptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 저장 영수증 보관함 ViewModel.
 * 보관함 목록(Picker)과 저장 화면(Save)이 nav_graph 스코프로 공유해
 * 선택한 이미지·OCR 추정 총액을 화면 간 전달한다.
 */
@HiltViewModel
class SavedReceiptViewModel @Inject constructor(
    private val repository: SavedReceiptRepository
) : ViewModel() {

    // ── 목록 ──────────────────────────────────────────────
    sealed interface ListState {
        data object Loading : ListState
        data class Loaded(val receipts: List<SavedReceipt>) : ListState
        data class Error(val message: String) : ListState
    }

    private val _listState = MutableStateFlow<ListState>(ListState.Loading)
    val listState = _listState.asStateFlow()

    fun loadReceipts() {
        viewModelScope.launch {
            _listState.value = ListState.Loading
            runCatching { repository.getMyReceipts() }
                .onSuccess { _listState.value = ListState.Loaded(it) }
                .onFailure { _listState.value = ListState.Error(it.message ?: "불러오기에 실패했어요") }
        }
    }

    // ── 저장 흐름(Picker에서 이미지 선택 → Save에서 확인·저장) ──
    /** Picker에서 고른 이미지. Save 화면이 미리보기·스캔·저장에 사용. */
    var pendingImage: ByteArray? = null
        private set
    private var pendingMime: String = "image/jpeg"

    fun setPendingImage(bytes: ByteArray, mimeType: String) {
        pendingImage = bytes
        pendingMime = mimeType
        _scanState.value = ScanState.Idle
        _saveState.value = SaveState.Idle
    }

    sealed interface ScanState {
        data object Idle : ScanState
        data object Loading : ScanState
        data class Success(val suggestedTotal: Long) : ScanState
        data class Error(val message: String) : ScanState
    }

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState = _scanState.asStateFlow()

    /** 선택한 이미지를 독립 OCR해 추정 총액을 받는다(저장 전 프리필용). */
    fun runScan() {
        val image = pendingImage ?: return
        if (_scanState.value is ScanState.Loading) return
        viewModelScope.launch {
            _scanState.value = ScanState.Loading
            runCatching { repository.scan(image, pendingMime) }
                .onSuccess { _scanState.value = ScanState.Success(it) }
                .onFailure { _scanState.value = ScanState.Error(it.message ?: "인식에 실패했어요") }
        }
    }

    sealed interface SaveState {
        data object Idle : SaveState
        data object Loading : SaveState
        data object Success : SaveState
        data class Error(val message: String) : SaveState
    }

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState = _saveState.asStateFlow()

    fun save(storeName: String, totalAmount: Long) {
        val image = pendingImage
        if (image == null) {
            _saveState.value = SaveState.Error("저장할 이미지가 없어요")
            return
        }
        if (_saveState.value is SaveState.Loading) return
        viewModelScope.launch {
            _saveState.value = SaveState.Loading
            runCatching { repository.save(image, pendingMime, storeName.trim(), totalAmount) }
                .onSuccess {
                    pendingImage = null
                    _saveState.value = SaveState.Success
                }
                .onFailure { _saveState.value = SaveState.Error(it.message ?: "저장에 실패했어요") }
        }
    }

    fun consumeSaveState() {
        _saveState.value = SaveState.Idle
    }

    // ── 삭제 ──────────────────────────────────────────────
    fun delete(id: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            runCatching { repository.delete(id) }
                .onSuccess { loadReceipts(); onResult(true) }
                .onFailure { onResult(false) }
        }
    }
}
