package com.android.bilzy.domain.repository

import com.android.bilzy.domain.model.SettlementHistory
import com.android.bilzy.domain.model.UserProfile

interface UserRepository {
    /** 내 프로필 조회 (GET /users/me). */
    suspend fun getMyProfile(): UserProfile

    /** 내 정산 내역(완료된 정산들, 최신순). (GET /users/me/history) */
    suspend fun getMyHistory(): List<SettlementHistory>
}
