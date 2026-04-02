package com.raqeem.app.domain.repository

import com.raqeem.app.domain.model.AiChatMessage
import com.raqeem.app.domain.model.Result
import java.time.YearMonth

interface AiAssistantRepository {
    suspend fun getMonthlyInsight(month: YearMonth): Result<String>
    suspend fun sendChatMessage(
        month: YearMonth,
        message: String,
        conversation: List<AiChatMessage>,
    ): Result<String>
}
