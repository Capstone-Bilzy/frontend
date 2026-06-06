package com.android.bilzy.ui.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.bilzy.domain.model.BankAccount
import com.android.bilzy.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyPageAccountViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {

    /** 현재 저장된 계좌. null = 아직 로딩 전. */
    private val _account = MutableStateFlow<BankAccount?>(null)
    val account = _account.asStateFlow()

    sealed interface SaveState {
        data object Idle : SaveState
        data object Saving : SaveState
        data class Success(val account: BankAccount) : SaveState
        data class Error(val message: String) : SaveState
    }

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState = _saveState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            runCatching { accountRepository.getMyAccount() }
                .onSuccess { _account.value = it }
                .onFailure { _account.value = BankAccount("", "", "") }
        }
    }

    fun save(bankName: String, accountNumber: String, accountHolder: String) {
        // 백엔드 검증: 계좌번호 10~30자, 은행/명의 1자 이상.
        val number = accountNumber.trim()
        val holder = accountHolder.trim()
        if (holder.isBlank()) {
            _saveState.value = SaveState.Error("계좌 명의를 입력해 주세요")
            return
        }
        if (number.length < 10) {
            _saveState.value = SaveState.Error("계좌번호를 정확히 입력해 주세요")
            return
        }

        _saveState.value = SaveState.Saving
        viewModelScope.launch {
            runCatching {
                accountRepository.saveMyAccount(BankAccount(bankName, number, holder))
            }.onSuccess {
                _account.value = it
                _saveState.value = SaveState.Success(it)
            }.onFailure {
                _saveState.value = SaveState.Error("저장에 실패했어요. 잠시 후 다시 시도해 주세요")
            }
        }
    }

    /** 토스트/네비 처리 후 상태 소비. */
    fun consumeSaveState() {
        _saveState.value = SaveState.Idle
    }

    companion object {
        val BANKS = listOf(
            "신한은행", "국민은행", "우리은행", "하나은행",
            "농협은행", "기업은행", "카카오뱅크", "토스뱅크"
        )
    }
}
