package com.android.bilzy.data.repository

import com.android.bilzy.data.remote.BilzyApi
import com.android.bilzy.data.remote.dto.AccountRequest
import com.android.bilzy.data.remote.dto.toDomain
import com.android.bilzy.domain.model.BankAccount
import com.android.bilzy.domain.repository.AccountRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val api: BilzyApi
) : AccountRepository {

    override suspend fun getMyAccount(): BankAccount =
        api.getAccount().toDomain()

    override suspend fun saveMyAccount(account: BankAccount): BankAccount =
        api.saveAccount(
            AccountRequest(
                bankName = account.bankName,
                accountNumber = account.accountNumber,
                accountHolder = account.accountHolder
            )
        ).toDomain()
}
