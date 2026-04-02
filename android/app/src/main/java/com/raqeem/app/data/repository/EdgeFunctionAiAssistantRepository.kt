package com.raqeem.app.data.repository

import com.raqeem.app.BuildConfig
import com.raqeem.app.domain.model.AiChatMessage
import com.raqeem.app.domain.model.AiChatRole
import com.raqeem.app.domain.model.Result
import com.raqeem.app.domain.repository.AiAssistantRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EdgeFunctionAiAssistantRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
) : AiAssistantRepository {

    override suspend fun getMonthlyInsight(month: YearMonth): Result<String> {
        return invokeEdgeFunction(
            AiRequest(
                type = "monthly_insight",
                month = month.toString(),
            ),
        )
    }

    override suspend fun sendChatMessage(
        month: YearMonth,
        message: String,
        conversation: List<AiChatMessage>,
    ): Result<String> {
        return invokeEdgeFunction(
            AiRequest(
                type = "chat",
                month = month.toString(),
                message = message,
                conversation = conversation.map {
                    AiConversationPayload(
                        role = if (it.role == AiChatRole.USER) "user" else "assistant",
                        content = it.content,
                    )
                },
            ),
        )
    }

    private suspend fun invokeEdgeFunction(request: AiRequest): Result<String> = withContext(Dispatchers.IO) {
        val authToken = supabaseClient.auth.currentSessionOrNull()?.accessToken
            ?: return@withContext Result.Error("Sign in again to use AI features.")

        if (BuildConfig.SUPABASE_URL.contains("YOUR_PROJECT", ignoreCase = true)) {
            return@withContext Result.Error("Supabase edge functions are not configured yet.")
        }

        return@withContext try {
            val endpoint = "${BuildConfig.SUPABASE_URL}/functions/v1/ai-insights"
            val connection = URL(endpoint).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $authToken")
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
            connection.setRequestProperty("Content-Type", "application/json")

            connection.outputStream.bufferedWriter().use { writer ->
                writer.write(Json.encodeToString(request))
            }

            val responseCode = connection.responseCode
            val responseText = (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()

            val response = runCatching {
                Json { ignoreUnknownKeys = true }.decodeFromString(AiResponse.serializer(), responseText)
            }.getOrNull()
            val content = response?.content.orEmpty()

            when {
                responseCode in 200..299 && content.isNotBlank() -> Result.Success(content)
                response != null -> Result.Error(response.message ?: "AI unavailable.")
                else -> Result.Error("AI unavailable.")
            }
        } catch (exception: Exception) {
            Result.Error("AI unavailable — please try again later.", exception)
        }
    }

    @Serializable
    private data class AiRequest(
        val type: String,
        val month: String? = null,
        val message: String? = null,
        val conversation: List<AiConversationPayload> = emptyList(),
    )

    @Serializable
    private data class AiConversationPayload(
        val role: String,
        val content: String,
    )

    @Serializable
    private data class AiResponse(
        val content: String? = null,
        val error: Boolean? = null,
        val message: String? = null,
        val code: String? = null,
    )
}
