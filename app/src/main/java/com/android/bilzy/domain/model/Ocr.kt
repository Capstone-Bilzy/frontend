package com.android.bilzy.domain.model

/** OCR 전/수정 중인 영수증 항목 (아직 DB id 없음) */
data class ReceiptItemDraft(
    val name: String,
    val price: Long,
    val quantity: Int = 1,
    /**
     * 사용자가 금액 칸에 직접 넣은 그 줄의 금액이 수량으로 나누어떨어지지 않을 때만 채워진다
     * (예: 3개 10,000원). 단가는 정수라 이런 줄은 단가×수량으로 금액을 되살릴 수 없어서(9,999원)
     * 입력한 금액을 따로 들고 있다가 합계와 서버 전송에 그대로 쓴다.
     */
    val lineAmount: Long? = null
) {
    val subtotal: Long get() = lineAmount ?: (price * quantity)

    /**
     * line_amount를 모르는 구버전 서버용 형태: 나누어떨어지지 않는 줄을 "금액 그대로 × 1개"로 바꾼다
     * (수량 표시는 1이 되지만 금액은 1원도 틀어지지 않는다). 최신 서버에는 수량·금액을 그대로 보낸다.
     */
    fun normalized(): ReceiptItemDraft =
        if (lineAmount == null) this else copy(price = lineAmount, quantity = 1, lineAmount = null)
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
