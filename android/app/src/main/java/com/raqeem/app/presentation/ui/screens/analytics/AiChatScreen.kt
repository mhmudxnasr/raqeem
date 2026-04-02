package com.raqeem.app.presentation.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.raqeem.app.domain.model.AiChatMessage
import com.raqeem.app.domain.model.AiChatRole
import com.raqeem.app.domain.model.Result
import com.raqeem.app.domain.usecase.SendAiChatMessageUseCase
import com.raqeem.app.presentation.ui.components.MonthSelector
import com.raqeem.app.presentation.ui.components.SurfaceCard
import com.raqeem.app.presentation.ui.theme.AppColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

data class AiChatUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val messages: List<AiChatMessage> = listOf(
        AiChatMessage(
            role = AiChatRole.ASSISTANT,
            content = "Ask anything about this month's finances, and I’ll answer using the synced data behind the AI edge function.",
        ),
    ),
    val isSending: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val sendAiChatMessage: SendAiChatMessageUseCase,
) : ViewModel() {
    private val selectedMonth = MutableStateFlow(YearMonth.now())
    private val messages = MutableStateFlow(
        listOf(
            AiChatMessage(
                role = AiChatRole.ASSISTANT,
                content = "Ask anything about this month's finances, and I’ll answer using the synced data behind the AI edge function.",
            ),
        ),
    )
    private val isSending = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val uiState = combine(selectedMonth, messages, isSending, error) { month, conversation, sending, problem ->
        AiChatUiState(
            selectedMonth = month,
            messages = conversation,
            isSending = sending,
            error = problem,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AiChatUiState(),
    )

    fun initialize(monthText: String?) {
        monthText?.let { text ->
            runCatching { YearMonth.parse(text) }.getOrNull()?.let { selectedMonth.value = it }
        }
    }

    fun showPreviousMonth() {
        selectedMonth.value = selectedMonth.value.minusMonths(1)
    }

    fun showNextMonth() {
        selectedMonth.value = selectedMonth.value.plusMonths(1)
    }

    fun clearConversation() {
        messages.value = listOf(
            AiChatMessage(
                role = AiChatRole.ASSISTANT,
                content = "Ask anything about this month's finances, and I’ll answer using the synced data behind the AI edge function.",
            ),
        )
        error.value = null
    }

    fun send(message: String) {
        val trimmed = message.trim()
        if (trimmed.isBlank()) return

        val updatedConversation = messages.value + AiChatMessage(AiChatRole.USER, trimmed)
        messages.value = updatedConversation
        error.value = null

        viewModelScope.launch {
            isSending.value = true
            when (val result = sendAiChatMessage(selectedMonth.value, trimmed, updatedConversation)) {
                is Result.Success -> {
                    messages.value = messages.value + AiChatMessage(AiChatRole.ASSISTANT, result.data)
                }
                is Result.Error -> {
                    error.value = result.message
                }
                Result.Loading -> Unit
            }
            isSending.value = false
        }
    }
}

@Composable
fun AiChatScreen(
    initialMonth: String?,
    onBack: () -> Unit,
    viewModel: AiChatViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(initialMonth) {
        viewModel.initialize(initialMonth)
    }
    LaunchedEffect(uiState.messages.size, uiState.error) {
        val lastIndex = uiState.messages.lastIndex + if (uiState.error != null) 1 else 0
        if (lastIndex >= 0) {
            listState.animateScrollToItem(lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.bgBase)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = AppColors.textSecondary)
                }
                Text("AI Chat", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = viewModel::clearConversation) {
                Text("Clear", color = AppColors.purple300)
            }
        }

        MonthSelector(
            label = "${uiState.selectedMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${uiState.selectedMonth.year}",
            onPrevious = viewModel::showPreviousMonth,
            onNext = viewModel::showNextMonth,
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            items(
                count = uiState.messages.size,
                key = { index -> "${uiState.messages[index].role}:${uiState.messages[index].content.hashCode()}:$index" },
            ) { index ->
                val message = uiState.messages[index]
                SurfaceCard(
                    backgroundColor = if (message.role == AiChatRole.USER) {
                        AppColors.purple500.copy(alpha = 0.18f)
                    } else {
                        AppColors.bgSurface
                    },
                    borderColor = if (message.role == AiChatRole.USER) AppColors.borderAccent else AppColors.borderDefault,
                ) {
                    Text(
                        text = if (message.role == AiChatRole.USER) "You" else "Raqeem AI",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.textSecondary,
                    )
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.textPrimary,
                    )
                }
            }

            if (uiState.error != null) {
                item("chat_error") {
                    SurfaceCard(
                        backgroundColor = AppColors.negativeBg,
                        borderColor = AppColors.borderNegative,
                    ) {
                        Text(uiState.error, color = AppColors.negative)
                    }
                }
            }
        }

        SurfaceCard(
            backgroundColor = AppColors.bgElevated,
            borderColor = AppColors.borderAccent.copy(alpha = 0.45f),
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.navigationBarsPadding(),
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Ask about this month") },
                minLines = 3,
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.borderAccent,
                    unfocusedBorderColor = AppColors.borderSubtle,
                    focusedContainerColor = AppColors.bgSubtle,
                    unfocusedContainerColor = AppColors.bgSubtle,
                ),
            )
            Button(
                onClick = {
                    viewModel.send(input)
                    input = ""
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSending,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.purple500,
                    contentColor = Color.White,
                ),
            ) {
                Text(if (uiState.isSending) "Thinking..." else "Send")
            }
        }
    }
}
