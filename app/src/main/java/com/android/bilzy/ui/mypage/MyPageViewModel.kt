package com.android.bilzy.ui.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.bilzy.data.local.TokenStore
import com.android.bilzy.domain.repository.AuthRepository
import com.android.bilzy.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyPageViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val tokenStore: TokenStore
) : ViewModel() {

    data class ProfileUi(val nickname: String, val loginType: String)

    private val _profile = MutableStateFlow<ProfileUi?>(null)
    val profile = _profile.asStateFlow()

    /** 로그아웃 완료(토큰 클리어 끝) 1회성 이벤트. 화면은 이걸 받고 온보딩으로 이동. */
    private val _loggedOut = Channel<Unit>(Channel.BUFFERED)
    val loggedOut = _loggedOut.receiveAsFlow()

    private var loggingOut = false

    init { load() }

    /** 백엔드 /auth/logout + 카카오 로그아웃 + 토큰 클리어(실패해도 토큰은 항상 비움). */
    fun logout() {
        if (loggingOut) return
        loggingOut = true
        viewModelScope.launch {
            runCatching { authRepository.logout() }
            _loggedOut.send(Unit)
        }
    }

    fun load() {
        // 캐시가 있으면 먼저 즉시 표시(재진입 깜빡임 제거) 후 네트워크로 갱신.
        userRepository.cachedProfile()?.let {
            _profile.value = ProfileUi(it.nickname.ifBlank { "사용자" }, loginTypeText(it.provider))
        }
        viewModelScope.launch {
            runCatching { userRepository.getMyProfile() }
                .onSuccess {
                    _profile.value = ProfileUi(
                        nickname = it.nickname.ifBlank { "사용자" },
                        loginType = loginTypeText(it.provider)
                    )
                }
                .onFailure {
                    // 네트워크 실패 시 로그인 때 저장해둔 닉네임으로 폴백.
                    val nickname = tokenStore.currentNickname()?.takeIf { it.isNotBlank() } ?: "사용자"
                    _profile.value = ProfileUi(nickname, "")
                }
        }
    }

    private fun loginTypeText(provider: String) = when (provider.lowercase()) {
        "kakao" -> "카카오 계정 로그인"
        "naver" -> "네이버 계정 로그인"
        else -> "소셜 로그인"
    }
}
