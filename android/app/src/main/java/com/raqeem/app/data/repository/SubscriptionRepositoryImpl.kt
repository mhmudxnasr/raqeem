package com.raqeem.app.data.repository

import com.raqeem.app.data.local.dao.SubscriptionDao
import com.raqeem.app.data.local.dao.SyncQueueDao
import com.raqeem.app.data.local.entity.SyncQueueEntity
import com.raqeem.app.data.mapper.toDomain
import com.raqeem.app.data.mapper.toEntity
import com.raqeem.app.domain.model.Result
import com.raqeem.app.domain.model.Subscription
import com.raqeem.app.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import javax.inject.Inject

class SubscriptionRepositoryImpl @Inject constructor(
    private val subscriptionDao: SubscriptionDao,
    private val syncQueueDao: SyncQueueDao,
) : SubscriptionRepository {

    override fun getAll(): Flow<List<Subscription>> {
        return subscriptionDao.getAll().map { entities -> entities.map { it.toDomain() } }
    }

    override fun getActive(): Flow<List<Subscription>> {
        return subscriptionDao.getActive().map { entities -> entities.map { it.toDomain() } }
    }

    override fun getById(id: String): Flow<Subscription?> {
        return subscriptionDao.getById(id).map { it?.toDomain() }
    }

    override suspend fun add(subscription: Subscription): Result<Unit> {
        return try {
            subscriptionDao.insert(subscription.toEntity())
            enqueueSync("upsert", subscription.id, buildSubscriptionJson(subscription))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to save subscription", e)
        }
    }

    override suspend fun update(subscription: Subscription): Result<Unit> {
        return try {
            subscriptionDao.update(subscription.toEntity())
            enqueueSync("upsert", subscription.id, buildSubscriptionJson(subscription))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to update subscription", e)
        }
    }

    override suspend fun delete(id: String): Result<Unit> {
        return try {
            val now = Clock.System.now().toEpochMilliseconds()
            subscriptionDao.softDelete(id, now, now)
            enqueueSync("delete", id, buildJsonObject { put("id", id) }.toString())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to delete subscription", e)
        }
    }

    private suspend fun enqueueSync(operation: String, recordId: String, payload: String) {
        syncQueueDao.insert(
            SyncQueueEntity(
                id = UUID.randomUUID().toString(),
                tableName = "subscriptions",
                recordId = recordId,
                operation = operation,
                payload = payload,
            )
        )
    }

    private fun buildSubscriptionJson(s: Subscription): String {
        return buildJsonObject {
            put("id", s.id)
            put("user_id", s.userId)
            put("account_id", s.accountId)
            s.categoryId?.let { put("category_id", it) }
            put("name", s.name)
            put("amount_cents", s.amountCents)
            put("currency", s.currency.name)
            put("billing_cycle", s.billingCycle.toApiString())
            put("next_billing_date", s.nextBillingDate.toString())
            put("is_active", s.isActive)
            put("auto_log", s.autoLog)
            put("updated_at", Clock.System.now().toString())
        }.toString()
    }
}
