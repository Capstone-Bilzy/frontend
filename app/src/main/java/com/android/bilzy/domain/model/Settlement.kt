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
    val receipts: List<Receipt> = emptyList()
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
    val quantity: Int
)

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
