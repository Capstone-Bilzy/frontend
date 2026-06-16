package com.android.bilzy.data.demo

import com.android.bilzy.domain.model.ReceiptItem
import com.android.bilzy.domain.model.Settlement
import com.android.bilzy.domain.model.SettlementHistory
import com.android.bilzy.domain.model.SettlementMember
import com.android.bilzy.domain.model.SettlementStatus

object DemoData {

    const val DEMO_ID = "demo-special-001"

    val historyItem = SettlementHistory(
        settlementId = DEMO_ID,
        title = "팀 회식",
        totalAmount = 143_000L,
        status = "done",
        createdAt = "2026-06-10T19:30:00.000Z",
        memberCount = 6
    )

    val memberItems = mapOf(
        "dm1" to "삼겹살, 소주, 냉면",
        "dm2" to "막걸리, 냉면",
        "dm3" to "삼겹살 추가, 소주",
        "dm4" to "삼겹살",
        "dm5" to "삼겹살, 막걸리, 반찬",
        "dm6" to "삼겹살, 소주, 냉면"
    )

    val settlement = Settlement(
        id = DEMO_ID,
        title = "팀 회식",
        createdBy = "김민준",
        status = SettlementStatus.DONE,
        totalAmount = 143_000L,
        receiptImageUrl = null,
        createdAt = "2026-06-10T19:30:00.000Z",
        members = listOf(
            SettlementMember(
                id = "dm1", settlementId = DEMO_ID,
                userId = "u1", nickname = "김민준",
                amount = 25_000L,
                reason = "냉면 +3,000원"
            ),
            SettlementMember(
                id = "dm2", settlementId = DEMO_ID,
                userId = "u2", nickname = "이서연",
                amount = 20_000L,
                reason = "육류 -5,000원"
            ),
            SettlementMember(
                id = "dm3", settlementId = DEMO_ID,
                userId = "u3", nickname = "박지호",
                amount = 28_500L,
                reason = "삼겹살 +6,500원"
            ),
            SettlementMember(
                id = "dm4", settlementId = DEMO_ID,
                userId = "u4", nickname = "최유진",
                amount = 18_000L,
                reason = "음식 -7,000원"
            ),
            SettlementMember(
                id = "dm5", settlementId = DEMO_ID,
                userId = "u5", nickname = "정다은",
                amount = 27_500L,
                reason = "사이드 +5,500원"
            ),
            SettlementMember(
                id = "dm6", settlementId = DEMO_ID,
                userId = "u6", nickname = "한승우",
                amount = 24_000L,
                reason = null
            )
        ),
        items = listOf(
            ReceiptItem(id = "di1", settlementId = DEMO_ID, name = "삼겹살 5인분", price = 75_000L, quantity = 1),
            ReceiptItem(id = "di2", settlementId = DEMO_ID, name = "소주 3병", price = 13_500L, quantity = 1),
            ReceiptItem(id = "di3", settlementId = DEMO_ID, name = "막걸리 2병", price = 10_000L, quantity = 1),
            ReceiptItem(id = "di4", settlementId = DEMO_ID, name = "냉면 4그릇", price = 32_000L, quantity = 1),
            ReceiptItem(id = "di5", settlementId = DEMO_ID, name = "반찬 추가", price = 12_500L, quantity = 1)
        )
    )
}
