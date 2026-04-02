package com.raqeem.app.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.raqeem.app.domain.model.TransactionType
import com.raqeem.app.domain.usecase.GetBudgetsUseCase
import com.raqeem.app.domain.usecase.GetLocalPreferencesUseCase
import com.raqeem.app.domain.usecase.GetSubscriptionsUseCase
import com.raqeem.app.domain.usecase.GetSettingsUseCase
import com.raqeem.app.domain.repository.TransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.temporal.ChronoUnit

@HiltWorker
class NotificationsWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val getBudgets: GetBudgetsUseCase,
    private val getSubscriptions: GetSubscriptionsUseCase,
    private val getLocalPreferences: GetLocalPreferencesUseCase,
    private val getSettings: GetSettingsUseCase,
    private val transactionRepository: TransactionRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        RaqeemNotificationManager.ensureChannels(applicationContext)
        val preferences = getLocalPreferences().first()
        val today = java.time.LocalDate.now()

        if (preferences.budgetWarningsEnabled) {
            val month = YearMonth.from(today)
            val budgets = getBudgets(
                monthStart = LocalDate(month.year, month.monthValue, 1),
                monthEnd = LocalDate(month.year, month.monthValue, month.lengthOfMonth()),
            ).first()
            val topWarning = budgets.maxByOrNull { it.percentage }
            if (topWarning != null && topWarning.percentage >= 80) {
                val label = if (topWarning.percentage >= 100) "Budget overspent" else "Budget warning"
                RaqeemNotificationManager.notify(
                    applicationContext,
                    RaqeemNotificationManager.BUDGET_CHANNEL_ID,
                    2001,
                    label,
                    "${topWarning.category.name} is at ${topWarning.percentage}% of budget.",
                )
            }
        }

        if (preferences.subscriptionRemindersEnabled) {
            val subscription = getSubscriptions().first()
                .filter { it.isActive }
                .minByOrNull { it.nextBillingDate.toString() }
            if (subscription != null) {
                val dueDate = java.time.LocalDate.of(
                    subscription.nextBillingDate.year,
                    subscription.nextBillingDate.monthNumber,
                    subscription.nextBillingDate.dayOfMonth,
                )
                val daysUntil = ChronoUnit.DAYS.between(today, dueDate)
                if (daysUntil in 0..3) {
                    RaqeemNotificationManager.notify(
                        applicationContext,
                        RaqeemNotificationManager.SUBSCRIPTION_CHANNEL_ID,
                        2002,
                        "Subscription due soon",
                        "${subscription.name} renews in ${daysUntil.toInt()} day(s).",
                    )
                }
            }
        }

        if (preferences.weeklySummaryEnabled && today.dayOfWeek == DayOfWeek.SUNDAY) {
            val start = today.minusDays(6)
            val weeklyTransactions = transactionRepository.getAll().first().filter { transaction ->
                val date = java.time.LocalDate.of(transaction.date.year, transaction.date.monthNumber, transaction.date.dayOfMonth)
                !date.isBefore(start) && !date.isAfter(today)
            }
            val income = weeklyTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amountCents }
            val expense = weeklyTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountCents }
            val settings = getSettings().first()
            RaqeemNotificationManager.notify(
                applicationContext,
                RaqeemNotificationManager.SUMMARY_CHANNEL_ID,
                2003,
                "Weekly summary",
                "Income ${income / 100.0} • Spend ${expense / 100.0} • Default account ${settings.defaultAccountId ?: "not set"}.",
            )
        }

        return Result.success()
    }
}
