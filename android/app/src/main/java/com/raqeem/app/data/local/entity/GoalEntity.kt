package com.raqeem.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    val name: String,
    @ColumnInfo(name = "target_cents") val targetCents: Int,
    @ColumnInfo(name = "current_cents") val currentCents: Int = 0,
    val currency: String = "USD",
    val deadline: LocalDate? = null,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean = false,
    val icon: String = "flag",
    val note: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
    @ColumnInfo(name = "deleted_at") val deletedAt: Instant? = null,
)
