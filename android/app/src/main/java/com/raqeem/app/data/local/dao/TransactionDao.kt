package com.raqeem.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.raqeem.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE deleted_at IS NULL ORDER BY date DESC, created_at DESC")
    fun getAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC, created_at DESC")
    suspend fun getAllOnce(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE deleted_at IS NULL ORDER BY date DESC, created_at DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE account_id = :accountId AND deleted_at IS NULL ORDER BY date DESC, created_at DESC")
    fun getByAccount(accountId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date >= :start AND date <= :end AND deleted_at IS NULL ORDER BY date DESC, created_at DESC")
    fun getByDateRange(start: String, end: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id AND deleted_at IS NULL")
    fun getById(id: String): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE id = :id AND deleted_at IS NULL")
    suspend fun findById(id: String): TransactionEntity?

    @Query(
        "SELECT transactions.* FROM transactions " +
            "LEFT JOIN categories ON categories.id = transactions.category_id AND categories.deleted_at IS NULL " +
            "WHERE transactions.deleted_at IS NULL AND (" +
            "transactions.note LIKE '%' || :query || '%' OR " +
            "categories.name LIKE '%' || :query || '%' OR " +
            "(:amountQuery IS NOT NULL AND transactions.amount_cents = :amountQuery)" +
            ") ORDER BY transactions.date DESC, transactions.created_at DESC"
    )
    fun search(query: String, amountQuery: Int?): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("UPDATE transactions SET deleted_at = :deletedAt, updated_at = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long)

    @Query(
        "SELECT COALESCE(SUM(CASE WHEN type = 'income' THEN amount_cents ELSE -amount_cents END), 0) " +
            "FROM transactions WHERE account_id = :accountId AND deleted_at IS NULL"
    )
    suspend fun getBalanceDeltaForAccount(accountId: String): Int

    @Query("SELECT COALESCE(SUM(CASE WHEN type = 'income' THEN amount_cents ELSE 0 END), 0) FROM transactions WHERE account_id = :accountId AND deleted_at IS NULL AND date >= :monthStart AND date <= :monthEnd")
    suspend fun getMonthlyIncome(accountId: String, monthStart: String, monthEnd: String): Int

    @Query("SELECT COALESCE(SUM(CASE WHEN type = 'expense' THEN amount_cents ELSE 0 END), 0) FROM transactions WHERE account_id = :accountId AND deleted_at IS NULL AND date >= :monthStart AND date <= :monthEnd")
    suspend fun getMonthlyExpenses(accountId: String, monthStart: String, monthEnd: String): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE account_id = :accountId AND deleted_at IS NULL")
    suspend fun countActiveByAccount(accountId: String): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE category_id = :categoryId AND deleted_at IS NULL")
    suspend fun countActiveByCategory(categoryId: String): Int

    @Query("UPDATE transactions SET user_id = :newUserId WHERE user_id = :oldUserId")
    suspend fun reassignUser(oldUserId: String, newUserId: String)
}
