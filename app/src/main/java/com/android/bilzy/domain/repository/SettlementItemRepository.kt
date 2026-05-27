package com.android.bilzy.domain.repository

import com.android.bilzy.data.model.SettlementItem

interface SettlementItemRepository {
    suspend fun getSettlementItems(settlementId: String): List<SettlementItem>
    suspend fun saveSettlementItem(item: SettlementItem): String
    suspend fun saveSettlementItems(items: List<SettlementItem>)
    suspend fun updateSettlementItem(id: String, updates: Map<String, Any>)
    suspend fun deleteSettlementItem(id: String)
    suspend fun deleteSettlementItems(settlementId: String)
}
