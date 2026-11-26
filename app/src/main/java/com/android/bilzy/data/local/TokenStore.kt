package com.android.bilzy.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore by preferencesDataStore(name = "auth")

/**
 * 백엔드(FastAPI)에서 발급한 액세스/리프레시 토큰을 보관한다.
 * 소셜 로그인 후 받은 앱 JWT가 여기에 저장되고, [AuthInterceptor]가 요청마다 읽어 헤더에 싣는다.
 */
@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val accessKey = stringPreferencesKey("access_token")
    private val refreshKey = stringPreferencesKey("refresh_token")
    private val nicknameKey = stringPreferencesKey("nickname")

    /**
     * 앱이 떠 있는 동안 로그인이 풀렸다는 알림(토큰 갱신 거절 등).
     * CONFLATED 채널이라 화면이 백그라운드일 때 생긴 알림도 다음에 화면이 보일 때 한 번 전달된다.
     */
    private val _sessionExpired = Channel<Unit>(Channel.CONFLATED)
    val sessionExpired: Flow<Unit> = _sessionExpired.receiveAsFlow()

    val accessToken: Flow<String?> = context.authDataStore.data.map { it[accessKey] }

    /** 로그인 시 저장한 내 닉네임(정산방 참여 시 사용). */
    suspend fun currentNickname(): String? = context.authDataStore.data.map { it[nicknameKey] }.first()

    suspend fun saveNickname(nickname: String) {
        context.authDataStore.edit { it[nicknameKey] = nickname }
    }

    /** 인터셉터가 매 요청에서 동기적으로 읽기 위한 헬퍼. */
    suspend fun currentAccessToken(): String? = accessToken.first()

    suspend fun isLoggedIn(): Boolean = currentAccessToken().isNullOrEmpty().not()

    suspend fun saveTokens(accessToken: String, refreshToken: String?) {
        context.authDataStore.edit { prefs ->
            prefs[accessKey] = accessToken
            if (refreshToken != null) prefs[refreshKey] = refreshToken
        }
    }

    suspend fun refreshToken(): String? = context.authDataStore.data.map { it[refreshKey] }.first()

    suspend fun clear() {
        context.authDataStore.edit { it.clear() }
    }

    /** 저장 토큰을 지우고 로그인이 풀렸다고 알린다. 직접 로그아웃·탈퇴는 [clear]를 쓴다(알림 없음). */
    suspend fun expireSession() {
        clear()
        _sessionExpired.trySend(Unit)
    }
}
