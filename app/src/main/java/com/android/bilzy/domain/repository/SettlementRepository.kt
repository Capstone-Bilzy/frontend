package com.android.bilzy.domain.repository

import com.android.bilzy.domain.model.InviteToken
import com.android.bilzy.domain.model.Settlement
import com.android.bilzy.domain.model.SettlementStatus

interface SettlementRepository {
    /** 정산방 생성. 성공 시 생성된 방 반환(status=scanning). */
    suspend fun createSettlement(title: String): Settlement

    /** 정산방 상세(멤버·항목 포함) 조회. */
    suspend fun getSettlement(id: String): Settlement

    /** 정산방 제목 수정. */
    suspend fun updateTitle(id: String, title: String): Settlement

    /** PeopleCount 화면에서 정한 정원(총 인원) 저장(방장만 가능). 이후 join이 이 값을 넘지 못하게 막는다. */
    suspend fun setMemberCapacity(id: String, memberCapacity: Int): Settlement
    suspend fun renameExtraPhoto(id: String, photoId: String, name: String)

    suspend fun updateStatus(id: String, status: SettlementStatus): Settlement

    suspend fun deleteSettlement(id: String)

    /** 정산건의 특정 라운드에 붙은 영수증 이미지 삭제(사용자가 '저장 안 함'/'다시 찍기' 선택 시). */
    suspend fun deleteReceiptImage(id: String, round: Int = 1)

    /** 특정 차수(영수증+항목)를 통째로 삭제. 뒤 차수는 서버에서 번호가 한 칸씩 당겨진다. */
    suspend fun deleteRound(id: String, round: Int)

    /** 본인이 참여한 라운드 집합을 설정(토글 결과 전체를 한 번에 보냄). */
    suspend fun setMyRounds(id: String, rounds: List<Int>)

    /** 본인이 그 라운드에서 안 먹은 항목(항목명)을 통째로 교체. */
    suspend fun setMyRoundAdjustment(id: String, round: Int, excludedItemNames: List<String>)

    /** 본인이 금액 조정을 마치고 "정산 시작하기"를 눌렀음을 표시(다른 멤버가 실시간으로 확인). */
    suspend fun markReady(id: String)

    /** QR로 인식한 정산방에 내 닉네임으로 참여. inviteToken은 신규 참여자에게만 필요(owner/기존 멤버는 무시됨). */
    suspend fun joinByQr(id: String, nickname: String, inviteToken: String? = null)

    /** 서명+24시간 만료 초대 토큰 발급(owner만 가능). regenerate=true면 기존 토큰을 전부 무효화하고 새로 발급. */
    suspend fun createInviteToken(id: String, regenerate: Boolean = false): InviteToken

    /** 정산 완료 처리(status=done, 내역 기록). */
    suspend fun markDone(id: String): Settlement

    /** AI(Gemini) 정산 계산 후, 멤버별 금액이 반영된 정산방 상세를 반환. */
    suspend fun calculate(id: String, aiNote: String): Settlement
}
