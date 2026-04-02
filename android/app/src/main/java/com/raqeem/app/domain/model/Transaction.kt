package com.raqeem.app.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

data class Transaction(
    val id: String,
    val userId: String = "",
    val accountId: String,
    val categoryId: String? = null,
    val type: TransactionType,
    val amountCents: Int,
    val currency: Currency,
    val note: String? = null,
    val date: LocalDate,
    val receiptUrl: String? = null,
    val createdAt: Instant = Instant.DISTANT_PAST,
    val updatedAt: Instant = Instant.DISTANT_PAST,
    val deletedAt: Instant? = null,
    // Joined fields
    val account: Account? = null,
    val category: Category? = null,
)
