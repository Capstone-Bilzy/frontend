package com.android.bilzy.data.remote.dto

import com.android.bilzy.domain.model.SavedReceipt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** GET /receipts, POST /receipts 응답의 한 행 */
@Serializable
data class SavedReceiptDto(
    val id: String = "",
    @SerialName("store_name") val storeName: String? = null,
    @SerialName("total_amount") val totalAmount: Long? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("created_at") val createdAt: String = ""
)

/** POST /receipts/scan 응답(독립 OCR — 저장 전 금액 프리필용) */
@Serializable
data class ReceiptScanResponse(
    val items: List<OcrItemDto> = emptyList(),
    val total: Long = 0
)

fun SavedReceiptDto.toDomain() = SavedReceipt(
    id = id,
    storeName = storeName.orEmpty(),
    totalAmount = totalAmount,
    imageUrl = imageUrl,
    createdAt = createdAt
)
