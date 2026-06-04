package com.android.bilzy.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 소셜 로그인 요청 바디.  POST /auth/social
 * 앱에서 카카오/네이버 SDK로 로그인 후 받은 access_token을 백엔드로 넘긴다.
 *   { "provider": "kakao", "access_token": "..." }
 * (provider 는 백엔드 enum: "kakao" | "naver")
 */
@Serializable
data class SocialLoginRequest(
    val provider: String,
    @SerialName("access_token") val accessToken: String
)

/** POST /auth/refresh */
@Serializable
data class RefreshRequest(
    @SerialName("refresh_token") val refreshToken: String
)

/**
 * 로그인 응답 (POST /auth/social).  백엔드 auth_service.social_login() 기준:
 *   { "access_token", "refresh_token", "user": { "id", "nickname", "profile_image_url" } }
 * 리프레시(POST /auth/refresh)는 { "access_token" }만 내려주므로 나머지는 null.
 */
@Serializable
data class AuthResponse(
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("refresh_token") val refreshToken: String? = null,
    val user: UserDto? = null
)

/** 백엔드 user 객체. 키는 `id` (uid 아님), 이메일은 내려주지 않음. */
@Serializable
data class UserDto(
    val id: String = "",
    val nickname: String = "",
    @SerialName("profile_image_url") val profileImageUrl: String = "",
    val provider: String = "",
    @SerialName("created_at") val createdAt: String? = null
)

fun UserDto.toProfile() = com.android.bilzy.domain.model.UserProfile(
    id = id,
    nickname = nickname,
    profileImageUrl = profileImageUrl,
    provider = provider
)
