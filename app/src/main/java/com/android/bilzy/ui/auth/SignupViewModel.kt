package com.android.bilzy.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.bilzy.data.auth.KakaoLoginManager
import com.android.bilzy.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 카카오 회원가입 흐름 ViewModel (nav_graph 스코프로 Signup→Terms→Info가 공유).
 *
 * 흐름: ① startKakaoSignup()에서 **OAuth를 먼저** 해 토큰+실제 프로필(닉네임·프로필이미지)을 확보 →
 * ② 동의 화면에 실제 정보 표시 → ③ completeSignup()에서 그 토큰으로 백엔드 가입(/auth/social).
 */
@HiltViewModel
class SignupViewModel @Inject constructor(
    private val kakaoLoginManager: KakaoLoginManager,
    private val authRepository: AuthRepository
) : ViewModel() {

    /** 확보한 카카오 인증 결과(토큰+프로필). */
    private var auth: KakaoLoginManager.KakaoAuth? = null

    val nickname: String? get() = auth?.nickname
    val profileImageUrl: String? get() = auth?.profileImageUrl

    sealed interface PrepareState {
        data object Idle : PrepareState
        data object Loading : PrepareState
        data object Ready : PrepareState     // OAuth 완료, 동의 화면으로 진행 가능
        data class Error(val message: String) : PrepareState
    }

    sealed interface CompleteState {
        data object Idle : CompleteState
        data object Loading : CompleteState
        data object Success : CompleteState  // 백엔드 가입 완료
        data class Error(val message: String) : CompleteState
    }

    private val _prepareState = MutableStateFlow<PrepareState>(PrepareState.Idle)
    val prepareState = _prepareState.asStateFlow()

    private val _completeState = MutableStateFlow<CompleteState>(CompleteState.Idle)
    val completeState = _completeState.asStateFlow()

    /** 1단계: 카카오 OAuth + 프로필 조회(백엔드 가입은 아직 안 함). */
    fun startKakaoSignup(context: Context) {
        if (_prepareState.value == PrepareState.Loading) return
        _prepareState.value = PrepareState.Loading
        viewModelScope.launch {
            runCatching { kakaoLoginManager.loginAndProfile(context) }
                .onSuccess {
                    auth = it
                    _prepareState.value = PrepareState.Ready
                }
                .onFailure { e ->
                    _prepareState.value = PrepareState.Error(e.message ?: "카카오 인증에 실패했어요")
                }
        }
    }

    /** 2단계: 확보한 토큰으로 백엔드 가입/로그인(/auth/social). */
    fun completeSignup() {
        val token = auth?.accessToken
        if (token == null) {
            _completeState.value = CompleteState.Error("카카오 인증 정보가 없어요. 다시 시도해 주세요")
            return
        }
        if (_completeState.value == CompleteState.Loading) return
        _completeState.value = CompleteState.Loading
        viewModelScope.launch {
            runCatching { authRepository.socialLogin(provider = "kakao", accessToken = token) }
                .onSuccess { _completeState.value = CompleteState.Success }
                .onFailure { e ->
                    _completeState.value = CompleteState.Error(e.message ?: "회원가입에 실패했어요")
                }
        }
    }

    fun consumePrepareState() { _prepareState.value = PrepareState.Idle }
    fun consumeCompleteState() { _completeState.value = CompleteState.Idle }
}
