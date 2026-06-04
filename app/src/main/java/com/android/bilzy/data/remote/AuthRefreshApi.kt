package com.android.bilzy.data.remote

import com.android.bilzy.data.remote.dto.AuthResponse
import com.android.bilzy.data.remote.dto.RefreshRequest
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 토큰 재발급 전용 API. 인증 인터셉터/Authenticator가 붙지 않은
 * 별도 클라이언트로 호출해 401 → refresh → 재시도 루프를 끊는다.
 */
interface AuthRefreshApi {
    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): AuthResponse
}
