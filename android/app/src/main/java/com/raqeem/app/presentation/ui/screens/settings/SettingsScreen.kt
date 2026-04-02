package com.raqeem.app.presentation.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
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
import com.raqeem.app.domain.model.Category
import com.raqeem.app.domain.model.Currency
import com.raqeem.app.domain.model.LocalPreferences
import com.raqeem.app.domain.model.Settings
import com.raqeem.app.domain.model.TransactionType
import com.raqeem.app.domain.model.UnlockState
import com.raqeem.app.domain.model.Result
import com.raqeem.app.domain.repository.AppLockRepository
import com.raqeem.app.domain.usecase.AddAccountUseCase
import com.raqeem.app.domain.usecase.AddCategoryUseCase
import com.raqeem.app.domain.usecase.DeleteAccountUseCase
import com.raqeem.app.domain.usecase.DeleteCategoryUseCase
import com.raqeem.app.domain.usecase.GetAccountsUseCase
import com.raqeem.app.domain.usecase.GetCategoriesUseCase
import com.raqeem.app.domain.usecase.GetLocalPreferencesUseCase
import com.raqeem.app.domain.usecase.GetSettingsUseCase
import com.raqeem.app.domain.usecase.SetAiInsightsEnabledUseCase
import com.raqeem.app.domain.usecase.SetBudgetWarningsEnabledUseCase
import com.raqeem.app.domain.usecase.SetDefaultAccountUseCase
import com.raqeem.app.domain.usecase.SetSubscriptionRemindersEnabledUseCase
import com.raqeem.app.domain.usecase.SetWeeklySummaryEnabledUseCase
import com.raqeem.app.domain.usecase.UpdateAccountUseCase
import com.raqeem.app.domain.usecase.UpdateCategoryUseCase
import com.raqeem.app.domain.usecase.UpdateExchangeRateUseCase
import com.raqeem.app.presentation.ui.components.EmptyState
import com.raqeem.app.presentation.ui.components.HeaderIconButton
import com.raqeem.app.presentation.ui.components.PageHeader
import com.raqeem.app.presentation.ui.components.PickerOption
import com.raqeem.app.presentation.ui.components.SectionLabel
import com.raqeem.app.presentation.ui.components.SheetPickerField
import com.raqeem.app.presentation.ui.components.SurfaceCard
import com.raqeem.app.presentation.ui.theme.AppColors
import com.raqeem.app.presentation.ui.theme.MonoFamily
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

