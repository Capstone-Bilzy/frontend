package com.android.bilzy.domain.repository

import com.android.bilzy.domain.model.Settlement
import com.android.bilzy.domain.model.SettlementStatus

interface SettlementRepository {
    /** 정산방 생성. 성공 시 생성된 방 반환(status=scanning). */
    suspend fun createSettlement(title: String): Settlement

    /** 정산방 상세(멤버·항목 포함) 조회. */
    suspend fun getSettlement(id: String): Settlement

    /** 정산방 제목 수정. */
    suspend fun updateTitle(id: String, title: String): Settlement

    suspend fun updateStatus(id: String, status: SettlementStatus): Settlement

    suspend fun deleteSettlement(id: String)

    /** 정산건에 붙은 영수증 이미지 삭제(사용자가 '저장 안 함'/'다시 찍기' 선택 시). */
    suspend fun deleteReceiptImage(id: String)

    /** QR로 인식한 정산방에 내 닉네임으로 참여. */
    suspend fun joinByQr(id: String, nickname: String)

    /** 정산 완료 처리(status=done, 내역 기록). */
    suspend fun markDone(id: String): Settlement

    /** AI(Gemini) 정산 계산 후, 멤버별 금액이 반영된 정산방 상세를 반환. */
    suspend fun calculate(id: String, aiNote: String): Settlement
}
