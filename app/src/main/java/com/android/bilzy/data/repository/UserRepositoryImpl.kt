package com.android.bilzy.data.repository

import com.android.bilzy.data.remote.BilzyApi
import com.android.bilzy.data.remote.dto.toDomain
import com.android.bilzy.data.remote.dto.toProfile
import com.android.bilzy.domain.model.SettlementHistory
import com.android.bilzy.domain.model.UserProfile
import com.android.bilzy.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val api: BilzyApi
) : UserRepository {

    override suspend fun getMyProfile(): UserProfile = api.getMe().toProfile()

    override suspend fun getMyHistory(): List<SettlementHistory> =
        api.getHistory().mapNotNull { it.toDomain() }
}
