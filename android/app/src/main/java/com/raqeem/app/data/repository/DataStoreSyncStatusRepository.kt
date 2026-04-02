package com.raqeem.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.raqeem.app.domain.model.SyncStatus
import com.raqeem.app.domain.repository.SyncStatusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreSyncStatusRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SyncStatusRepository {

    override fun observeStatus(): Flow<SyncStatus> {
        return dataStore.data.map { prefs ->
            when (prefs[STATUS_KIND] ?: STATUS_IDLE) {
                STATUS_SYNCING -> SyncStatus.Syncing
                STATUS_OFFLINE -> SyncStatus.Offline
                STATUS_FAILED -> SyncStatus.Failed(
                    message = prefs[STATUS_MESSAGE] ?: "Sync failed.",
                    failedCount = prefs[FAILED_COUNT] ?: 0,
                )
                STATUS_SYNCED -> SyncStatus.Synced(
                    lastSyncAtMillis = prefs[LAST_SYNC_AT] ?: 0L,
                    pendingCount = prefs[PENDING_COUNT] ?: 0,
                    failedCount = prefs[FAILED_COUNT] ?: 0,
                )
                else -> SyncStatus.Idle
            }
        }
    }

    override suspend fun setIdle() {
        updateStatus(kind = STATUS_IDLE, message = null)
    }

    override suspend fun setSyncing() {
        updateStatus(kind = STATUS_SYNCING, message = null)
    }

    override suspend fun setOffline() {
        updateStatus(kind = STATUS_OFFLINE, message = "Offline")
    }

    override suspend fun setFailed(message: String, failedCount: Int) {
        updateStatus(kind = STATUS_FAILED, message = message, failedCount = failedCount)
    }

    override suspend fun setSynced(lastSyncAtMillis: Long, pendingCount: Int, failedCount: Int) {
        dataStore.edit { prefs ->
            prefs[STATUS_KIND] = STATUS_SYNCED
            prefs[LAST_SYNC_AT] = lastSyncAtMillis
            prefs[PENDING_COUNT] = pendingCount
            prefs[FAILED_COUNT] = failedCount
            prefs.remove(STATUS_MESSAGE)
        }
    }

    private suspend fun updateStatus(
        kind: String,
        message: String?,
        failedCount: Int = 0,
    ) {
        dataStore.edit { prefs ->
            prefs[STATUS_KIND] = kind
            prefs[FAILED_COUNT] = failedCount
            if (message == null) {
                prefs.remove(STATUS_MESSAGE)
            } else {
                prefs[STATUS_MESSAGE] = message
            }
        }
    }

    private companion object {
        private const val STATUS_IDLE = "idle"
        private const val STATUS_SYNCING = "syncing"
        private const val STATUS_OFFLINE = "offline"
        private const val STATUS_FAILED = "failed"
        private const val STATUS_SYNCED = "synced"

        private val STATUS_KIND = stringPreferencesKey("sync_status_kind")
        private val STATUS_MESSAGE = stringPreferencesKey("sync_status_message")
        private val LAST_SYNC_AT = longPreferencesKey("sync_last_sync_at")
        private val PENDING_COUNT = intPreferencesKey("sync_pending_count")
        private val FAILED_COUNT = intPreferencesKey("sync_failed_count")
    }
}

