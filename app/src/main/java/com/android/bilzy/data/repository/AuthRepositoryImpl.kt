package com.android.bilzy.data.repository

import com.android.bilzy.data.auth.KakaoLoginManager
import com.android.bilzy.data.local.TokenStore
import com.android.bilzy.data.remote.BilzyApi
import com.android.bilzy.data.remote.dto.SocialLoginRequest
import com.android.bilzy.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: BilzyApi,
    private val tokenStore: TokenStore,
    private val kakaoLoginManager: KakaoLoginManager
) : AuthRepository {

    override suspend fun socialLogin(provider: String, accessToken: String) {
        val res = api.socialLogin(SocialLoginRequest(provider, accessToken))
        tokenStore.saveTokens(res.accessToken, res.refreshToken)
        res.user?.nickname?.takeIf { it.isNotBlank() }?.let { tokenStore.saveNickname(it) }
    }

    override suspend fun logout() {
        runCatching { api.logout() }
        runCatching { kakaoLoginManager.logout() }
        tokenStore.clear()
    }

    override suspend fun isLoggedIn(): Boolean = tokenStore.isLoggedIn()
}
