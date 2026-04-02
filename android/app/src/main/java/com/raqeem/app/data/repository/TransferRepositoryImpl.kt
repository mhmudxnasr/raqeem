package com.raqeem.app.data.repository

import com.raqeem.app.data.local.LocalFinanceSyncer
import com.raqeem.app.data.local.dao.SyncQueueDao
import com.raqeem.app.data.local.dao.TransferDao
import com.raqeem.app.data.local.entity.SyncQueueEntity
import com.raqeem.app.data.mapper.toDomain
import com.raqeem.app.data.mapper.toEntity
import com.raqeem.app.domain.model.Result
import com.raqeem.app.domain.model.Transfer
import com.raqeem.app.domain.repository.TransferRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import javax.inject.Inject

class TransferRepositoryImpl @Inject constructor(
    private val transferDao: TransferDao,
    private val syncQueueDao: SyncQueueDao,
    private val localFinanceSyncer: LocalFinanceSyncer,
) : TransferRepository {

    override fun getAll(): Flow<List<Transfer>> {
        return transferDao.getAll().map { entities -> entities.map { it.toDomain() } }
    }

    override fun getByAccount(accountId: String): Flow<List<Transfer>> {
        return transferDao.getByAccount(accountId).map { entities -> entities.map { it.toDomain() } }
    }

    override fun getById(id: String): Flow<Transfer?> {
        return transferDao.getById(id).map { it?.toDomain() }
    }

    override suspend fun add(transfer: Transfer): Result<Unit> {
        return try {
            transferDao.insert(transfer.toEntity())
            enqueueSync("upsert", transfer.id, buildTransferJson(transfer))
            localFinanceSyncer.syncAccounts(setOf(transfer.fromAccountId, transfer.toAccountId))
            transfer.goalId?.let { goalId ->
                localFinanceSyncer.syncGoals(setOf(goalId))
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to save transfer", e)
        }
    }

    override suspend fun update(transfer: Transfer): Result<Unit> {
        return try {
            val existing = transferDao.findById(transfer.id)
            transferDao.update(transfer.toEntity())
            enqueueSync("upsert", transfer.id, buildTransferJson(transfer))
            localFinanceSyncer.syncAccounts(
                setOfNotNull(
                    existing?.fromAccountId,
                    existing?.toAccountId,
                    transfer.fromAccountId,
                    transfer.toAccountId,
                ),
            )
            localFinanceSyncer.syncGoals(
                setOfNotNull(
                    existing?.goalId,
                    transfer.goalId,
                ),
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to update transfer", e)
        }
    }

    override suspend fun delete(id: String): Result<Unit> {
        return try {
            val existing = transferDao.findById(id)
            val now = Clock.System.now().toEpochMilliseconds()
            transferDao.softDelete(id, now, now)
            enqueueSync("delete", id, buildJsonObject { put("id", id) }.toString())
            if (existing != null) {
                localFinanceSyncer.syncAccounts(setOf(existing.fromAccountId, existing.toAccountId))
                existing.goalId?.let { goalId ->
                    localFinanceSyncer.syncGoals(setOf(goalId))
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to delete transfer", e)
        }
    }

    private suspend fun enqueueSync(operation: String, recordId: String, payload: String) {
        syncQueueDao.insert(
            SyncQueueEntity(
                id = UUID.randomUUID().toString(),
                tableName = "transfers",
                recordId = recordId,
                operation = operation,
                payload = payload,
            )
        )
    }

    private fun buildTransferJson(t: Transfer): String {
        return buildJsonObject {
            put("id", t.id)
            put("user_id", t.userId)
            put("from_account_id", t.fromAccountId)
            put("to_account_id", t.toAccountId)
            put("from_amount_cents", t.fromAmountCents)
            put("to_amount_cents", t.toAmountCents)
            put("from_currency", t.fromCurrency.name)
            put("to_currency", t.toCurrency.name)
            put("exchange_rate", t.exchangeRate)
            put("is_currency_conversion", t.isCurrencyConversion)
            t.goalId?.let { put("goal_id", it) }
            t.note?.let { put("note", it) }
            put("date", t.date.toString())
            put("updated_at", Clock.System.now().toString())
        }.toString()
    }
}
