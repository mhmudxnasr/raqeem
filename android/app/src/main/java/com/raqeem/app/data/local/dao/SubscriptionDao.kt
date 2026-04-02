package com.raqeem.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.raqeem.app.data.local.entity.SubscriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {

    @Query("SELECT * FROM subscriptions ORDER BY next_billing_date ASC")
    suspend fun getAllOnce(): List<SubscriptionEntity>

    @Query("SELECT * FROM subscriptions WHERE deleted_at IS NULL ORDER BY next_billing_date ASC")
    fun getAll(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE is_active = 1 AND deleted_at IS NULL ORDER BY next_billing_date ASC")
    fun getActive(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE id = :id AND deleted_at IS NULL")
    fun getById(id: String): Flow<SubscriptionEntity?>

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    suspend fun findById(id: String): SubscriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subscription: SubscriptionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(subscriptions: List<SubscriptionEntity>)

    @Update
    suspend fun update(subscription: SubscriptionEntity)

    @Query("UPDATE subscriptions SET deleted_at = :deletedAt, updated_at = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM subscriptions WHERE account_id = :accountId AND deleted_at IS NULL")
    suspend fun countActiveByAccount(accountId: String): Int

    @Query("SELECT COUNT(*) FROM subscriptions WHERE category_id = :categoryId AND deleted_at IS NULL")
    suspend fun countActiveByCategory(categoryId: String): Int

    @Query("UPDATE subscriptions SET user_id = :newUserId WHERE user_id = :oldUserId")
    suspend fun reassignUser(oldUserId: String, newUserId: String)
}
