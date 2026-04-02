package com.raqeem.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.raqeem.app.data.local.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {

    @Query("SELECT * FROM settings LIMIT 1")
    fun get(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings LIMIT 1")
    suspend fun find(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: SettingsEntity)

    @Query("DELETE FROM settings WHERE user_id = :userId")
    suspend fun deleteByUserId(userId: String)

    @Query("UPDATE settings SET usd_to_egp_rate = :rate, updated_at = :updatedAt WHERE user_id = :userId")
    suspend fun updateExchangeRate(userId: String, rate: Double, updatedAt: Long)

    @Query("UPDATE settings SET default_account_id = :accountId, updated_at = :updatedAt WHERE user_id = :userId")
    suspend fun setDefaultAccount(userId: String, accountId: String?, updatedAt: Long)
}
