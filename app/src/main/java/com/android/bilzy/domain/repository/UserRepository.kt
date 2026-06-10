package com.android.bilzy.domain.repository

import com.android.bilzy.domain.model.SettlementHistory
import com.android.bilzy.domain.model.UserProfile

interface UserRepository {
    /** 내 프로필 조회 (GET /users/me). 성공 결과는 인메모리 캐시에 보관된다. */
    suspend fun getMyProfile(): UserProfile

    /** 내 정산 내역(완료된 정산들, 최신순). (GET /users/me/history) 성공 결과는 캐시된다. */
    suspend fun getMyHistory(): List<SettlementHistory>

    /** 마지막으로 성공한 프로필 캐시(없으면 null). 재진입 시 즉시 표시용. */
    fun cachedProfile(): UserProfile?

    /** 마지막으로 성공한 정산 내역 캐시(없으면 null). 재진입 시 즉시 표시용. */
    fun cachedHistory(): List<SettlementHistory>?

    /** 인메모리 캐시 비우기. 로그아웃 시 호출해 다음 사용자에게 잔상이 남지 않게 한다. */
    fun clearCache()
}
