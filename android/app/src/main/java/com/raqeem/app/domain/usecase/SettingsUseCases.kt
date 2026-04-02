package com.raqeem.app.domain.usecase

import com.raqeem.app.domain.model.Result
import com.raqeem.app.domain.model.Settings
import com.raqeem.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<Settings> {
        return repository.get()
    }
}

class UpdateExchangeRateUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(rate: Double): Result<Unit> {
        if (rate <= 0) {
            return Result.Error("Exchange rate must be greater than 0")
        }
        return repository.updateExchangeRate(rate)
    }
}

class SetDefaultAccountUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(accountId: String?): Result<Unit> {
        return repository.setDefaultAccount(accountId)
    }
}
