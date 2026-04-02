package com.raqeem.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.raqeem.app.data.local.entity.SyncQueueEntity

@Dao
interface SyncQueueDao {

    @Query("SELECT * FROM sync_queue WHERE is_synced = 0 ORDER BY created_at ASC LIMIT :limit")
    suspend fun getPendingItems(limit: Int = 50): List<SyncQueueEntity>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE is_synced = 0")
    suspend fun getPendingCount(): Int

    @Query("SELECT COUNT(*) FROM sync_queue WHERE is_synced = 0 AND attempts >= 5")
    suspend fun getFailedCount(): Int

    @Query("SELECT COUNT(*) > 0 FROM sync_queue WHERE is_synced = 0 AND table_name = :tableName AND record_id = :recordId")
    suspend fun hasPendingItem(tableName: String, recordId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncQueueEntity)

    @Query("UPDATE sync_queue SET is_synced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE sync_queue SET attempts = attempts + 1, last_error = :error WHERE id = :id")
    suspend fun incrementAttempts(id: String, error: String?)

    @Query("DELETE FROM sync_queue WHERE is_synced = 1")
    suspend fun clearSynced()

    @Query("DELETE FROM sync_queue WHERE attempts >= 5")
    suspend fun clearFailed()

    @Query("DELETE FROM sync_queue")
    suspend fun clearAll()
}