data class SettingsUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val settings: Settings = Settings(),
    val localPreferences: LocalPreferences = LocalPreferences(),
    val unlockState: UnlockState = UnlockState(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val defaultAccountName: String? = null,
    val accountCount: Int = 0,
    val categoryCount: Int = 0,
    val error: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    getSettings: GetSettingsUseCase,
    getAccounts: GetAccountsUseCase,
    getCategories: GetCategoriesUseCase,
    getLocalPreferences: GetLocalPreferencesUseCase,
    appLockRepository: AppLockRepository,
    private val updateExchangeRateUseCase: UpdateExchangeRateUseCase,
    private val setDefaultAccountUseCase: SetDefaultAccountUseCase,
    private val addAccountUseCase: AddAccountUseCase,
    private val updateAccountUseCase: UpdateAccountUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val addCategoryUseCase: AddCategoryUseCase,
    private val updateCategoryUseCase: UpdateCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase,
    private val setAiInsightsEnabledUseCase: SetAiInsightsEnabledUseCase,
    private val setBudgetWarningsEnabledUseCase: SetBudgetWarningsEnabledUseCase,
    private val setSubscriptionRemindersEnabledUseCase: SetSubscriptionRemindersEnabledUseCase,
    private val setWeeklySummaryEnabledUseCase: SetWeeklySummaryEnabledUseCase,
    private val transactionDao: TransactionDao,
    private val transferDao: TransferDao,
    private val subscriptionDao: SubscriptionDao,
) : ViewModel() {

    private val isSaving = MutableStateFlow(false)
    private val actionError = MutableStateFlow<String?>(null)
    private val appLock = appLockRepository

    private val baseUiState = combine(
        getSettings(),
        getAccounts(),
        getCategories(),
        getLocalPreferences(),
        appLockRepository.observeUnlockState(),
    ) { settings, accounts, categories, localPreferences, unlockState ->
        SettingsUiState(
            isLoading = false,
            settings = settings,
            localPreferences = localPreferences,
            unlockState = unlockState,
            accounts = accounts,
            categories = categories,
            defaultAccountName = accounts.firstOrNull { it.id == settings.defaultAccountId }?.name,
            accountCount = accounts.size,
            categoryCount = categories.size,
        )
    }

    val uiState = combine(
        baseUiState,
        isSaving,
        actionError,
    ) { base, saving, error ->
        base.copy(
            isSaving = saving,
            error = error,
        )
    }
        .catch { throwable ->
            emit(
                SettingsUiState(
                    isLoading = false,
                    error = throwable.message ?: "Unable to load settings.",
                ),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

    suspend fun updateExchangeRate(rateInput: String): Boolean {
        val rate = rateInput.trim().toDoubleOrNull()
            ?: return setError("Enter a valid exchange rate.")

        isSaving.value = true
        actionError.value = null

        val result = updateExchangeRateUseCase(rate)
        isSaving.value = false
        return when (result) {
            is Result.Success -> true
            is Result.Error -> setError(result.message)
            Result.Loading -> false
        }
    }

    suspend fun updateDefaultAccount(accountId: String?): Boolean {
        isSaving.value = true
        actionError.value = null

        val result = setDefaultAccountUseCase(accountId)
        isSaving.value = false
        return when (result) {
            is Result.Success -> true
            is Result.Error -> setError(result.message)
            Result.Loading -> false
        }
    }

    suspend fun saveAccount(
        existingAccount: Account?,
        name: String,
        type: AccountType,
        currency: Currency,
        openingBalanceInput: String,
        isHidden: Boolean,
    ): Boolean {
        val openingBalanceCents = parseAmountToCents(openingBalanceInput)
            ?: return setError("Enter a valid opening balance.")

        isSaving.value = true
        actionError.value = null
        val now = Clock.System.now()
        val account = (existingAccount ?: Account(
            id = UUID.randomUUID().toString(),
            userId = uiState.value.settings.userId.ifBlank { LOCAL_USER_ID },
            name = name.trim(),
            type = type,
            currency = currency,
            initialAmountCents = openingBalanceCents,
            balanceCents = openingBalanceCents,
            sortOrder = (uiState.value.accounts.maxOfOrNull(Account::sortOrder) ?: -1) + 1,
            createdAt = now,
            updatedAt = now,
        )).copy(
            userId = existingAccount?.userId ?: uiState.value.settings.userId.ifBlank { LOCAL_USER_ID },
            name = name.trim(),
            type = type,
            currency = currency,
            initialAmountCents = openingBalanceCents,
            balanceCents = existingAccount?.balanceCents ?: openingBalanceCents,
            isHidden = isHidden,
            updatedAt = now,
        )

        val result = if (existingAccount == null) addAccountUseCase(account) else updateAccountUseCase(account)
        isSaving.value = false
        return when (result) {
            is Result.Success -> true
            is Result.Error -> setError(result.message)
            Result.Loading -> false
        }
    }

    fun toggleAccountHidden(account: Account) {
        viewModelScope.launch {
            isSaving.value = true
            actionError.value = null
            when (val result = updateAccountUseCase(account.copy(isHidden = !account.isHidden, updatedAt = Clock.System.now()))) {
                is Result.Error -> actionError.value = result.message
                else -> actionError.value = null
            }
            isSaving.value = false
        }
    }

    fun deleteAccount(account: Account) {
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
            when (val result = deleteAccountUseCase(account.id)) {
                is Result.Error -> actionError.value = result.message
                else -> {
                    if (uiState.value.settings.defaultAccountId == account.id) {
                        setDefaultAccountUseCase(null)
                    }
                    actionError.value = null
                }
            }
            isSaving.value = false
        }
    }

    suspend fun saveCategory(
        existingCategory: Category?,
        name: String,
        type: TransactionType,
        icon: String,
        color: String,
        budgetInput: String,
    ): Boolean {
        val budgetCents = budgetInput.trim().replace(",", "").takeIf { it.isNotBlank() }?.toDoubleOrNull()?.let { amount ->
            if (amount < 0.0) return setError("Budget must be zero or greater.")
            (amount * 100).roundToInt()
        }

        isSaving.value = true
        actionError.value = null
        val now = Clock.System.now()
        val category = (existingCategory ?: Category(
            id = UUID.randomUUID().toString(),
            userId = uiState.value.settings.userId.ifBlank { LOCAL_USER_ID },
            name = name.trim(),
            type = type,
            icon = icon.trim().ifBlank { "circle" },
            color = color.trim().ifBlank { "#8B5CF6" },
            budgetCents = budgetCents,
            createdAt = now,
            updatedAt = now,
        )).copy(
            userId = existingCategory?.userId ?: uiState.value.settings.userId.ifBlank { LOCAL_USER_ID },
            name = name.trim(),
            type = type,
            icon = icon.trim().ifBlank { "circle" },
            color = color.trim().ifBlank { "#8B5CF6" },
            budgetCents = budgetCents,
            updatedAt = now,
        )

        val result = if (existingCategory == null) addCategoryUseCase(category) else updateCategoryUseCase(category)
        isSaving.value = false
        return when (result) {
            is Result.Success -> true
            is Result.Error -> setError(result.message)
            Result.Loading -> false
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            val transactionCount = transactionDao.countActiveByCategory(category.id)
            val subscriptionCount = subscriptionDao.countActiveByCategory(category.id)
            if (transactionCount > 0 || subscriptionCount > 0) {
                actionError.value = "This category is already used by transactions or subscriptions."
                return@launch
            }

            isSaving.value = true
            actionError.value = null
            when (val result = deleteCategoryUseCase(category.id)) {
                is Result.Error -> actionError.value = result.message
                else -> actionError.value = null
            }
            isSaving.value = false
        }
    }

    fun setAiInsightsEnabled(enabled: Boolean) = updateLocalToggle { setAiInsightsEnabledUseCase(enabled) }
    fun setBudgetWarningsEnabled(enabled: Boolean) = updateLocalToggle { setBudgetWarningsEnabledUseCase(enabled) }
    fun setSubscriptionRemindersEnabled(enabled: Boolean) = updateLocalToggle { setSubscriptionRemindersEnabledUseCase(enabled) }
    fun setWeeklySummaryEnabled(enabled: Boolean) = updateLocalToggle { setWeeklySummaryEnabledUseCase(enabled) }

    fun clearError() {
        actionError.value = null
    }

    fun setLockOnLaunch(enabled: Boolean) {
        viewModelScope.launch {
            appLock.setLockOnLaunchEnabled(enabled)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appLock.setBiometricEnabled(enabled)
        }
    }

    fun setPin(pin: String) {
        viewModelScope.launch {
            when (val result = appLock.setPin(pin)) {
                is Result.Error -> actionError.value = result.message
                else -> actionError.value = null
            }
        }
    }

    fun clearPin() {
        viewModelScope.launch {
            appLock.clearPin()
        }
    }

    private fun setError(message: String): Boolean {
        actionError.value = message
        return false
    }

    private fun updateLocalToggle(action: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            when (val result = action()) {
                is Result.Error -> actionError.value = result.message
                else -> actionError.value = null
            }
        }
    }

    private fun parseAmountToCents(input: String): Int? {
        val sanitized = input.trim().replace(",", "")
        if (sanitized.isBlank()) return 0
        val amount = sanitized.toDoubleOrNull() ?: return null
        if (amount < 0.0) return null
        return (amount * 100).roundToInt()
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onSetLockOnLaunchEnabled: (Boolean) -> Unit = {},
    onSetBiometricEnabled: (Boolean) -> Unit = {},
    onSetPin: (String) -> Unit = {},
    onClearPin: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    var showRateSheet by rememberSaveable { mutableStateOf(false) }
    var showDefaultAccountSheet by rememberSaveable { mutableStateOf(false) }
    var showPinSheet by rememberSaveable { mutableStateOf(false) }
    var showAccountSheet by rememberSaveable { mutableStateOf(false) }
    var showCategorySheet by rememberSaveable { mutableStateOf(false) }
    var editingAccountId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var categoryDraftType by rememberSaveable { mutableStateOf<String?>(null) }
    val editingAccount = uiState.accounts.firstOrNull { it.id == editingAccountId }
    val editingCategory = uiState.categories.firstOrNull { it.id == editingCategoryId }
    val expenseCategories = uiState.categories.filter { it.type == TransactionType.EXPENSE }
    val incomeSources = uiState.categories.filter { it.type == TransactionType.INCOME }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.bgBase)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PageHeader(
            title = "Settings",
            supportingText = "Defaults, security, categories, and local finance preferences.",
            leading = {
                HeaderIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Go back",
                    onClick = onBack,
                )
            },
        )

        SectionLabel("Core")
        SettingCard(
            title = "Currency",
            valueText = buildAnnotatedString {
                append("1 USD = ")
                withStyle(SpanStyle(fontFamily = MonoFamily, fontFeatureSettings = "tnum")) {
                    append(String.format(Locale.US, "%.2f", uiState.settings.usdToEgpRate))
                }
                append(" EGP")
            },
            subtitle = "This rate powers account net worth, budget normalization, and transfer previews.",
            actionLabel = "Edit",
            onAction = {
                viewModel.clearError()
                showRateSheet = true
            },
        )
        SettingCard(
            title = "Default Account",
            value = uiState.defaultAccountName ?: "Not set",
            subtitle = "Used as the default quick-add and transfer starting point.",
            actionLabel = "Choose",
            onAction = {
                viewModel.clearError()
                showDefaultAccountSheet = true
            },
        )

        SectionLabel("Security")
        SecurityCard(
            unlockState = uiState.unlockState,
            onToggleLockOnLaunch = {
                viewModel.setLockOnLaunch(it)
                onSetLockOnLaunchEnabled(it)
            },
            onToggleBiometric = {
                viewModel.setBiometricEnabled(it)
                onSetBiometricEnabled(it)
            },
            onSetPin = { showPinSheet = true },
            onClearPin = {
                viewModel.clearPin()
                onClearPin()
            },
        )

        SectionLabel("Structure")
        SettingCard(
            title = "Accounts",
            value = "${uiState.accountCount}",
            subtitle = "Manage names, archive state, and clean up unused accounts.",
            actionLabel = "Add",
            onAction = {
                viewModel.clearError()
                editingAccountId = null
                showAccountSheet = true
            },
        )
        AccountsManagementCard(
            accounts = uiState.accounts,
            onEdit = {
                viewModel.clearError()
                editingAccountId = it
                showAccountSheet = true
            },
            onToggleHidden = { viewModel.toggleAccountHidden(it) },
            onDelete = { viewModel.deleteAccount(it) },
        )
        SettingCard(
            title = "Expense Categories",
            value = "${expenseCategories.size}",
            subtitle = "Add, edit, or remove spending categories and monthly budgets.",
            actionLabel = "Add",
            onAction = {
                viewModel.clearError()
                editingCategoryId = null
                categoryDraftType = TransactionType.EXPENSE.name
                showCategorySheet = true
            },
        )
        CategoriesManagementCard(
            title = "Expense Categories",
            categories = expenseCategories,
            emptyTitle = "No expense categories yet",
            emptySubtitle = "Add categories to organize spending and budgets.",
            onEdit = {
                viewModel.clearError()
                editingCategoryId = it
                categoryDraftType = null
                showCategorySheet = true
            },
            onDelete = { viewModel.deleteCategory(it) },
        )
        SettingCard(
            title = "Income Sources",
            value = "${incomeSources.size}",
            subtitle = "Track salary, freelance work, and other income sources separately.",
            actionLabel = "Add",
            onAction = {
                viewModel.clearError()
                editingCategoryId = null
                categoryDraftType = TransactionType.INCOME.name
                showCategorySheet = true
            },
        )
        CategoriesManagementCard(
            title = "Income Sources",
            categories = incomeSources,
            emptyTitle = "No income sources yet",
            emptySubtitle = "Add an income source to classify paychecks and deposits.",
            onEdit = {
                viewModel.clearError()
                editingCategoryId = it
                categoryDraftType = null
                showCategorySheet = true
            },
            onDelete = { viewModel.deleteCategory(it) },
        )

        SectionLabel("Preferences")
        PreferencesCard(
            title = "AI Assistant",
            rows = listOf(
                PreferenceRowState(
                    title = "AI insights",
                    subtitle = "Show monthly insight cards and unlock future assistant flows.",
                    checked = uiState.localPreferences.aiInsightsEnabled,
                    onCheckedChange = viewModel::setAiInsightsEnabled,
                ),
            ),
        )
        PreferencesCard(
            title = "Notifications",
            rows = listOf(
                PreferenceRowState(
                    title = "Budget warnings",
                    subtitle = "Warn when category spend reaches 80% and 100% of budget.",
                    checked = uiState.localPreferences.budgetWarningsEnabled,
                    onCheckedChange = viewModel::setBudgetWarningsEnabled,
                ),
                PreferenceRowState(
                    title = "Subscription reminders",
                    subtitle = "Prepare local reminders before recurring charges are due.",
                    checked = uiState.localPreferences.subscriptionRemindersEnabled,
                    onCheckedChange = viewModel::setSubscriptionRemindersEnabled,
                ),
                PreferenceRowState(
                    title = "Weekly summary",
                    subtitle = "Enable the future Sunday snapshot notification.",
                    checked = uiState.localPreferences.weeklySummaryEnabled,
                    onCheckedChange = viewModel::setWeeklySummaryEnabled,
                ),
            ),
        )

        SectionLabel("System")
        SettingCard(
            title = "Sync",
            value = "Worker scheduled",
            subtitle = "The offline queue is live. Remote sync will activate after Supabase credentials are configured.",
        )
        SettingCard(
            title = "Data",
            value = "Import / export pending",
            subtitle = "CSV export and import remain a separate milestone, but the settings surface is ready for them.",
        )
        SettingCard(
            title = "About",
            value = "Raqeem Android",
            subtitle = "Offline-first personal finance with Supabase auth, sync queue, budgets, goals, and shell sync status.",
        )
        SectionLabel("Account")
        SettingCard(
            title = "Account",
            value = uiState.settings.userId.ifBlank { "Signed in" },
            subtitle = "Sign out of the current Supabase session on this device.",
            actionLabel = "Sign out",
            onAction = onSignOut,
        )

        uiState.error?.let { error ->
            SurfaceCard(
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

    if (showRateSheet) {
        ExchangeRateBottomSheet(
            currentRate = uiState.settings.usdToEgpRate,
            isSaving = uiState.isSaving,
            errorMessage = uiState.error,
            onDismiss = { showRateSheet = false },
            onSave = { input ->
                viewModel.updateExchangeRate(input)
            },
        )
    }

    if (showDefaultAccountSheet) {
        DefaultAccountBottomSheet(
            accounts = uiState.accounts,
            currentDefaultAccountId = uiState.settings.defaultAccountId,
            isSaving = uiState.isSaving,
            errorMessage = uiState.error,
            onDismiss = { showDefaultAccountSheet = false },
            onSave = { accountId ->
                viewModel.updateDefaultAccount(accountId)
            },
        )
    }

    if (showPinSheet) {
        PinBottomSheet(
            onDismiss = { showPinSheet = false },
            onSave = { pin ->
                viewModel.setPin(pin)
                onSetPin(pin)
                showPinSheet = false
            },
        )
    }

    if (showAccountSheet) {
        AccountManagementBottomSheet(
            existingAccount = editingAccount,
            isSaving = uiState.isSaving,
            errorMessage = uiState.error,
            onDismiss = { showAccountSheet = false },
            onSave = { name, type, currency, openingBalance, isHidden ->
                viewModel.saveAccount(editingAccount, name, type, currency, openingBalance, isHidden)
            },
        )
    }

    if (showCategorySheet) {
        CategoryManagementBottomSheet(
            existingCategory = editingCategory,
            presetType = categoryDraftType?.let(TransactionType::valueOf),
            isSaving = uiState.isSaving,
            errorMessage = uiState.error,
            onDismiss = {
                showCategorySheet = false
                categoryDraftType = null
            },
            onSave = { name, type, icon, color, budget ->
                viewModel.saveCategory(editingCategory, name, type, icon, color, budget)
            },
        )
    }
}

@Composable
private fun SettingCard(
    title: String,
    value: String = "",
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    valueText: androidx.compose.ui.text.AnnotatedString? = null,
) {
    SurfaceCard(
        backgroundColor = AppColors.bgElevated,
        borderColor = AppColors.borderDefault,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title.uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelLarge,
                    color = AppColors.textMuted,
                )
                if (valueText != null) {
                    Text(
                        text = valueText,
                        style = MaterialTheme.typography.titleLarge,
                    )
                } else {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary,
                )
            }
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction) {
                    Text(
                        text = actionLabel,
                        color = AppColors.purple300,
                    )
                }
            }
        }
    }
}

