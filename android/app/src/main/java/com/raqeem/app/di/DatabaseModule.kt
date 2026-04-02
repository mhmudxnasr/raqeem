package com.raqeem.app.di

import android.content.Context
import androidx.room.Room
import com.raqeem.app.data.local.AppDatabase
import com.raqeem.app.data.local.dao.AccountDao
import com.raqeem.app.data.local.dao.CategoryDao
import com.raqeem.app.data.local.dao.GoalDao
import com.raqeem.app.data.local.dao.SettingsDao
import com.raqeem.app.data.local.dao.SubscriptionDao
import com.raqeem.app.data.local.dao.SyncQueueDao
import com.raqeem.app.data.local.dao.TransactionDao
import com.raqeem.app.data.local.dao.TransferDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "raqeem.db",
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideAccountDao(database: AppDatabase): AccountDao = database.accountDao()

    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideTransactionDao(database: AppDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun provideTransferDao(database: AppDatabase): TransferDao = database.transferDao()

    @Provides
    fun provideGoalDao(database: AppDatabase): GoalDao = database.goalDao()

    @Provides
    fun provideSubscriptionDao(database: AppDatabase): SubscriptionDao = database.subscriptionDao()

    @Provides
    fun provideSettingsDao(database: AppDatabase): SettingsDao = database.settingsDao()

    @Provides
    fun provideSyncQueueDao(database: AppDatabase): SyncQueueDao = database.syncQueueDao()
}
