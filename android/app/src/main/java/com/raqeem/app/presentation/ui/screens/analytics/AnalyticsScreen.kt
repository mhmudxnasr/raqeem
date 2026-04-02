package com.raqeem.app.presentation.ui.screens.analytics

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.raqeem.app.domain.model.AnalyticsSummary
import com.raqeem.app.domain.model.CategorySpend
import com.raqeem.app.domain.model.Currency
import com.raqeem.app.domain.model.LocalPreferences
import com.raqeem.app.domain.model.MonthTrend
import com.raqeem.app.domain.model.Result
import com.raqeem.app.domain.model.Transaction
import com.raqeem.app.domain.model.TransactionType
import com.raqeem.app.domain.model.WeekdaySpend
import com.raqeem.app.domain.repository.TransactionRepository
import com.raqeem.app.domain.usecase.GetAccountsUseCase
import com.raqeem.app.domain.usecase.GetCategoriesUseCase
import com.raqeem.app.domain.usecase.GetLocalPreferencesUseCase
import com.raqeem.app.domain.usecase.GetMonthlyInsightUseCase
import com.raqeem.app.presentation.ui.components.BudgetBar
import com.raqeem.app.presentation.ui.components.HeaderIconButton
import com.raqeem.app.presentation.ui.components.MonthSelector
import com.raqeem.app.presentation.ui.components.PageHeader
import com.raqeem.app.presentation.ui.components.SectionLabel
import com.raqeem.app.presentation.ui.components.SurfaceCard
import com.raqeem.app.presentation.ui.theme.AppColors
import com.raqeem.app.presentation.ui.theme.AppTypography
import com.raqeem.app.util.formatAmount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import kotlin.math.absoluteValue

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val summary: AnalyticsSummary = AnalyticsSummary(),
    val localPreferences: LocalPreferences = LocalPreferences(),
    val aiInsight: String? = null,
    val isAiLoading: Boolean = false,
    val aiError: String? = null,
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    transactionRepository: TransactionRepository,
    getAccounts: GetAccountsUseCase,
    getCategories: GetCategoriesUseCase,
    getLocalPreferences: GetLocalPreferencesUseCase,
    private val getMonthlyInsight: GetMonthlyInsightUseCase,
) : ViewModel() {
    private val selectedMonth = MutableStateFlow(YearMonth.now())
    private val aiInsight = MutableStateFlow<String?>(null)
    private val isAiLoading = MutableStateFlow(false)
    private val aiError = MutableStateFlow<String?>(null)
    private var lastInsightMonth: YearMonth? = null

    private val baseUiState = combine(
        transactionRepository.getAll(),
        getAccounts(),
        getCategories(),
        getLocalPreferences(),
        selectedMonth,
    ) { transactions, accounts, categories, localPreferences, month ->
        val summary = buildSummary(
            transactions = transactions,
            selectedMonth = month,
            categoryNames = categories.associate { it.id to it.name },
            netWorthCents = accounts.filterNot { it.isHidden }.sumOf { it.balanceCents },
        )
        AnalyticsUiState(
            isLoading = false,
            summary = summary,
            localPreferences = localPreferences,
        )
    }

    val uiState = combine(
        baseUiState,
        aiInsight,
        isAiLoading,
        aiError,
    ) { base, insight, aiLoading, insightError ->
        base.copy(
            aiInsight = insight,
            isAiLoading = aiLoading,
            aiError = insightError,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AnalyticsUiState(),
    )

    fun showPreviousMonth() {
        selectedMonth.value = selectedMonth.value.minusMonths(1)
    }

    fun showNextMonth() {
        selectedMonth.value = selectedMonth.value.plusMonths(1)
    }

    fun refreshInsight(force: Boolean = false) {
        if (!uiState.value.localPreferences.aiInsightsEnabled) {
            aiInsight.value = null
            aiError.value = null
            isAiLoading.value = false
            return
        }
        val month = selectedMonth.value
        if (!force && lastInsightMonth == month && (aiInsight.value != null || isAiLoading.value)) return
        viewModelScope.launch {
            isAiLoading.value = true
            aiError.value = null
            when (val result = getMonthlyInsight(month)) {
                is Result.Success -> {
                    aiInsight.value = result.data
                    lastInsightMonth = month
                }
                is Result.Error -> {
                    aiError.value = result.message
                    aiInsight.value = null
                }
                Result.Loading -> Unit
            }
            isAiLoading.value = false
        }
    }

    private fun buildSummary(
        transactions: List<Transaction>,
        selectedMonth: YearMonth,
        categoryNames: Map<String, String>,
        netWorthCents: Int,
    ): AnalyticsSummary {
        var income = 0
        var expense = 0
        val categoryTotals = linkedMapOf<String, Int>()
        val weekdayTotals = DayOfWeek.entries.associateWith { 0 }.toMutableMap()
        val monthlyIncome = mutableMapOf<YearMonth, Int>()
        val monthlyExpense = mutableMapOf<YearMonth, Int>()

        transactions.forEach { transaction ->
            val month = YearMonth.of(transaction.date.year, transaction.date.monthNumber)
            when (transaction.type) {
                TransactionType.INCOME -> {
                    monthlyIncome[month] = monthlyIncome.getOrDefault(month, 0) + transaction.amountCents
                    if (month == selectedMonth) {
                        income += transaction.amountCents
                    }
                }
                TransactionType.EXPENSE -> {
                    monthlyExpense[month] = monthlyExpense.getOrDefault(month, 0) + transaction.amountCents
                    if (month == selectedMonth) {
                        expense += transaction.amountCents
                        val categoryLabel = categoryNames[transaction.categoryId] ?: "Uncategorized"
                        categoryTotals[categoryLabel] = categoryTotals.getOrDefault(categoryLabel, 0) + transaction.amountCents
                        val dayOfWeek = LocalDate.of(
                            transaction.date.year,
                            transaction.date.monthNumber,
                            transaction.date.dayOfMonth,
                        ).dayOfWeek
                        weekdayTotals[dayOfWeek] = weekdayTotals.getOrDefault(dayOfWeek, 0) + transaction.amountCents
                    }
                }
            }
        }

        val topCategories = categoryTotals
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { (label, amount) ->
            CategorySpend(
                label = label,
                amountCents = amount,
                percentage = if (expense > 0) ((amount.toLong() * 100) / expense).toInt() else 0,
            )
        }

        val monthlyTrend = (5 downTo 0).map { offset ->
            val month = selectedMonth.minusMonths(offset.toLong())
            MonthTrend(
                label = month.month.getDisplayName(TextStyle.SHORT, Locale.US),
                incomeCents = monthlyIncome.getOrDefault(month, 0),
                expenseCents = monthlyExpense.getOrDefault(month, 0),
            )
        }

        val weekdaySpend = DayOfWeek.entries.map { day ->
            WeekdaySpend(
                label = day.getDisplayName(TextStyle.SHORT, Locale.US),
                amountCents = weekdayTotals.getOrDefault(day, 0),
            )
        }

        return AnalyticsSummary(
            selectedMonth = selectedMonth,
            totalIncomeCents = income,
            totalExpenseCents = expense,
            netFlowCents = income - expense,
            averageDailySpendCents = if (selectedMonth.lengthOfMonth() > 0) expense / selectedMonth.lengthOfMonth() else 0,
            netWorthCents = netWorthCents,
            topCategories = topCategories,
            monthlyTrend = monthlyTrend,
            weekdaySpend = weekdaySpend,
            insight = buildInsight(topCategories, income, expense),
        )
    }

    private fun buildInsight(
        topCategories: List<CategorySpend>,
        income: Int,
        expense: Int,
    ): String {
        val topCategory = topCategories.firstOrNull()
        return when {
            topCategory == null && expense == 0 -> "No expense activity this month yet. Once spending lands, Raqeem will surface category and weekday patterns here."
            income > expense -> "Cash flow is positive this month. ${topCategory?.label ?: "Your top category"} is leading spend, so that is the first place to trim if you want to widen the gap."
            else -> "Spending is currently ahead of income. ${topCategory?.label ?: "Your top category"} is the biggest driver, and that is the best place to review first."
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnalyticsScreen(
    onOpenSettings: () -> Unit,
    onOpenChat: (String) -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val summary = uiState.summary
    val maxTrend = remember(summary.monthlyTrend) {
        (summary.monthlyTrend.maxOfOrNull { maxOf(it.incomeCents, it.expenseCents) } ?: 0).coerceAtLeast(1)
    }
    val maxWeekday = remember(summary.weekdaySpend) {
        (summary.weekdaySpend.maxOfOrNull { it.amountCents } ?: 0).coerceAtLeast(1)
    }
    LaunchedEffect(summary.selectedMonth, uiState.localPreferences.aiInsightsEnabled) {
        viewModel.refreshInsight()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.bgBase)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item("analytics_header", contentType = "header") {
            PageHeader(
                title = "Analytics",
                supportingText = "Spending, cash flow, and AI context for the selected month.",
                trailing = {
                    HeaderIconButton(
                        icon = Icons.Rounded.Settings,
                        contentDescription = "Open settings",
                        onClick = onOpenSettings,
                    )
                },
            )
        }

        item("analytics_month_selector", contentType = "selector") {
            MonthSelector(
                label = summary.selectedMonth.month.name.lowercase().replaceFirstChar { it.uppercase() } + " ${summary.selectedMonth.year}",
                onPrevious = viewModel::showPreviousMonth,
                onNext = viewModel::showNextMonth,
            )
        }

        item("analytics_snapshot", contentType = "snapshot") {
            SurfaceCard(
                modifier = Modifier.animateItemPlacement(),
                backgroundColor = AppColors.bgElevated,
                borderColor = AppColors.borderAccent.copy(alpha = 0.5f),
            ) {
                SectionLabel("Monthly Snapshot")
                Text(
                    text = summary.netFlowCents.formatAmount(Currency.USD, showSign = true),
                    style = AppTypography.heroAmount,
                    color = if (summary.netFlowCents >= 0) AppColors.positive else AppColors.negative,
                )
                Text(
                    text = "Net worth ${summary.netWorthCents.formatAmount(Currency.USD)} with average daily spend at ${summary.averageDailySpendCents.formatAmount(Currency.USD)}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.textSecondary,
                )
            }
        }

        item("analytics_metrics_primary", contentType = "metrics") {
            Row(
                modifier = Modifier.fillMaxWidth().animateItemPlacement(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Income",
                    value = summary.totalIncomeCents.formatAmount(Currency.USD),
                    tone = AppColors.positive,
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Expense",
                    value = summary.totalExpenseCents.formatAmount(Currency.USD),
                    tone = AppColors.negative,
                )
            }
        }

        item("analytics_metrics_secondary", contentType = "metrics") {
            Row(
                modifier = Modifier.fillMaxWidth().animateItemPlacement(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Net Flow",
                    value = summary.netFlowCents.formatAmount(Currency.USD, showSign = true),
                    tone = if (summary.netFlowCents >= 0) AppColors.positive else AppColors.negative,
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Avg Daily",
                    value = summary.averageDailySpendCents.formatAmount(Currency.USD),
                    tone = AppColors.warning,
                )
            }
        }

        item("analytics_net_worth", contentType = "net_worth") {
            SurfaceCard(modifier = Modifier.animateItemPlacement()) {
                Text("Net Worth", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = summary.netWorthCents.formatAmount(Currency.USD),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "Based on current visible account balances in the local ledger.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary,
                )
            }
        }

        item("analytics_categories_label", contentType = "section_label") {
            SectionLabel("Spending by Category")
        }
        item("analytics_categories", contentType = "category_chart") {
            if (summary.topCategories.isEmpty()) {
                SurfaceCard(modifier = Modifier.animateItemPlacement()) {
                    EmptyAnalytics("No category spending in this month yet.", Icons.Rounded.PieChart)
                }
            } else {
                CategorySpendChart(summary.topCategories, modifier = Modifier.animateItemPlacement())
            }
        }

        item("analytics_trend_label", contentType = "section_label") {
            SectionLabel("Income vs Expenses")
        }
        item("analytics_trends", contentType = "trend_chart") {
            if (summary.monthlyTrend.isEmpty()) {
                SurfaceCard(modifier = Modifier.animateItemPlacement()) {
                    EmptyAnalytics("No monthly history yet.", Icons.AutoMirrored.Rounded.TrendingUp)
                }
            } else {
                MonthlyTrendChart(summary.monthlyTrend, maxTrend, modifier = Modifier.animateItemPlacement())
            }
        }

        item("analytics_weekdays_label", contentType = "section_label") {
            SectionLabel("Weekday Heatmap")
        }
        item("analytics_weekdays", contentType = "weekday_chart") {
            WeekdayHeatmapChart(summary.weekdaySpend, maxWeekday, modifier = Modifier.animateItemPlacement())
        }

        item("analytics_ai_label", contentType = "section_label") {
            SectionLabel("AI Insights")
        }
        item("analytics_ai", contentType = "ai_insight") {
            SurfaceCard(
                modifier = Modifier.animateItemPlacement(),
                backgroundColor = AppColors.bgElevated,
                borderColor = AppColors.borderAccent.copy(alpha = 0.45f),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = if (uiState.localPreferences.aiInsightsEnabled) "Assistant Enabled" else "Assistant Disabled",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = when {
                                !uiState.localPreferences.aiInsightsEnabled -> "Turn AI insights back on in Settings to surface cloud-backed monthly summaries and chat."
                                uiState.isAiLoading -> "Generating a fresh monthly summary from the edge function..."
                                uiState.aiInsight != null -> uiState.aiInsight
                                uiState.aiError != null -> "${uiState.aiError}\n\nFallback: ${summary.insight}"
                                else -> summary.insight ?: "No insight yet."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.textSecondary,
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = AppColors.purple300,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = { viewModel.refreshInsight(force = true) },
                        modifier = Modifier.weight(1f),
                        enabled = uiState.localPreferences.aiInsightsEnabled && !uiState.isAiLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.bgSubtle,
                            contentColor = AppColors.textPrimary,
                        ),
                    ) {
                        Text("Refresh Insight")
                    }
                    Button(
                        onClick = { onOpenChat(summary.selectedMonth.toString()) },
                        modifier = Modifier.weight(1f),
                        enabled = uiState.localPreferences.aiInsightsEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.purple500,
                            contentColor = androidx.compose.ui.graphics.Color.White,
                        ),
                    ) {
                        Text("Open Chat")
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    tone: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    SurfaceCard(
        modifier = modifier,
        backgroundColor = tone.copy(alpha = 0.08f),
        borderColor = tone.copy(alpha = 0.28f),
    ) {
        Text(title.uppercase(Locale.US), style = MaterialTheme.typography.labelLarge, color = AppColors.textMuted)
        Text(value, style = AppTypography.largeAmount)
    }
}

@Composable
private fun CategorySpendChart(items: List<CategorySpend>, modifier: Modifier = Modifier) {
    SurfaceCard(modifier = modifier) {
        items.forEach { spend ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(spend.label, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${spend.percentage}% • ${spend.amountCents.formatAmount(Currency.USD)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.textSecondary,
                    )
                }
                BudgetBar(progress = spend.percentage / 100f, color = AppColors.purple400)
            }
        }
    }
}

@Composable
private fun MonthlyTrendChart(
    trends: List<MonthTrend>,
    maxTrend: Int,
    modifier: Modifier = Modifier,
) {
    SurfaceCard(modifier = modifier) {
        trends.forEach { trend ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(trend.label, style = MaterialTheme.typography.titleSmall, color = AppColors.textSecondary)
                TrendBar("Income", trend.incomeCents, maxTrend, AppColors.positive)
                TrendBar("Expense", trend.expenseCents, maxTrend, AppColors.negative)
            }
        }
    }
}

@Composable
private fun WeekdayHeatmapChart(
    days: List<WeekdaySpend>,
    maxWeekday: Int,
    modifier: Modifier = Modifier,
) {
    SurfaceCard(modifier = modifier) {
        days.forEach { day ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(day.label, style = MaterialTheme.typography.bodyMedium)
                    Text(day.amountCents.formatAmount(Currency.USD), style = MaterialTheme.typography.bodySmall, color = AppColors.textSecondary)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .background(AppColors.bgSubtle),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(day.amountCents.toFloat() / maxWeekday.toFloat())
                            .height(16.dp)
                            .background(AppColors.warning),
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendBar(
    label: String,
    amountCents: Int,
    maxAmountCents: Int,
    color: androidx.compose.ui.graphics.Color,
) {
    Text(
        text = "$label • ${amountCents.formatAmount(Currency.USD)}",
        style = MaterialTheme.typography.bodySmall,
        color = AppColors.textSecondary,
    )
    BudgetBar(
        progress = amountCents.absoluteValue.toFloat() / maxAmountCents.toFloat(),
        color = color,
    )
}

@Composable
private fun EmptyAnalytics(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(imageVector = icon, contentDescription = null, tint = AppColors.textMuted)
        Text(title, style = MaterialTheme.typography.bodyMedium, color = AppColors.textSecondary)
    }
}
