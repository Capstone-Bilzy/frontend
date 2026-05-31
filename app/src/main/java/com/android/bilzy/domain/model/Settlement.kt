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
    val items: List<ReceiptItem> = emptyList()
)

data class SettlementMember(
    val id: String,
    val settlementId: String,
    val userId: String,
    val nickname: String,
    val amount: Long,
    val reason: String?
)

data class ReceiptItem(
    val id: String,
    val settlementId: String,
    val name: String,
    val price: Long,
    val quantity: Int
)
