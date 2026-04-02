package com.raqeem.app.domain.model

data class UnlockState(
    val isLockOnLaunchEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val hasPin: Boolean = false,
    val isLocked: Boolean = false,
    val lastBackgroundAtMillis: Long? = null,
)

