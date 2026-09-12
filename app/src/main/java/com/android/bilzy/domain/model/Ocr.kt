package com.android.bilzy.domain.model

/** OCR 전/수정 중인 영수증 항목 (아직 DB id 없음) */
data class ReceiptItemDraft(
    val name: String,
    val price: Long,
    val quantity: Int = 1
) {
    val subtotal: Long get() = price * quantity
}

/** POST /ocr/scan 결과 (사용자가 확인·수정할 초안) */
data class ScannedReceipt(
    val settlementId: String,
    val round: Int = 1,
    val imageUrl: String?,
    val items: List<ReceiptItemDraft>,
    val total: Long
)

/** POST /ocr/confirm 결과. 해당 라운드의 확정 총액과, 그 라운드까지 반영된 정산방 전체 총액. */
data class OcrConfirmResult(
    val round: Int,
    val totalAmount: Long,
    val settlementTotalAmount: Long
)
