package com.android.bilzy.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.bilzy.data.auth.KakaoLoginManager
import com.android.bilzy.data.auth.NaverLoginManager
import com.android.bilzy.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val kakaoLoginManager: KakaoLoginManager,
    private val naverLoginManager: NaverLoginManager,
    private val authRepository: AuthRepository
) : ViewModel() {

    sealed interface LoginState {
        data object Idle : LoginState
        data object Loading : LoginState
        data object Success : LoginState
        data class Error(val message: String) : LoginState
    }

    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state = _state.asStateFlow()

    fun loginWithKakao(context: Context) {
        if (_state.value == LoginState.Loading) return
        viewModelScope.launch {
            _state.value = LoginState.Loading
            runCatching {
                val kakaoToken = kakaoLoginManager.login(context)
                authRepository.socialLogin(provider = "kakao", accessToken = kakaoToken)
            }.onSuccess {
                _state.value = LoginState.Success
            }.onFailure { e ->
                _state.value = LoginState.Error(e.message ?: "로그인에 실패했습니다")
            }
        }
    }

    fun loginWithNaver(context: Context) {
        if (_state.value == LoginState.Loading) return
        viewModelScope.launch {
            _state.value = LoginState.Loading
            runCatching {
                val naverToken = naverLoginManager.login(context)
                authRepository.socialLogin(provider = "naver", accessToken = naverToken)
            }.onSuccess {
                _state.value = LoginState.Success
            }.onFailure { e ->
                _state.value = LoginState.Error(e.message ?: "로그인에 실패했습니다")
            }
        }
    }

    /** 에러 메시지 소비 후 Idle로 복귀 */
    fun consumeState() {
        _state.value = LoginState.Idle
    }
}
