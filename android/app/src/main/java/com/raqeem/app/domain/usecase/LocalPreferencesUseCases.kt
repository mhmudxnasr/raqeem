package com.raqeem.app.domain.usecase

import com.raqeem.app.domain.model.LocalPreferences
import com.raqeem.app.domain.model.Result
import com.raqeem.app.domain.repository.LocalPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLocalPreferencesUseCase @Inject constructor(
    private val repository: LocalPreferencesRepository,
) {
    operator fun invoke(): Flow<LocalPreferences> = repository.observe()
}

class SetAiInsightsEnabledUseCase @Inject constructor(
    private val repository: LocalPreferencesRepository,
) {
    suspend operator fun invoke(enabled: Boolean): Result<Unit> = repository.setAiInsightsEnabled(enabled)
}

class SetBudgetWarningsEnabledUseCase @Inject constructor(
    private val repository: LocalPreferencesRepository,
) {
    suspend operator fun invoke(enabled: Boolean): Result<Unit> = repository.setBudgetWarningsEnabled(enabled)
}

class SetSubscriptionRemindersEnabledUseCase @Inject constructor(
    private val repository: LocalPreferencesRepository,
) {
    suspend operator fun invoke(enabled: Boolean): Result<Unit> = repository.setSubscriptionRemindersEnabled(enabled)
}

class SetWeeklySummaryEnabledUseCase @Inject constructor(
    private val repository: LocalPreferencesRepository,
) {
    suspend operator fun invoke(enabled: Boolean): Result<Unit> = repository.setWeeklySummaryEnabled(enabled)
}
