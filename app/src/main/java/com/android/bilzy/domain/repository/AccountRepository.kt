package com.android.bilzy.domain.repository

import com.android.bilzy.domain.model.BankAccount

/**
 * 사용자 대표 계좌. 백엔드 REST(/users/me/account)로 통신.
 * (옛 Firebase accounts 컬렉션 구현은 REST로 교체됨.)
 */
interface AccountRepository {
    /** 내 대표 계좌 조회. 미설정 시 빈 값(BankAccount.isEmpty). */
    suspend fun getMyAccount(): BankAccount

    /** 내 대표 계좌 저장/수정. 저장된 계좌를 반환. */
    suspend fun saveMyAccount(account: BankAccount): BankAccount
}
