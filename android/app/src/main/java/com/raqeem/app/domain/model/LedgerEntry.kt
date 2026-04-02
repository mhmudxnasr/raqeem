package com.raqeem.app.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

sealed interface LedgerEntry {
    val id: String
    val date: LocalDate
    val createdAt: Instant

    data class TransactionEntry(
        val transaction: Transaction,
    ) : LedgerEntry {
        override val id: String = transaction.id
        override val date: LocalDate = transaction.date
        override val createdAt: Instant = transaction.createdAt
    }

    data class TransferEntry(
        val transfer: Transfer,
    ) : LedgerEntry {
        override val id: String = transfer.id
        override val date: LocalDate = transfer.date
        override val createdAt: Instant = transfer.createdAt
    }
}

