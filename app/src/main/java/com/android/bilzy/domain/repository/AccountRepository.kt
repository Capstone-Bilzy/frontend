package com.android.bilzy.domain.repository

import com.android.bilzy.data.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    suspend fun getAccounts(userUid: String): List<Account>
    fun observeAccounts(userUid: String): Flow<List<Account>>
    suspend fun saveAccount(account: Account): String
    suspend fun updateAccount(id: String, updates: Map<String, Any>)
    suspend fun deleteAccount(id: String)
    suspend fun setPrimaryAccount(userUid: String, accountId: String)
}
