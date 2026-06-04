package com.android.bilzy.data.remote.dto

import com.android.bilzy.domain.model.SettlementHistory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GET /users/me/history 항목.
 * history 행 + 중첩된 settlements 요약(title/total_amount/status/created_at).
 */
@Serializable
data class HistoryDto(
    @SerialName("settlement_id") val settlementId: String = "",
    val settlements: SettlementBriefDto? = null
)

@Serializable
data class SettlementBriefDto(
    val id: String = "",
    val title: String = "",
    @SerialName("total_amount") val totalAmount: Long = 0,
    val status: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("member_count") val memberCount: Int = 0
)

fun HistoryDto.toDomain(): SettlementHistory? {
    val s = settlements ?: return null
    return SettlementHistory(
        settlementId = s.id.ifBlank { settlementId },
        title = s.title,
        totalAmount = s.totalAmount,
        status = s.status,
        createdAt = s.createdAt,
        memberCount = s.memberCount
    )
}
