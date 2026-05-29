package com.android.bilzy.data.remote

import com.android.bilzy.data.local.TokenStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 저장된 액세스 토큰이 있으면 모든 요청에 `Authorization: Bearer <token>` 헤더를 붙인다.
 * 이미 헤더가 지정된 요청(예: 로그인)은 건드리지 않는다.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.header("Authorization") != null) {
            return chain.proceed(request)
        }
        val token = runBlocking { tokenStore.currentAccessToken() }
        val newRequest = if (!token.isNullOrEmpty()) {
            request.newBuilder().header("Authorization", "Bearer $token").build()
        } else {
            request
        }
        return chain.proceed(newRequest)
    }
}
