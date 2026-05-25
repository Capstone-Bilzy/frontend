package com.android.bilzy.data.repository

import com.android.bilzy.data.model.SettlementItem
import com.android.bilzy.data.model.toMap
import com.android.bilzy.data.model.toSettlementItem
import com.android.bilzy.domain.repository.SettlementItemRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettlementItemRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : SettlementItemRepository {

    private val collection = firestore.collection("settlement_items")

    override suspend fun getSettlementItems(settlementId: String): List<SettlementItem> =
        collection.whereEqualTo("settlement_id", settlementId)
            .get().await()
            .documents.mapNotNull { it.toSettlementItem() }

    override suspend fun saveSettlementItem(item: SettlementItem): String {
        val docRef = if (item.id.isEmpty()) collection.document()
                     else collection.document(item.id)
        docRef.set(item.toMap()).await()
        return docRef.id
    }

    override suspend fun saveSettlementItems(items: List<SettlementItem>) {
        val batch = firestore.batch()
        items.forEach { item ->
            val docRef = if (item.id.isEmpty()) collection.document()
                         else collection.document(item.id)
            batch.set(docRef, item.toMap())
        }
        batch.commit().await()
    }

    override suspend fun updateSettlementItem(id: String, updates: Map<String, Any>) {
        collection.document(id).update(updates).await()
    }

    override suspend fun deleteSettlementItem(id: String) {
        collection.document(id).delete().await()
    }

    override suspend fun deleteSettlementItems(settlementId: String) {
        val items = getSettlementItems(settlementId)
        if (items.isEmpty()) return
        val batch = firestore.batch()
        items.forEach { batch.delete(collection.document(it.id)) }
        batch.commit().await()
    }
}
