package com.raqeem.app.presentation.ui.screens.accounts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.raqeem.app.data.local.LOCAL_USER_ID
import com.raqeem.app.data.local.dao.SubscriptionDao
import com.raqeem.app.data.local.dao.TransactionDao
import com.raqeem.app.data.local.dao.TransferDao
import com.raqeem.app.domain.model.Account
import com.raqeem.app.domain.model.AccountType
import com.raqeem.app.domain.model.Currency
import com.raqeem.app.domain.model.Result
import com.raqeem.app.domain.usecase.AddAccountUseCase
import com.raqeem.app.domain.usecase.DeleteAccountUseCase
import com.raqeem.app.domain.usecase.GetAccountsUseCase
import com.raqeem.app.domain.usecase.GetSettingsUseCase
import com.raqeem.app.domain.usecase.SetDefaultAccountUseCase
import com.raqeem.app.domain.usecase.UpdateAccountUseCase
import com.raqeem.app.presentation.ui.components.AmountText
import com.raqeem.app.presentation.ui.components.EmptyState
import com.raqeem.app.presentation.ui.components.HeaderIconButton
import com.raqeem.app.presentation.ui.components.PageHeader
import com.raqeem.app.presentation.ui.components.PickerOption
import com.raqeem.app.presentation.ui.components.SheetPickerField
import com.raqeem.app.presentation.ui.components.SurfaceCard
import com.raqeem.app.presentation.ui.theme.AppColors
import com.raqeem.app.presentation.ui.theme.AppTypography
import com.raqeem.app.presentation.ui.theme.MonoFamily
import com.raqeem.app.util.convertToUsd
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlin.math.roundToInt

