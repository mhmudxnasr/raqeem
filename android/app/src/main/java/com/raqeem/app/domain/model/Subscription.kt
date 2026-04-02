package com.raqeem.app.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

data class Subscription(
    val id: String,
    val userId: String = "",
    val accountId: String,
    val categoryId: String? = null,
    val name: String,
    val amountCents: Int,
    val currency: Currency = Currency.USD,
    val billingCycle: BillingCycle,
    val nextBillingDate: LocalDate,
    val isActive: Boolean = true,
    val autoLog: Boolean = false,
    val createdAt: Instant = Instant.DISTANT_PAST,
    val updatedAt: Instant = Instant.DISTANT_PAST,
    val deletedAt: Instant? = null,
)
