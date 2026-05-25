package com.android.bilzy.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class Notification(
    val id: String = "",
    val userUid: String = "",
    val settlementId: String = "",
    val type: String = "",
    val message: String = "",
    val isRead: Boolean = false,
    val createdAt: Timestamp? = null
)

fun DocumentSnapshot.toNotification() = Notification(
    id = id,
    userUid = getString("user_uid") ?: "",
    settlementId = getString("settlement_id") ?: "",
    type = getString("type") ?: "",
    message = getString("message") ?: "",
    isRead = getBoolean("is_read") ?: false,
    createdAt = getTimestamp("created_at")
)

fun Notification.toMap(): Map<String, Any?> = mapOf(
    "user_uid" to userUid,
    "settlement_id" to settlementId,
    "type" to type,
    "message" to message,
    "is_read" to isRead,
    "created_at" to createdAt
)
