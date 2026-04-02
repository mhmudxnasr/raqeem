package com.raqeem.app.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raqeem.app.domain.model.AuthSession
import com.raqeem.app.domain.model.Result
import com.raqeem.app.domain.model.SyncStatus
import com.raqeem.app.domain.model.UnlockState
import com.raqeem.app.domain.repository.AppLockRepository
import com.raqeem.app.domain.repository.AuthRepository
import com.raqeem.app.domain.repository.SyncStatusRepository
import com.raqeem.app.sync.SyncService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppRootUiState(
    val authSession: AuthSession? = null,
    val unlockState: UnlockState = UnlockState(),
    val syncStatus: SyncStatus = SyncStatus.Idle,
    val isBootstrapping: Boolean = true,
    val authError: String? = null,
)

@HiltViewModel
class AppRootViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val appLockRepository: AppLockRepository,
    private val syncStatusRepository: SyncStatusRepository,
    private val syncService: SyncService,
) : ViewModel() {

    private val isBootstrapping = MutableStateFlow(true)
    private val authError = MutableStateFlow<String?>(null)
    private val bootstrappedUsers = mutableSetOf<String>()

    val uiState: StateFlow<AppRootUiState> = combine(
        authRepository.observeSession(),
        appLockRepository.observeUnlockState(),
        syncStatusRepository.observeStatus(),
        isBootstrapping,
        authError,
    ) { session, unlockState, syncStatus, bootstrapping, error ->
        AppRootUiState(
            authSession = session,
            unlockState = unlockState,
            syncStatus = syncStatus,
            isBootstrapping = bootstrapping,
            authError = error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppRootUiState(),
    )

    init {
        viewModelScope.launch {
            authRepository.observeSession().collect { session ->
                authError.value = null
                when {
                    session == null -> {
                        isBootstrapping.value = false
                    }
                    bootstrappedUsers.add(session.userId) -> {
                        isBootstrapping.value = true
                        syncService.bootstrapForSignedInUser(session.userId)
                        appLockRepository.refreshLockState()
                        isBootstrapping.value = false
                    }
                    else -> {
                        isBootstrapping.value = false
                    }
                }
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            when (val result = authRepository.signIn(email, password)) {
                is Result.Success -> authError.value = null
                is Result.Error -> authError.value = result.message
                Result.Loading -> Unit
            }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            when (val result = authRepository.signUp(email, password)) {
                is Result.Success -> authError.value = null
                is Result.Error -> authError.value = result.message
                Result.Loading -> Unit
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            when (val result = authRepository.resetPassword(email)) {
                is Result.Success -> authError.value = "Password reset email sent."
                is Result.Error -> authError.value = result.message
                Result.Loading -> Unit
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            appLockRepository.markUnlocked()
        }
    }

    fun verifyPin(pin: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (appLockRepository.verifyPin(pin)) {
                appLockRepository.markUnlocked()
                authError.value = null
                onSuccess()
            } else {
                authError.value = "Incorrect PIN."
            }
        }
    }

    fun unlockWithBiometric() {
        viewModelScope.launch {
            appLockRepository.unlockWithBiometric()
            authError.value = null
        }
    }

    fun setLockOnLaunch(enabled: Boolean) {
        viewModelScope.launch {
            appLockRepository.setLockOnLaunchEnabled(enabled)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appLockRepository.setBiometricEnabled(enabled)
        }
    }

    fun setPin(pin: String) {
        viewModelScope.launch {
            when (val result = appLockRepository.setPin(pin)) {
                is Result.Error -> authError.value = result.message
                else -> authError.value = null
            }
        }
    }

    fun clearPin() {
        viewModelScope.launch {
            appLockRepository.clearPin()
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            syncService.syncNow()
        }
    }

    fun clearMessage() {
        authError.value = null
    }
}