data class AccountsUiState(
    val userId: String = LOCAL_USER_ID,
    val defaultAccountId: String? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val netWorthCents: Int = 0,
    val error: String? = null,
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    getAccounts: GetAccountsUseCase,
    getSettings: GetSettingsUseCase,
    private val addAccount: AddAccountUseCase,
    private val setDefaultAccount: SetDefaultAccountUseCase,
    private val updateAccount: UpdateAccountUseCase,
    private val deleteAccount: DeleteAccountUseCase,
    private val transactionDao: TransactionDao,
    private val transferDao: TransferDao,
    private val subscriptionDao: SubscriptionDao,
) : ViewModel() {

    private val isSaving = MutableStateFlow(false)
    private val actionError = MutableStateFlow<String?>(null)

    val uiState = combine(
        getAccounts(),
        getSettings(),
        isSaving,
        actionError,
    ) { accounts, settings, saving, error ->
        val netWorth = accounts.sumOf { account ->
            if (account.currency == Currency.USD) {
                account.balanceCents
            } else {
                convertToUsd(account.balanceCents, account.currency, settings.usdToEgpRate)
            }
        }

        AccountsUiState(
            userId = settings.userId.ifBlank { LOCAL_USER_ID },
            defaultAccountId = settings.defaultAccountId,
            isLoading = false,
            isSaving = saving,
            accounts = accounts,
            netWorthCents = netWorth,
            error = error,
        )
    }
        .catch { throwable ->
            emit(
                AccountsUiState(
                    isLoading = false,
                    error = throwable.message ?: "Unable to load accounts.",
                ),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AccountsUiState(),
        )

    suspend fun createAccount(
        name: String,
        type: AccountType,
        currency: Currency,
        openingBalanceInput: String,
        setAsDefault: Boolean,
    ): Boolean {
        val openingBalanceCents = parseAmountToCents(openingBalanceInput)
            ?: return setError("Enter a valid opening balance.")

        isSaving.value = true
        actionError.value = null

        val now = Clock.System.now()
        val accountId = UUID.randomUUID().toString()
        val result = addAccount(
            Account(
                id = accountId,
                userId = uiState.value.userId,
                name = name,
                type = type,
                currency = currency,
                initialAmountCents = openingBalanceCents,
                balanceCents = openingBalanceCents,
                sortOrder = (uiState.value.accounts.maxOfOrNull(Account::sortOrder) ?: -1) + 1,
                createdAt = now,
                updatedAt = now,
            ),
        )

        if (result is Result.Error) {
            isSaving.value = false
            return setError(result.message)
        }

        if (setAsDefault) {
            val defaultResult = setDefaultAccount(accountId)
            if (defaultResult is Result.Error) {
                isSaving.value = false
                return setError(defaultResult.message)
            }
        }

        isSaving.value = false
        actionError.value = null
        return true
    }

    suspend fun saveAccount(
        existingAccount: Account,
        name: String,
        type: AccountType,
        currency: Currency,
        openingBalanceInput: String,
    ): Boolean {
        val openingBalanceCents = parseAmountToCents(openingBalanceInput)
            ?: return setError("Enter a valid opening balance.")

        isSaving.value = true
        actionError.value = null

        val result = updateAccount(
            existingAccount.copy(
                name = name.trim(),
                type = type,
                currency = currency,
                initialAmountCents = openingBalanceCents,
                updatedAt = Clock.System.now(),
            ),
        )

        isSaving.value = false
        return when (result) {
            is Result.Success -> true
            is Result.Error -> setError(result.message)
            Result.Loading -> false
        }
    }

    fun toggleArchived(account: Account) {
        viewModelScope.launch {
            isSaving.value = true
            actionError.value = null
            when (
                val result = updateAccount(
                    account.copy(
                        isHidden = !account.isHidden,
                        updatedAt = Clock.System.now(),
                    ),
                )
            ) {
                is Result.Error -> actionError.value = result.message
                else -> actionError.value = null
            }
            isSaving.value = false
        }
    }

    fun removeAccount(account: Account) {
        viewModelScope.launch {
            val transactionCount = transactionDao.countActiveByAccount(account.id)
            val transferCount = transferDao.countActiveByAccount(account.id)
            val subscriptionCount = subscriptionDao.countActiveByAccount(account.id)
            if (transactionCount > 0 || transferCount > 0 || subscriptionCount > 0) {
                actionError.value = "This account still has activity. Archive it instead of deleting."
                return@launch
            }

            isSaving.value = true
            actionError.value = null
            when (val result = deleteAccount(account.id)) {
                is Result.Error -> actionError.value = result.message
                else -> {
                    if (uiState.value.defaultAccountId == account.id) {
                        setDefaultAccount(null)
                    }
                }
            }
            isSaving.value = false
        }
    }

    fun clearError() {
        actionError.value = null
    }

    private fun setError(message: String): Boolean {
        actionError.value = message
        return false
    }

    private fun parseAmountToCents(input: String): Int? {
        val sanitized = input.trim().replace(",", "")
        if (sanitized.isBlank()) return 0
        val amount = sanitized.toDoubleOrNull() ?: return null
        if (amount < 0.0) return null
        return (amount * 100).roundToInt()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountsScreen(
    onOpenSettings: () -> Unit,
    onOpenAccount: (String) -> Unit,
    viewModel: AccountsViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    var revealedNetWorth by remember { mutableStateOf(false) }
    var revealedAccounts by remember { mutableStateOf(setOf<String>()) }
    var showAddAccountSheet by rememberSaveable { mutableStateOf(false) }
    var showEditAccountSheet by rememberSaveable { mutableStateOf(false) }
    var editingAccountId by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingAccountId by rememberSaveable { mutableStateOf<String?>(null) }
    val editingAccount = uiState.accounts.firstOrNull { it.id == editingAccountId }
    val deletingAccount = uiState.accounts.firstOrNull { it.id == deletingAccountId }
    val sortedAccounts = uiState.accounts.sortedWith(compareBy<Account> { it.isHidden }.thenBy { it.sortOrder })

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.bgBase)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item("accounts_header", contentType = "header") {
            PageHeader(
                title = "Accounts",
                supportingText = "Net worth and balances stay hidden until you reveal them.",
                trailing = {
                    HeaderIconButton(
                        icon = Icons.Rounded.Add,
                        contentDescription = "Add account",
                        onClick = {
                            viewModel.clearError()
                            showAddAccountSheet = true
                        },
                        tint = AppColors.textPrimary,
                    )
                    HeaderIconButton(
                        icon = Icons.Rounded.Settings,
                        contentDescription = "Open settings",
                        onClick = onOpenSettings,
                    )
                },
            )
        }

        item("accounts_net_worth", contentType = "net_worth") {
            SurfaceCard(
                backgroundColor = AppColors.bgElevated,
                borderColor = AppColors.borderAccent.copy(alpha = 0.35f),
                modifier = Modifier.animateItemPlacement(),
            ) {
                Text(
                    text = "NET WORTH",
                    style = AppTypography.sectionLabel,
                    color = AppColors.textMuted,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (revealedNetWorth) {
                        AmountText(
                            amountCents = uiState.netWorthCents,
                            currency = Currency.USD,
                            style = AppTypography.heroAmount,
                        )
                    } else {
                        Text(
                            text = "${Currency.USD.symbol} •••••••",
                            style = AppTypography.heroAmount,
                        )
                    }
                    IconButton(onClick = { revealedNetWorth = !revealedNetWorth }) {
                        Icon(
                            imageVector = if (revealedNetWorth) {
                                Icons.Rounded.Visibility
                            } else {
                                Icons.Rounded.VisibilityOff
                            },
                            contentDescription = "Toggle net worth visibility",
                            tint = AppColors.textSecondary,
                        )
                    }
                }
                Text(
                    text = "USD base currency",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary,
                )
            }
        }

        if (uiState.accounts.isEmpty() && !uiState.isLoading) {
            item("accounts_empty", contentType = "empty") {
                SurfaceCard(modifier = Modifier.animateItemPlacement()) {
                    EmptyState(
                        title = "No accounts yet",
                        subtitle = "Create the first local account to start logging balances, transfers, and quick-add activity.",
                        icon = Icons.Rounded.AccountBalanceWallet,
                        actionLabel = "Add Account",
                        onAction = {
                            viewModel.clearError()
                            showAddAccountSheet = true
                        },
                    )
                }
            }
        } else {
            items(
                sortedAccounts,
                key = { it.id },
                contentType = { "account_card" }
            ) { account ->
                AccountCard(
                    modifier = Modifier.animateItemPlacement(),
                    account = account,
                    isDefault = account.id == uiState.defaultAccountId,
                    isRevealed = revealedAccounts.contains(account.id),
                    onOpen = { onOpenAccount(account.id) },
                    onEdit = {
                        viewModel.clearError()
                        editingAccountId = account.id
                        showEditAccountSheet = true
                    },
                    onArchive = { viewModel.toggleArchived(account) },
                    onDelete = {
                        viewModel.clearError()
                        deletingAccountId = account.id
                    },
                    onToggleReveal = {
                        revealedAccounts = if (revealedAccounts.contains(account.id)) {
                            revealedAccounts - account.id
                        } else {
                            revealedAccounts + account.id
                        }
                    },
                )
            }
        }

        uiState.error?.let { error ->
            item("accounts_error", contentType = "error") {
                SurfaceCard(
                    modifier = Modifier.animateItemPlacement(),
                    borderColor = AppColors.borderNegative,
                    backgroundColor = AppColors.negativeBg,
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.negative,
                    )
                }
            }
        }
    }

    if (showAddAccountSheet) {
        AddAccountBottomSheet(
            uiState = uiState,
            onDismiss = { showAddAccountSheet = false },
            onSave = viewModel::createAccount,
        )
    }

    if (showEditAccountSheet && editingAccount != null) {
        EditAccountBottomSheet(
            account = editingAccount,
            uiState = uiState,
            onDismiss = {
                showEditAccountSheet = false
                editingAccountId = null
            },
            onSave = { name, type, currency, openingBalance ->
                viewModel.saveAccount(editingAccount, name, type, currency, openingBalance)
            },
        )
    }

    deletingAccount?.let { account ->
        AlertDialog(
            onDismissRequest = { deletingAccountId = null },
            title = { Text("Delete account?") },
            text = { Text("This permanently removes the account only if it has no linked transactions, transfers, or subscriptions.") },
            confirmButton = {
                TextButton(onClick = {
                    deletingAccountId = null
                    viewModel.removeAccount(account)
                }) {
                    Text(if (uiState.isSaving) "Deleting..." else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingAccountId = null }) {
                    Text("Cancel")
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = AppColors.negative,
                )
            },
        )
    }
}

