package com.android.bilzy.ui.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.bilzy.data.local.TokenStore
import com.android.bilzy.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyPageViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val tokenStore: TokenStore
) : ViewModel() {

    data class ProfileUi(val nickname: String, val loginType: String)

    private val _profile = MutableStateFlow<ProfileUi?>(null)
    val profile = _profile.asStateFlow()

    init { load() }

    fun load() {
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
