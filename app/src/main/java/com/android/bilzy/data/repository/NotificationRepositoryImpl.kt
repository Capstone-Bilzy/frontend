package com.android.bilzy.data.repository

import com.android.bilzy.data.model.Notification
import com.android.bilzy.data.model.toMap
import com.android.bilzy.data.model.toNotification
import com.android.bilzy.domain.repository.NotificationRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : NotificationRepository {

    private val collection = firestore.collection("notifications")

    override suspend fun getNotifications(userUid: String): List<Notification> =
        collection.whereEqualTo("user_uid", userUid)
            .orderBy("created_at", Query.Direction.DESCENDING)
            .get().await()
            .documents.mapNotNull { it.toNotification() }

    override fun observeNotifications(userUid: String): Flow<List<Notification>> = callbackFlow {
        val listener = collection.whereEqualTo("user_uid", userUid)
            .orderBy("created_at", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents?.mapNotNull { it.toNotification() } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun saveNotification(notification: Notification): String {
        val docRef = if (notification.id.isEmpty()) collection.document()
                     else collection.document(notification.id)
        docRef.set(notification.toMap()).await()
        return docRef.id
    }

    override suspend fun markAsRead(id: String) {
        collection.document(id).update("is_read", true).await()
    }

    override suspend fun markAllAsRead(userUid: String) {
        val unread = collection
            .whereEqualTo("user_uid", userUid)
            .whereEqualTo("is_read", false)
            .get().await()
        if (unread.isEmpty) return
        val batch = firestore.batch()
        unread.documents.forEach { batch.update(it.reference, "is_read", true) }
        batch.commit().await()
    }
}
