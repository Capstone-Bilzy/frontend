package com.android.bilzy.data.auth

import android.content.Context
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.user.UserApiClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 카카오 SDK 로그인을 코루틴으로 감싼다.
 * 카카오톡 앱이 있으면 톡으로, 없으면 카카오계정(웹)으로 로그인하고
 * 성공 시 카카오 access_token을 반환한다. 이 토큰을 백엔드 /auth/social 로 넘긴다.
 */
@Singleton
class KakaoLoginManager @Inject constructor(
    @ApplicationContext private val appContext: Context
) {

    /** @param context 로그인 UI를 띄울 Activity context */
    suspend fun login(context: Context): String = suspendCancellableCoroutine { cont ->
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            when {
                error != null -> cont.resumeWithException(error)
                token != null -> cont.resume(token.accessToken)
                else -> cont.resumeWithException(IllegalStateException("카카오 로그인 결과가 비어 있습니다"))
            }
        }

        if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
            UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                // 카카오톡 로그인이 실패하면 카카오계정 로그인으로 폴백
                if (error != null) {
                    UserApiClient.instance.loginWithKakaoAccount(context, callback = callback)
                } else {
                    callback(token, null)
                }
            }
        } else {
            UserApiClient.instance.loginWithKakaoAccount(context, callback = callback)
        }
    }

    suspend fun logout(): Unit = suspendCancellableCoroutine { cont ->
        UserApiClient.instance.logout { error ->
            if (error != null) cont.resumeWithException(error) else cont.resume(Unit)
        }
    }
}
