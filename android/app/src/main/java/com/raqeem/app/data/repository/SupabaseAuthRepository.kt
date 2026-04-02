package com.raqeem.app.data.repository

import com.raqeem.app.domain.model.AuthSession
import com.raqeem.app.domain.model.Result
import com.raqeem.app.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseAuthRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
) : AuthRepository {

    override fun observeSession(): Flow<AuthSession?> {
        return supabaseClient.auth.sessionStatus.map { status ->
            when (status) {
                is SessionStatus.Authenticated -> {
                    val user = status.session.user
                    AuthSession(
                        userId = user?.id.orEmpty(),
                        email = user?.email,
                        accessToken = status.session.accessToken,
                    )
                }
                else -> null
            }
        }
    }

    override suspend fun signIn(email: String, password: String): Result<AuthSession> {
        return runCatching {
            supabaseClient.auth.signInWith(Email) {
                this.email = email.trim()
                this.password = password
            }
            currentSession()
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(it.message ?: "Unable to sign in.", it) },
        )
    }

    override suspend fun signUp(email: String, password: String): Result<AuthSession> {
        return runCatching {
            supabaseClient.auth.signUpWith(Email) {
                this.email = email.trim()
                this.password = password
            }
            currentSession()
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(it.message ?: "Unable to create account.", it) },
        )
    }

    override suspend fun signOut(): Result<Unit> {
        return runCatching {
            supabaseClient.auth.signOut()
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { Result.Error(it.message ?: "Unable to sign out.", it) },
        )
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        return runCatching {
            supabaseClient.auth.resetPasswordForEmail(email.trim())
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { Result.Error(it.message ?: "Unable to send reset email.", it) },
        )
    }

    override suspend fun changePassword(newPassword: String): Result<Unit> {
        return runCatching {
            supabaseClient.auth.modifyUser {
                password = newPassword
            }
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { Result.Error(it.message ?: "Unable to change password.", it) },
        )
    }

    private suspend fun currentSession(): AuthSession {
        val session = supabaseClient.auth.currentSessionOrNull()
            ?: error("Session not available.")
        return AuthSession(
            userId = session.user?.id.orEmpty(),
            email = session.user?.email,
            accessToken = session.accessToken,
        )
    }
}
