package com.raqeem.app.domain.repository

import com.raqeem.app.domain.model.Result
import com.raqeem.app.domain.model.Transfer
import kotlinx.coroutines.flow.Flow

interface TransferRepository {
    fun getAll(): Flow<List<Transfer>>
    fun getByAccount(accountId: String): Flow<List<Transfer>>
    fun getById(id: String): Flow<Transfer?>
    suspend fun add(transfer: Transfer): Result<Unit>
    suspend fun update(transfer: Transfer): Result<Unit>
    suspend fun delete(id: String): Result<Unit>
}
