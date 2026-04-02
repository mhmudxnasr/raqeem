package com.raqeem.app.data.repository

import com.raqeem.app.data.local.LocalFinanceSyncer
import com.raqeem.app.data.local.dao.SyncQueueDao
import com.raqeem.app.data.local.dao.TransactionDao
import com.raqeem.app.data.local.entity.SyncQueueEntity
import com.raqeem.app.data.mapper.toDomain
import com.raqeem.app.data.mapper.toEntity
import com.raqeem.app.domain.model.Result
import com.raqeem.app.domain.model.Transaction
import com.raqeem.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import javax.inject.Inject
import kotlin.math.roundToInt

class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val syncQueueDao: SyncQueueDao,
    private val localFinanceSyncer: LocalFinanceSyncer,
) : TransactionRepository {

    override fun getAll(): Flow<List<Transaction>> {
        return transactionDao.getAll().map { entities -> entities.map { it.toDomain() } }
    }

    override fun getRecent(limit: Int): Flow<List<Transaction>> {
        return transactionDao.getRecent(limit).map { entities -> entities.map { it.toDomain() } }
    }

    override fun getByAccount(accountId: String): Flow<List<Transaction>> {
        return transactionDao.getByAccount(accountId).map { entities -> entities.map { it.toDomain() } }
    }

    override fun getByDateRange(start: LocalDate, end: LocalDate): Flow<List<Transaction>> {
        return transactionDao.getByDateRange(start.toString(), end.toString())
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun getById(id: String): Flow<Transaction?> {
        return transactionDao.getById(id).map { it?.toDomain() }
    }

    override fun search(query: String): Flow<List<Transaction>> {
        val amountQuery = query
            .replace("$", "")
            .replace(",", "")
            .trim()
            .toDoubleOrNull()
            ?.let { amount ->
                (amount * 100).roundToInt()
            }

        return transactionDao.search(query, amountQuery).map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun add(transaction: Transaction): Result<Unit> {
        return try {
            transactionDao.insert(transaction.toEntity())
            enqueueSync("upsert", "transactions", transaction.id, buildTransactionJson(transaction))
            localFinanceSyncer.syncAccounts(setOf(transaction.accountId))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to save transaction", e)
        }
    }

    override suspend fun update(transaction: Transaction): Result<Unit> {
        return try {
            val existing = transactionDao.findById(transaction.id)
            transactionDao.update(transaction.toEntity())
            enqueueSync("upsert", "transactions", transaction.id, buildTransactionJson(transaction))
            localFinanceSyncer.syncAccounts(
                setOfNotNull(
                    existing?.accountId,
                    transaction.accountId,
                ),
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to update transaction", e)
        }
    }

    override suspend fun delete(id: String): Result<Unit> {
        return try {
            val existing = transactionDao.findById(id)
            val now = Clock.System.now().toEpochMilliseconds()
            transactionDao.softDelete(id, now, now)
            enqueueSync("delete", "transactions", id, buildJsonObject { put("id", id) }.toString())
            existing?.accountId?.let { accountId ->
                localFinanceSyncer.syncAccounts(setOf(accountId))
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to delete transaction", e)
        }
    }

    private suspend fun enqueueSync(operation: String, table: String, recordId: String, payload: String) {
        syncQueueDao.insert(
            SyncQueueEntity(
                id = UUID.randomUUID().toString(),
                tableName = table,
                recordId = recordId,
                operation = operation,
                payload = payload,
            )
        )
    }

    private fun buildTransactionJson(tx: Transaction): String {
        return buildJsonObject {
            put("id", tx.id)
            put("user_id", tx.userId)
            put("account_id", tx.accountId)
            tx.categoryId?.let { put("category_id", it) }
            put("type", tx.type.toApiString())
            put("amount_cents", tx.amountCents)
            put("currency", tx.currency.name)
            tx.note?.let { put("note", it) }
            put("date", tx.date.toString())
            tx.receiptUrl?.let { put("receipt_url", it) }
            put("updated_at", Clock.System.now().toString())
        }.toString()
    }
}
