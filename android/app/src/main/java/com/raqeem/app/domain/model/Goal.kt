package com.raqeem.app.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

data class Goal(
    val id: String,
    val userId: String = "",
    val name: String,
    val targetCents: Int,
    val currentCents: Int = 0,
    val currency: Currency = Currency.USD,
    val deadline: LocalDate? = null,
    val isCompleted: Boolean = false,
    val icon: String = "flag",
    val note: String? = null,
    val createdAt: Instant = Instant.DISTANT_PAST,
    val updatedAt: Instant = Instant.DISTANT_PAST,
    val deletedAt: Instant? = null,
) {
    val progressPercent: Int
        get() = if (targetCents > 0) ((currentCents.toLong() * 100) / targetCents).toInt() else 0
}
