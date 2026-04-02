package com.raqeem.app.domain.model

enum class AiChatRole {
    USER,
    ASSISTANT,
}

data class AiChatMessage(
    val role: AiChatRole,
    val content: String,
)
