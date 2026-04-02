package com.raqeem.app.domain.model

import kotlinx.datetime.Instant

data class Settings(
    val userId: String = "",
    val usdToEgpRate: Double = 52.0,
    val defaultAccountId: String? = null,
    val analyticsCurrency: Currency = Currency.USD,
    val createdAt: Instant = Instant.DISTANT_PAST,
    val updatedAt: Instant = Instant.DISTANT_PAST,
)
