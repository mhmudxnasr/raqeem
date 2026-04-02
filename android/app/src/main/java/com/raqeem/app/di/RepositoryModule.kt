package com.raqeem.app.di

import com.raqeem.app.data.repository.AccountRepositoryImpl
import com.raqeem.app.data.repository.CategoryRepositoryImpl
import com.raqeem.app.data.repository.DataStoreAppLockRepository
import com.raqeem.app.data.repository.DataStoreLocalPreferencesRepository
import com.raqeem.app.data.repository.DataStoreSyncStatusRepository
import com.raqeem.app.data.repository.EdgeFunctionAiAssistantRepository
import com.raqeem.app.data.repository.GoalRepositoryImpl
import com.raqeem.app.data.repository.SettingsRepositoryImpl
import com.raqeem.app.data.repository.SubscriptionRepositoryImpl
import com.raqeem.app.data.repository.SupabaseAuthRepository
import com.raqeem.app.data.repository.TransactionRepositoryImpl
import com.raqeem.app.data.repository.TransferRepositoryImpl
import com.raqeem.app.domain.repository.AccountRepository
import com.raqeem.app.domain.repository.AiAssistantRepository
import com.raqeem.app.domain.repository.AppLockRepository
import com.raqeem.app.domain.repository.AuthRepository
import com.raqeem.app.domain.repository.CategoryRepository
import com.raqeem.app.domain.repository.GoalRepository
import com.raqeem.app.domain.repository.LocalPreferencesRepository
import com.raqeem.app.domain.repository.SettingsRepository
import com.raqeem.app.domain.repository.SubscriptionRepository
import com.raqeem.app.domain.repository.SyncStatusRepository
import com.raqeem.app.domain.repository.TransactionRepository
import com.raqeem.app.domain.repository.TransferRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        implementation: SupabaseAuthRepository,
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindAppLockRepository(
        implementation: DataStoreAppLockRepository,
    ): AppLockRepository

    @Binds
    @Singleton
    abstract fun bindAiAssistantRepository(
        implementation: EdgeFunctionAiAssistantRepository,
    ): AiAssistantRepository

    @Binds
    @Singleton
    abstract fun bindSyncStatusRepository(
        implementation: DataStoreSyncStatusRepository,
    ): SyncStatusRepository

    @Binds
    @Singleton
    abstract fun bindLocalPreferencesRepository(
        implementation: DataStoreLocalPreferencesRepository,
    ): LocalPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(
        implementation: AccountRepositoryImpl,
    ): AccountRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        implementation: CategoryRepositoryImpl,
    ): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        implementation: TransactionRepositoryImpl,
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindTransferRepository(
        implementation: TransferRepositoryImpl,
    ): TransferRepository

    @Binds
    @Singleton
    abstract fun bindGoalRepository(
        implementation: GoalRepositoryImpl,
    ): GoalRepository

    @Binds
    @Singleton
    abstract fun bindSubscriptionRepository(
        implementation: SubscriptionRepositoryImpl,
    ): SubscriptionRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        implementation: SettingsRepositoryImpl,
    ): SettingsRepository
}
