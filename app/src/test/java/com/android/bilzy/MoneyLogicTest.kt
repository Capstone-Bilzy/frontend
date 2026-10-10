package com.android.bilzy

import com.android.bilzy.domain.model.ReceiptItem
import com.android.bilzy.domain.model.ReceiptItemDraft
import com.android.bilzy.ui.room.RoomViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 금액 계산이 1원도 틀어지지 않는지 확인한다(./gradlew test). */
class MoneyLogicTest {

    @Test
    fun evenSplit_sumsToTotal_andGivesRemainderToTheFirstMembers() {
        assertEquals(listOf(3334L, 3333L, 3333L), RoomViewModel.evenSplit(10_000, 3))
        assertEquals(listOf(5000L, 5000L), RoomViewModel.evenSplit(10_000, 2))
        for (n in 1..20) {
            assertEquals(123_457L, RoomViewModel.evenSplit(123_457, n).sum())
        }
    }

    @Test
    fun evenSplit_withNoMembers_isEmpty() {
        assertEquals(emptyList<Long>(), RoomViewModel.evenSplit(10_000, 0))
    }

    @Test
    fun draft_subtotal_isUnitPriceTimesQuantity() {
        assertEquals(9_000L, ReceiptItemDraft("콜라", price = 3_000, quantity = 3).subtotal)
    }

    @Test
    fun draft_keepsEnteredAmount_whenNotDivisibleByQuantity() {
        // 3개 10,000원: 단가 3,333 × 3 = 9,999가 되면 안 된다
        val draft = ReceiptItemDraft("세트", price = 3_333, quantity = 3, lineAmount = 10_000)
        assertEquals(10_000L, draft.subtotal)
    }

    @Test
    fun draft_normalized_keepsTheAmount_forServersWithoutLineAmount() {
        val normalized = ReceiptItemDraft("세트", price = 3_333, quantity = 3, lineAmount = 10_000).normalized()
        assertEquals(10_000L, normalized.price)
        assertEquals(1, normalized.quantity)
        assertNull(normalized.lineAmount)
        assertEquals(10_000L, normalized.subtotal)
    }

    @Test
    fun draft_normalized_leavesDivisibleLinesAlone() {
        val draft = ReceiptItemDraft("콜라", price = 3_000, quantity = 3)
        assertEquals(draft, draft.normalized())
    }

    @Test
    fun receiptItem_total_prefersLineAmount() {
        assertEquals(10_000L, ReceiptItem("1", "s", "세트", price = 3_333, quantity = 3, lineAmount = 10_000).total)
        assertEquals(9_000L, ReceiptItem("2", "s", "콜라", price = 3_000, quantity = 3).total)
    }
}
