package com.android.bilzy.domain.repository

interface AuthRepository {
    /** 소셜 access_token을 백엔드(/auth/social)로 보내 앱 토큰을 발급·저장한다. */
    suspend fun socialLogin(provider: String, accessToken: String)
    suspend fun logout()
    suspend fun isLoggedIn(): Boolean
}
