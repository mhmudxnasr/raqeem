package com.raqeem.app.util

import androidx.compose.ui.graphics.Color
import com.raqeem.app.domain.model.Currency
import com.raqeem.app.domain.model.TransactionType
import com.raqeem.app.presentation.ui.theme.AppColors
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Format cents/piastres as a human-readable currency string.
 * - 12450 USD -> "$124.50"
 * - 12450 USD showSign -> "+$124.50"
 * - -12450 USD -> "−$124.50"
 * - 52000 EGP -> "EGP 520.00"
 */
fun Int.formatAmount(currency: Currency, showSign: Boolean = false): String {
    val amount = abs(this) / 100.0
    val symbol = if (currency == Currency.USD) "$" else "EGP "
    val formatted = "%.2f".format(amount)
    val sign = when {
        showSign && this > 0 -> "+"
        this < 0 -> "\u2212" // − (minus sign, not hyphen)
        else -> ""
    }
    return "$sign$symbol$formatted"
}

/**
 * Get the semantic color for a transaction type.
 */
fun TransactionType.toColor(): Color = when (this) {
    TransactionType.INCOME -> AppColors.positive
    TransactionType.EXPENSE -> AppColors.negative
}

/**
 * Convert EGP piastres to USD cents at a given rate.
 * Example: 52000 piastres / 52.0 rate = 1000 cents ($10.00)
 */
fun convertToUsd(amountCents: Int, currency: Currency, usdToEgpRate: Double): Int {
    if (currency == Currency.USD) return amountCents
    return (amountCents / usdToEgpRate).roundToInt()
}

/**
 * Convert USD cents to EGP piastres at a given rate.
 * Example: 1000 cents * 52.0 = 52000 piastres (EGP 520.00)
 */
fun convertUsdToEgp(usdCents: Int, usdToEgpRate: Double): Int {
    return (usdCents * usdToEgpRate).roundToInt()
}

/**
 * Budget utilization color based on percentage.
 */
fun budgetColor(percentage: Int): Color = when {
    percentage >= 100 -> AppColors.negative
    percentage >= 80 -> AppColors.warning
    else -> AppColors.positive
}
