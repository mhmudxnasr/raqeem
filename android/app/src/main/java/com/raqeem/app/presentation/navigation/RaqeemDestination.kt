package com.raqeem.app.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class RaqeemDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    data object Home : RaqeemDestination("home", "Home", Icons.Rounded.Home)
    data object Accounts : RaqeemDestination("accounts", "Accounts", Icons.Rounded.AccountBalanceWallet)
    data object AccountDetail : RaqeemDestination("accounts/{accountId}", "Account", Icons.Rounded.AccountBalanceWallet) {
        fun createRoute(accountId: String): String = "accounts/$accountId"
    }
    data object Analytics : RaqeemDestination("analytics", "Analytics", Icons.AutoMirrored.Rounded.TrendingUp)
    data object AnalyticsChat : RaqeemDestination("assistant/{month}", "Assistant", Icons.AutoMirrored.Rounded.TrendingUp) {
        fun createRoute(month: String): String = "assistant/$month"
    }
    data object Budgets : RaqeemDestination("budgets", "Budgets", Icons.Rounded.PieChart)
    data object Goals : RaqeemDestination("goals", "Goals", Icons.Rounded.Flag)
    data object Settings : RaqeemDestination("settings", "Settings", Icons.Rounded.Settings)
    data object Transactions : RaqeemDestination("transactions", "Transactions", Icons.Rounded.Home)
    data object TransactionDetail : RaqeemDestination("transaction/{transactionId}", "Transaction", Icons.Rounded.Home) {
        fun createRoute(transactionId: String): String = "transaction/$transactionId"
    }

    companion object {
        val topLevelDestinations = listOf(Home, Accounts, Analytics, Budgets, Goals)

        fun isTopLevel(route: String?): Boolean {
            return topLevelDestinations.any { destination ->
                route?.startsWith(destination.route) == true
            }
        }
    }
}
