package com.android.bilzy

import com.android.bilzy.domain.model.ReceiptItem
import com.android.bilzy.domain.model.ReceiptItemDraft
import com.android.bilzy.domain.model.RoundSplit
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

    // ── 금액 조정 미리보기(RoundSplit)는 서버 규칙 계산(tests/test_split.py)과 같은 답을 내야 한다 ──

    private fun item(name: String, amount: Long) = ReceiptItem(name, "s", name, price = amount, quantity = 1)
    private fun person(key: String, vararg excluded: String) = RoundSplit.Participant(key, excluded.toSet())

    @Test
    fun split_even() {
        assertEquals(
            mapOf("가" to 10_000L, "나" to 10_000L),
            RoundSplit.split(listOf(item("피자", 20_000)), listOf(person("가"), person("나")))
        )
    }

    @Test
    fun split_excludedItemIsPaidByTheOthers() {
        assertEquals(
            mapOf("가" to 10_000L, "나" to 20_000L),
            RoundSplit.split(
                listOf(item("피자", 20_000), item("맥주", 10_000)),
                listOf(person("가", "맥주"), person("나"))
            )
        )
    }

    @Test
    fun split_itemEveryoneExcluded_isSplitByAll() {
        assertEquals(
            mapOf("가" to 3_000L, "나" to 3_000L, "다" to 3_000L),
            RoundSplit.split(
                listOf(item("맥주", 9_000)),
                listOf(person("가", "맥주"), person("나", "맥주"), person("다", "맥주"))
            )
        )
    }

    @Test
    fun split_remainderGoesInOrder_andKeepsTheTotal() {
        val result = RoundSplit.split(listOf(item("세트", 10_000)), listOf(person("가"), person("나"), person("다")))
        assertEquals(10_000L, result.values.sum())
        assertEquals(mapOf("가" to 3_334L, "나" to 3_333L, "다" to 3_333L), result)
    }

    @Test
    fun split_alwaysSumsToTheReceipt() {
        val items = listOf(item("삼겹살", 160_000), item("냉면", 45_000), item("소주", 30_000), item("세트", 10_001))
        val people = listOf(
            person("가", "냉면"), person("나", "소주", "냉면"), person("다"),
            person("라", "소주"), person("마"), person("바", "세트")
        )
        assertEquals(245_001L, RoundSplit.split(items, people).values.sum())
    }

    @Test
    fun split_withNoParticipants_isEmpty() {
        assertEquals(emptyMap<String, Long>(), RoundSplit.split(listOf(item("피자", 20_000)), emptyList()))
    }
}
