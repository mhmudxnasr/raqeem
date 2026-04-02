package com.raqeem.app.domain.repository

import com.raqeem.app.domain.model.Result
import com.raqeem.app.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface TransactionRepository {
    fun getAll(): Flow<List<Transaction>>
    fun getRecent(limit: Int = 20): Flow<List<Transaction>>
    fun getByAccount(accountId: String): Flow<List<Transaction>>
    fun getByDateRange(start: LocalDate, end: LocalDate): Flow<List<Transaction>>
    fun getById(id: String): Flow<Transaction?>
    fun search(query: String): Flow<List<Transaction>>
    suspend fun add(transaction: Transaction): Result<Unit>
    suspend fun update(transaction: Transaction): Result<Unit>
    suspend fun delete(id: String): Result<Unit>
}
