package com.raqeem.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.raqeem.app.domain.model.LocalPreferences
import com.raqeem.app.domain.model.Result
import com.raqeem.app.domain.repository.LocalPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreLocalPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : LocalPreferencesRepository {

    override fun observe(): Flow<LocalPreferences> {
        return dataStore.data.map { prefs ->
            LocalPreferences(
                aiInsightsEnabled = prefs[AI_INSIGHTS_ENABLED] ?: true,
                budgetWarningsEnabled = prefs[BUDGET_WARNINGS_ENABLED] ?: true,
                subscriptionRemindersEnabled = prefs[SUBSCRIPTION_REMINDERS_ENABLED] ?: true,
                weeklySummaryEnabled = prefs[WEEKLY_SUMMARY_ENABLED] ?: true,
            )
        }
    }

    override suspend fun setAiInsightsEnabled(enabled: Boolean): Result<Unit> = setFlag(AI_INSIGHTS_ENABLED, enabled)

    override suspend fun setBudgetWarningsEnabled(enabled: Boolean): Result<Unit> = setFlag(BUDGET_WARNINGS_ENABLED, enabled)

    override suspend fun setSubscriptionRemindersEnabled(enabled: Boolean): Result<Unit> = setFlag(SUBSCRIPTION_REMINDERS_ENABLED, enabled)

    override suspend fun setWeeklySummaryEnabled(enabled: Boolean): Result<Unit> = setFlag(WEEKLY_SUMMARY_ENABLED, enabled)

    private suspend fun setFlag(key: Preferences.Key<Boolean>, enabled: Boolean): Result<Unit> {
        dataStore.edit { prefs ->
            prefs[key] = enabled
        }
        return Result.Success(Unit)
    }

    private companion object {
        private val AI_INSIGHTS_ENABLED = booleanPreferencesKey("ai_insights_enabled")
        private val BUDGET_WARNINGS_ENABLED = booleanPreferencesKey("budget_warnings_enabled")
        private val SUBSCRIPTION_REMINDERS_ENABLED = booleanPreferencesKey("subscription_reminders_enabled")
        private val WEEKLY_SUMMARY_ENABLED = booleanPreferencesKey("weekly_summary_enabled")
    }
}
