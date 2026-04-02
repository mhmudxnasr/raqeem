package com.raqeem.app.domain.repository

import com.raqeem.app.domain.model.AuthSession
import com.raqeem.app.domain.model.Result
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun observeSession(): Flow<AuthSession?>
    suspend fun signIn(email: String, password: String): Result<AuthSession>
    suspend fun signUp(email: String, password: String): Result<AuthSession>
    suspend fun signOut(): Result<Unit>
    suspend fun resetPassword(email: String): Result<Unit>
    suspend fun changePassword(newPassword: String): Result<Unit>
}

