package com.android.bilzy.data.remote.dto

import com.android.bilzy.domain.model.MemberRoundAmount
import com.android.bilzy.domain.model.Receipt
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

/** PATCH /settlements/{id}/members/me/rounds 요청 바디. */
@Serializable
data class SetMemberRoundsRequest(val rounds: List<Int> = emptyList())

/** PATCH /settlements/{id}/members/me/rounds 응답. */
@Serializable
data class SetMemberRoundsResponse(
    @SerialName("member_id") val memberId: String = "",
    val rounds: List<SettlementMemberRoundDto> = emptyList()
)

/** PATCH /settlements/{id}/members/me/rounds/{round} 요청 바디. */
@Serializable
data class SetRoundAdjustmentRequest(
    @SerialName("excluded_item_names") val excludedItemNames: List<String> = emptyList()
)

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
    val items: List<ReceiptItemDto> = emptyList(),
    val receipts: List<ReceiptDto> = emptyList()
)

@Serializable
data class SettlementMemberDto(
    val id: String = "",
    @SerialName("settlement_id") val settlementId: String = "",
    @SerialName("user_id") val userId: String = "",
    val nickname: String = "",
    val amount: Long = 0,
    val reason: String? = null,
    @SerialName("joined_at") val joinedAt: String? = null,
    @SerialName("profile_image_url") val profileImageUrl: String? = null,
    val ready: Boolean = false,
    val rounds: List<SettlementMemberRoundDto> = emptyList()
)

@Serializable
data class ReceiptItemDto(
    val id: String = "",
    @SerialName("settlement_id") val settlementId: String = "",
    @SerialName("receipt_id") val receiptId: String = "",
    val name: String = "",
    val price: Long = 0,
    val quantity: Int = 1,
    @SerialName("created_at") val createdAt: String? = null
)

/** 라운드(영수증) 1건. 다차 정산의 실제 데이터. */
@Serializable
data class ReceiptDto(
    val id: String = "",
    @SerialName("settlement_id") val settlementId: String = "",
    val round: Int = 1,
    @SerialName("store_name") val storeName: String? = null,
    @SerialName("receipt_image_url") val receiptImageUrl: String? = null,
    @SerialName("receipt_text") val receiptText: String? = null,
    @SerialName("total_amount") val totalAmount: Long = 0,
    @SerialName("created_at") val createdAt: String? = null,
    val items: List<ReceiptItemDto> = emptyList()
)

/** 멤버가 특정 라운드에 참여한 내역(제외 항목·라운드별 금액·사유). */
@Serializable
data class SettlementMemberRoundDto(
    val id: String = "",
    @SerialName("settlement_member_id") val settlementMemberId: String = "",
    val round: Int = 1,
    @SerialName("excluded_item_names") val excludedItemNames: List<String> = emptyList(),
    val amount: Long = 0,
    val reason: String? = null
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
    items = items.map { it.toDomain() },
    receipts = receipts.map { it.toDomain() }
)

fun SettlementMemberDto.toDomain() = SettlementMember(
    id = id,
    settlementId = settlementId,
    userId = userId,
    nickname = nickname,
    amount = amount,
    reason = reason,
    profileImageUrl = profileImageUrl,
    ready = ready,
    rounds = rounds.map { it.toDomain() }
)

fun ReceiptItemDto.toDomain() = ReceiptItem(
    id = id,
    settlementId = settlementId,
    name = name,
    price = price,
    quantity = quantity
)

fun ReceiptDto.toDomain() = Receipt(
    id = id,
    settlementId = settlementId,
    round = round,
    storeName = storeName,
    receiptImageUrl = receiptImageUrl,
    totalAmount = totalAmount,
    items = items.map { it.toDomain() }
)

fun SettlementMemberRoundDto.toDomain() = MemberRoundAmount(
    id = id,
    settlementMemberId = settlementMemberId,
    round = round,
    excludedItemNames = excludedItemNames,
    amount = amount,
    reason = reason
)
