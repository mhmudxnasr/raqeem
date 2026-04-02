package com.raqeem.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.raqeem.app.data.local.converter.Converters
import com.raqeem.app.data.local.dao.AccountDao
import com.raqeem.app.data.local.dao.CategoryDao
import com.raqeem.app.data.local.dao.GoalDao
import com.raqeem.app.data.local.dao.SettingsDao
import com.raqeem.app.data.local.dao.SubscriptionDao
import com.raqeem.app.data.local.dao.SyncQueueDao
import com.raqeem.app.data.local.dao.TransactionDao
import com.raqeem.app.data.local.dao.TransferDao
import com.raqeem.app.data.local.entity.AccountEntity
import com.raqeem.app.data.local.entity.CategoryEntity
import com.raqeem.app.data.local.entity.GoalEntity
import com.raqeem.app.data.local.entity.SettingsEntity
import com.raqeem.app.data.local.entity.SubscriptionEntity
import com.raqeem.app.data.local.entity.SyncQueueEntity
import com.raqeem.app.data.local.entity.TransactionEntity
import com.raqeem.app.data.local.entity.TransferEntity

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        TransferEntity::class,
        GoalEntity::class,
        SubscriptionEntity::class,
        SettingsEntity::class,
        SyncQueueEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun transferDao(): TransferDao
    abstract fun goalDao(): GoalDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun settingsDao(): SettingsDao
    abstract fun syncQueueDao(): SyncQueueDao
}
