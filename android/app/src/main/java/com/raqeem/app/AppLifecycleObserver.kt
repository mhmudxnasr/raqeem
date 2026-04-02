package com.raqeem.app

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.raqeem.app.domain.repository.AppLockRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLifecycleObserver @Inject constructor(
    private val appLockRepository: AppLockRepository,
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStart(owner: LifecycleOwner) {
        scope.launch {
            appLockRepository.refreshLockState()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        scope.launch {
            appLockRepository.markAppBackgrounded()
            appLockRepository.refreshLockState()
        }
    }
}

