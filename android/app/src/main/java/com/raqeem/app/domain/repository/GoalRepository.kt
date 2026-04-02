package com.raqeem.app.domain.repository

import com.raqeem.app.domain.model.Goal
import com.raqeem.app.domain.model.Result
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun getAll(): Flow<List<Goal>>
    fun getById(id: String): Flow<Goal?>
    suspend fun add(goal: Goal): Result<Unit>
    suspend fun update(goal: Goal): Result<Unit>
    suspend fun delete(id: String): Result<Unit>
    suspend fun markComplete(id: String): Result<Unit>
}
