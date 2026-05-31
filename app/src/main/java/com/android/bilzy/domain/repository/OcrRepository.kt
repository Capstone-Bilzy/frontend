package com.android.bilzy.domain.repository

import com.android.bilzy.domain.model.ReceiptItem
import com.android.bilzy.domain.model.ReceiptItemDraft
import com.android.bilzy.domain.model.ScannedReceipt

interface OcrRepository {
    /** 영수증 이미지를 업로드해 서버 OCR 결과(항목 초안)를 받는다. */
    suspend fun scan(settlementId: String, imageBytes: ByteArray, mimeType: String): ScannedReceipt

    /** 사용자가 확인·수정한 항목을 확정한다(status=waiting). 반환: 총액. */
    suspend fun confirm(settlementId: String, items: List<ReceiptItemDraft>): Long

    /** 항목 1개 추가. */
    suspend fun addItem(settlementId: String, name: String, price: Long, quantity: Int): ReceiptItem
}
