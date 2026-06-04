package com.android.bilzy.domain.model

/** 내 정산 내역 한 건(GET /users/me/history). */
data class SettlementHistory(
    val settlementId: String,
    val title: String,
    val totalAmount: Long,
    val status: String,
    /** ISO8601 문자열(예: "2026-05-29T08:36:49..."), 없을 수 있음. */
    val createdAt: String?,
    /** 참여 인원수(GET /users/me/history의 member_count). */
    val memberCount: Int = 0
)
