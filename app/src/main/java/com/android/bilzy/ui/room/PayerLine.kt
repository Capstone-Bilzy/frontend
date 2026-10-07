package com.android.bilzy.ui.room

import com.android.bilzy.domain.model.Settlement

/**
 * 정산 결과·정산내역 상세의 요약 한 줄("{결제자} 전액 결제 · …")에 들어갈 문구와 금액.
 *
 * 보는 사람에 따라 달라진다. 결제자(방장)에게는 다른 사람들에게서 받을 돈을, 참여자에게는 본인이 낼 돈을 보여준다.
 * 예전엔 누가 보든 "받을 금액"이라 참여자 화면에서도 받을 돈처럼 읽혔다.
 * 결제자를 찾을 수 없으면 null(호출 측에서 줄을 숨긴다).
 */
fun payerLine(settlement: Settlement, total: Long, myUserId: String?): Pair<String, Long>? {
    val payer = settlement.members.find { it.userId == settlement.createdBy } ?: return null
    val me = settlement.members.find { it.userId == myUserId }
    return if (me != null && me.userId != payer.userId) {
        "${payer.nickname} 전액 결제 · 내가 낼 금액 " to me.amount
    } else {
        "${payer.nickname} 전액 결제 · 받을 금액 " to (total - payer.amount).coerceAtLeast(0)
    }
}
