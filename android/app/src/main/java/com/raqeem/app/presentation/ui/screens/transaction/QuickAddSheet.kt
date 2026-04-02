package com.raqeem.app.presentation.ui.screens.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.raqeem.app.data.local.LOCAL_USER_ID
import com.raqeem.app.domain.model.Account
import com.raqeem.app.domain.model.Goal
import com.raqeem.app.domain.model.Settings
import com.raqeem.app.domain.model.TransactionType
import com.raqeem.app.domain.model.Transfer
import com.raqeem.app.domain.usecase.AddTransferUseCase
import com.raqeem.app.domain.usecase.GetAccountsUseCase
import com.raqeem.app.domain.usecase.GetGoalsUseCase
import com.raqeem.app.domain.usecase.GetSettingsUseCase
import com.raqeem.app.presentation.ui.components.SurfaceCard
import com.raqeem.app.presentation.ui.components.PickerOption
import com.raqeem.app.presentation.ui.components.SheetPickerField
import com.raqeem.app.presentation.ui.theme.AppColors
import com.raqeem.app.util.convertToUsd
import com.raqeem.app.util.convertUsdToEgp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.UUID
import javax.inject.Inject
import kotlin.math.roundToInt

enum class QuickAddContext {
    Overview,
    Expense,
    Income,
    Transfer,
}