@Composable
private fun SecurityCard(
    unlockState: UnlockState,
    onToggleLockOnLaunch: (Boolean) -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onSetPin: () -> Unit,
    onClearPin: () -> Unit,
) {
    SurfaceCard(
        backgroundColor = AppColors.bgElevated,
        borderColor = AppColors.borderDefault,
    ) {
        SecurityToggleRow(
            title = "Lock on launch",
            subtitle = "Require unlock after the app has been backgrounded for 5 minutes.",
            checked = unlockState.isLockOnLaunchEnabled,
            onCheckedChange = onToggleLockOnLaunch,
        )
        SecurityToggleRow(
            title = "Biometric unlock",
            subtitle = "Use the device biometric prompt as an unlock method.",
            checked = unlockState.isBiometricEnabled,
            onCheckedChange = onToggleBiometric,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "PIN",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = if (unlockState.hasPin) "Configured" else "Not set",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Set a 4 to 6 digit fallback PIN for the unlock gate.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary,
                )
            }
            TextButton(onClick = onSetPin) {
                Text(
                    text = if (unlockState.hasPin) "Replace" else "Set",
                    color = AppColors.purple300,
                )
            }
        }
        if (unlockState.hasPin) {
            TextButton(
                onClick = onClearPin,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Remove PIN", color = AppColors.negative)
            }
        }
    }
}

