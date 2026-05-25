package com.android.bilzy.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

enum class SettlementStatus(val value: String) {
    DRAFT("draft"),
    IN_PROGRESS("in_progress"),
    COMPLETED("completed");

    companion object {
        fun fromValue(value: String?) = entries.find { it.value == value } ?: DRAFT
    }
}

data class Settlement(
    val id: String = "",
    val creatorUid: String = "",
    val title: String = "",
    val status: SettlementStatus = SettlementStatus.DRAFT,
    val totalAmount: Long = 0L,
    val ocrImageUrl: String = "",
    val paidCount: Int = 0,
    val totalCount: Int = 0,
    val settledAt: Timestamp? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

fun DocumentSnapshot.toSettlement() = Settlement(
    id = id,
    creatorUid = getString("creator_uid") ?: "",
    title = getString("title") ?: "",
    status = SettlementStatus.fromValue(getString("status")),
    totalAmount = getLong("total_amount") ?: 0L,
    ocrImageUrl = getString("ocr_image_url") ?: "",
    paidCount = (getLong("paid_count") ?: 0L).toInt(),
    totalCount = (getLong("total_count") ?: 0L).toInt(),
    settledAt = getTimestamp("settled_at"),
    createdAt = getTimestamp("created_at"),
    updatedAt = getTimestamp("updated_at")
)

fun Settlement.toMap(): Map<String, Any?> = mapOf(
    "creator_uid" to creatorUid,
    "title" to title,
    "status" to status.value,
    "total_amount" to totalAmount,
    "ocr_image_url" to ocrImageUrl,
    "paid_count" to paidCount,
    "total_count" to totalCount,
    "settled_at" to settledAt,
    "created_at" to createdAt,
    "updated_at" to updatedAt
)
