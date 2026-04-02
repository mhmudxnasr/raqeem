package com.raqeem.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.raqeem.app.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Query("SELECT COUNT(*) FROM accounts WHERE deleted_at IS NULL")
    suspend fun getCount(): Int

    @Query("SELECT * FROM accounts ORDER BY sort_order ASC")
    suspend fun getAllOnce(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE deleted_at IS NULL ORDER BY sort_order ASC")
    fun getAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id AND deleted_at IS NULL")
    fun getById(id: String): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts WHERE id = :id AND deleted_at IS NULL")
    suspend fun findById(id: String): AccountEntity?

    @Query("SELECT COALESCE(SUM(balance_cents), 0) FROM accounts WHERE deleted_at IS NULL AND currency = 'USD'")
    suspend fun getTotalUsdBalanceCents(): Int

    @Query("SELECT initial_amount_cents FROM accounts WHERE id = :id AND deleted_at IS NULL")
    suspend fun getInitialAmountCents(id: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<AccountEntity>)

    @Update
    suspend fun update(account: AccountEntity)

    @Query("UPDATE accounts SET deleted_at = :deletedAt, updated_at = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long)

    @Query("UPDATE accounts SET balance_cents = :balanceCents, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateBalance(id: String, balanceCents: Int, updatedAt: Long)

    @Query("UPDATE accounts SET user_id = :newUserId WHERE user_id = :oldUserId")
    suspend fun reassignUser(oldUserId: String, newUserId: String)
}
