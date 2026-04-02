package com.raqeem.app.domain.model

import java.time.YearMonth

data class AnalyticsSummary(
    val selectedMonth: YearMonth = YearMonth.now(),
    val totalIncomeCents: Int = 0,
    val totalExpenseCents: Int = 0,
    val netFlowCents: Int = 0,
    val averageDailySpendCents: Int = 0,
    val netWorthCents: Int = 0,
    val topCategories: List<CategorySpend> = emptyList(),
    val monthlyTrend: List<MonthTrend> = emptyList(),
    val weekdaySpend: List<WeekdaySpend> = emptyList(),
    val insight: String? = null,
)

data class CategorySpend(
    val label: String,
    val amountCents: Int,
    val percentage: Int,
)

data class MonthTrend(
    val label: String,
    val incomeCents: Int,
    val expenseCents: Int,
)

data class WeekdaySpend(
    val label: String,
    val amountCents: Int,
)
