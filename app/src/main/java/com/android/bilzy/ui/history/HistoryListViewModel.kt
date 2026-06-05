package com.android.bilzy.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.bilzy.domain.model.SettlementHistory
import com.android.bilzy.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 정산내역 목록 화면. 전체 내역(GET /users/me/history)을 불러온다. */
@HiltViewModel
class HistoryListViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    /** null=로딩 중, emptyList=내역 없음. */
    private val _history = MutableStateFlow<List<SettlementHistory>?>(null)
    val history = _history.asStateFlow()

    fun load() {
        viewModelScope.launch {
            runCatching { userRepository.getMyHistory() }
                .onSuccess { _history.value = it }
                .onFailure { _history.value = emptyList() }
        }
    }
}
