package com.raqeem.app.domain.model

data class AuthSession(
    val userId: String,
    val email: String?,
    val accessToken: String? = null,
)

