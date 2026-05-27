package com.android.bilzy.domain.repository

import com.android.bilzy.data.model.Settlement
import com.android.bilzy.data.model.SettlementStatus
import kotlinx.coroutines.flow.Flow

interface SettlementRepository {
    suspend fun getSettlement(id: String): Settlement?
    suspend fun getSettlements(creatorUid: String): List<Settlement>
    fun observeSettlements(creatorUid: String): Flow<List<Settlement>>
    suspend fun saveSettlement(settlement: Settlement): String
    suspend fun updateSettlement(id: String, updates: Map<String, Any>)
    suspend fun updateStatus(id: String, status: SettlementStatus)
    suspend fun deleteSettlement(id: String)
}
