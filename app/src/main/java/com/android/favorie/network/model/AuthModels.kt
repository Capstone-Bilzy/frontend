package com.android.favorie.network.model

// POST /auth/kakao 요청
data class KakaoLoginRequest(
    val kakaoAccessToken: String
)

// POST /auth/kakao, /auth/refresh 공통 응답
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String
)

// POST /auth/refresh 요청
data class RefreshTokenRequest(
    val refreshToken: String
)