package com.android.bilzy.data.remote.dto

import com.android.bilzy.domain.model.ReceiptItemDraft
import com.android.bilzy.domain.model.ScannedReceipt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 영수증 한 항목 (스캔 응답 items, confirm 요청 items 공용) */
@Serializable
data class OcrItemDto(
    val name: String = "",
    val price: Long = 0,
    val quantity: Int = 1
)

/** POST /ocr/scan 응답 */
@Serializable
data class OcrScanResponse(
    @SerialName("settlement_id") val settlementId: String = "",
    @SerialName("image_url") val imageUrl: String? = null,
    val items: List<OcrItemDto> = emptyList(),
    val total: Long = 0,
    val message: String? = null
)

/** POST /ocr/confirm 요청 */
@Serializable
data class OcrConfirmRequest(
    @SerialName("settlement_id") val settlementId: String,
    val items: List<OcrItemDto>
)

/** POST /ocr/confirm 응답 */
@Serializable
data class OcrConfirmResponse(
    @SerialName("total_amount") val totalAmount: Long = 0,
    val items: List<OcrItemDto> = emptyList()
)

/** POST /ocr/add-item 요청 */
@Serializable
data class AddItemRequest(
    @SerialName("settlement_id") val settlementId: String,
    val name: String,
    val price: Long,
    val quantity: Int
)

// ── 매퍼 ─────────────────────────────────────────────
fun OcrItemDto.toDraft() = ReceiptItemDraft(name = name, price = price, quantity = quantity)
fun ReceiptItemDraft.toDto() = OcrItemDto(name = name, price = price, quantity = quantity)

fun OcrScanResponse.toDomain() = ScannedReceipt(
    settlementId = settlementId,
    imageUrl = imageUrl,
    items = items.map { it.toDraft() },
    total = total
)
