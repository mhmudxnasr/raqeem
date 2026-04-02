package com.raqeem.app.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

data class Transfer(
    val id: String,
    val userId: String = "",
    val fromAccountId: String,
    val toAccountId: String,
    val fromAmountCents: Int,
    val toAmountCents: Int,
    val fromCurrency: Currency,
    val toCurrency: Currency,
    val exchangeRate: Double = 1.0,
    val isCurrencyConversion: Boolean = false,
    val goalId: String? = null,
    val note: String? = null,
    val date: LocalDate,
    val createdAt: Instant = Instant.DISTANT_PAST,
    val updatedAt: Instant = Instant.DISTANT_PAST,
    val deletedAt: Instant? = null,
    // Joined
    val fromAccount: Account? = null,
    val toAccount: Account? = null,
    val goal: Goal? = null,
)
