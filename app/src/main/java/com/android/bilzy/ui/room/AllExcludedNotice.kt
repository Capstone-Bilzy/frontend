package com.android.bilzy.ui.room

import com.android.bilzy.domain.model.RoundSplit
import com.android.bilzy.domain.model.Settlement

/** 차수 참여자 전원이 안 먹었다고 고른 메뉴가 있을 때 띄우는 안내(금액 조정·정산 결과 공용, 사용자 확정 문구 2026-11-26). */
const val ALL_EXCLUDED_NOTICE = "모두 안 먹은 메뉴는 다 같이 나눠서 내요"

/** 어느 차수든 그 차수 참여자 전원이 제외한 메뉴가 하나라도 있으면 true. */
fun Settlement.hasItemExcludedByEveryone(): Boolean = receipts.any { receipt ->
    val participants = members.mapNotNull { m ->
        m.rounds.firstOrNull { it.round == receipt.round }
            ?.let { RoundSplit.Participant(m.id, it.excludedItemNames.toSet()) }
    }
    RoundSplit.excludedByEveryone(receipt.items, participants).isNotEmpty()
}
