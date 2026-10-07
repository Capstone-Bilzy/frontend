package com.android.bilzy.data.remote

import com.android.bilzy.data.local.TokenStore
import com.android.bilzy.data.remote.dto.RefreshRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 401 응답을 받으면 저장된 refresh_token으로 /auth/refresh를 호출해
 * 새 액세스 토큰을 발급받고 원래 요청을 한 번 재시도한다.
 * 리프레시 토큰이 없거나 서버가 거절(401/403)하면 저장 토큰을 삭제하고 null을 반환한다.
 * 네트워크 오류 등 일시적 실패는 토큰을 지우지 않고 null만 반환한다(로그인 유지).
 * → 현재 요청은 401 그대로, 다음 앱 실행 시 진입 게이트가 로그인으로 보낸다.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStore: TokenStore,
    private val refreshApi: AuthRefreshApi
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // 이미 한 번 재발급해 재시도한 요청이면 더 시도하지 않는다(무한 루프 방지).
        if (responseCount(response) >= 2) return null

        synchronized(this) {
            val failedToken = response.request.header("Authorization")
                ?.removePrefix("Bearer ")
            val currentToken = runBlocking { tokenStore.currentAccessToken() }

            // 다른 요청이 이미 토큰을 갱신했다면 새 토큰으로 바로 재시도.
            if (!currentToken.isNullOrEmpty() && currentToken != failedToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            val refresh = runBlocking { tokenStore.refreshToken() }
            if (refresh.isNullOrEmpty()) {
                runBlocking { tokenStore.clear() }
                return null
            }

            val newAccess = try {
                runBlocking { refreshApi.refresh(RefreshRequest(refresh)).accessToken }
            } catch (e: HttpException) {
                // 서버가 리프레시 토큰을 거절한 경우(만료·무효·로그아웃됨)에만 세션을 끝낸다.
                if (e.code() == 401 || e.code() == 403) runBlocking { tokenStore.clear() }
                return null
            } catch (e: Exception) {
                // 네트워크 끊김·타임아웃·서버 오류(5xx) 같은 일시적 실패는 로그아웃 사유가 아니다. 토큰을 그대로 두면
                // 다음 요청에서 다시 갱신을 시도한다. 예전엔 여기서도 토큰을 지워서, 서버가 잠깐 느리거나
                // 신호가 약한 순간에 토큰 갱신이 걸리면 사용자가 로그아웃됐다.
                return null
            }
            if (newAccess.isEmpty()) {
                runBlocking { tokenStore.clear() }
                return null
            }

            runBlocking { tokenStore.saveTokens(newAccess, null) }
            return response.request.newBuilder()
                .header("Authorization", "Bearer $newAccess")
                .build()
        }
    }

    private fun responseCount(response: Response): Int {
        var prior = response.priorResponse
        var count = 1
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
