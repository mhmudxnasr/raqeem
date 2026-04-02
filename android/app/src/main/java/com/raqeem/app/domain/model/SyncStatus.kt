package com.raqeem.app.domain.model

sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Syncing : SyncStatus
    data object Offline : SyncStatus
    data class Failed(
        val message: String,
        val failedCount: Int = 0,
    ) : SyncStatus
    data class Synced(
        val lastSyncAtMillis: Long,
        val pendingCount: Int = 0,
        val failedCount: Int = 0,
    ) : SyncStatus
}

