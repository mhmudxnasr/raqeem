package com.raqeem.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "table_name") val tableName: String,
    @ColumnInfo(name = "record_id") val recordId: String,
    val operation: String, // "upsert" | "delete"
    val payload: String,   // JSON-serialized record
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    val attempts: Int = 0,
    @ColumnInfo(name = "last_error") val lastError: String? = null,
    @ColumnInfo(name = "is_synced") val isSynced: Boolean = false,
)
