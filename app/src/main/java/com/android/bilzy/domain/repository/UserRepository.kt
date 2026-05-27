package com.android.bilzy.domain.repository

import com.android.bilzy.data.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getUser(uid: String): User?
    fun observeUser(uid: String): Flow<User?>
    suspend fun saveUser(user: User)
    suspend fun updateUser(uid: String, updates: Map<String, Any>)
}