@Composable
private fun SecurityToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textSecondary,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

private data class PreferenceRowState(
    val title: String,
    val subtitle: String,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit,
)

@Composable
private fun PreferencesCard(
    title: String,
    rows: List<PreferenceRowState>,
) {
    SurfaceCard(
        backgroundColor = AppColors.bgElevated,
        borderColor = AppColors.borderDefault,
    ) {
        Text(
            text = title.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelLarge,
            color = AppColors.textMuted,
        )
        rows.forEach { row ->
            SecurityToggleRow(
                title = row.title,
                subtitle = row.subtitle,
                checked = row.checked,
                onCheckedChange = row.onCheckedChange,
            )
        }
    }
}

@Composable
private fun AccountsManagementCard(
    accounts: List<Account>,
    onEdit: (String) -> Unit,
    onToggleHidden: (Account) -> Unit,
    onDelete: (Account) -> Unit,
) {
    SurfaceCard(
        backgroundColor = AppColors.bgElevated,
        borderColor = AppColors.borderDefault,
    ) {
        if (accounts.isEmpty()) {
            EmptyState(
                title = "No accounts yet",
                subtitle = "Add an account to manage it from Settings.",
                icon = Icons.Rounded.Settings,
            )
        } else {
            accounts.forEach { account ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(account.name, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${account.type.name.lowercase(Locale.getDefault())} • ${account.currency.name}${if (account.isHidden) " • Hidden" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.textSecondary,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { onEdit(account.id) }) {
                            Text("Edit", color = AppColors.purple300)
                        }
                        TextButton(onClick = { onToggleHidden(account) }) {
                            Text(if (account.isHidden) "Restore" else "Archive", color = AppColors.purple300)
                        }
                        TextButton(onClick = { onDelete(account) }) {
                            Text("Delete", color = AppColors.negative)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoriesManagementCard(
    title: String,
    categories: List<Category>,
    emptyTitle: String,
    emptySubtitle: String,
    onEdit: (String) -> Unit,
    onDelete: (Category) -> Unit,
) {
    SurfaceCard(
        backgroundColor = AppColors.bgElevated,
        borderColor = AppColors.borderDefault,
    ) {
        if (categories.isEmpty()) {
            EmptyState(
                title = emptyTitle,
                subtitle = emptySubtitle,
                icon = Icons.Rounded.Settings,
            )
        } else {
            Text(
                text = title.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelLarge,
                color = AppColors.textMuted,
            )
            categories.forEach { category ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(category.name, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = buildAnnotatedString {
                                append("${category.type.name.lowercase(Locale.getDefault())} • ${category.icon} • ")
                                if (category.budgetCents != null) {
                                    withStyle(SpanStyle(fontFamily = MonoFamily, fontFeatureSettings = "tnum")) {
                                        append("%.2f".format(Locale.US, category.budgetCents.toDouble() / 100.0))
                                    }
                                } else {
                                    append(if (category.type == TransactionType.INCOME) "No default amount" else "No budget")
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.textSecondary,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { onEdit(category.id) }) {
                            Text("Edit", color = AppColors.purple300)
                        }
                        TextButton(onClick = { onDelete(category) }) {
                            Text("Delete", color = AppColors.negative)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PinBottomSheet(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var pinInput by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppColors.bgElevated,
        contentColor = AppColors.textPrimary,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Set PIN",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Create a 4 to 6 digit fallback PIN for the local unlock gate.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.textSecondary,
            )
            OutlinedTextField(
                value = pinInput,
                onValueChange = { pinInput = it.filter(Char::isDigit).take(6) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("PIN") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.borderAccent,
                    unfocusedBorderColor = AppColors.borderSubtle,
                    focusedContainerColor = AppColors.bgSubtle,
                    unfocusedContainerColor = AppColors.bgSubtle,
                ),
            )
            Button(
                onClick = { onSave(pinInput) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.purple500,
                    contentColor = Color.White,
                ),
            ) {
                Text("Save PIN")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExchangeRateBottomSheet(
    currentRate: Double,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: suspend (String) -> Boolean,
) {
    val scope = rememberCoroutineScope()
    var rateInput by rememberSaveable(currentRate) {
        mutableStateOf(String.format(Locale.US, "%.2f", currentRate))
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Exchange Rate",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Set the current USD to EGP rate used across analytics and transfers.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.textSecondary,
            )

            errorMessage?.let { message ->
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
                value = rateInput,
                onValueChange = { rateInput = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("1 USD = X EGP") },
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
                        if (onSave(rateInput)) {
                            onDismiss()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.purple500,
                    contentColor = Color.White,
                ),
            ) {
                Text(if (isSaving) "Saving..." else "Save Rate")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountManagementBottomSheet(
    existingAccount: Account?,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: suspend (String, AccountType, Currency, String, Boolean) -> Boolean,
) {
    val scope = rememberCoroutineScope()
    var nameInput by rememberSaveable(existingAccount?.id) { mutableStateOf(existingAccount?.name.orEmpty()) }
    var openingBalanceInput by rememberSaveable(existingAccount?.id) {
        mutableStateOf(existingAccount?.let { "%.2f".format(Locale.US, it.initialAmountCents / 100.0) }.orEmpty())
    }
    var selectedType by rememberSaveable(existingAccount?.id) { mutableStateOf((existingAccount?.type ?: AccountType.CHECKING).name) }
    var selectedCurrency by rememberSaveable(existingAccount?.id) { mutableStateOf((existingAccount?.currency ?: Currency.USD).name) }
    var isHidden by rememberSaveable(existingAccount?.id) { mutableStateOf(existingAccount?.isHidden ?: false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppColors.bgElevated,
        contentColor = AppColors.textPrimary,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(if (existingAccount == null) "Add Account" else "Edit Account", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            errorMessage?.let {
                SurfaceCard(backgroundColor = AppColors.negativeBg, borderColor = AppColors.borderNegative) {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = AppColors.negative)
                }
            }
            OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Account Name") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.borderAccent, unfocusedBorderColor = AppColors.borderSubtle, focusedContainerColor = AppColors.bgSubtle, unfocusedContainerColor = AppColors.bgSubtle))
            EnumSelectionField("Type", AccountType.valueOf(selectedType).name.lowercase(Locale.getDefault()).replaceFirstChar { it.uppercase() }, AccountType.entries.map { it.name to it.name.lowercase(Locale.getDefault()).replaceFirstChar { c -> c.uppercase() } }) { selectedType = it }
            EnumSelectionField("Currency", selectedCurrency, Currency.entries.map { it.name to it.name }) { selectedCurrency = it }
            OutlinedTextField(value = openingBalanceInput, onValueChange = { openingBalanceInput = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Opening Balance") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.borderAccent, unfocusedBorderColor = AppColors.borderSubtle, focusedContainerColor = AppColors.bgSubtle, unfocusedContainerColor = AppColors.bgSubtle))
            SecurityToggleRow("Hide account", "Hidden accounts stay out of most pickers while keeping history intact.", isHidden) { isHidden = it }
            Button(
                onClick = { scope.launch { if (onSave(nameInput, AccountType.valueOf(selectedType), Currency.valueOf(selectedCurrency), openingBalanceInput, isHidden)) onDismiss() } },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.purple500, contentColor = Color.White),
            ) { Text(if (isSaving) "Saving..." else "Save Account") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryManagementBottomSheet(
    existingCategory: Category?,
    presetType: TransactionType?,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: suspend (String, TransactionType, String, String, String) -> Boolean,
) {
    val scope = rememberCoroutineScope()
    var nameInput by rememberSaveable(existingCategory?.id) { mutableStateOf(existingCategory?.name.orEmpty()) }
    var selectedType by rememberSaveable(existingCategory?.id, presetType) { mutableStateOf((existingCategory?.type ?: presetType ?: TransactionType.EXPENSE).name) }
    var iconInput by rememberSaveable(existingCategory?.id) { mutableStateOf(existingCategory?.icon.orEmpty()) }
    var colorInput by rememberSaveable(existingCategory?.id) { mutableStateOf(existingCategory?.color.orEmpty()) }
    var budgetInput by rememberSaveable(existingCategory?.id) {
        mutableStateOf(existingCategory?.budgetCents?.let { "%.2f".format(Locale.US, it / 100.0) }.orEmpty())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppColors.bgElevated,
        contentColor = AppColors.textPrimary,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                if (existingCategory == null) {
                    if (presetType == TransactionType.INCOME) "Add Income Source" else "Add Category"
                } else {
                    "Edit Category"
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            errorMessage?.let {
                SurfaceCard(backgroundColor = AppColors.negativeBg, borderColor = AppColors.borderNegative) {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = AppColors.negative)
                }
            }
            OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Category Name") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.borderAccent, unfocusedBorderColor = AppColors.borderSubtle, focusedContainerColor = AppColors.bgSubtle, unfocusedContainerColor = AppColors.bgSubtle))
            if (presetType == null) {
                EnumSelectionField("Type", selectedType.lowercase(Locale.getDefault()).replaceFirstChar { it.uppercase() }, TransactionType.entries.map { it.name to it.name.lowercase(Locale.getDefault()).replaceFirstChar { c -> c.uppercase() } }) { selectedType = it }
            } else {
                OutlinedTextField(
                    value = presetType.name.lowercase(Locale.getDefault()).replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    label = { Text("Type") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.borderAccent,
                        unfocusedBorderColor = AppColors.borderSubtle,
                        focusedContainerColor = AppColors.bgSubtle,
                        unfocusedContainerColor = AppColors.bgSubtle,
                    ),
                )
            }
            OutlinedTextField(value = iconInput, onValueChange = { iconInput = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Icon key") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.borderAccent, unfocusedBorderColor = AppColors.borderSubtle, focusedContainerColor = AppColors.bgSubtle, unfocusedContainerColor = AppColors.bgSubtle))
            OutlinedTextField(value = colorInput, onValueChange = { colorInput = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Color hex") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.borderAccent, unfocusedBorderColor = AppColors.borderSubtle, focusedContainerColor = AppColors.bgSubtle, unfocusedContainerColor = AppColors.bgSubtle))
            if ((presetType ?: TransactionType.valueOf(selectedType)) == TransactionType.EXPENSE) {
                OutlinedTextField(value = budgetInput, onValueChange = { budgetInput = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Budget (optional)") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.borderAccent, unfocusedBorderColor = AppColors.borderSubtle, focusedContainerColor = AppColors.bgSubtle, unfocusedContainerColor = AppColors.bgSubtle))
            }
            Button(
                onClick = {
                    scope.launch {
                        val type = presetType ?: TransactionType.valueOf(selectedType)
                        val budget = if (type == TransactionType.EXPENSE) budgetInput else ""
                        if (onSave(nameInput, type, iconInput, colorInput, budget)) onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.purple500, contentColor = Color.White),
            ) { Text(if (isSaving) "Saving..." else if ((presetType ?: TransactionType.valueOf(selectedType)) == TransactionType.INCOME) "Save Income Source" else "Save Category") }
        }
    }
}

@Composable
private fun EnumSelectionField(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultAccountBottomSheet(
    accounts: List<Account>,
    currentDefaultAccountId: String?,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: suspend (String?) -> Boolean,
) {
    val scope = rememberCoroutineScope()
    var selectedAccountId by rememberSaveable(currentDefaultAccountId) {
        mutableStateOf(currentDefaultAccountId ?: "")
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Default Account",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Choose the account that should be preselected in quick add and transfers.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.textSecondary,
            )

            errorMessage?.let { message ->
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

            if (accounts.isEmpty()) {
                SurfaceCard {
                    EmptyState(
                        title = "No accounts available",
                        subtitle = "Create an account first, then come back here to set a default.",
                        icon = Icons.Rounded.Settings,
                    )
                }
            } else {
                DefaultAccountOption(
                    label = "No default account",
                    subtitle = "Leave selection manual in quick add.",
                    selected = selectedAccountId.isBlank(),
                    onSelect = { selectedAccountId = "" },
                )
                accounts.forEach { account ->
                    DefaultAccountOption(
                        label = account.name,
                        subtitle = "${account.type.name.lowercase(Locale.getDefault())} • ${account.currency.name}",
                        selected = selectedAccountId == account.id,
                        onSelect = { selectedAccountId = account.id },
                    )
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        if (onSave(selectedAccountId.ifBlank { null })) {
                            onDismiss()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.purple500,
                    contentColor = Color.White,
                ),
            ) {
                Text(if (isSaving) "Saving..." else "Save Default")
            }
        }
    }
}

@Composable
private fun DefaultAccountOption(
    label: String,
    subtitle: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onSelect)
            .background(AppColors.bgSurface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textSecondary,
            )
        }
    }
}
