package com.raqeem.app.domain.repository

import com.raqeem.app.domain.model.Result
import com.raqeem.app.domain.model.UnlockState
import kotlinx.coroutines.flow.Flow

interface AppLockRepository {
    fun observeUnlockState(): Flow<UnlockState>
    suspend fun refreshLockState()
    suspend fun markAppBackgrounded()
    suspend fun markUnlocked()
    suspend fun setLockOnLaunchEnabled(enabled: Boolean): Result<Unit>
    suspend fun setBiometricEnabled(enabled: Boolean): Result<Unit>
    suspend fun setPin(pin: String): Result<Unit>
    suspend fun clearPin(): Result<Unit>
    suspend fun verifyPin(pin: String): Boolean
    suspend fun unlockWithBiometric(): Result<Unit>
    suspend fun lockNow(): Result<Unit>
}

