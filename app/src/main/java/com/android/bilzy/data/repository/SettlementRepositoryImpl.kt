package com.android.bilzy.data.repository

import com.android.bilzy.data.remote.BilzyApi
import com.android.bilzy.data.remote.dto.AddMemberRequest
import com.android.bilzy.data.remote.dto.CreateSettlementRequest
import com.android.bilzy.data.remote.dto.SetMemberRoundsRequest
import com.android.bilzy.data.remote.dto.SetRoundAdjustmentRequest
import com.android.bilzy.data.remote.dto.UpdateSettlementRequest
import com.android.bilzy.data.remote.dto.UpdateStatusRequest
import com.android.bilzy.data.remote.dto.toDomain
import com.android.bilzy.domain.model.InviteToken
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

    override suspend fun deleteReceiptImage(id: String, round: Int) =
        api.deleteSettlementReceipt(id, round)

    override suspend fun setMyRounds(id: String, rounds: List<Int>) {
        api.setMyRounds(id, SetMemberRoundsRequest(rounds))
    }

    override suspend fun setMyRoundAdjustment(id: String, round: Int, excludedItemNames: List<String>) {
        api.setMyRoundAdjustment(id, round, SetRoundAdjustmentRequest(excludedItemNames))
    }

    override suspend fun markReady(id: String) {
        api.setMyReady(id)
    }

    override suspend fun joinByQr(id: String, nickname: String, inviteToken: String?) {
        api.joinSettlement(id, AddMemberRequest(nickname, inviteToken))
    }

    override suspend fun createInviteToken(id: String, regenerate: Boolean): InviteToken =
        api.createInviteToken(id, regenerate).toDomain()

    override suspend fun markDone(id: String): Settlement =
        api.markSettlementDone(id).toDomain()

    override suspend fun calculate(id: String, aiNote: String): Settlement {
        api.calculateSplit(id, com.android.bilzy.data.remote.dto.CalculateRequest(aiNote))
        // 계산 결과(멤버별 금액·사유)는 서버에 저장되므로 상세를 다시 받아 반영
        return api.getSettlement(id).toDomain()
    }
}
