package com.android.bilzy.data.repository

import com.android.bilzy.data.model.User
import com.android.bilzy.data.model.toMap
import com.android.bilzy.data.model.toUser
import com.android.bilzy.domain.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserRepository {

    private val collection = firestore.collection("users")

    override suspend fun getUser(uid: String): User? =
        collection.document(uid).get().await()
            .takeIf { it.exists() }?.toUser()

    override fun observeUser(uid: String): Flow<User?> = callbackFlow {
        val listener = collection.document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.takeIf { it.exists() }?.toUser())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun saveUser(user: User) {
        collection.document(user.uid).set(user.toMap()).await()
    }

    override suspend fun updateUser(uid: String, updates: Map<String, Any>) {
        collection.document(uid).update(updates).await()
    }
}
