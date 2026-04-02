package com.raqeem.app.data.repository

import com.raqeem.app.data.local.LOCAL_USER_ID
import com.raqeem.app.data.local.dao.SettingsDao
import com.raqeem.app.data.local.dao.SyncQueueDao
import com.raqeem.app.data.local.entity.SyncQueueEntity
import com.raqeem.app.data.mapper.toDomain
import com.raqeem.app.data.mapper.toEntity
import com.raqeem.app.domain.model.Result
import com.raqeem.app.domain.model.Settings
import com.raqeem.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val settingsDao: SettingsDao,
    private val syncQueueDao: SyncQueueDao,
) : SettingsRepository {

    override fun get(): Flow<Settings> {
        return settingsDao.get().map { entity ->
            entity?.toDomain() ?: Settings(userId = LOCAL_USER_ID)
        }
    }

    override suspend fun update(settings: Settings): Result<Unit> {
        return try {
            settingsDao.upsert(settings.toEntity())
            enqueueSync(settings)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to update settings", e)
        }
    }

    override suspend fun updateExchangeRate(rate: Double): Result<Unit> {
        return try {
            val current = settingsDao.get().first()?.toDomain() ?: Settings(userId = LOCAL_USER_ID)
            val updated = current.copy(usdToEgpRate = rate)
            settingsDao.upsert(updated.toEntity())
            enqueueSync(updated)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to update exchange rate", e)
        }
    }

    override suspend fun setDefaultAccount(accountId: String?): Result<Unit> {
        return try {
            val current = settingsDao.get().first()?.toDomain() ?: Settings(userId = LOCAL_USER_ID)
            val updated = current.copy(defaultAccountId = accountId)
            settingsDao.upsert(updated.toEntity())
            enqueueSync(updated)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to update default account", e)
        }
    }

    private suspend fun enqueueSync(settings: Settings) {
        syncQueueDao.insert(
            SyncQueueEntity(
                id = UUID.randomUUID().toString(),
                tableName = "settings",
                recordId = settings.userId,
                operation = "upsert",
                payload = buildJsonObject {
                    put("user_id", settings.userId)
                    put("usd_to_egp_rate", settings.usdToEgpRate)
                    settings.defaultAccountId?.let { put("default_account_id", it) }
                    put("analytics_currency", settings.analyticsCurrency.name)
                    put("updated_at", Clock.System.now().toString())
                }.toString(),
            )
        )
    }
}
