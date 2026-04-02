package com.raqeem.app.domain.model

enum class Currency(val symbol: String) {
    USD("$"),
    EGP("EGP ");

    companion object {
        fun fromString(value: String): Currency = when (value.uppercase()) {
            "USD" -> USD
            "EGP" -> EGP
            else -> USD
        }
    }
}

enum class AccountType {
    CASH, CHECKING, SAVING, INVESTMENT, CRYPTO;

    companion object {
        fun fromString(value: String): AccountType = when (value.lowercase()) {
            "cash" -> CASH
            "checking" -> CHECKING
            "saving" -> SAVING
            "investment" -> INVESTMENT
            "crypto" -> CRYPTO
            else -> CHECKING
        }
    }

    fun toApiString(): String = name.lowercase()
}

enum class TransactionType {
    INCOME, EXPENSE;

    companion object {
        fun fromString(value: String): TransactionType = when (value.lowercase()) {
            "income" -> INCOME
            "expense" -> EXPENSE
            else -> EXPENSE
        }
    }

    fun toApiString(): String = name.lowercase()
}

enum class BillingCycle {
    WEEKLY, MONTHLY, YEARLY;

    companion object {
        fun fromString(value: String): BillingCycle = when (value.lowercase()) {
            "weekly" -> WEEKLY
            "monthly" -> MONTHLY
            "yearly" -> YEARLY
            else -> MONTHLY
        }
    }

    fun toApiString(): String = name.lowercase()
}
