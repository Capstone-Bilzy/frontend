package com.android.bilzy.domain.model

/**
 * 한 차수의 금액을 서버 규칙 계산(백엔드 ai_service.calculate_without_ai)과 같은 방법으로 나눈다.
 * 금액 조정 화면의 미리보기가 최종 결과와 같은 숫자를 보여주기 위한 것 — 실제 정산 금액은 항상 서버가 정한다.
 *
 * 규칙:
 * 1. 품목마다, 그 품목을 "안 먹었다"고 한 사람을 뺀 나머지 참여자가 똑같이 나눈다.
 *    참여자 전원이 제외한 품목은 낼 사람이 없으므로 전원이 나눈다.
 * 2. 차수 합계는 원 단위까지 맞춘다 — 나누어떨어지지 않는 나머지는 소수 부분이 큰 사람부터 1원씩(같으면 목록 순서대로).
 */
object RoundSplit {

    /** [excludedItemNames]는 그 사람이 안 먹었다고 고른 품목 이름들. */
    data class Participant(val key: String, val excludedItemNames: Set<String>)

    /** 참여자 key → 그 차수에서 낼 금액. 참여자가 없으면 빈 맵. */
    fun split(items: List<ReceiptItem>, participants: List<Participant>): Map<String, Long> {
        if (participants.isEmpty()) return emptyMap()
        val shares = DoubleArray(participants.size)
        var roundTotal = 0L
        for (item in items) {
            roundTotal += item.total
            val eaters = participants.indices.filter { item.name !in participants[it].excludedItemNames }
                .ifEmpty { participants.indices.toList() }
            eaters.forEach { shares[it] += item.total.toDouble() / eaters.size }
        }
        val amounts = LongArray(participants.size) { shares[it].toLong() }
        val remainder = (roundTotal - amounts.sum()).toInt().coerceAtLeast(0)
        participants.indices
            .sortedWith(compareByDescending<Int> { shares[it] - shares[it].toLong() }.thenBy { it })
            .take(remainder)
            .forEach { amounts[it] += 1 }
        return participants.indices.associate { participants[it].key to amounts[it] }
    }
}
