package com.android.bilzy.data.auth

import android.content.Context
import android.util.Log
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
 *
 * 닉네임/프로필 사진이 '선택 동의'라 빠져 있으면(이미 연결된 계정은 자동 재요청 안 됨),
 * loginWithNewScopes로 추가 동의를 받아 닉네임이 포함된 토큰을 돌려준다.
 */
@Singleton
class KakaoLoginManager @Inject constructor(
    @ApplicationContext private val appContext: Context
) {

    /** 카카오 인증 결과(토큰 + 표시용 프로필). 회원가입 동의 화면에 실제 정보를 보여줄 때 쓴다. */
    data class KakaoAuth(
        val accessToken: String,
        val nickname: String?,
        val profileImageUrl: String?
    )

    /** @param context 로그인 UI를 띄울 Activity context */
    suspend fun login(context: Context): String {
        val token = basicLogin(context)
        // 닉네임/프로필 동의가 빠져 있으면 추가 동의를 받아 토큰을 갱신(실패해도 기존 토큰으로 진행).
        return ensureProfileConsent(context, token)
    }

    /** OAuth(+동의 보강) 후 카카오 프로필(닉네임·프로필이미지)까지 조회해서 반환. 회원가입 흐름용. */
    suspend fun loginAndProfile(context: Context): KakaoAuth {
        val token = login(context)
        val profile = fetchProfile()
        return KakaoAuth(token, profile.first, profile.second)
    }

    /** me()로 (닉네임, 프로필이미지URL) 조회. 실패하면 (null, null). */
    private suspend fun fetchProfile(): Pair<String?, String?> =
        suspendCancellableCoroutine { cont ->
            UserApiClient.instance.me { user, error ->
                if (error != null || user == null) {
                    cont.resume(null to null)
                } else {
                    val p = user.kakaoAccount?.profile
                    cont.resume(p?.nickname to p?.profileImageUrl)
                }
            }
        }

    /** 카카오톡 → 실패 시 카카오계정 폴백으로 access_token 획득. */
    private suspend fun basicLogin(context: Context): String =
        suspendCancellableCoroutine { cont ->
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

    /**
     * 닉네임/프로필 사진 동의가 빠졌으면 추가 동의를 요청해 갱신된 토큰을 반환.
     * 동의를 거부/실패해도 로그인 자체는 막지 않고 기존 토큰을 그대로 쓴다.
     */
    private suspend fun ensureProfileConsent(context: Context, currentToken: String): String {
        val missing = missingProfileScopes()
        if (missing.isEmpty()) return currentToken
        return try {
            loginWithNewScopes(context, missing)
        } catch (e: Throwable) {
            Log.w(TAG, "추가 동의 실패/취소 — 기존 토큰으로 진행", e)
            currentToken
        }
    }

    /**
     * me() 호출로 추가 동의가 필요한 프로필 scope 목록을 구한다.
     * needsAgreement 플래그가 true이거나 실제 값이 비어 있으면 요청 대상에 넣는다
     * (콘솔에 동의항목이 있는데 값이 안 온 경우까지 커버). 실패하면 빈 목록.
     */
    private suspend fun missingProfileScopes(): List<String> =
        suspendCancellableCoroutine { cont ->
            UserApiClient.instance.me { user, error ->
                if (error != null || user == null) {
                    Log.w(TAG, "me() 실패 — 추가동의 생략", error)
                    cont.resume(emptyList())
                    return@me
                }
                val account = user.kakaoAccount
                val profile = account?.profile
                Log.d(TAG, "me(): nickname=${profile?.nickname} " +
                    "nickNeed=${account?.profileNicknameNeedsAgreement} imgNeed=${account?.profileImageNeedsAgreement}")
                val scopes = buildList {
                    if (account?.profileNicknameNeedsAgreement == true || profile?.nickname.isNullOrBlank()) {
                        add("profile_nickname")
                    }
                    if (account?.profileImageNeedsAgreement == true) add("profile_image")
                }
                cont.resume(scopes)
            }
        }

    /** 지정한 scope에 대한 추가 동의 화면을 띄우고 새 access_token을 반환. */
    private suspend fun loginWithNewScopes(context: Context, scopes: List<String>): String =
        suspendCancellableCoroutine { cont ->
            UserApiClient.instance.loginWithNewScopes(context, scopes) { token, error ->
                when {
                    error != null -> cont.resumeWithException(error)
                    token != null -> cont.resume(token.accessToken)
                    else -> cont.resumeWithException(IllegalStateException("추가 동의 결과가 비어 있습니다"))
                }
            }
        }

    suspend fun logout(): Unit = suspendCancellableCoroutine { cont ->
        UserApiClient.instance.logout { error ->
            if (error != null) cont.resumeWithException(error) else cont.resume(Unit)
        }
    }

    private companion object {
        const val TAG = "KakaoLoginManager"
    }
}
