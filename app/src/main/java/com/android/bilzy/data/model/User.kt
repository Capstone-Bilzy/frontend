package com.android.bilzy.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class User(
    val uid: String = "",
    val email: String = "",
    val nickname: String = "",
    val profileImageUrl: String = "",
    val language: String = "ko",
    val createdAt: Timestamp? = null,
    val lastLoginAt: Timestamp? = null
)

fun DocumentSnapshot.toUser() = User(
    uid = id,
    email = getString("email") ?: "",
    nickname = getString("nickname") ?: "",
    profileImageUrl = getString("profile_image_url") ?: "",
    language = getString("language") ?: "ko",
    createdAt = getTimestamp("created_at"),
    lastLoginAt = getTimestamp("last_login_at")
)

fun User.toMap(): Map<String, Any?> = mapOf(
    "email" to email,
    "nickname" to nickname,
    "profile_image_url" to profileImageUrl,
    "language" to language,
    "created_at" to createdAt,
    "last_login_at" to lastLoginAt
)
