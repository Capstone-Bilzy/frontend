package com.android.bilzy.data.repository

import com.android.bilzy.data.auth.KakaoLoginManager
import com.android.bilzy.data.local.TokenStore
import com.android.bilzy.data.remote.BilzyApi
import com.android.bilzy.data.remote.dto.SocialLoginRequest
import com.android.bilzy.domain.repository.AuthRepository
import com.android.bilzy.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: BilzyApi,
    private val tokenStore: TokenStore,
    private val kakaoLoginManager: KakaoLoginManager,
    private val userRepository: UserRepository
) : AuthRepository {

    override suspend fun socialLogin(provider: String, accessToken: String) {
        val res = api.socialLogin(SocialLoginRequest(provider, accessToken))
        tokenStore.saveTokens(res.accessToken, res.refreshToken)
        // 카카오 미동의 시 닉네임이 "사용자" 폴백으로 옴 — 사용자가 직접 정한 이름이 이미 있으면 덮어쓰지 않는다.
        res.user?.nickname?.takeIf { it.isNotBlank() }?.let { incoming ->
            val isGeneric = incoming == "사용자" || incoming == "참여자"
            val existing = tokenStore.currentNickname()
            if (!isGeneric || existing.isNullOrBlank()) tokenStore.saveNickname(incoming)
        }
    }

    override suspend fun logout() {
        runCatching { api.logout() }
        runCatching { kakaoLoginManager.logout() }
        tokenStore.clear()
        userRepository.clearCache()
    }

    override suspend fun isLoggedIn(): Boolean = tokenStore.isLoggedIn()
}