@Composable
private fun AccountCard(
    account: Account,
    isDefault: Boolean,
    isRevealed: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onToggleReveal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SurfaceCard(
        modifier = modifier.clickable(onClick = onOpen),
        backgroundColor = if (account.isHidden) AppColors.bgSurface else AppColors.bgElevated,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    StatusBadge(account.type.displayName())
                    if (isDefault) {
                        DefaultBadge()
                    }
                }
                Text(
                    text = buildString {
                        append(account.type.displayName())
                        append(" • ")
                        append(account.currency.displayName())
                        if (account.isHidden) append(" • Archived")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textMuted,
                )
                if (isRevealed) {
                    AmountText(
                        amountCents = account.balanceCents,
                        currency = account.currency,
                        style = AppTypography.largeAmount,
                    )
                } else {
                    Text(
                        text = "${account.currency.name} •••••••",
                        style = AppTypography.largeAmount,
                    )
                }
                Text(
                    text = buildAnnotatedString {
                        append("Opening ")
                        withStyle(
                            SpanStyle(
                                fontFamily = MonoFamily,
                                fontFeatureSettings = "tnum"
                            )
                        ) {
                            append(account.initialAmountCents.formatDisplay(account.currency))
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onEdit) {
                        Text("Edit", color = AppColors.purple300)
                    }
                    TextButton(onClick = onArchive) {
                        Text(if (account.isHidden) "Restore" else "Archive", color = AppColors.purple300)
                    }
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = AppColors.negative)
                    }
                }
            }
            IconButton(onClick = onToggleReveal) {
                Icon(
                    imageVector = if (isRevealed) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                    contentDescription = "Toggle balance visibility",
                    tint = AppColors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(AppColors.bgSubtle)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = label.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.textSecondary,
        )
    }
}

