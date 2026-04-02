package com.raqeem.app.data.local

import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppBootstrapper @Inject constructor(
    private val database: AppDatabase,
) {

    suspend fun seedIfNeeded() {
        val hasAccounts = database.accountDao().getCount() > 0
        val hasCategories = database.categoryDao().getCount() > 0
        val hasSettings = database.settingsDao().find() != null

        if (hasAccounts && hasCategories && hasSettings) {
            return
        }

        database.withTransaction {
            if (!hasAccounts) {
                database.accountDao().insertAll(defaultAccountEntities())
            }
            if (!hasCategories) {
                database.categoryDao().insertAll(defaultCategoryEntities())
            }
            if (!hasSettings) {
                database.settingsDao().upsert(defaultSettingsEntity())
            }
        }
    }
}
