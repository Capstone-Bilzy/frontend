package com.android.bilzy.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.bilzy.domain.model.Settlement
import com.android.bilzy.domain.repository.SettlementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 정산내역 상세 화면. settlement_id로 상세(GET /settlements/{id})를 불러온다. */
@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    private val settlementRepository: SettlementRepository
) : ViewModel() {

    private val _settlement = MutableStateFlow<Settlement?>(null)
    val settlement = _settlement.asStateFlow()

    private var loadedId: String? = null

    fun load(id: String) {
        if (id == loadedId && _settlement.value != null) return
        loadedId = id
        viewModelScope.launch {
            runCatching { settlementRepository.getSettlement(id) }
                .onSuccess { _settlement.value = it }
        }
    }
}
