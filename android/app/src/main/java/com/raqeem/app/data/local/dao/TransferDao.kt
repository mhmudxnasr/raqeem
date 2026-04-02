package com.raqeem.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.raqeem.app.data.local.entity.TransferEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferDao {

    @Query("SELECT * FROM transfers WHERE deleted_at IS NULL ORDER BY date DESC, created_at DESC")
    fun getAll(): Flow<List<TransferEntity>>

    @Query("SELECT * FROM transfers ORDER BY date DESC, created_at DESC")
    suspend fun getAllOnce(): List<TransferEntity>

    @Query("SELECT * FROM transfers WHERE (from_account_id = :accountId OR to_account_id = :accountId) AND deleted_at IS NULL ORDER BY date DESC")
    fun getByAccount(accountId: String): Flow<List<TransferEntity>>

    @Query("SELECT * FROM transfers WHERE id = :id AND deleted_at IS NULL")
    fun getById(id: String): Flow<TransferEntity?>

    @Query("SELECT * FROM transfers WHERE id = :id AND deleted_at IS NULL")
    suspend fun findById(id: String): TransferEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transfer: TransferEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transfers: List<TransferEntity>)

    @Update
    suspend fun update(transfer: TransferEntity)

    @Query("UPDATE transfers SET deleted_at = :deletedAt, updated_at = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long)

    @Query("SELECT COALESCE(SUM(to_amount_cents), 0) FROM transfers WHERE to_account_id = :accountId AND deleted_at IS NULL")
    suspend fun getIncomingTotal(accountId: String): Int

    @Query("SELECT COALESCE(SUM(from_amount_cents), 0) FROM transfers WHERE from_account_id = :accountId AND deleted_at IS NULL")
    suspend fun getOutgoingTotal(accountId: String): Int

    @Query(
        "SELECT CAST(COALESCE(SUM(CASE " +
            "WHEN from_currency = 'USD' THEN from_amount_cents " +
            "ELSE ROUND((from_amount_cents * 1.0) / exchange_rate) END), 0) AS INTEGER) " +
            "FROM transfers WHERE goal_id = :goalId AND deleted_at IS NULL"
    )
    suspend fun getGoalFundingTotal(goalId: String): Int

    @Query("SELECT COUNT(*) FROM transfers WHERE (from_account_id = :accountId OR to_account_id = :accountId) AND deleted_at IS NULL")
    suspend fun countActiveByAccount(accountId: String): Int

    @Query("UPDATE transfers SET user_id = :newUserId WHERE user_id = :oldUserId")
    suspend fun reassignUser(oldUserId: String, newUserId: String)
}
