package com.android.favorie.network.model

data class MemberResponse(
    val nickname: String,
    val email: String
)

data class NicknameUpdateRequest(
    val nickname: String
)