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
import androidx.compose.material.icons.rounded.Payments
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.raqeem.app.data.local.LOCAL_USER_ID
import com.raqeem.app.domain.model.Account
import com.raqeem.app.domain.model.Category
import com.raqeem.app.domain.model.Settings
import com.raqeem.app.domain.model.Transaction
import com.raqeem.app.domain.model.TransactionType
import com.raqeem.app.domain.usecase.AddTransactionUseCase
import com.raqeem.app.domain.usecase.GetAccountsUseCase
import com.raqeem.app.domain.usecase.GetCategoriesUseCase
import com.raqeem.app.domain.usecase.GetSettingsUseCase
import com.raqeem.app.domain.usecase.GetTransactionUseCase
import com.raqeem.app.domain.usecase.UpdateTransactionUseCase
import com.raqeem.app.presentation.ui.components.EmptyState
import com.raqeem.app.presentation.ui.components.SurfaceCard
import com.raqeem.app.presentation.ui.theme.AppColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlin.math.roundToInt

enum class TransactionEditorMode {
    Add,
    Edit,
}

data class TransactionEditorUiState(
    val isLoading: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val settings: Settings = Settings(userId = LOCAL_USER_ID),
    val initialType: TransactionType = TransactionType.EXPENSE,
    val existingTransaction: Transaction? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

private data class AddTransactionFormState(
    val type: TransactionType = TransactionType.EXPENSE,
    val amountRaw: String = "",
    val note: String = "",
    val selectedAccountId: String? = null,
    val selectedCategoryId: String? = null,
    val date: LocalDate = todayLocalDate(),
)

private data class TransactionEditorContentState(
    val accounts: List<Account>,
    val categories: List<Category>,
    val settings: Settings,
)

@HiltViewModel
class TransactionEditorViewModel @Inject constructor(
    private val getTransaction: GetTransactionUseCase,
    getAccounts: GetAccountsUseCase,
    getCategories: GetCategoriesUseCase,
    getSettings: GetSettingsUseCase,
    private val addTransaction: AddTransactionUseCase,
    private val updateTransaction: UpdateTransactionUseCase,
) : ViewModel() {

    private val isSaving = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val addFormState = MutableStateFlow(AddTransactionFormState())
    private val accounts = getAccounts()
    private val categories = getCategories()
    private val settings = getSettings()
    private val addContentState = combine(
        accounts,
        categories,
        settings,
    ) { accountList, categoryList, currentSettings ->
        TransactionEditorContentState(
            accounts = accountList,
            categories = categoryList,
            settings = currentSettings,
        )
    }

    val addUiState: StateFlow<AddTransactionUiState> = combine(
        addContentState,
        addFormState,
        isSaving,
        errorMessage,
    ) { content, form, saving, error ->
        val accountList = content.accounts
        val categoryList = content.categories
        val currentSettings = content.settings
        val availableAccounts = accountList
            .filterNot(Account::isHidden)
            .ifEmpty { accountList }
        val selectedAccount = availableAccounts.firstOrNull { it.id == form.selectedAccountId }
            ?: currentSettings.defaultAccountId
                ?.let { preferredId -> availableAccounts.firstOrNull { it.id == preferredId } }
            ?: availableAccounts.firstOrNull()
        val categoriesForType = categoryList.filter { it.type == form.type }
        val selectedCategory = categoriesForType.firstOrNull { it.id == form.selectedCategoryId }
            ?: categoriesForType.firstOrNull()
        val amountValue = form.amountRaw.toDoubleOrNull()

        AddTransactionUiState(
            accounts = availableAccounts,
            categories = categoriesForType,
            type = form.type,
            amountRaw = form.amountRaw,
            note = form.note,
            selectedAccount = selectedAccount,
            selectedCategory = selectedCategory,
            date = form.date,
            formattedDate = formatAddTransactionDate(form.date),
            isSaving = saving,
            errorMessage = error,
            isSubmitEnabled = !saving &&
                amountValue != null &&
                amountValue > 0.0 &&
                selectedAccount != null &&
                selectedCategory != null,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AddTransactionUiState(),
        )

    fun startAddSession(initialType: TransactionType) {
        viewModelScope.launch {
            val accountList = accounts.first()
            val categoryList = categories.first()
            val currentSettings = settings.first()
            val availableAccounts = accountList
                .filterNot(Account::isHidden)
                .ifEmpty { accountList }
            val defaultAccountId = currentSettings.defaultAccountId
                ?.takeIf { preferredId -> availableAccounts.any { it.id == preferredId } }
                ?: availableAccounts.firstOrNull()?.id
            val defaultCategoryId = categoryList
                .firstOrNull { it.type == initialType }
                ?.id

            addFormState.value = AddTransactionFormState(
                type = initialType,
                selectedAccountId = defaultAccountId,
                selectedCategoryId = defaultCategoryId,
                date = todayLocalDate(),
            )
            errorMessage.value = null
        }
    }

    fun setType(type: TransactionType) {
        addFormState.update { current ->
            if (current.type == type) {
                current
            } else {
                current.copy(
                    type = type,
                    selectedCategoryId = null,
                )
            }
        }
        errorMessage.value = null
    }

    fun setAmount(raw: String) {
        val cleaned = raw.filter { it.isDigit() || it == '.' }
        val parts = cleaned.split(".")
        val valid = when {
            parts.size > 2 -> return
            parts.size == 2 && parts[1].length > 2 -> return
            cleaned.toDoubleOrNull()?.let { it > 999_999.99 } == true -> return
            else -> cleaned
        }

        addFormState.update { current -> current.copy(amountRaw = valid) }
        errorMessage.value = null
    }

    fun setAccount(account: Account) {
        addFormState.update { current ->
            current.copy(selectedAccountId = account.id)
        }
        errorMessage.value = null
    }

    fun setCategory(category: Category) {
        addFormState.update { current ->
            current.copy(selectedCategoryId = category.id)
        }
        errorMessage.value = null
    }

    fun setDate(date: LocalDate) {
        addFormState.update { current -> current.copy(date = date) }
        errorMessage.value = null
    }

    fun setNote(note: String) {
        addFormState.update { current -> current.copy(note = note) }
    }

    suspend fun submitAddTransaction(): Boolean {
        val state = addUiState.value
        val account = state.selectedAccount ?: return setError("Choose an account.")
        val category = state.selectedCategory ?: return setError("Choose a category.")
        val amountCents = amountToCents(state.amountRaw)
        if (amountCents <= 0) {
            return setError("Enter a valid amount.")
        }

        isSaving.value = true
        errorMessage.value = null

        val now = Clock.System.now()
        val currentSettings = settings.first()
        val result = addTransaction(
            Transaction(
                id = UUID.randomUUID().toString(),
                userId = currentSettings.userId.ifBlank { LOCAL_USER_ID },
                accountId = account.id,
                categoryId = category.id,
                type = state.type,
                amountCents = amountCents,
                currency = account.currency,
                note = state.note.trim().ifBlank { null },
                date = state.date,
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

    fun uiState(
        mode: TransactionEditorMode,
        transactionId: String?,
        presetType: TransactionType?,
    ): Flow<TransactionEditorUiState> {
        val existingTransaction = if (mode == TransactionEditorMode.Edit && !transactionId.isNullOrBlank()) {
            getTransaction(transactionId)
        } else {
            flowOf(null)
        }

        val contentState = combine(
            accounts,
            categories,
            settings,
        ) { accountList, categoryList, currentSettings ->
            TransactionEditorContentState(
                accounts = accountList,
                categories = categoryList,
                settings = currentSettings,
            )
        }

        return combine(
            contentState,
            existingTransaction,
            isSaving,
            errorMessage,
        ) { content, transaction, saving, error ->
            TransactionEditorUiState(
                isLoading = false,
                accounts = content.accounts,
                categories = content.categories,
                settings = content.settings,
                initialType = transaction?.type ?: presetType ?: TransactionType.EXPENSE,
                existingTransaction = transaction,
                isSaving = saving,
                errorMessage = error,
            )
        }
            .catch { throwable ->
                emit(
                    TransactionEditorUiState(
                        isLoading = false,
                        errorMessage = throwable.message ?: "Unable to load transaction editor.",
                    ),
                )
            }
    }

    suspend fun submit(
        mode: TransactionEditorMode,
        transactionId: String?,
        existingTransaction: Transaction?,
        userId: String,
        type: TransactionType,
        accountId: String,
        categoryId: String?,
        amountInput: String,
        note: String,
        dateInput: String,
    ): Boolean {
        val account = accountsFromLatest().firstOrNull { it.id == accountId }
            ?: return setError("Choose an account.")
        val amountCents = parseAmountToCents(amountInput)
            ?: return setError("Enter a valid amount.")
        val parsedDate = parseDate(dateInput)
            ?: return setError("Enter a valid date in YYYY-MM-DD format.")
        if (categoryId.isNullOrBlank()) {
            return setError("Choose a category.")
        }

        isSaving.value = true
        errorMessage.value = null

        val now = Clock.System.now()
        val result = when (mode) {
            TransactionEditorMode.Add -> {
                addTransaction(
                    Transaction(
                        id = UUID.randomUUID().toString(),
                        userId = userId.ifBlank { LOCAL_USER_ID },
                        accountId = account.id,
                        categoryId = categoryId,
                        type = type,
                        amountCents = amountCents,
                        currency = account.currency,
                        note = note.trim().ifBlank { null },
                        date = parsedDate,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            }
            TransactionEditorMode.Edit -> {
                val baseTransaction = existingTransaction
                    ?: return finishWithError("Transaction not found.")
                updateTransaction(
                    baseTransaction.copy(
                        id = transactionId ?: baseTransaction.id,
                        userId = baseTransaction.userId.ifBlank { userId.ifBlank { LOCAL_USER_ID } },
                        accountId = account.id,
                        categoryId = categoryId,
                        type = type,
                        amountCents = amountCents,
                        currency = account.currency,
                        note = note.trim().ifBlank { null },
                        date = parsedDate,
                        updatedAt = now,
                    ),
                )
            }
        }

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

    private suspend fun accountsFromLatest(): List<Account> {
        return accounts.first()
    }

    private fun setError(message: String): Boolean {
        errorMessage.value = message
        return false
    }

    private fun finishWithError(message: String): Boolean {
        isSaving.value = false
        errorMessage.value = message
        return false
    }

    private fun amountToCents(raw: String): Int {
        val value = raw.toDoubleOrNull() ?: return 0
        return (value * 100).roundToInt()
    }

    private fun parseAmountToCents(input: String): Int? {
        val sanitized = input.trim().replace(",", "")
        val amount = sanitized.toDoubleOrNull() ?: return null
        if (amount <= 0.0) return null
        return (amount * 100).roundToInt()
    }

    private fun parseDate(input: String): LocalDate? {
        return try {
            LocalDate.parse(input.trim())
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditorBottomSheet(
    mode: TransactionEditorMode,
    onDismiss: () -> Unit,
    transactionId: String? = null,
    presetType: TransactionType? = null,
    viewModel: TransactionEditorViewModel = hiltViewModel(),
) {
    if (mode == TransactionEditorMode.Add) {
        AddTransactionSheet(
            initialType = presetType ?: TransactionType.EXPENSE,
            onDismiss = onDismiss,
            onSaved = onDismiss,
            viewModel = viewModel,
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
        TransactionEditorSheetContent(
            mode = mode,
            onSaved = onDismiss,
            transactionId = transactionId,
            presetType = presetType,
            viewModel = viewModel,
        )
    }
}

@Composable
fun TransactionEditorSheetContent(
    mode: TransactionEditorMode,
    onSaved: () -> Unit,
    transactionId: String? = null,
    presetType: TransactionType? = null,
    viewModel: TransactionEditorViewModel = hiltViewModel(),
) {
    val stateFlow = remember(mode, transactionId, presetType) {
        viewModel.uiState(mode, transactionId, presetType)
    }
    val uiState = stateFlow.collectAsStateWithLifecycle(
        initialValue = TransactionEditorUiState(
            isLoading = mode == TransactionEditorMode.Edit,
            initialType = presetType ?: TransactionType.EXPENSE,
        ),
    ).value

    val editorKey = remember(mode, transactionId, presetType) {
        "${mode.name}:${transactionId.orEmpty()}:${presetType?.name.orEmpty()}"
    }
    val scope = rememberCoroutineScope()
    var didInitialize by remember(editorKey) { mutableStateOf(false) }
    var selectedTypeName by remember(editorKey) { mutableStateOf(uiState.initialType.name) }
    var amountInput by remember(editorKey) { mutableStateOf("") }
    var noteInput by remember(editorKey) { mutableStateOf("") }
    var dateInput by remember(editorKey) { mutableStateOf(todayDateString()) }
    var selectedAccountId by remember(editorKey) { mutableStateOf<String?>(null) }
    var selectedCategoryId by remember(editorKey) { mutableStateOf<String?>(null) }

    LaunchedEffect(editorKey, uiState.accounts, uiState.settings.defaultAccountId, uiState.existingTransaction?.id) {
        if (didInitialize) return@LaunchedEffect

        when (mode) {
            TransactionEditorMode.Add -> {
                if (uiState.accounts.isEmpty()) return@LaunchedEffect
                val initialType = uiState.initialType
                selectedTypeName = initialType.name
                selectedAccountId = uiState.settings.defaultAccountId
                    ?.takeIf { preferred -> uiState.accounts.any { it.id == preferred } }
                    ?: uiState.accounts.firstOrNull()?.id
                selectedCategoryId = uiState.categories
                    .firstOrNull { it.type == initialType }
                    ?.id
                amountInput = ""
                noteInput = ""
                dateInput = todayDateString()
                didInitialize = true
            }
            TransactionEditorMode.Edit -> {
                val transaction = uiState.existingTransaction ?: return@LaunchedEffect
                selectedTypeName = transaction.type.name
                amountInput = "%.2f".format(Locale.US, transaction.amountCents / 100.0)
                noteInput = transaction.note.orEmpty()
                dateInput = transaction.date.toString()
                selectedAccountId = transaction.accountId
                selectedCategoryId = transaction.categoryId
                didInitialize = true
            }
        }
    }

    val selectedType = TransactionType.valueOf(selectedTypeName)
    val matchingCategories = remember(uiState.categories, selectedType) {
        uiState.categories.filter { it.type == selectedType }
    }
    val availableAccounts = remember(uiState.accounts, selectedAccountId) {
        buildList<Account> {
            addAll(uiState.accounts.filterNot { it.isHidden })
            val currentAccount = uiState.accounts.firstOrNull { it.id == selectedAccountId }
            if (currentAccount != null && none { account -> account.id == currentAccount.id }) {
                add(currentAccount)
            }
        }
    }
    val isFormReady = !uiState.isLoading && selectedAccountId != null && selectedCategoryId != null

    LaunchedEffect(matchingCategories, didInitialize) {
        if (!didInitialize) return@LaunchedEffect
        if (selectedCategoryId == null || matchingCategories.none { it.id == selectedCategoryId }) {
            selectedCategoryId = matchingCategories.firstOrNull()?.id
        }
    }

    val selectedAccount = uiState.accounts.firstOrNull { it.id == selectedAccountId }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 24.dp)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = when (mode) {
                TransactionEditorMode.Add -> if (selectedType == TransactionType.INCOME) "Add Income" else "Add Expense"
                TransactionEditorMode.Edit -> "Edit Transaction"
            },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )

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

        when {
            uiState.isLoading -> {
                SurfaceCard {
                    Text(
                        text = "Loading transaction...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.textSecondary,
                    )
                }
            }

            mode == TransactionEditorMode.Edit && uiState.existingTransaction == null -> {
                SurfaceCard {
                    EmptyState(
                        title = "Transaction not found",
                        subtitle = "This transaction may have been deleted or is no longer available in the local ledger.",
                        icon = Icons.Rounded.Payments,
                    )
                }
            }

            else -> {
                TransactionTypePicker(
                    selectedType = selectedType,
                    onSelect = { nextType ->
                        selectedTypeName = nextType.name
                        viewModel.clearError()
                    },
                )
                AmountHeroField(
                    amountInput = amountInput,
                    currencyLabel = selectedAccount?.currency?.name ?: "USD",
                    onValueChange = { amountInput = it },
                )
                PickerSectionLabel("Account")
                AccountPickerGrid(
                    accounts = availableAccounts,
                    selectedAccountId = selectedAccountId,
                    onSelect = { selectedAccountId = it },
                )
                PickerSectionLabel("Category")
                CategoryPickerGrid(
                    categories = matchingCategories,
                    selectedCategoryId = selectedCategoryId,
                    onSelect = { selectedCategoryId = it },
                )
                DateQuickPicker(
                    dateInput = dateInput,
                    onDateChange = { dateInput = it },
                )
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_editor_note_input"),
                    label = { Text("Note") },
                    minLines = 2,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.borderAccent,
                        unfocusedBorderColor = AppColors.borderSubtle,
                        focusedContainerColor = AppColors.bgSubtle,
                        unfocusedContainerColor = AppColors.bgSubtle,
                    ),
                )
                EditorInfoRow(
                    label = "Receipt",
                    value = if (uiState.existingTransaction?.receiptUrl != null) "Attached" else "Upload flow next",
                )
                Button(
                    onClick = {
                        scope.launch {
                            if (
                                viewModel.submit(
                                    mode = mode,
                                    transactionId = transactionId,
                                    existingTransaction = uiState.existingTransaction,
                                    userId = uiState.settings.userId.ifBlank { LOCAL_USER_ID },
                                    type = selectedType,
                                    accountId = selectedAccountId.orEmpty(),
                                    categoryId = selectedCategoryId,
                                    amountInput = amountInput,
                                    note = noteInput,
                                    dateInput = dateInput,
                                )
                            ) {
                                onSaved()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_editor_save_button"),
                    enabled = !uiState.isSaving && isFormReady,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.purple500,
                        contentColor = Color.White,
                    ),
                    contentPadding = PaddingValues(vertical = 14.dp),
                ) {
                    Text(
                        if (uiState.isSaving) {
                            "Saving..."
                        } else when (mode) {
                            TransactionEditorMode.Add -> if (selectedType == TransactionType.INCOME) "Add Income" else "Add Expense"
                            TransactionEditorMode.Edit -> "Save Changes"
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionTypePicker(
    selectedType: TransactionType,
    onSelect: (TransactionType) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TransactionTypeChip(
            modifier = Modifier.weight(1f),
            selected = selectedType == TransactionType.EXPENSE,
            title = "Expense",
            onClick = { onSelect(TransactionType.EXPENSE) },
        )
        TransactionTypeChip(
            modifier = Modifier.weight(1f),
            selected = selectedType == TransactionType.INCOME,
            title = "Income",
            onClick = { onSelect(TransactionType.INCOME) },
        )
    }
}

@Composable
private fun TransactionTypeChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SurfaceCard(
        modifier = modifier.clickable(onClick = onClick),
        backgroundColor = if (selected) AppColors.purple500.copy(alpha = 0.16f) else AppColors.bgSurface,
        borderColor = if (selected) AppColors.borderAccent else AppColors.borderSubtle,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) AppColors.textPrimary else AppColors.textSecondary,
        )
    }
}

@Composable
private fun AmountHeroField(
    amountInput: String,
    currencyLabel: String,
    onValueChange: (String) -> Unit,
) {
    SurfaceCard(
        backgroundColor = AppColors.bgSurface,
        borderColor = AppColors.borderAccent,
    ) {
        Text(
            text = "Amount",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.textSecondary,
        )
        OutlinedTextField(
            value = amountInput,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("transaction_editor_amount_input"),
            label = { Text("Amount ($currencyLabel)") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.borderAccent,
                unfocusedBorderColor = AppColors.borderSubtle,
                focusedContainerColor = AppColors.bgSubtle,
                unfocusedContainerColor = AppColors.bgSubtle,
            ),
        )
    }
}

@Composable
private fun PickerSectionLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = AppColors.textSecondary,
    )
}

@Composable
private fun AccountPickerGrid(
    accounts: List<Account>,
    selectedAccountId: String?,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        accounts.chunked(2).forEach { rowAccounts ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowAccounts.forEach { account ->
                    val selected = account.id == selectedAccountId
                    SurfaceCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelect(account.id) },
                        backgroundColor = if (selected) AppColors.purple500.copy(alpha = 0.16f) else AppColors.bgSurface,
                        borderColor = if (selected) AppColors.borderAccent else AppColors.borderSubtle,
                    ) {
                        Text(
                            text = account.name,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "${account.type.name.lowercase().replaceFirstChar { it.uppercase() }} • ${account.currency.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.textSecondary,
                        )
                    }
                }
                if (rowAccounts.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CategoryPickerGrid(
    categories: List<Category>,
    selectedCategoryId: String?,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.chunked(2).forEach { rowCategories ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowCategories.forEach { category ->
                    val selected = category.id == selectedCategoryId
                    SurfaceCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelect(category.id) },
                        backgroundColor = if (selected) AppColors.purple500.copy(alpha = 0.16f) else AppColors.bgSurface,
                        borderColor = if (selected) AppColors.borderAccent else AppColors.borderSubtle,
                    ) {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = category.icon.ifBlank { "category" },
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.textMuted,
                        )
                    }
                }
                if (rowCategories.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DateQuickPicker(
    dateInput: String,
    onDateChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PickerSectionLabel("Date")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DateShortcutChip(
                modifier = Modifier.weight(1f),
                title = "Today",
                selected = dateInput == todayDateString(),
                onClick = { onDateChange(todayDateString()) },
            )
            DateShortcutChip(
                modifier = Modifier.weight(1f),
                title = "Yesterday",
                selected = dateInput == yesterdayDateString(),
                onClick = { onDateChange(yesterdayDateString()) },
            )
        }
        OutlinedTextField(
            value = dateInput,
            onValueChange = onDateChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("transaction_editor_date_input"),
            label = { Text("Date (YYYY-MM-DD)") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.borderAccent,
                unfocusedBorderColor = AppColors.borderSubtle,
                focusedContainerColor = AppColors.bgSubtle,
                unfocusedContainerColor = AppColors.bgSubtle,
            ),
        )
    }
}

@Composable
private fun DateShortcutChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SurfaceCard(
        modifier = modifier.clickable(onClick = onClick),
        backgroundColor = if (selected) AppColors.purple500.copy(alpha = 0.16f) else AppColors.bgSurface,
        borderColor = if (selected) AppColors.borderAccent else AppColors.borderSubtle,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) AppColors.textPrimary else AppColors.textSecondary,
        )
    }
}

@Composable
private fun EditorSelectionField(
    label: String,
    selectedText: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.textSecondary,
        )
        Box {
            SurfaceCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true },
                backgroundColor = AppColors.bgSurface,
            ) {
                Text(
                    text = selectedText,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.92f),
            ) {
                options.forEach { (value, text) ->
                    DropdownMenuItem(
                        text = { Text(text) },
                        onClick = {
                            expanded = false
                            onSelect(value)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorInfoRow(
    label: String,
    value: String,
) {
    SurfaceCard(backgroundColor = AppColors.bgSurface) {
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

private fun todayDateString(): String {
    return Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
}

private fun yesterdayDateString(): String {
    val date = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    return java.time.LocalDate.of(date.year, date.monthNumber, date.dayOfMonth)
        .minusDays(1)
        .toString()
}

private fun todayLocalDate(): LocalDate {
    return Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
}

private fun formatAddTransactionDate(date: LocalDate): String {
    val today = todayLocalDate()
    val yesterday = java.time.LocalDate.of(today.year, today.monthNumber, today.dayOfMonth)
        .minusDays(1)
    val javaDate = java.time.LocalDate.of(date.year, date.monthNumber, date.dayOfMonth)

    return when {
        javaDate == java.time.LocalDate.of(today.year, today.monthNumber, today.dayOfMonth) -> "Today"
        javaDate == yesterday -> "Yesterday"
        else -> javaDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))
    }
}
