package com.android.bilzy.domain.repository

import com.android.bilzy.data.model.PaymentStatus
import com.android.bilzy.data.model.SettlementMember
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow

interface SettlementMemberRepository {
    suspend fun getSettlementMembers(settlementId: String): List<SettlementMember>
    fun observeSettlementMembers(settlementId: String): Flow<List<SettlementMember>>
    suspend fun saveSettlementMember(member: SettlementMember): String
    suspend fun updatePaymentStatus(id: String, status: PaymentStatus, paidAt: Timestamp? = null)
    suspend fun deleteSettlementMember(id: String)
}
