package com.android.bilzy.data.remote.dto

import com.android.bilzy.domain.model.BankAccount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 계좌 조회/저장 응답.  GET·POST /users/me/account
 * 백엔드 account_service 기준: { bank_name, account_number, account_holder }
 * (account_number 는 서버에서 복호화돼 평문으로 내려온다. 신규 유저는 빈 문자열.)
 */
@Serializable
data class AccountDto(
    // 미설정 유저는 키가 존재하면서 값이 명시적 null로 내려온다(기본값이 아니라 null!).
    // 비-nullable이면 kotlinx.serialization이 파싱 예외를 던지므로 nullable로 받는다.
    @SerialName("bank_name") val bankName: String? = null,
    @SerialName("account_number") val accountNumber: String? = null,
    @SerialName("account_holder") val accountHolder: String? = null
)

/**
 * 계좌 저장 요청 바디.  POST /users/me/account
 * 백엔드 검증: account_number 10~30자, bank_name·account_holder 1~20자.
 */
@Serializable
data class AccountRequest(
    @SerialName("bank_name") val bankName: String,
    @SerialName("account_number") val accountNumber: String,
    @SerialName("account_holder") val accountHolder: String
)

fun AccountDto.toDomain() = BankAccount(
    bankName = bankName.orEmpty(),
    accountNumber = accountNumber.orEmpty(),
    accountHolder = accountHolder.orEmpty()
)
