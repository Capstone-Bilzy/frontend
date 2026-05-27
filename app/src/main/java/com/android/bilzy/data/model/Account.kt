package com.android.bilzy.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class Account(
    val id: String = "",
    val userUid: String = "",
    val bankName: String = "",
    val accountNumber: String = "",
    val accountHolder: String = "",
    val isPrimary: Boolean = false,
    val createdAt: Timestamp? = null
)

fun DocumentSnapshot.toAccount() = Account(
    id = id,
    userUid = getString("user_uid") ?: "",
    bankName = getString("bank_name") ?: "",
    accountNumber = getString("account_number") ?: "",
    accountHolder = getString("account_holder") ?: "",
    isPrimary = getBoolean("is_primary") ?: false,
    createdAt = getTimestamp("created_at")
)

fun Account.toMap(): Map<String, Any?> = mapOf(
    "user_uid" to userUid,
    "bank_name" to bankName,
    "account_number" to accountNumber,
    "account_holder" to accountHolder,
    "is_primary" to isPrimary,
    "created_at" to createdAt
)
