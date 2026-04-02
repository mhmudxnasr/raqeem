package com.raqeem.app.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.raqeem.app.data.local.dao.SyncQueueDao
import com.raqeem.app.domain.model.Result
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParameters: WorkerParameters,
    private val syncQueueDao: SyncQueueDao,
    private val syncService: SyncService,
) : CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result {
        val pendingCount = syncQueueDao.getPendingCount()
        if (pendingCount == 0) {
            return Result.success()
        }

        return when (val result = syncService.syncNow()) {
            is com.raqeem.app.domain.model.Result.Success -> {
                Log.i(TAG, "Sync completed for $pendingCount queued item(s).")
                Result.success()
            }
            is com.raqeem.app.domain.model.Result.Error -> {
                Log.w(TAG, "Sync failed: ${result.message}")
                Result.retry()
            }
            com.raqeem.app.domain.model.Result.Loading -> Result.retry()
        }
    }

    companion object {
        private const val TAG = "RaqeemSyncWorker"
    }
}
