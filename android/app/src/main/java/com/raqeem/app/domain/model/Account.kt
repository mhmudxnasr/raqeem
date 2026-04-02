package com.raqeem.app.domain.model

import kotlinx.datetime.Instant

data class Account(
    val id: String,
    val userId: String = "",
    val name: String,
    val type: AccountType,
    val currency: Currency,
    val initialAmountCents: Int,
    val balanceCents: Int,
    val isHidden: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Instant = Instant.DISTANT_PAST,
    val updatedAt: Instant = Instant.DISTANT_PAST,
    val deletedAt: Instant? = null,
)
