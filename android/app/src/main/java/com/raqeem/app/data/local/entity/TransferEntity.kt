package com.raqeem.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

@Entity(tableName = "transfers")
data class TransferEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "from_account_id") val fromAccountId: String,
    @ColumnInfo(name = "to_account_id") val toAccountId: String,
    @ColumnInfo(name = "from_amount_cents") val fromAmountCents: Int,
    @ColumnInfo(name = "to_amount_cents") val toAmountCents: Int,
    @ColumnInfo(name = "from_currency") val fromCurrency: String,
    @ColumnInfo(name = "to_currency") val toCurrency: String,
    @ColumnInfo(name = "exchange_rate") val exchangeRate: Double = 1.0,
    @ColumnInfo(name = "is_currency_conversion") val isCurrencyConversion: Boolean = false,
    @ColumnInfo(name = "goal_id") val goalId: String? = null,
    val note: String? = null,
    val date: LocalDate,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
    @ColumnInfo(name = "deleted_at") val deletedAt: Instant? = null,
)
