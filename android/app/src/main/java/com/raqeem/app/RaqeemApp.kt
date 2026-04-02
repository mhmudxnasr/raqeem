package com.raqeem.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.lifecycle.ProcessLifecycleOwner
import com.raqeem.app.notifications.NotificationsWorker
import com.raqeem.app.notifications.RaqeemNotificationManager
import com.raqeem.app.sync.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class RaqeemApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var appLifecycleObserver: AppLifecycleObserver

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        RaqeemNotificationManager.ensureChannels(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)
        setupPeriodicSync()
        setupPeriodicNotifications()
    }

    private fun setupPeriodicSync() {
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag("raqeem_sync")
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "raqeem_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest,
        )
    }

    private fun setupPeriodicNotifications() {
        val notificationRequest = PeriodicWorkRequestBuilder<NotificationsWorker>(12, TimeUnit.HOURS)
            .addTag("raqeem_notifications")
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "raqeem_notifications",
            ExistingPeriodicWorkPolicy.KEEP,
            notificationRequest,
        )
    }
}
