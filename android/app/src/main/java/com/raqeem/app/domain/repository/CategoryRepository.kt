package com.raqeem.app.domain.repository

import com.raqeem.app.domain.model.Category
import com.raqeem.app.domain.model.Result
import com.raqeem.app.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAll(): Flow<List<Category>>
    fun getByType(type: TransactionType): Flow<List<Category>>
    fun getById(id: String): Flow<Category?>
    suspend fun add(category: Category): Result<Unit>
    suspend fun update(category: Category): Result<Unit>
    suspend fun delete(id: String): Result<Unit>
}
