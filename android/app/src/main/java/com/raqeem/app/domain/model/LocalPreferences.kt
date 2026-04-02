package com.raqeem.app.domain.model

data class LocalPreferences(
    val aiInsightsEnabled: Boolean = true,
    val budgetWarningsEnabled: Boolean = true,
    val subscriptionRemindersEnabled: Boolean = true,
    val weeklySummaryEnabled: Boolean = true,
)
