package com.raqeem.app.domain.repository

import com.raqeem.app.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

interface SyncStatusRepository {
    fun observeStatus(): Flow<SyncStatus>
    suspend fun setIdle()
    suspend fun setSyncing()
    suspend fun setOffline()
    suspend fun setFailed(message: String, failedCount: Int = 0)
    suspend fun setSynced(lastSyncAtMillis: Long, pendingCount: Int = 0, failedCount: Int = 0)
}

