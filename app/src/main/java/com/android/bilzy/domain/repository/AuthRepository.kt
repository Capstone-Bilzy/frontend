package com.android.bilzy.domain.repository

interface AuthRepository {
    /** 소셜 access_token을 백엔드(/auth/social)로 보내 앱 토큰을 발급·저장한다. */
    suspend fun socialLogin(provider: String, accessToken: String)

    /** 이 소셜 계정이 이미 가입한 회원인지. 확인에 실패하면(구버전 서버 등) false — 신규 가입 흐름으로 처리. */
    suspend fun isRegistered(provider: String, accessToken: String): Boolean
    suspend fun logout()

    /** 회원 탈퇴. 서버 삭제가 실패하면 예외를 던지고 로그인 상태를 그대로 둔다. */
    suspend fun withdraw()
    suspend fun isLoggedIn(): Boolean
}
