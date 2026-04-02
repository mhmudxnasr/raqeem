package com.raqeem.app.domain.model

import kotlinx.datetime.Instant

data class Category(
    val id: String,
    val userId: String = "",
    val name: String,
    val type: TransactionType,
    val icon: String = "circle",
    val color: String = "#8B5CF6",
    val budgetCents: Int? = null,
    val createdAt: Instant = Instant.DISTANT_PAST,
    val updatedAt: Instant = Instant.DISTANT_PAST,
    val deletedAt: Instant? = null,
)
