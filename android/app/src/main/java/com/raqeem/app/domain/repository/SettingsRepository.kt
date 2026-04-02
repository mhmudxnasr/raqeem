package com.raqeem.app.domain.repository

import com.raqeem.app.domain.model.Result
import com.raqeem.app.domain.model.Settings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun get(): Flow<Settings>
    suspend fun update(settings: Settings): Result<Unit>
    suspend fun updateExchangeRate(rate: Double): Result<Unit>
    suspend fun setDefaultAccount(accountId: String?): Result<Unit>
}
