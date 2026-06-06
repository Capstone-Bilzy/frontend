package com.android.bilzy.domain.model

/**
 * 사용자의 대표 계좌(정산 송금 받을 계좌).
 * 백엔드 GET/POST /users/me/account 의 plain 모델.
 * 신규 유저는 세 필드 모두 빈 문자열일 수 있다.
 */
data class BankAccount(
    val bankName: String,
    val accountNumber: String,
    val accountHolder: String
) {
    /** 화면에 보여줄 만한 정보가 하나라도 있는지. */
    val isEmpty: Boolean
        get() = bankName.isBlank() && accountNumber.isBlank() && accountHolder.isBlank()
}
