package com.raqeem.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.raqeem.app.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Query("SELECT * FROM goals ORDER BY is_completed ASC, created_at DESC")
    suspend fun getAllOnce(): List<GoalEntity>

    @Query("SELECT * FROM goals WHERE deleted_at IS NULL ORDER BY is_completed ASC, created_at DESC")
    fun getAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id AND deleted_at IS NULL")
    fun getById(id: String): Flow<GoalEntity?>

    @Query("SELECT * FROM goals WHERE id = :id AND deleted_at IS NULL")
    suspend fun findById(id: String): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: GoalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(goals: List<GoalEntity>)

    @Update
    suspend fun update(goal: GoalEntity)

    @Query("UPDATE goals SET is_completed = 1, updated_at = :updatedAt WHERE id = :id")
    suspend fun markComplete(id: String, updatedAt: Long)

    @Query("UPDATE goals SET current_cents = :currentCents, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateCurrentCents(id: String, currentCents: Int, updatedAt: Long)

    @Query("UPDATE goals SET deleted_at = :deletedAt, updated_at = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long)

    @Query("UPDATE goals SET user_id = :newUserId WHERE user_id = :oldUserId")
    suspend fun reassignUser(oldUserId: String, newUserId: String)
}
