package com.raqeem.app.data.repository

import com.raqeem.app.data.local.dao.AccountDao
import com.raqeem.app.data.local.dao.SyncQueueDao
import com.raqeem.app.data.local.entity.SyncQueueEntity
import com.raqeem.app.data.mapper.toDomain
import com.raqeem.app.data.mapper.toEntity
import com.raqeem.app.domain.model.Account
import com.raqeem.app.domain.model.Result
import com.raqeem.app.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao,
    private val syncQueueDao: SyncQueueDao,
) : AccountRepository {

    override fun getAll(): Flow<List<Account>> {
        return accountDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getById(id: String): Flow<Account?> {
        return accountDao.getById(id).map { it?.toDomain() }
    }

    override suspend fun add(account: Account): Result<Unit> {
        return try {
            val entity = account.toEntity()
            accountDao.insert(entity)
            enqueueSyncUpsert("accounts", account.id, buildAccountJson(account))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to save account", e)
        }
    }

    override suspend fun update(account: Account): Result<Unit> {
        return try {
            val entity = account.toEntity()
            accountDao.update(entity)
            enqueueSyncUpsert("accounts", account.id, buildAccountJson(account))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to update account", e)
        }
    }

    override suspend fun delete(id: String): Result<Unit> {
        return try {
            val now = Clock.System.now().toEpochMilliseconds()
            accountDao.softDelete(id, now, now)
            enqueueSyncDelete("accounts", id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to delete account", e)
        }
    }

    override suspend fun getNetWorthCents(): Int {
        return accountDao.getTotalUsdBalanceCents()
    }

    private suspend fun enqueueSyncUpsert(table: String, recordId: String, payload: String) {
        syncQueueDao.insert(
            SyncQueueEntity(
                id = UUID.randomUUID().toString(),
                tableName = table,
                recordId = recordId,
                operation = "upsert",
                payload = payload,
            )
        )
    }

    private suspend fun enqueueSyncDelete(table: String, recordId: String) {
        syncQueueDao.insert(
            SyncQueueEntity(
                id = UUID.randomUUID().toString(),
                tableName = table,
                recordId = recordId,
                operation = "delete",
                payload = buildJsonObject { put("id", recordId) }.toString(),
            )
        )
    }

    private fun buildAccountJson(account: Account): String {
        return buildJsonObject {
            put("id", account.id)
            put("user_id", account.userId)
            put("name", account.name)
            put("type", account.type.toApiString())
            put("currency", account.currency.name)
            put("initial_amount_cents", account.initialAmountCents)
            put("balance_cents", account.balanceCents)
            put("is_hidden", account.isHidden)
            put("sort_order", account.sortOrder)
            put("updated_at", Clock.System.now().toString())
        }.toString()
    }
}
