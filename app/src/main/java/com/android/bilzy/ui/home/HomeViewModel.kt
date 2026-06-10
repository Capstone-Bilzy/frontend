package com.android.bilzy.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.bilzy.domain.model.SettlementHistory
import com.android.bilzy.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    /** 홈 "최근 정산 내역"에 보여줄 항목(최대 4개). null=로딩 중. */
    private val _history = MutableStateFlow<List<SettlementHistory>?>(null)
    val history = _history.asStateFlow()

    fun loadHistory() {
        // 캐시가 있으면 먼저 즉시 표시(재진입 깜빡임 제거) 후 네트워크로 갱신.
        userRepository.cachedHistory()?.let { _history.value = it.take(MAX_HOME_ITEMS) }
        viewModelScope.launch {
            runCatching { userRepository.getMyHistory() }
                .onSuccess { _history.value = it.take(MAX_HOME_ITEMS) }
                .onFailure { if (_history.value == null) _history.value = emptyList() }
        }
    }

    private companion object {
        const val MAX_HOME_ITEMS = 4
    }
}
