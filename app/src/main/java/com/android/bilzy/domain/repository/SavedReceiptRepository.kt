package com.android.bilzy.domain.repository

import com.android.bilzy.domain.model.SavedReceipt

/** 저장 영수증 보관함. */
interface SavedReceiptRepository {

    /** 내 보관함 목록(최신순). */
    suspend fun getMyReceipts(): List<SavedReceipt>

    /** 저장 전 독립 OCR — 추정 총액 반환(저장 안 함). */
    suspend fun scan(imageBytes: ByteArray, mimeType: String): Long

    /** 이미지 + 가게명 + 총액을 보관함에 저장. */
    suspend fun save(imageBytes: ByteArray, mimeType: String, storeName: String, totalAmount: Long): SavedReceipt

    /** 보관함에서 삭제. */
    suspend fun delete(id: String)
}
