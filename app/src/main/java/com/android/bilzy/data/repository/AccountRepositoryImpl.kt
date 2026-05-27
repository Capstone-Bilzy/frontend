package com.android.bilzy.data.repository

import com.android.bilzy.data.model.Account
import com.android.bilzy.data.model.toAccount
import com.android.bilzy.data.model.toMap
import com.android.bilzy.domain.repository.AccountRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : AccountRepository {

    private val collection = firestore.collection("accounts")

    override suspend fun getAccounts(userUid: String): List<Account> =
        collection.whereEqualTo("user_uid", userUid)
            .get().await()
            .documents.mapNotNull { it.toAccount() }

    override fun observeAccounts(userUid: String): Flow<List<Account>> = callbackFlow {
        val listener = collection.whereEqualTo("user_uid", userUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents?.mapNotNull { it.toAccount() } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun saveAccount(account: Account): String {
        val docRef = if (account.id.isEmpty()) collection.document()
                     else collection.document(account.id)
        docRef.set(account.toMap()).await()
        return docRef.id
    }

    override suspend fun updateAccount(id: String, updates: Map<String, Any>) {
        collection.document(id).update(updates).await()
    }

    override suspend fun deleteAccount(id: String) {
        collection.document(id).delete().await()
    }

    override suspend fun setPrimaryAccount(userUid: String, accountId: String) {
        val accounts = getAccounts(userUid)
        val batch = firestore.batch()
        accounts.forEach { account ->
            batch.update(collection.document(account.id), "is_primary", account.id == accountId)
        }
        batch.commit().await()
    }
}
