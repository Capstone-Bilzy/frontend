package com.android.bilzy.data.repository

import com.android.bilzy.data.model.Settlement
import com.android.bilzy.data.model.SettlementStatus
import com.android.bilzy.data.model.toMap
import com.android.bilzy.data.model.toSettlement
import com.android.bilzy.domain.repository.SettlementRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettlementRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : SettlementRepository {

    private val collection = firestore.collection("settlements")

    override suspend fun getSettlement(id: String): Settlement? =
        collection.document(id).get().await()
            .takeIf { it.exists() }?.toSettlement()

    override suspend fun getSettlements(creatorUid: String): List<Settlement> =
        collection.whereEqualTo("creator_uid", creatorUid)
            .orderBy("created_at", Query.Direction.DESCENDING)
            .get().await()
            .documents.mapNotNull { it.toSettlement() }

    override fun observeSettlements(creatorUid: String): Flow<List<Settlement>> = callbackFlow {
        val listener = collection.whereEqualTo("creator_uid", creatorUid)
            .orderBy("created_at", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents?.mapNotNull { it.toSettlement() } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun saveSettlement(settlement: Settlement): String {
        val docRef = if (settlement.id.isEmpty()) collection.document()
                     else collection.document(settlement.id)
        val now = Timestamp.now()
        val map = settlement.toMap().toMutableMap()
        if (settlement.id.isEmpty()) map["created_at"] = now
        map["updated_at"] = now
        docRef.set(map).await()
        return docRef.id
    }

    override suspend fun updateSettlement(id: String, updates: Map<String, Any>) {
        val mutableUpdates = updates.toMutableMap()
        mutableUpdates["updated_at"] = Timestamp.now()
        collection.document(id).update(mutableUpdates).await()
    }

    override suspend fun updateStatus(id: String, status: SettlementStatus) {
        collection.document(id).update(
            "status", status.value,
            "updated_at", Timestamp.now()
        ).await()
    }

    override suspend fun deleteSettlement(id: String) {
        collection.document(id).delete().await()
    }
}
