package com.raqeem.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "usd_to_egp_rate") val usdToEgpRate: Double = 52.0,
    @ColumnInfo(name = "default_account_id") val defaultAccountId: String? = null,
    @ColumnInfo(name = "analytics_currency") val analyticsCurrency: String = "USD",
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
)
