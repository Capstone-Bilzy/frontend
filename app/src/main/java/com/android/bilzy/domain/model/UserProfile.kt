package com.android.bilzy.domain.model

/** 내 프로필(GET /users/me). */
data class UserProfile(
    val id: String,
    val nickname: String,
    val profileImageUrl: String,
    val provider: String
)
