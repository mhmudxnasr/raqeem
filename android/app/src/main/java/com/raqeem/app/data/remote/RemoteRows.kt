package com.raqeem.app.data.remote

import com.raqeem.app.data.local.entity.AccountEntity
import com.raqeem.app.data.local.entity.CategoryEntity
import com.raqeem.app.data.local.entity.GoalEntity
import com.raqeem.app.data.local.entity.SettingsEntity
import com.raqeem.app.data.local.entity.SubscriptionEntity
import com.raqeem.app.data.local.entity.TransactionEntity
import com.raqeem.app.data.local.entity.TransferEntity
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class RemoteAccountRow(
    val id: String,
    val user_id: String,
    val name: String,
    val type: String,
    val currency: String,
    val initial_amount_cents: Int,
    val balance_cents: Int,
    val is_hidden: Boolean = false,
    val sort_order: Int = 0,
    val created_at: Instant,
    val updated_at: Instant,
    val deleted_at: Instant? = null,
)

@Serializable
data class RemoteCategoryRow(
    val id: String,
    val user_id: String,
    val name: String,
    val type: String,
    val icon: String,
    val color: String,
    val budget_cents: Int? = null,
    val created_at: Instant,
    val updated_at: Instant,
    val deleted_at: Instant? = null,
)

@Serializable
data class RemoteTransactionRow(
    val id: String,
    val user_id: String,
    val account_id: String,
    val category_id: String? = null,
    val type: String,
    val amount_cents: Int,
    val currency: String,
    val note: String? = null,
    val date: LocalDate,
    val receipt_url: String? = null,
    val created_at: Instant,
    val updated_at: Instant,
    val deleted_at: Instant? = null,
)

@Serializable
data class RemoteTransferRow(
    val id: String,
    val user_id: String,
    val from_account_id: String,
    val to_account_id: String,
    val from_amount_cents: Int,
    val to_amount_cents: Int,
    val from_currency: String,
    val to_currency: String,
    val exchange_rate: Double,
    val is_currency_conversion: Boolean = false,
    val goal_id: String? = null,
    val note: String? = null,
    val date: LocalDate,
    val created_at: Instant,
    val updated_at: Instant,
    val deleted_at: Instant? = null,
)

@Serializable
data class RemoteGoalRow(
    val id: String,
    val user_id: String,
    val name: String,
    val target_cents: Int,
    val current_cents: Int,
    val currency: String,
    val deadline: LocalDate? = null,
    val is_completed: Boolean = false,
    val icon: String,
    val note: String? = null,
    val created_at: Instant,
    val updated_at: Instant,
    val deleted_at: Instant? = null,
)

@Serializable
data class RemoteSubscriptionRow(
    val id: String,
    val user_id: String,
    val account_id: String,
    val category_id: String? = null,
    val name: String,
    val amount_cents: Int,
    val currency: String,
    val billing_cycle: String,
    val next_billing_date: LocalDate,
    val is_active: Boolean = true,
    val auto_log: Boolean = false,
    val created_at: Instant,
    val updated_at: Instant,
    val deleted_at: Instant? = null,
)

@Serializable
data class RemoteSettingsRow(
    val user_id: String,
    val usd_to_egp_rate: Double,
    val default_account_id: String? = null,
    val analytics_currency: String = "USD",
    val created_at: Instant,
    val updated_at: Instant,
)

fun RemoteAccountRow.toEntity(): AccountEntity = AccountEntity(
    id = id,
    userId = user_id,
    name = name,
    type = type,
    currency = currency,
    initialAmountCents = initial_amount_cents,
    balanceCents = balance_cents,
    isHidden = is_hidden,
    sortOrder = sort_order,
    createdAt = created_at,
    updatedAt = updated_at,
    deletedAt = deleted_at,
)

fun RemoteCategoryRow.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    userId = user_id,
    name = name,
    type = type,
    icon = icon,
    color = color,
    budgetCents = budget_cents,
    createdAt = created_at,
    updatedAt = updated_at,
    deletedAt = deleted_at,
)

fun RemoteTransactionRow.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    userId = user_id,
    accountId = account_id,
    categoryId = category_id,
    type = type,
    amountCents = amount_cents,
    currency = currency,
    note = note,
    date = date,
    receiptUrl = receipt_url,
    createdAt = created_at,
    updatedAt = updated_at,
    deletedAt = deleted_at,
)

fun RemoteTransferRow.toEntity(): TransferEntity = TransferEntity(
    id = id,
    userId = user_id,
    fromAccountId = from_account_id,
    toAccountId = to_account_id,
    fromAmountCents = from_amount_cents,
    toAmountCents = to_amount_cents,
    fromCurrency = from_currency,
    toCurrency = to_currency,
    exchangeRate = exchange_rate,
    isCurrencyConversion = is_currency_conversion,
    goalId = goal_id,
    note = note,
    date = date,
    createdAt = created_at,
    updatedAt = updated_at,
    deletedAt = deleted_at,
)

fun RemoteGoalRow.toEntity(): GoalEntity = GoalEntity(
    id = id,
    userId = user_id,
    name = name,
    targetCents = target_cents,
    currentCents = current_cents,
    currency = currency,
    deadline = deadline,
    isCompleted = is_completed,
    icon = icon,
    note = note,
    createdAt = created_at,
    updatedAt = updated_at,
    deletedAt = deleted_at,
)

fun RemoteSubscriptionRow.toEntity(): SubscriptionEntity = SubscriptionEntity(
    id = id,
    userId = user_id,
    accountId = account_id,
    categoryId = category_id,
    name = name,
    amountCents = amount_cents,
    currency = currency,
    billingCycle = billing_cycle,
    nextBillingDate = next_billing_date,
    isActive = is_active,
    autoLog = auto_log,
    createdAt = created_at,
    updatedAt = updated_at,
    deletedAt = deleted_at,
)

fun RemoteSettingsRow.toEntity(): SettingsEntity = SettingsEntity(
    userId = user_id,
    usdToEgpRate = usd_to_egp_rate,
    defaultAccountId = default_account_id,
    analyticsCurrency = analytics_currency,
    createdAt = created_at,
    updatedAt = updated_at,
)

