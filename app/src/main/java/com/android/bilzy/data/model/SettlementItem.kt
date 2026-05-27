package com.android.bilzy.data.model

import com.google.firebase.firestore.DocumentSnapshot

data class SettlementItem(
    val id: String = "",
    val settlementId: String = "",
    val itemName: String = "",
    val price: Long = 0L,
    val quantity: Int = 1,
    val assignedMemberId: String = ""
)

fun DocumentSnapshot.toSettlementItem() = SettlementItem(
    id = id,
    settlementId = getString("settlement_id") ?: "",
    itemName = getString("item_name") ?: "",
    price = getLong("price") ?: 0L,
    quantity = (getLong("quantity") ?: 1L).toInt(),
    assignedMemberId = getString("assigned_member_id") ?: ""
)

fun SettlementItem.toMap(): Map<String, Any?> = mapOf(
    "settlement_id" to settlementId,
    "item_name" to itemName,
    "price" to price,
    "quantity" to quantity,
    "assigned_member_id" to assignedMemberId
)
