package com.raqeem.app.data.mapper

import com.raqeem.app.data.local.entity.AccountEntity
import com.raqeem.app.data.local.entity.CategoryEntity
import com.raqeem.app.data.local.entity.GoalEntity
import com.raqeem.app.data.local.entity.SettingsEntity
import com.raqeem.app.data.local.entity.SubscriptionEntity
import com.raqeem.app.data.local.entity.TransactionEntity
import com.raqeem.app.data.local.entity.TransferEntity
import com.raqeem.app.domain.model.Account
import com.raqeem.app.domain.model.AccountType
import com.raqeem.app.domain.model.BillingCycle
import com.raqeem.app.domain.model.Category
import com.raqeem.app.domain.model.Currency
import com.raqeem.app.domain.model.Goal
import com.raqeem.app.domain.model.Settings
import com.raqeem.app.domain.model.Subscription
import com.raqeem.app.domain.model.Transaction
import com.raqeem.app.domain.model.TransactionType
import com.raqeem.app.domain.model.Transfer
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

// ================= Account =================

fun AccountEntity.toDomain(): Account = Account(
    id = id,
    userId = userId,
    name = name,
    type = AccountType.fromString(type),
    currency = Currency.fromString(currency),
    initialAmountCents = initialAmountCents,
    balanceCents = balanceCents,
    isHidden = isHidden,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

fun Account.toEntity(): AccountEntity = AccountEntity(
    id = id,
    userId = userId,
    name = name,
    type = type.toApiString(),
    currency = currency.name,
    initialAmountCents = initialAmountCents,
    balanceCents = balanceCents,
    isHidden = isHidden,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = Clock.System.now(),
    deletedAt = deletedAt,
)

// ================= Category =================

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    userId = userId,
    name = name,
    type = TransactionType.fromString(type),
    icon = icon,
    color = color,
    budgetCents = budgetCents,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    userId = userId,
    name = name,
    type = type.toApiString(),
    icon = icon,
    color = color,
    budgetCents = budgetCents,
    createdAt = createdAt,
    updatedAt = Clock.System.now(),
    deletedAt = deletedAt,
)

// ================= Transaction =================

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    userId = userId,
    accountId = accountId,
    categoryId = categoryId,
    type = TransactionType.fromString(type),
    amountCents = amountCents,
    currency = Currency.fromString(currency),
    note = note,
    date = date,
    receiptUrl = receiptUrl,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    userId = userId,
    accountId = accountId,
    categoryId = categoryId,
    type = type.toApiString(),
    amountCents = amountCents,
    currency = currency.name,
    note = note,
    date = date,
    receiptUrl = receiptUrl,
    createdAt = createdAt,
    updatedAt = Clock.System.now(),
    deletedAt = deletedAt,
)

// ================= Transfer =================

fun TransferEntity.toDomain(): Transfer = Transfer(
    id = id,
    userId = userId,
    fromAccountId = fromAccountId,
    toAccountId = toAccountId,
    fromAmountCents = fromAmountCents,
    toAmountCents = toAmountCents,
    fromCurrency = Currency.fromString(fromCurrency),
    toCurrency = Currency.fromString(toCurrency),
    exchangeRate = exchangeRate,
    isCurrencyConversion = isCurrencyConversion,
    goalId = goalId,
    note = note,
    date = date,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

fun Transfer.toEntity(): TransferEntity = TransferEntity(
    id = id,
    userId = userId,
    fromAccountId = fromAccountId,
    toAccountId = toAccountId,
    fromAmountCents = fromAmountCents,
    toAmountCents = toAmountCents,
    fromCurrency = fromCurrency.name,
    toCurrency = toCurrency.name,
    exchangeRate = exchangeRate,
    isCurrencyConversion = isCurrencyConversion,
    goalId = goalId,
    note = note,
    date = date,
    createdAt = createdAt,
    updatedAt = Clock.System.now(),
    deletedAt = deletedAt,
)

// ================= Goal =================

fun GoalEntity.toDomain(): Goal = Goal(
    id = id,
    userId = userId,
    name = name,
    targetCents = targetCents,
    currentCents = currentCents,
    currency = Currency.fromString(currency),
    deadline = deadline,
    isCompleted = isCompleted,
    icon = icon,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

fun Goal.toEntity(): GoalEntity = GoalEntity(
    id = id,
    userId = userId,
    name = name,
    targetCents = targetCents,
    currentCents = currentCents,
    currency = currency.name,
    deadline = deadline,
    isCompleted = isCompleted,
    icon = icon,
    note = note,
    createdAt = createdAt,
    updatedAt = Clock.System.now(),
    deletedAt = deletedAt,
)

// ================= Subscription =================

fun SubscriptionEntity.toDomain(): Subscription = Subscription(
    id = id,
    userId = userId,
    accountId = accountId,
    categoryId = categoryId,
    name = name,
    amountCents = amountCents,
    currency = Currency.fromString(currency),
    billingCycle = BillingCycle.fromString(billingCycle),
    nextBillingDate = nextBillingDate,
    isActive = isActive,
    autoLog = autoLog,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

fun Subscription.toEntity(): SubscriptionEntity = SubscriptionEntity(
    id = id,
    userId = userId,
    accountId = accountId,
    categoryId = categoryId,
    name = name,
    amountCents = amountCents,
    currency = currency.name,
    billingCycle = billingCycle.toApiString(),
    nextBillingDate = nextBillingDate,
    isActive = isActive,
    autoLog = autoLog,
    createdAt = createdAt,
    updatedAt = Clock.System.now(),
    deletedAt = deletedAt,
)

// ================= Settings =================

fun SettingsEntity.toDomain(): Settings = Settings(
    userId = userId,
    usdToEgpRate = usdToEgpRate,
    defaultAccountId = defaultAccountId,
    analyticsCurrency = Currency.fromString(analyticsCurrency),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Settings.toEntity(): SettingsEntity = SettingsEntity(
    userId = userId,
    usdToEgpRate = usdToEgpRate,
    defaultAccountId = defaultAccountId,
    analyticsCurrency = analyticsCurrency.name,
    createdAt = createdAt,
    updatedAt = Clock.System.now(),
)
