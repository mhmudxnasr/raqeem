package com.raqeem.app.data.local

import com.raqeem.app.data.local.entity.AccountEntity
import com.raqeem.app.data.local.entity.CategoryEntity
import com.raqeem.app.data.local.entity.SettingsEntity
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

const val LOCAL_USER_ID: String = "00000000-0000-0000-0000-000000000001"

fun defaultAccountEntities(): List<AccountEntity> = emptyList()

fun defaultCategoryEntities(): List<CategoryEntity> = emptyList()

fun defaultSettingsEntity(now: Instant = Clock.System.now()): SettingsEntity {
    return SettingsEntity(
        userId = LOCAL_USER_ID,
        usdToEgpRate = 52.0,
        defaultAccountId = null,
        analyticsCurrency = "USD",
        createdAt = now,
        updatedAt = now,
    )
}