@Composable
private fun DefaultBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(AppColors.purple500.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = "DEFAULT",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.purple200,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAccountBottomSheet(
    uiState: AccountsUiState,
    onDismiss: () -> Unit,
    onSave: suspend (String, AccountType, Currency, String, Boolean) -> Boolean,
) {
    val scope = rememberCoroutineScope()
    var nameInput by rememberSaveable { mutableStateOf("") }
    var openingBalanceInput by rememberSaveable { mutableStateOf("") }
    var selectedType by rememberSaveable { mutableStateOf(AccountType.CHECKING.name) }
    var selectedCurrency by rememberSaveable { mutableStateOf(Currency.USD.name) }
    var setAsDefault by rememberSaveable { mutableStateOf(uiState.accounts.isEmpty()) }

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Add Account",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Create a local account for balances, quick add, and transfers.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.textSecondary,
            )

            uiState.error?.let { message ->
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

            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Account Name") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.borderAccent,
                    unfocusedBorderColor = AppColors.borderSubtle,
                    focusedContainerColor = AppColors.bgSubtle,
                    unfocusedContainerColor = AppColors.bgSubtle,
                ),
            )
            AccountSelectionField(
                label = "Type",
                selectedText = AccountType.valueOf(selectedType).displayName(),
                options = AccountType.entries.map { it.name to it.displayName() },
                onSelect = { selectedType = it },
            )
            AccountSelectionField(
                label = "Currency",
                selectedText = Currency.valueOf(selectedCurrency).displayName(),
                options = Currency.entries.map { it.name to it.displayName() },
                onSelect = { selectedCurrency = it },
            )
            OutlinedTextField(
                value = openingBalanceInput,
                onValueChange = { openingBalanceInput = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Opening Balance") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.borderAccent,
                    unfocusedBorderColor = AppColors.borderSubtle,
                    focusedContainerColor = AppColors.bgSubtle,
                    unfocusedContainerColor = AppColors.bgSubtle,
                ),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { setAsDefault = !setAsDefault }
                    .background(AppColors.bgSurface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Checkbox(
                    checked = setAsDefault,
                    onCheckedChange = { setAsDefault = it },
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Make default account",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Used to prefill quick-add and settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.textSecondary,
                    )
                }
            }
            Button(
                onClick = {
                    scope.launch {
                        val didSave = onSave(
                            nameInput,
                            AccountType.valueOf(selectedType),
                            Currency.valueOf(selectedCurrency),
                            openingBalanceInput,
                            setAsDefault,
                        )
                        if (didSave) {
                            onDismiss()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.purple500,
                    contentColor = Color.White,
                ),
            ) {
                Text(if (uiState.isSaving) "Saving..." else "Create Account")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditAccountBottomSheet(
    account: Account,
    uiState: AccountsUiState,
    onDismiss: () -> Unit,
    onSave: suspend (String, AccountType, Currency, String) -> Boolean,
) {
    val scope = rememberCoroutineScope()
    var nameInput by rememberSaveable(account.id) { mutableStateOf(account.name) }
    var openingBalanceInput by rememberSaveable(account.id) {
        mutableStateOf("%.2f".format(Locale.US, account.initialAmountCents / 100.0))
    }
    var selectedType by rememberSaveable(account.id) { mutableStateOf(account.type.name) }
    var selectedCurrency by rememberSaveable(account.id) { mutableStateOf(account.currency.name) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppColors.bgElevated,
        contentColor = AppColors.textPrimary,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Edit Account",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Update the account name, type, currency, or opening balance. Use Archive on the card to hide it without losing history.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.textSecondary,
            )

            uiState.error?.let { message ->
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

            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Account Name") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.borderAccent,
                    unfocusedBorderColor = AppColors.borderSubtle,
                    focusedContainerColor = AppColors.bgSubtle,
                    unfocusedContainerColor = AppColors.bgSubtle,
                ),
            )
            AccountSelectionField(
                label = "Type",
                selectedText = AccountType.valueOf(selectedType).displayName(),
                options = AccountType.entries.map { it.name to it.displayName() },
                onSelect = { selectedType = it },
            )
            AccountSelectionField(
                label = "Currency",
                selectedText = Currency.valueOf(selectedCurrency).displayName(),
                options = Currency.entries.map { it.name to it.displayName() },
                onSelect = { selectedCurrency = it },
            )
            OutlinedTextField(
                value = openingBalanceInput,
                onValueChange = { openingBalanceInput = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Opening Balance") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.borderAccent,
                    unfocusedBorderColor = AppColors.borderSubtle,
                    focusedContainerColor = AppColors.bgSubtle,
                    unfocusedContainerColor = AppColors.bgSubtle,
                ),
            )
            Button(
                onClick = {
                    scope.launch {
                        if (
                            onSave(
                                nameInput,
                                AccountType.valueOf(selectedType),
                                Currency.valueOf(selectedCurrency),
                                openingBalanceInput,
                            )
                        ) {
                            onDismiss()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.purple500,
                    contentColor = Color.White,
                ),
            ) {
                Text(if (uiState.isSaving) "Saving..." else "Save Account")
            }
        }
    }
}

@Composable
private fun AccountSelectionField(
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

private fun AccountType.displayName(): String {
    return name.lowercase(Locale.getDefault()).replaceFirstChar { char ->
        char.titlecase(Locale.getDefault())
    }
}

private fun Currency.displayName(): String {
    return name
}

private fun Int.formatDisplay(currency: Currency): String {
    val prefix = if (currency == Currency.USD) "$" else "EGP "
    return "$prefix${"%.2f".format(this / 100.0)}"
}