data class QuickAddUiState(
    val accounts: List<Account> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val settings: Settings = Settings(userId = LOCAL_USER_ID),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

private data class QuickAddContentState(
    val accounts: List<Account>,
    val goals: List<Goal>,
    val settings: Settings,
)

@HiltViewModel
class QuickAddViewModel @Inject constructor(
    getAccounts: GetAccountsUseCase,
    getGoals: GetGoalsUseCase,
    getSettings: GetSettingsUseCase,
    private val addTransfer: AddTransferUseCase,
) : ViewModel() {

    private val isSaving = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    private val contentState = combine(
        getAccounts(),
        getGoals(),
        getSettings(),
    ) { accounts, goals, settings ->
        QuickAddContentState(
            accounts = accounts,
            goals = goals,
            settings = settings,
        )
    }

    val uiState = combine(
        contentState,
        isSaving,
        errorMessage,
    ) { content, saving, error ->
        QuickAddUiState(
            accounts = content.accounts,
            goals = content.goals,
            settings = content.settings,
            isSaving = saving,
            errorMessage = error,
        )
    }
        .catch { throwable ->
            emit(
                QuickAddUiState(
                    errorMessage = throwable.message ?: "Unable to load quick-add data.",
                ),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = QuickAddUiState(),
        )

    suspend fun submitTransfer(
        fromAccountId: String,
        toAccountId: String,
        goalId: String?,
        amountInput: String,
        rateInput: String,
        note: String,
    ): Boolean {
        val fromAccount = uiState.value.accounts.firstOrNull { it.id == fromAccountId }
            ?: return setError("Choose a source account.")
        val toAccount = uiState.value.accounts.firstOrNull { it.id == toAccountId }
            ?: return setError("Choose a destination account.")
        val fromAmountCents = parseAmountToCents(amountInput)
            ?: return setError("Enter a valid amount.")
        val exchangeRate = rateInput.toDoubleOrNull()
            ?: return setError("Enter a valid exchange rate.")

        val toAmountCents = when {
            fromAccount.currency == toAccount.currency -> fromAmountCents
            fromAccount.currency.name == "USD" && toAccount.currency.name == "EGP" ->
                convertUsdToEgp(fromAmountCents, exchangeRate)
            else -> convertToUsd(fromAmountCents, fromAccount.currency, exchangeRate)
        }

        isSaving.value = true
        errorMessage.value = null

        val now = Clock.System.now()
        val result = addTransfer(
            Transfer(
                id = UUID.randomUUID().toString(),
                userId = uiState.value.settings.userId.ifBlank { LOCAL_USER_ID },
                fromAccountId = fromAccount.id,
                toAccountId = toAccount.id,
                fromAmountCents = fromAmountCents,
                toAmountCents = toAmountCents,
                fromCurrency = fromAccount.currency,
                toCurrency = toAccount.currency,
                exchangeRate = exchangeRate,
                isCurrencyConversion = fromAccount.currency != toAccount.currency,
                goalId = goalId,
                note = note.trim().ifBlank { null },
                date = now.toLocalDateTime(TimeZone.currentSystemDefault()).date,
                createdAt = now,
                updatedAt = now,
            ),
        )

        isSaving.value = false
        return when (result) {
            is com.raqeem.app.domain.model.Result.Success -> true
            is com.raqeem.app.domain.model.Result.Error -> setError(result.message)
            com.raqeem.app.domain.model.Result.Loading -> false
        }
    }

    fun clearError() {
        errorMessage.value = null
    }

    private fun setError(message: String): Boolean {
        errorMessage.value = message
        return false
    }

    private fun parseAmountToCents(input: String): Int? {
        val sanitized = input.trim().replace(",", "")
        val amount = sanitized.toDoubleOrNull() ?: return null
        if (amount <= 0.0) return null
        return (amount * 100).roundToInt()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddBottomSheet(
    initialContext: QuickAddContext,
    initialGoalId: String? = null,
    onDismiss: () -> Unit,
    viewModel: QuickAddViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    var currentContext by rememberSaveable(initialContext) { mutableStateOf(initialContext) }

    if (currentContext == QuickAddContext.Expense || currentContext == QuickAddContext.Income) {
        AddTransactionSheet(
            initialType = if (currentContext == QuickAddContext.Income) {
                TransactionType.INCOME
            } else {
                TransactionType.EXPENSE
            },
            onDismiss = onDismiss,
            onSaved = onDismiss,
        )
        return
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppColors.bgElevated,
        contentColor = AppColors.textPrimary,
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .size(36.dp, 4.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(AppColors.borderStrong),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = when (currentContext) {
                        QuickAddContext.Overview -> "Quick Add"
                        QuickAddContext.Transfer -> "Transfer"
                        else -> "Quick Add"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = AppColors.textMuted,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            if (currentContext == QuickAddContext.Transfer) {
                uiState.errorMessage?.let { message ->
                SurfaceCard(
                    backgroundColor = AppColors.negativeBg,
                    borderColor = AppColors.borderNegative,
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.negative,
                    )
                }
                }
            }

            when (currentContext) {
                QuickAddContext.Overview -> QuickAddChooser(
                    onSelect = { nextContext ->
                        viewModel.clearError()
                        currentContext = nextContext
                    },
                )
                QuickAddContext.Transfer -> AddTransferForm(
                    uiState = uiState,
                    viewModel = viewModel,
                    initialGoalId = initialGoalId,
                    onSaved = onDismiss,
                )
                QuickAddContext.Expense,
                QuickAddContext.Income,
                -> Unit
            }
        }
    }
}

@Composable
private fun QuickAddChooser(
    onSelect: (QuickAddContext) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FlowChoiceCard("Expense", "Fast log for purchases and daily spending.", AppColors.negative) {
            onSelect(QuickAddContext.Expense)
        }
        FlowChoiceCard("Income", "Capture salary, deposits, and money in.", AppColors.positive) {
            onSelect(QuickAddContext.Income)
        }
        FlowChoiceCard("Transfer", "Move funds between accounts and goals.", AppColors.purple400) {
            onSelect(QuickAddContext.Transfer)
        }
    }
}

@Composable
private fun FlowChoiceCard(
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
) {
    SurfaceCard(
        modifier = Modifier.clickable(onClick = onClick),
        backgroundColor = accent.copy(alpha = 0.08f),
        borderColor = accent.copy(alpha = 0.35f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(accent),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun AddTransferForm(
    uiState: QuickAddUiState,
    viewModel: QuickAddViewModel,
    initialGoalId: String?,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var amountInput by rememberSaveable { mutableStateOf("") }
    var noteInput by rememberSaveable { mutableStateOf("") }
    var fromAccountId by rememberSaveable { mutableStateOf<String?>(null) }
    var toAccountId by rememberSaveable { mutableStateOf<String?>(null) }
    var goalId by rememberSaveable(initialGoalId) { mutableStateOf<String?>(initialGoalId) }
    var rateInput by rememberSaveable { mutableStateOf(uiState.settings.usdToEgpRate.toString()) }

    LaunchedEffect(uiState.settings.usdToEgpRate) {
        if (rateInput.isBlank()) {
            rateInput = uiState.settings.usdToEgpRate.toString()
        }
    }

    LaunchedEffect(uiState.accounts) {
        if (fromAccountId == null) {
            fromAccountId = uiState.settings.defaultAccountId
                ?.takeIf { preferred -> uiState.accounts.any { it.id == preferred } }
                ?: uiState.accounts.firstOrNull()?.id
        }
        if (toAccountId == null) {
            toAccountId = uiState.accounts.firstOrNull { it.id != fromAccountId }?.id
                ?: uiState.accounts.firstOrNull()?.id
        }
    }

    val fromAccount = uiState.accounts.firstOrNull { it.id == fromAccountId }
    val toAccount = uiState.accounts.firstOrNull { it.id == toAccountId }
    val parsedAmountCents = amountInput.trim().replace(",", "").toDoubleOrNull()?.takeIf { it > 0 }?.times(100)?.roundToInt()
    val parsedRate = rateInput.toDoubleOrNull()
    val previewAmount = if (fromAccount != null && toAccount != null && parsedAmountCents != null && parsedRate != null) {
        when {
            fromAccount.currency == toAccount.currency -> parsedAmountCents
            fromAccount.currency.name == "USD" && toAccount.currency.name == "EGP" ->
                convertUsdToEgp(parsedAmountCents, parsedRate)
            else -> convertToUsd(parsedAmountCents, fromAccount.currency, parsedRate)
        }
    } else {
        null
    }

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SelectionField(
            label = "From",
            selectedText = fromAccount?.let { "${it.name} • ${it.currency.name}" } ?: "Choose source account",
            options = uiState.accounts.map { it.id to "${it.name} • ${it.currency.name}" },
            onSelect = { fromAccountId = it },
        )
        SelectionField(
            label = "To",
            selectedText = toAccount?.let { "${it.name} • ${it.currency.name}" } ?: "Choose destination account",
            options = uiState.accounts.map { it.id to "${it.name} • ${it.currency.name}" },
            onSelect = { toAccountId = it },
        )
        OutlinedTextField(
            value = amountInput,
            onValueChange = { amountInput = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Amount (${fromAccount?.currency?.name ?: "USD"})") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.borderAccent,
                unfocusedBorderColor = AppColors.borderSubtle,
                focusedContainerColor = AppColors.bgSubtle,
                unfocusedContainerColor = AppColors.bgSubtle,
            ),
        )
        if (fromAccount != null && toAccount != null && fromAccount.currency != toAccount.currency) {
            OutlinedTextField(
                value = rateInput,
                onValueChange = { rateInput = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Exchange Rate (1 USD = X EGP)") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.borderAccent,
                    unfocusedBorderColor = AppColors.borderSubtle,
                    focusedContainerColor = AppColors.bgSubtle,
                    unfocusedContainerColor = AppColors.bgSubtle,
                ),
            )
            SurfaceCard {
                Text(
                    text = "You'll receive: ${
                        if (previewAmount != null) {
                            "${toAccount.currency.name} ${(previewAmount / 100.0).toString().take(12)}"
                        } else {
                            "Enter amount and rate"
                        }
                    }",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        SelectionField(
            label = "Fund Goal (Optional)",
            selectedText = uiState.goals.firstOrNull { it.id == goalId }?.name ?: "No goal selected",
            options = listOf("" to "No goal selected") + uiState.goals.map { it.id to it.name },
            onSelect = { selected ->
                goalId = selected.ifBlank { null }
            },
        )
        OutlinedTextField(
            value = noteInput,
            onValueChange = { noteInput = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Note") },
            minLines = 2,
            maxLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.borderAccent,
                unfocusedBorderColor = AppColors.borderSubtle,
                focusedContainerColor = AppColors.bgSubtle,
                unfocusedContainerColor = AppColors.bgSubtle,
            ),
        )
        InfoRow("Date", "Today")
        Button(
            onClick = {
                scope.launch {
                    if (viewModel.submitTransfer(fromAccountId.orEmpty(), toAccountId.orEmpty(), goalId, amountInput, rateInput, noteInput)) {
                        onSaved()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSaving,
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.purple500,
                contentColor = Color.White,
            ),
            contentPadding = PaddingValues(vertical = 14.dp),
        ) {
            Text(if (uiState.isSaving) "Saving..." else "Confirm Transfer")
        }
    }
}

@Composable
private fun SelectionField(
    label: String,
    selectedText: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
) {
    SheetPickerField(
        label = label,
        selectedText = selectedText,
        options = options.map { PickerOption(value = it.first, label = it.second) },
        onSelect = onSelect,
    )
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
) {
    SurfaceCard(
        backgroundColor = AppColors.bgSurface,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textSecondary,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
