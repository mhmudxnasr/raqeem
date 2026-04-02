package com.raqeem.app.domain.repository

import com.raqeem.app.domain.model.LocalPreferences
import com.raqeem.app.domain.model.Result
import kotlinx.coroutines.flow.Flow

interface LocalPreferencesRepository {
    fun observe(): Flow<LocalPreferences>
    suspend fun setAiInsightsEnabled(enabled: Boolean): Result<Unit>
    suspend fun setBudgetWarningsEnabled(enabled: Boolean): Result<Unit>
    suspend fun setSubscriptionRemindersEnabled(enabled: Boolean): Result<Unit>
    suspend fun setWeeklySummaryEnabled(enabled: Boolean): Result<Unit>
}
