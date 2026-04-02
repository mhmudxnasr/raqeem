package com.raqeem.app.domain.repository

import com.raqeem.app.domain.model.Account
import com.raqeem.app.domain.model.Result
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAll(): Flow<List<Account>>
    fun getById(id: String): Flow<Account?>
    suspend fun add(account: Account): Result<Unit>
    suspend fun update(account: Account): Result<Unit>
    suspend fun delete(id: String): Result<Unit>
    suspend fun getNetWorthCents(): Int
}
