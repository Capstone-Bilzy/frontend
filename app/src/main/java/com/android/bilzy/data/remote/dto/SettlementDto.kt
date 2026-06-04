package com.android.bilzy.data.remote.dto

import com.android.bilzy.domain.model.ReceiptItem
import com.android.bilzy.domain.model.Settlement
import com.android.bilzy.domain.model.SettlementMember
import com.android.bilzy.domain.model.SettlementStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── 요청 ─────────────────────────────────────────────
@Serializable
data class CreateSettlementRequest(val title: String)

@Serializable
data class UpdateSettlementRequest(val title: String)

@Serializable
data class UpdateStatusRequest(val status: String)

/** POST /settlements/{id}/join, /members 요청 바디. */
@Serializable
data class AddMemberRequest(val nickname: String)

/** POST /settlements/{id}/calculate 요청. ai_note에 특이사항(칩 선택 등)을 문자열로 담는다. */
@Serializable
data class CalculateRequest(@SerialName("ai_note") val aiNote: String = "")

/** AI 계산 응답(요약/면책). 멤버별 금액은 calculate 후 GET 상세로 다시 받는다. */
@Serializable
data class CalculateResultDto(
    val summary: String? = null,
    @SerialName("ai_disclaimer") val aiDisclaimer: String? = null
)

// ── 응답 ─────────────────────────────────────────────
/** GET/POST /settlements 응답. members/items는 GET 상세에서만 채워짐. */
@Serializable
data class SettlementDto(
    val id: String = "",
    val title: String = "",
    @SerialName("created_by") val createdBy: String = "",
    val status: String = "scanning",
    @SerialName("total_amount") val totalAmount: Long = 0,
    @SerialName("receipt_image_url") val receiptImageUrl: String? = null,
    @SerialName("receipt_text") val receiptText: String? = null,
    @SerialName("ai_note") val aiNote: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val members: List<SettlementMemberDto> = emptyList(),
    val items: List<ReceiptItemDto> = emptyList()
)

@Serializable
data class SettlementMemberDto(
    val id: String = "",
    @SerialName("settlement_id") val settlementId: String = "",
    @SerialName("user_id") val userId: String = "",
    val nickname: String = "",
    val amount: Long = 0,
    val reason: String? = null,
    @SerialName("joined_at") val joinedAt: String? = null
)

@Serializable
data class ReceiptItemDto(
    val id: String = "",
    @SerialName("settlement_id") val settlementId: String = "",
    val name: String = "",
    val price: Long = 0,
    val quantity: Int = 1,
    @SerialName("created_at") val createdAt: String? = null
)

// ── 매퍼 (DTO → 도메인) ───────────────────────────────
fun SettlementDto.toDomain() = Settlement(
    id = id,
    title = title,
    createdBy = createdBy,
    status = SettlementStatus.from(status),
    totalAmount = totalAmount,
    receiptImageUrl = receiptImageUrl,
    createdAt = createdAt,
    members = members.map { it.toDomain() },
    items = items.map { it.toDomain() }
)

fun SettlementMemberDto.toDomain() = SettlementMember(
    id = id,
    settlementId = settlementId,
    userId = userId,
    nickname = nickname,
    amount = amount,
    reason = reason
)

fun ReceiptItemDto.toDomain() = ReceiptItem(
    id = id,
    settlementId = settlementId,
    name = name,
    price = price,
    quantity = quantity
)
