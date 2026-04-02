package com.raqeem.app.domain.usecase

import com.raqeem.app.domain.model.AiChatMessage
import com.raqeem.app.domain.model.Result
import com.raqeem.app.domain.repository.AiAssistantRepository
import java.time.YearMonth
import javax.inject.Inject

class GetMonthlyInsightUseCase @Inject constructor(
    private val repository: AiAssistantRepository,
) {
    suspend operator fun invoke(month: YearMonth): Result<String> {
        return repository.getMonthlyInsight(month)
    }
}

class SendAiChatMessageUseCase @Inject constructor(
    private val repository: AiAssistantRepository,
) {
    suspend operator fun invoke(
        month: YearMonth,
        message: String,
        conversation: List<AiChatMessage>,
    ): Result<String> {
        return repository.sendChatMessage(month, message, conversation)
    }
}
