package com.android.bilzy.data.repository

import com.android.bilzy.data.remote.BilzyApi
import com.android.bilzy.data.remote.dto.AddMemberRequest
import com.android.bilzy.data.remote.dto.CreateSettlementRequest
import com.android.bilzy.data.remote.dto.UpdateSettlementRequest
import com.android.bilzy.data.remote.dto.UpdateStatusRequest
import com.android.bilzy.data.remote.dto.toDomain
import com.android.bilzy.domain.model.Settlement
import com.android.bilzy.domain.model.SettlementStatus
import com.android.bilzy.domain.repository.SettlementRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettlementRepositoryImpl @Inject constructor(
    private val api: BilzyApi
) : SettlementRepository {

    override suspend fun createSettlement(title: String): Settlement =
        api.createSettlement(CreateSettlementRequest(title)).toDomain()

    override suspend fun getSettlement(id: String): Settlement =
        api.getSettlement(id).toDomain()

    override suspend fun updateTitle(id: String, title: String): Settlement =
        api.updateSettlement(id, UpdateSettlementRequest(title)).toDomain()

    override suspend fun updateStatus(id: String, status: SettlementStatus): Settlement =
        api.updateSettlementStatus(id, UpdateStatusRequest(status.value)).toDomain()

    override suspend fun deleteSettlement(id: String) =
        api.deleteSettlement(id)

    override suspend fun joinByQr(id: String, nickname: String) {
        api.joinSettlement(id, AddMemberRequest(nickname))
    }
}
