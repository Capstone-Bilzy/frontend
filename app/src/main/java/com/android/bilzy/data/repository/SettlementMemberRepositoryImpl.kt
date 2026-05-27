package com.android.bilzy.data.repository

import com.android.bilzy.data.model.PaymentStatus
import com.android.bilzy.data.model.SettlementMember
import com.android.bilzy.data.model.toMap
import com.android.bilzy.data.model.toSettlementMember
import com.android.bilzy.domain.repository.SettlementMemberRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettlementMemberRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : SettlementMemberRepository {

    private val collection = firestore.collection("settlement_members")

    override suspend fun getSettlementMembers(settlementId: String): List<SettlementMember> =
        collection.whereEqualTo("settlement_id", settlementId)
            .get().await()
            .documents.mapNotNull { it.toSettlementMember() }

    override fun observeSettlementMembers(settlementId: String): Flow<List<SettlementMember>> = callbackFlow {
        val listener = collection.whereEqualTo("settlement_id", settlementId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents?.mapNotNull { it.toSettlementMember() } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun saveSettlementMember(member: SettlementMember): String {
        val docRef = if (member.id.isEmpty()) collection.document()
                     else collection.document(member.id)
        docRef.set(member.toMap()).await()
        return docRef.id
    }

    override suspend fun updatePaymentStatus(id: String, status: PaymentStatus, paidAt: Timestamp?) {
        collection.document(id).update(
            "payment_status", status.value,
            "paid_at", paidAt
        ).await()
    }

    override suspend fun deleteSettlementMember(id: String) {
        collection.document(id).delete().await()
    }
}
