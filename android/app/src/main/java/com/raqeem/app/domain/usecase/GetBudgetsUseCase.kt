package com.raqeem.app.domain.usecase

import com.raqeem.app.domain.model.Category
import com.raqeem.app.domain.model.Currency
import com.raqeem.app.domain.model.TransactionType
import com.raqeem.app.domain.repository.CategoryRepository
import com.raqeem.app.domain.repository.SettingsRepository
import com.raqeem.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import kotlin.math.roundToInt

data class BudgetItem(
    val category: Category,
    val budgetCents: Int,
    val spentCents: Int,
) {
    val percentage: Int
        get() = if (budgetCents > 0) ((spentCents.toLong() * 100) / budgetCents).toInt() else 0
    val remainingCents: Int
        get() = budgetCents - spentCents
}

class GetBudgetsUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val settingsRepository: SettingsRepository,
) {
    operator fun invoke(monthStart: LocalDate, monthEnd: LocalDate): Flow<List<BudgetItem>> {
        return combine(
            categoryRepository.getByType(TransactionType.EXPENSE),
            transactionRepository.getByDateRange(monthStart, monthEnd),
            settingsRepository.get(),
        ) { categories, transactions, settings ->
            categories
                .filter { it.budgetCents != null }
                .map { category ->
                    val spent = transactions
                        .filter { it.categoryId == category.id && it.type == TransactionType.EXPENSE }
                        .sumOf { transaction ->
                            normalizeToUsd(
                                amountCents = transaction.amountCents,
                                currency = transaction.currency,
                                usdToEgpRate = settings.usdToEgpRate,
                            )
                        }
                    BudgetItem(
                        category = category,
                        budgetCents = category.budgetCents ?: 0,
                        spentCents = spent,
                    )
                }
                .sortedByDescending { it.percentage }
        }
    }

    private fun normalizeToUsd(amountCents: Int, currency: Currency, usdToEgpRate: Double): Int {
        return if (currency == Currency.USD) {
            amountCents
        } else {
            (amountCents / usdToEgpRate).roundToInt()
        }
    }
}
