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

    // 프로세스 수명 인메모리 캐시. 화면 재진입 시 깜빡임 없이 즉시 표시하기 위함.
    @Volatile private var profileCache: UserProfile? = null
    @Volatile private var historyCache: List<SettlementHistory>? = null

    override suspend fun getMyProfile(): UserProfile =
        api.getMe().toProfile().also { profileCache = it }

    override suspend fun getMyHistory(): List<SettlementHistory> =
        api.getHistory().mapNotNull { it.toDomain() }.also { historyCache = it }

    override fun cachedProfile(): UserProfile? = profileCache

    override fun cachedHistory(): List<SettlementHistory>? = historyCache

    override fun clearCache() {
        profileCache = null
        historyCache = null
    }
}
