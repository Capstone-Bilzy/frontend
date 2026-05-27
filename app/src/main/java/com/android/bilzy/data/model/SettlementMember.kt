package com.android.bilzy.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

enum class PaymentStatus(val value: String) {
    PENDING("pending"),
    PAID("paid");

    companion object {
        fun fromValue(value: String?) = entries.find { it.value == value } ?: PENDING
    }
}

data class SettlementMember(
    val id: String = "",
    val settlementId: String = "",
    val userUid: String = "",
    val nickname: String = "",
    val amountOwed: Long = 0L,
    val paymentStatus: PaymentStatus = PaymentStatus.PENDING,
    val paymentMethod: String = "",
    val paidAt: Timestamp? = null
)

fun DocumentSnapshot.toSettlementMember() = SettlementMember(
    id = id,
    settlementId = getString("settlement_id") ?: "",
    userUid = getString("user_uid") ?: "",
    nickname = getString("nickname") ?: "",
    amountOwed = getLong("amount_owed") ?: 0L,
    paymentStatus = PaymentStatus.fromValue(getString("payment_status")),
    paymentMethod = getString("payment_method") ?: "",
    paidAt = getTimestamp("paid_at")
)

fun SettlementMember.toMap(): Map<String, Any?> = mapOf(
    "settlement_id" to settlementId,
    "user_uid" to userUid,
    "nickname" to nickname,
    "amount_owed" to amountOwed,
    "payment_status" to paymentStatus.value,
    "payment_method" to paymentMethod,
    "paid_at" to paidAt
)
