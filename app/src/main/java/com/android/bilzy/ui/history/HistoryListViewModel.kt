package com.android.bilzy.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.bilzy.domain.model.SettlementHistory
import com.android.bilzy.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    /** 내역을 불러오지 못했을 때 한 번 울린다(화면이 토스트로 알림). 예전엔 실패를 "내역 없음"으로 보여줬다. */
    private val _loadFailed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val loadFailed = _loadFailed.asSharedFlow()

    fun load() {
        // 캐시가 있으면 먼저 즉시 표시(재진입 깜빡임 제거) 후 네트워크로 갱신.
        userRepository.cachedHistory()?.let { _history.value = it }
        viewModelScope.launch {
            runCatching { userRepository.getMyHistory() }
                .onSuccess { _history.value = it }
                .onFailure { _loadFailed.tryEmit(Unit) }
        }
    }
}
