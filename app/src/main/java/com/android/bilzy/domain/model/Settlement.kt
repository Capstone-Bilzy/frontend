package com.android.bilzy.domain.model

/** 백엔드 정산방 상태: scanning → waiting → calculating → calculated → done */
enum class SettlementStatus(val value: String) {
    SCANNING("scanning"),
    WAITING("waiting"),
    CALCULATING("calculating"),
    CALCULATED("calculated"),
    DONE("done");

    companion object {
        fun from(value: String?) = entries.find { it.value == value } ?: SCANNING
    }
}

data class Settlement(
    val id: String,
    val title: String,
    val createdBy: String,
    val status: SettlementStatus,
    val totalAmount: Long,
    val receiptImageUrl: String?,
    val createdAt: String?,
    val members: List<SettlementMember> = emptyList(),
    val items: List<ReceiptItem> = emptyList(),
    val receipts: List<Receipt> = emptyList(),
    val extraPhotos: List<ExtraPhoto> = emptyList(),
    /** 결제자(방장)의 송금 계좌. 금액이 확정된 뒤(calculated/done)에만 서버가 내려주고, 미등록이면 null. */
    val payerAccount: BankAccount? = null,
    /** 방장이 정한 정원(총 인원). 미설정이면 null. */
    val memberCapacity: Int? = null
) {
    /** 그 차수를 고른(참여한) 멤버 수. */
    fun roundParticipantCount(round: Int): Int = members.count { m -> m.rounds.any { it.round == round } }
}

/** 라운드와 무관하게 순수 기록용으로 첨부된 영수증 사진(정산 계산에 영향 없음). */
data class ExtraPhoto(
    /** 구버전 서버 응답에는 없다 — 없으면 이름을 바꿀 수 없다. */
    val id: String? = null,
    /** 방장이 붙인 이름. 없으면 화면이 "영수증 사진"으로 표시. */
    val name: String? = null,
    val imageUrl: String,
    val createdAt: String?
)

data class SettlementMember(
    val id: String,
    val settlementId: String,
    val userId: String,
    val nickname: String,
    val amount: Long,
    val reason: String?,
    val profileImageUrl: String? = null,
    val ready: Boolean = false,
    val rounds: List<MemberRoundAmount> = emptyList()
)

data class ReceiptItem(
    val id: String,
    val settlementId: String,
    val name: String,
    val price: Long,
    val quantity: Int,
    /** 그 줄의 금액이 단가×수량으로 표현되지 않을 때만 값이 있다(3개 10,000원). */
    val lineAmount: Long? = null
) {
    /** 그 줄의 금액. 화면·계산에서는 price*quantity 대신 이 값을 쓴다. */
    val total: Long get() = lineAmount ?: (price * quantity)
}

/** 라운드(영수증) 1건 — 다차 정산의 실제 데이터. */
data class Receipt(
    val id: String,
    val settlementId: String,
    val round: Int,
    val storeName: String?,
    val receiptImageUrl: String?,
    val totalAmount: Long,
    val items: List<ReceiptItem> = emptyList()
)

/** 멤버가 특정 라운드에 참여한 내역(제외 항목·라운드별 금액·사유). */
data class MemberRoundAmount(
    val id: String,
    val settlementMemberId: String,
    val round: Int,
    val excludedItemNames: List<String> = emptyList(),
    val amount: Long,
    val reason: String?
)

/** 서명+만료(24시간) 초대 토큰. 딥링크/QR에 담아 신규 참여자 인가에 쓴다. */
data class InviteToken(
    val token: String,
    val deepLink: String,
    val expiresAt: String,
    val expiresIn: Long
)
