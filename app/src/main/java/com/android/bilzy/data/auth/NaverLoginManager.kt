package com.android.bilzy.data.auth

import android.content.Context
import android.util.Log
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.NidOAuthLogin
import com.navercorp.nid.oauth.OAuthLoginCallback
import com.navercorp.nid.profile.NidProfileCallback
import com.navercorp.nid.profile.data.NidProfile
import com.navercorp.nid.profile.data.NidProfileResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 네이버 아이디로그인 SDK를 코루틴으로 감싼다.
 * 인증 성공 시 네이버 access_token을 반환한다. 이 토큰을 백엔드 /auth/social 로 넘긴다.
 * 닉네임/프로필사진은 백엔드가 /auth/social에서 직접 조회해 채워주므로,
 * 클라에서 프로필 조회가 실패해도 로그인 자체는 막지 않는다.
 */
@Singleton
class NaverLoginManager @Inject constructor(
    @ApplicationContext private val appContext: Context
) {

    /** 네이버 인증 결과(토큰 + 표시용 프로필, 조회 실패 시 null). */
    data class NaverAuth(
        val accessToken: String,
        val nickname: String?,
        val profileImageUrl: String?
    )

    /**
     * @param context 로그인 UI를 띄울 Activity context
     * SDK가 초기화되지 않았으면(NAVER_CLIENT_ID 미설정) authenticate()가 콜백을 아예 호출하지 않고
     * 무한 대기하므로, 여기서 먼저 걸러 즉시 실패시킨다.
     */
    suspend fun login(context: Context): String {
        if (!NaverIdLoginSDK.isInitialized()) {
            throw IllegalStateException("네이버 로그인이 설정되지 않았어요. local.properties의 NAVER_CLIENT_ID를 확인해 주세요")
        }
        return authenticate(context)
    }

    private suspend fun authenticate(context: Context): String = suspendCancellableCoroutine { cont ->
        val callback = object : OAuthLoginCallback {
            override fun onSuccess() {
                val token = NaverIdLoginSDK.getAccessToken()
                if (token != null) {
                    cont.resume(token)
                } else {
                    cont.resumeWithException(IllegalStateException("네이버 로그인 결과가 비어 있습니다"))
                }
            }

            override fun onFailure(httpStatus: Int, message: String) {
                cont.resumeWithException(IllegalStateException("네이버 로그인 실패: $message"))
            }

            override fun onError(errorCode: Int, message: String) {
                cont.resumeWithException(IllegalStateException("네이버 로그인 오류: $message"))
            }
        }
        NaverIdLoginSDK.authenticate(context, callback)
    }

    /** OAuth 후 네이버 프로필(닉네임·프로필이미지)까지 조회해서 반환. 회원가입 흐름용. */
    suspend fun loginAndProfile(context: Context): NaverAuth {
        val token = login(context)
        val profile = fetchProfile()
        return NaverAuth(token, profile?.nickname, profile?.profileImage)
    }

    /** callProfileApi()로 (닉네임, 프로필이미지) 조회. 실패하면 null. */
    private suspend fun fetchProfile(): NidProfile? =
        suspendCancellableCoroutine { cont ->
            NidOAuthLogin().callProfileApi(object : NidProfileCallback<NidProfileResponse> {
                override fun onSuccess(result: NidProfileResponse) {
                    cont.resume(result.profile)
                }

                override fun onFailure(httpStatus: Int, message: String) {
                    Log.w(TAG, "프로필 조회 실패($httpStatus): $message")
                    cont.resume(null)
                }

                override fun onError(errorCode: Int, message: String) {
                    Log.w(TAG, "프로필 조회 오류($errorCode): $message")
                    cont.resume(null)
                }
            })
        }

    fun logout() {
        NaverIdLoginSDK.logout()
    }

    private companion object {
        const val TAG = "NaverLoginManager"
    }
}
