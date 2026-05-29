package com.android.bilzy.data.remote

import com.android.bilzy.data.remote.dto.AuthResponse
import com.android.bilzy.data.remote.dto.RefreshRequest
import com.android.bilzy.data.remote.dto.SocialLoginRequest
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.POST

/**
 * Bilzy 백엔드(FastAPI) REST 엔드포인트.
 * 기능을 하나씩 옮길 때마다 여기에 엔드포인트를 추가한다.
 */
interface BilzyApi {

    // ── 인증 ─────────────────────────────────────────────
    @POST("auth/social")
    suspend fun socialLogin(@Body body: SocialLoginRequest): AuthResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): AuthResponse

    @HTTP(method = "DELETE", path = "auth/logout", hasBody = false)
    suspend fun logout()
}
