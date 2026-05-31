package com.android.bilzy.domain.repository

import com.android.bilzy.domain.model.Settlement
import com.android.bilzy.domain.model.SettlementStatus

interface SettlementRepository {
    /** 정산방 생성. 성공 시 생성된 방 반환(status=scanning). */
    suspend fun createSettlement(title: String): Settlement

    /** 정산방 상세(멤버·항목 포함) 조회. */
    suspend fun getSettlement(id: String): Settlement

    suspend fun updateStatus(id: String, status: SettlementStatus): Settlement

    suspend fun deleteSettlement(id: String)
}
