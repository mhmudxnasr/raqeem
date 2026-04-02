package com.raqeem.app.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.room.withTransaction
import com.raqeem.app.data.local.AppDatabase
import com.raqeem.app.data.local.LOCAL_USER_ID
import com.raqeem.app.data.local.LocalFinanceSyncer
import com.raqeem.app.data.local.dao.AccountDao
import com.raqeem.app.data.local.dao.CategoryDao
import com.raqeem.app.data.local.dao.GoalDao
import com.raqeem.app.data.local.dao.SettingsDao
import com.raqeem.app.data.local.dao.SubscriptionDao
import com.raqeem.app.data.local.dao.SyncQueueDao
import com.raqeem.app.data.local.dao.TransactionDao
import com.raqeem.app.data.local.dao.TransferDao
import com.raqeem.app.data.local.entity.SyncQueueEntity
import com.raqeem.app.data.mapper.toDomain
import com.raqeem.app.data.remote.RemoteAccountRow
import com.raqeem.app.data.remote.RemoteCategoryRow
import com.raqeem.app.data.remote.RemoteGoalRow
import com.raqeem.app.data.remote.RemoteSettingsRow
import com.raqeem.app.data.remote.RemoteSubscriptionRow
import com.raqeem.app.data.remote.RemoteTransactionRow
import com.raqeem.app.data.remote.RemoteTransferRow
import com.raqeem.app.data.remote.toEntity
import com.raqeem.app.domain.model.Result
import com.raqeem.app.domain.repository.SyncStatusRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabaseClient: SupabaseClient,
    private val database: AppDatabase,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val transferDao: TransferDao,
    private val goalDao: GoalDao,
    private val subscriptionDao: SubscriptionDao,
    private val settingsDao: SettingsDao,
    private val syncQueueDao: SyncQueueDao,
    private val localFinanceSyncer: LocalFinanceSyncer,
    private val syncStatusRepository: SyncStatusRepository,
) {

    suspend fun bootstrapForSignedInUser(userId: String): Result<Unit> {
        return try {
            migrateLegacyLocalUser(userId)
            syncNow()
        } catch (throwable: Throwable) {
            Result.Error("Unable to bootstrap local finance data.", throwable)
        }
    }

    suspend fun syncNow(): Result<Unit> {
        if (!isSupabaseConfigured()) {
            syncStatusRepository.setFailed("Supabase credentials are still placeholders.")
            return Result.Error("Supabase credentials are still placeholders.")
        }
        if (!isNetworkAvailable()) {
            syncStatusRepository.setOffline()
            return Result.Error("No active network connection.")
        }
        return runCatching {
            syncStatusRepository.setSyncing()
            pushPendingItems()
            pullRemoteState()
            val pendingCount = syncQueueDao.getPendingCount()
            val failedCount = syncQueueDao.getFailedCount()
            syncStatusRepository.setSynced(
                lastSyncAtMillis = System.currentTimeMillis(),
                pendingCount = pendingCount,
                failedCount = failedCount,
            )
            Unit
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { throwable ->
                val failedCount = syncQueueDao.getFailedCount()
                syncStatusRepository.setFailed(
                    message = throwable.message ?: "Sync failed.",
                    failedCount = failedCount,
                )
                Result.Error("Sync failed.", throwable)
            },
        )
    }

    private suspend fun migrateLegacyLocalUser(newUserId: String) {
        val currentSettings = settingsDao.find()
        val shouldMigrate = currentSettings?.userId == LOCAL_USER_ID ||
            currentSettings == null && (
                accountDao.getCount() > 0 ||
                    categoryDao.getCount() > 0 ||
                    transactionDao.getAllOnce().isNotEmpty()
                )

        if (!shouldMigrate) {
            return
        }

        cleanupRemoteSeedRowsIfFreshUser()

        database.withTransaction {
            accountDao.reassignUser(LOCAL_USER_ID, newUserId)
            categoryDao.reassignUser(LOCAL_USER_ID, newUserId)
            transactionDao.reassignUser(LOCAL_USER_ID, newUserId)
            transferDao.reassignUser(LOCAL_USER_ID, newUserId)
            goalDao.reassignUser(LOCAL_USER_ID, newUserId)
            subscriptionDao.reassignUser(LOCAL_USER_ID, newUserId)

            val migratedSettings = currentSettings?.copy(
                userId = newUserId,
                updatedAt = Clock.System.now(),
            )
            settingsDao.deleteByUserId(LOCAL_USER_ID)
            if (migratedSettings != null) {
                settingsDao.upsert(migratedSettings)
            }

            syncQueueDao.clearAll()
            rebuildQueueFromLocalSnapshots()
        }
    }

    private suspend fun cleanupRemoteSeedRowsIfFreshUser() {
        val hasRemoteTransactions = supabaseClient.from("transactions").select().decodeList<JsonObject>().isNotEmpty()
        val hasRemoteTransfers = supabaseClient.from("transfers").select().decodeList<JsonObject>().isNotEmpty()
        val hasRemoteGoals = supabaseClient.from("goals").select().decodeList<JsonObject>().isNotEmpty()
        val hasRemoteSubscriptions = supabaseClient.from("subscriptions").select().decodeList<JsonObject>().isNotEmpty()

        if (hasRemoteTransactions || hasRemoteTransfers || hasRemoteGoals || hasRemoteSubscriptions) {
            return
        }

        supabaseClient.from("accounts").delete()
        supabaseClient.from("categories").delete()
    }

    private suspend fun rebuildQueueFromLocalSnapshots() {
        accountDao.getAllOnce().forEach { entity ->
            syncQueueDao.insert(
                SyncQueueEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    tableName = "accounts",
                    recordId = entity.id,
                    operation = "upsert",
                    payload = buildJsonObject {
                        put("id", entity.id)
                        put("user_id", entity.userId)
                        put("name", entity.name)
                        put("type", entity.type)
                        put("currency", entity.currency)
                        put("initial_amount_cents", entity.initialAmountCents)
                        put("balance_cents", entity.balanceCents)
                        put("is_hidden", entity.isHidden)
                        put("sort_order", entity.sortOrder)
                        put("updated_at", entity.updatedAt.toString())
                    }.toString(),
                ),
            )
        }
        categoryDao.getAllOnce().forEach { entity ->
            syncQueueDao.insert(
                SyncQueueEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    tableName = "categories",
                    recordId = entity.id,
                    operation = "upsert",
                    payload = buildJsonObject {
                        put("id", entity.id)
                        put("user_id", entity.userId)
                        put("name", entity.name)
                        put("type", entity.type)
                        put("icon", entity.icon)
                        put("color", entity.color)
                        entity.budgetCents?.let { put("budget_cents", it) }
                        put("updated_at", entity.updatedAt.toString())
                    }.toString(),
                ),
            )
        }
        transactionDao.getAllOnce().forEach { entity ->
            syncQueueDao.insert(
                SyncQueueEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    tableName = "transactions",
                    recordId = entity.id,
                    operation = "upsert",
                    payload = buildJsonObject {
                        put("id", entity.id)
                        put("user_id", entity.userId)
                        put("account_id", entity.accountId)
                        entity.categoryId?.let { put("category_id", it) }
                        put("type", entity.type)
                        put("amount_cents", entity.amountCents)
                        put("currency", entity.currency)
                        entity.note?.let { put("note", it) }
                        put("date", entity.date.toString())
                        entity.receiptUrl?.let { put("receipt_url", it) }
                        put("updated_at", entity.updatedAt.toString())
                    }.toString(),
                ),
            )
        }
        transferDao.getAllOnce().forEach { entity ->
            syncQueueDao.insert(
                SyncQueueEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    tableName = "transfers",
                    recordId = entity.id,
                    operation = "upsert",
                    payload = buildJsonObject {
                        put("id", entity.id)
                        put("user_id", entity.userId)
                        put("from_account_id", entity.fromAccountId)
                        put("to_account_id", entity.toAccountId)
                        put("from_amount_cents", entity.fromAmountCents)
                        put("to_amount_cents", entity.toAmountCents)
                        put("from_currency", entity.fromCurrency)
                        put("to_currency", entity.toCurrency)
                        put("exchange_rate", entity.exchangeRate)
                        put("is_currency_conversion", entity.isCurrencyConversion)
                        entity.goalId?.let { put("goal_id", it) }
                        entity.note?.let { put("note", it) }
                        put("date", entity.date.toString())
                        put("updated_at", entity.updatedAt.toString())
                    }.toString(),
                ),
            )
        }
        goalDao.getAllOnce().forEach { entity ->
            syncQueueDao.insert(
                SyncQueueEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    tableName = "goals",
                    recordId = entity.id,
                    operation = "upsert",
                    payload = buildJsonObject {
                        put("id", entity.id)
                        put("user_id", entity.userId)
                        put("name", entity.name)
                        put("target_cents", entity.targetCents)
                        put("current_cents", entity.currentCents)
                        put("currency", entity.currency)
                        entity.deadline?.let { put("deadline", it.toString()) }
                        put("is_completed", entity.isCompleted)
                        put("icon", entity.icon)
                        entity.note?.let { put("note", it) }
                        put("updated_at", entity.updatedAt.toString())
                    }.toString(),
                ),
            )
        }
        subscriptionDao.getAllOnce().forEach { entity ->
            syncQueueDao.insert(
                SyncQueueEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    tableName = "subscriptions",
                    recordId = entity.id,
                    operation = "upsert",
                    payload = buildJsonObject {
                        put("id", entity.id)
                        put("user_id", entity.userId)
                        put("account_id", entity.accountId)
                        entity.categoryId?.let { put("category_id", it) }
                        put("name", entity.name)
                        put("amount_cents", entity.amountCents)
                        put("currency", entity.currency)
                        put("billing_cycle", entity.billingCycle)
                        put("next_billing_date", entity.nextBillingDate.toString())
                        put("is_active", entity.isActive)
                        put("auto_log", entity.autoLog)
                        put("updated_at", entity.updatedAt.toString())
                    }.toString(),
                ),
            )
        }
        settingsDao.find()?.let { entity ->
            syncQueueDao.insert(
                SyncQueueEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    tableName = "settings",
                    recordId = entity.userId,
                    operation = "upsert",
                    payload = buildJsonObject {
                        put("user_id", entity.userId)
                        put("usd_to_egp_rate", entity.usdToEgpRate)
                        entity.defaultAccountId?.let { put("default_account_id", it) }
                        put("analytics_currency", entity.analyticsCurrency)
                        put("updated_at", entity.updatedAt.toString())
                    }.toString(),
                ),
            )
        }
    }

    private suspend fun pushPendingItems() {
        syncQueueDao.getPendingItems(limit = 100).forEach { item ->
            try {
                when (item.operation) {
                    "upsert" -> {
                        val payload = Json.parseToJsonElement(item.payload)
                        supabaseClient.from(item.tableName).upsert(payload)
                    }
                    "delete" -> {
                        supabaseClient.from(item.tableName).delete {
                            filter {
                                eq(recordKey(item.tableName), item.recordId)
                            }
                        }
                    }
                }
                syncQueueDao.markSynced(item.id)
            } catch (throwable: Throwable) {
                syncQueueDao.incrementAttempts(item.id, throwable.message)
            }
        }
        syncQueueDao.clearSynced()
    }

    private suspend fun pullRemoteState() {
        val remoteAccounts = supabaseClient.from("accounts").select().decodeList<RemoteAccountRow>()
        val remoteCategories = supabaseClient.from("categories").select().decodeList<RemoteCategoryRow>()
        val remoteTransactions = supabaseClient.from("transactions").select().decodeList<RemoteTransactionRow>()
        val remoteTransfers = supabaseClient.from("transfers").select().decodeList<RemoteTransferRow>()
        val remoteGoals = supabaseClient.from("goals").select().decodeList<RemoteGoalRow>()
        val remoteSubscriptions = supabaseClient.from("subscriptions").select().decodeList<RemoteSubscriptionRow>()
        val remoteSettings = supabaseClient.from("settings").select().decodeList<RemoteSettingsRow>().firstOrNull()

        database.withTransaction {
            remoteAccounts.forEach { row ->
                val local = accountDao.findById(row.id)
                if (local == null || shouldReplaceLocal("accounts", row.id, row.updated_at, local.updatedAt)) {
                    accountDao.insert(row.toEntity())
                }
            }
            remoteCategories.forEach { row ->
                val local = categoryDao.findById(row.id)
                if (local == null || shouldReplaceLocal("categories", row.id, row.updated_at, local.updatedAt)) {
                    categoryDao.insert(row.toEntity())
                }
            }
            remoteTransactions.forEach { row ->
                val local = transactionDao.findById(row.id)
                if (local == null || shouldReplaceLocal("transactions", row.id, row.updated_at, local.updatedAt)) {
                    transactionDao.insert(row.toEntity())
                }
            }
            remoteTransfers.forEach { row ->
                val local = transferDao.findById(row.id)
                if (local == null || shouldReplaceLocal("transfers", row.id, row.updated_at, local.updatedAt)) {
                    transferDao.insert(row.toEntity())
                }
            }
            remoteGoals.forEach { row ->
                val local = goalDao.findById(row.id)
                if (local == null || shouldReplaceLocal("goals", row.id, row.updated_at, local.updatedAt)) {
                    goalDao.insert(row.toEntity())
                }
            }
            remoteSubscriptions.forEach { row ->
                val local = subscriptionDao.findById(row.id)
                if (local == null || shouldReplaceLocal("subscriptions", row.id, row.updated_at, local.updatedAt)) {
                    subscriptionDao.insert(row.toEntity())
                }
            }
            remoteSettings?.let { row ->
                val local = settingsDao.find()
                if (local == null || shouldReplaceLocal("settings", row.user_id, row.updated_at, local.updatedAt)) {
                    settingsDao.upsert(row.toEntity())
                }
            }
        }

        localFinanceSyncer.syncAccounts(accountDao.getAllOnce().map { it.id }.toSet())
        localFinanceSyncer.syncGoals(goalDao.getAllOnce().map { it.id }.toSet())
    }

    private suspend fun shouldReplaceLocal(
        tableName: String,
        recordId: String,
        remoteUpdatedAt: kotlinx.datetime.Instant,
        localUpdatedAt: kotlinx.datetime.Instant,
    ): Boolean {
        val hasPendingLocalChange = syncQueueDao.hasPendingItem(tableName, recordId)
        return remoteUpdatedAt > localUpdatedAt || !hasPendingLocalChange
    }

    private fun recordKey(tableName: String): String {
        return if (tableName == "settings") "user_id" else "id"
    }

    private fun isSupabaseConfigured(): Boolean {
        return !com.raqeem.app.BuildConfig.SUPABASE_URL.contains("YOUR_PROJECT") &&
            !com.raqeem.app.BuildConfig.SUPABASE_ANON_KEY.contains("YOUR_ANON_KEY")
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
