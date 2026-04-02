package com.raqeem.app.domain.repository

import com.raqeem.app.domain.model.Result
import com.raqeem.app.domain.model.Subscription
import kotlinx.coroutines.flow.Flow

interface SubscriptionRepository {
    fun getAll(): Flow<List<Subscription>>
    fun getActive(): Flow<List<Subscription>>
    fun getById(id: String): Flow<Subscription?>
    suspend fun add(subscription: Subscription): Result<Unit>
    suspend fun update(subscription: Subscription): Result<Unit>
    suspend fun delete(id: String): Result<Unit>
}
