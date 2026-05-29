package com.android.bilzy.di

import com.android.bilzy.data.repository.AccountRepositoryImpl
import com.android.bilzy.data.repository.AuthRepositoryImpl
import com.android.bilzy.data.repository.NotificationRepositoryImpl
import com.android.bilzy.data.repository.SettlementItemRepositoryImpl
import com.android.bilzy.data.repository.SettlementMemberRepositoryImpl
import com.android.bilzy.data.repository.SettlementRepositoryImpl
import com.android.bilzy.data.repository.UserRepositoryImpl
import com.android.bilzy.domain.repository.AccountRepository
import com.android.bilzy.domain.repository.AuthRepository
import com.android.bilzy.domain.repository.NotificationRepository
import com.android.bilzy.domain.repository.SettlementItemRepository
import com.android.bilzy.domain.repository.SettlementMemberRepository
import com.android.bilzy.domain.repository.SettlementRepository
import com.android.bilzy.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds @Singleton
    abstract fun bindSettlementRepository(impl: SettlementRepositoryImpl): SettlementRepository

    @Binds @Singleton
    abstract fun bindSettlementMemberRepository(impl: SettlementMemberRepositoryImpl): SettlementMemberRepository

    @Binds @Singleton
    abstract fun bindSettlementItemRepository(impl: SettlementItemRepositoryImpl): SettlementItemRepository

    @Binds @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository
}
