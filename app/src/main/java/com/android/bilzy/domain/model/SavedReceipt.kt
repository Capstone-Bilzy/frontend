package com.android.bilzy.domain.model

/**
 * 보관함에 저장된 영수증 한 건.
 * 가게명·총액은 스캔(OCR) 후 사용자가 수동확인한 값이며, 둘 다 없을 수도 있다(레거시 행).
 */
data class SavedReceipt(
    val id: String,
    val storeName: String,
    val totalAmount: Long?,
    val imageUrl: String?,   // 단기 signed URL(서버 발급) — Coil 직로드
    val createdAt: String
)
