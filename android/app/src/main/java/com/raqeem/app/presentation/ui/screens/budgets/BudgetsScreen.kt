package com.raqeem.app.presentation.ui.screens.budgets

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PieChart
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
import com.raqeem.app.domain.model.Account
import com.raqeem.app.domain.model.BillingCycle
import com.raqeem.app.domain.model.Category
import com.raqeem.app.domain.model.Currency
import com.raqeem.app.domain.model.Result
import com.raqeem.app.domain.model.Subscription
import com.raqeem.app.domain.model.TransactionType
import com.raqeem.app.domain.usecase.AddSubscriptionUseCase
import com.raqeem.app.domain.usecase.BudgetItem
import com.raqeem.app.domain.usecase.DeleteSubscriptionUseCase
import com.raqeem.app.domain.usecase.GetAccountsUseCase
import com.raqeem.app.domain.usecase.GetAllSubscriptionsUseCase
import com.raqeem.app.domain.usecase.GetBudgetsUseCase
import com.raqeem.app.domain.usecase.GetCategoriesByTypeUseCase
import com.raqeem.app.domain.usecase.GetSettingsUseCase
import com.raqeem.app.domain.usecase.UpdateCategoryUseCase
import com.raqeem.app.domain.usecase.UpdateSubscriptionUseCase
import com.raqeem.app.presentation.ui.components.BudgetBar
import com.raqeem.app.presentation.ui.components.EmptyState
import com.raqeem.app.presentation.ui.components.HeaderIconButton
import com.raqeem.app.presentation.ui.components.MonthSelector
import com.raqeem.app.presentation.ui.components.PageHeader
import com.raqeem.app.presentation.ui.components.PickerOption
import com.raqeem.app.presentation.ui.components.SectionLabel
import com.raqeem.app.presentation.ui.components.SheetPickerField
import com.raqeem.app.presentation.ui.components.SurfaceCard
import com.raqeem.app.presentation.ui.theme.AppColors
import com.raqeem.app.presentation.ui.theme.MonoFamily
import com.raqeem.app.util.budgetColor
import com.raqeem.app.util.formatAmount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

private data class BudgetsData(
    val subscriptions: List<Subscription>,
    val categories: List<Category>,
    val accounts: List<Account>,
    val userId: String,
)

data class BudgetsUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val userId: String = LOCAL_USER_ID,
    val selectedMonth: YearMonth = YearMonth.now(),
    val monthLabel: String = formatMonthLabel(YearMonth.now()),
    val budgetItems: List<BudgetItem> = emptyList(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val subscriptions: List<Subscription> = emptyList(),
    val totalBudgetCents: Int = 0,
    val totalSpentCents: Int = 0,
    val error: String? = null,
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class BudgetsViewModel @Inject constructor(
    getBudgets: GetBudgetsUseCase,
    getAllSubscriptions: GetAllSubscriptionsUseCase,
    getCategoriesByType: GetCategoriesByTypeUseCase,
    getAccounts: GetAccountsUseCase,
    getSettings: GetSettingsUseCase,
    private val updateCategory: UpdateCategoryUseCase,
    private val addSubscription: AddSubscriptionUseCase,
    private val updateSubscription: UpdateSubscriptionUseCase,
    private val deleteSubscription: DeleteSubscriptionUseCase,
) : ViewModel() {
    private val selectedMonth = MutableStateFlow(YearMonth.now())
    private val isSaving = MutableStateFlow(false)
    private val actionError = MutableStateFlow<String?>(null)
    private val monthlyBudgets = selectedMonth.flatMapLatest {
        val range = monthRange(it)
        getBudgets(range.first, range.second)
    }
    private val baseData = combine(
        getAllSubscriptions(),
        getCategoriesByType(TransactionType.EXPENSE),
        getAccounts(),
        getSettings(),
    ) { subscriptions, categories, accounts, settings ->
        BudgetsData(
            subscriptions = subscriptions.sortedWith(compareBy<Subscription>({ !it.isActive }, { it.nextBillingDate.toString() }, { it.name.lowercase(Locale.getDefault()) })),
            categories = categories.sortedBy { it.name.lowercase(Locale.getDefault()) },
            accounts = accounts.sortedWith(compareBy<Account>({ it.isHidden }, { it.sortOrder }, { it.name.lowercase(Locale.getDefault()) })),
            userId = settings.userId.ifBlank { LOCAL_USER_ID },
        )
    }

    val uiState = combine(selectedMonth, monthlyBudgets, baseData, isSaving, actionError) { month, budgetItems, data, saving, error ->
        BudgetsUiState(
            isLoading = false,
            isSaving = saving,
            userId = data.userId,
            selectedMonth = month,
            monthLabel = formatMonthLabel(month),
            budgetItems = budgetItems,
            categories = data.categories,
            accounts = data.accounts,
            subscriptions = data.subscriptions,
            totalBudgetCents = budgetItems.sumOf { it.budgetCents },
            totalSpentCents = budgetItems.sumOf { it.spentCents },
            error = error,
        )
    }.catch {
        emit(BudgetsUiState(isLoading = false, error = it.message ?: "Unable to load budgets."))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetsUiState())

    fun clearError() { actionError.value = null }
    fun showPreviousMonth() { selectedMonth.value = selectedMonth.value.minusMonths(1) }
    fun showNextMonth() { selectedMonth.value = selectedMonth.value.plusMonths(1) }

    suspend fun saveBudget(category: Category, budgetInput: String): Boolean {
        val raw = budgetInput.trim().replace(",", "")
        val budgetCents = when {
            raw.isBlank() -> null
            else -> raw.toDoubleOrNull()?.takeIf { it >= 0.0 }?.let { (it * 100).roundToInt() }
                ?: return setError("Enter a valid budget amount, or leave it blank to clear the budget.")
        }
        isSaving.value = true
        actionError.value = null
        val result = updateCategory(category.copy(budgetCents = budgetCents, updatedAt = Clock.System.now()))
        isSaving.value = false
        return when (result) {
            is Result.Success -> true
            is Result.Error -> setError(result.message)
            Result.Loading -> false
        }
    }

    suspend fun saveSubscription(
        existing: Subscription?,
        name: String,
        amountInput: String,
        accountId: String,
        categoryId: String?,
        billingCycle: BillingCycle,
        nextBillingDateInput: String,
        isActive: Boolean,
        autoLog: Boolean,
    ): Boolean {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return setError("Subscription name is required.")
        val amountCents = parsePositiveAmountToCents(amountInput) ?: return setError("Enter a valid amount greater than 0.")
        val nextBillingDate = runCatching { LocalDate.parse(nextBillingDateInput.trim()) }.getOrNull()
            ?: return setError("Next billing date must use YYYY-MM-DD.")
        val account = uiState.value.accounts.firstOrNull { it.id == accountId }
            ?: return setError("Choose an account for this subscription.")
        isSaving.value = true
        actionError.value = null
        val now = Clock.System.now()
        val sub = (existing ?: Subscription(
            id = UUID.randomUUID().toString(),
            userId = uiState.value.userId,
            accountId = account.id,
            name = trimmedName,
            amountCents = amountCents,
            currency = account.currency,
            billingCycle = billingCycle,
            nextBillingDate = nextBillingDate,
            isActive = isActive,
            autoLog = autoLog,
            createdAt = now,
            updatedAt = now,
        )).copy(
            userId = uiState.value.userId,
            accountId = account.id,
            categoryId = categoryId,
            name = trimmedName,
            amountCents = amountCents,
            currency = account.currency,
            billingCycle = billingCycle,
            nextBillingDate = nextBillingDate,
            isActive = isActive,
            autoLog = autoLog,
            updatedAt = now,
        )
        val result = if (existing == null) addSubscription(sub) else updateSubscription(sub)
        isSaving.value = false
        return when (result) {
            is Result.Success -> true
            is Result.Error -> setError(result.message)
            Result.Loading -> false
        }
    }

    fun removeSubscription(id: String) {
        viewModelScope.launch {
            isSaving.value = true
            actionError.value = null
            when (val result = deleteSubscription(id)) {
                is Result.Error -> actionError.value = result.message
                else -> actionError.value = null
            }
            isSaving.value = false
        }
    }

    private fun setError(message: String): Boolean { actionError.value = message; return false }
    private fun parsePositiveAmountToCents(input: String): Int? {
        val amount = input.trim().replace(",", "").toDoubleOrNull() ?: return null
        if (amount <= 0.0) return null
        return (amount * 100).roundToInt()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BudgetsScreen(onOpenSettings: () -> Unit, viewModel: BudgetsViewModel = hiltViewModel()) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val scope = rememberCoroutineScope()
    var editingCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingSubscriptionId by rememberSaveable { mutableStateOf<String?>(null) }
    var showSubscriptionEditor by rememberSaveable { mutableStateOf(false) }
    val itemByCategory = uiState.budgetItems.associateBy { it.category.id }
    val accountById = uiState.accounts.associateBy { it.id }
    val categoryById = uiState.categories.associateBy { it.id }
    val editingCategory = uiState.categories.firstOrNull { it.id == editingCategoryId }
    val editingSubscription = uiState.subscriptions.firstOrNull { it.id == editingSubscriptionId }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.bgBase),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item("budgets_header", contentType = "header") {
            PageHeader(
                title = "Budgets",
                supportingText = "Category limits and subscriptions for the current month.",
                trailing = {
                    HeaderIconButton(
                        icon = Icons.Rounded.Add,
                        contentDescription = "Add subscription",
                        onClick = {
                            viewModel.clearError()
                            editingSubscriptionId = null
                            showSubscriptionEditor = true
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
 
        item("month_selector", contentType = "selector") {
            MonthSelector(uiState.monthLabel, viewModel::showPreviousMonth, viewModel::showNextMonth)
        }

        item("total_budget_card", contentType = "summary") {
            OverallBudgetCard(
                totalSpentCents = uiState.totalSpentCents,
                totalBudgetCents = uiState.totalBudgetCents
            )
        }

        item("category_header", contentType = "section_label") {
            SectionHeader("Category Budgets")
        }

        if (uiState.categories.isEmpty() && !uiState.isLoading) {
            item("categories_empty", contentType = "empty") {
                SurfaceCard {
                    EmptyState("No expense categories", "Create categories in Settings first, then set monthly budgets here.", Icons.Rounded.PieChart)
                }
            }
        } else {
            items(
                uiState.categories,
                key = { it.id },
                contentType = { "budget_category" }
            ) { category ->
                BudgetCategoryCard(
                    category = category,
                    item = itemByCategory[category.id],
                    modifier = Modifier.animateItemPlacement(),
                ) {
                    viewModel.clearError()
                    editingCategoryId = category.id
                }
            }
        }
 
        item("subscriptions_header", contentType = "section_label") {
            SectionHeader("Subscriptions")
        }

        if (uiState.subscriptions.isEmpty() && !uiState.isLoading) {
            item("subscriptions_empty", contentType = "empty") {
                SurfaceCard {
                    EmptyState(
                        title = "No subscriptions yet",
                        subtitle = "Track recurring charges with account, category, cycle, and next billing date.",
                        icon = Icons.Rounded.Payments,
                        actionLabel = "Add subscription",
                        onAction = {
                            viewModel.clearError()
                            editingSubscriptionId = null
                            showSubscriptionEditor = true
                        },
                    )
                }
            }
        } else {
            items(
                uiState.subscriptions,
                key = { it.id },
                contentType = { "subscription" }
            ) { subscription ->
                SubscriptionCard(
                    subscription = subscription,
                    accountName = accountById[subscription.accountId]?.name ?: "Unknown account",
                    categoryName = subscription.categoryId?.let(categoryById::get)?.name,
                    modifier = Modifier.animateItemPlacement(),
                    onEdit = {
                        viewModel.clearError()
                        editingSubscriptionId = subscription.id
                        showSubscriptionEditor = true
                    },
                    onDelete = { viewModel.removeSubscription(subscription.id) },
                )
            }
        }
 
        uiState.error?.let {
            item("budgets_error", contentType = "error") {
                SurfaceCard(
                    borderColor = AppColors.borderNegative,
                    backgroundColor = AppColors.negativeBg
                ) {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = AppColors.negative)
                }
            }
        }
    }

    if (editingCategory != null) {
        BudgetEditorBottomSheet(editingCategory, uiState.isSaving, uiState.error, { editingCategoryId = null }) { input ->
            scope.launch { if (viewModel.saveBudget(editingCategory, input)) editingCategoryId = null }
        }
    }
    if (showSubscriptionEditor) {
        SubscriptionEditorBottomSheet(
            existingSubscription = editingSubscription,
            accounts = uiState.accounts,
            categories = uiState.categories,
            isSaving = uiState.isSaving,
            errorMessage = uiState.error,
            onDismiss = { showSubscriptionEditor = false },
            onSave = { name, amount, accountId, categoryId, cycle, nextDate, isActive, autoLog ->
                scope.launch {
                    if (viewModel.saveSubscription(editingSubscription, name, amount, accountId, categoryId, cycle, nextDate, isActive, autoLog)) {
                        showSubscriptionEditor = false
                    }
                }
            },
        )
    }
}

@Composable
private fun OverallBudgetCard(totalSpentCents: Int, totalBudgetCents: Int, modifier: Modifier = Modifier) {
    val percentage = if (totalBudgetCents > 0) ((totalSpentCents.toLong() * 100) / totalBudgetCents).toInt() else 0
    val tone = budgetTone(percentage)
    SurfaceCard(
        backgroundColor = AppColors.bgElevated,
        borderColor = AppColors.borderAccent.copy(alpha = 0.35f),
        modifier = modifier,
    ) {
        SectionLabel("Monthly Snapshot")
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontFamily = MonoFamily, fontFeatureSettings = "tnum")) { append(totalSpentCents.formatAmount(Currency.USD)) }
                append(" of ")
                withStyle(SpanStyle(fontFamily = MonoFamily, fontFeatureSettings = "tnum")) { append(totalBudgetCents.formatAmount(Currency.USD)) }
            },
            style = MaterialTheme.typography.titleLarge
        )
        BudgetBar(
            progress = if (totalBudgetCents > 0) totalSpentCents.toFloat() / totalBudgetCents.toFloat() else 0f,
            color = tone.second,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(tone.first, style = MaterialTheme.typography.bodyMedium, color = tone.second)
            Text("$percentage%", style = MaterialTheme.typography.bodySmall, color = AppColors.textSecondary)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    SectionLabel(title)
}

@Composable
private fun BudgetCategoryCard(category: Category, item: BudgetItem?, modifier: Modifier = Modifier, onEdit: () -> Unit) {
    val budgetCents = item?.budgetCents ?: category.budgetCents
    val spentCents = item?.spentCents ?: 0
    val percentage = item?.percentage ?: 0
    val remainingCents = (budgetCents ?: 0) - spentCents
    val tone = budgetTone(percentage).second
    SurfaceCard(modifier = modifier.clickable(onClick = onEdit)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(category.name, style = MaterialTheme.typography.titleMedium)
                if (budgetCents != null) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontFamily = MonoFamily, fontFeatureSettings = "tnum")) { append(spentCents.formatAmount(Currency.USD)) }
                            append(" spent of ")
                            withStyle(SpanStyle(fontFamily = MonoFamily, fontFeatureSettings = "tnum")) { append(budgetCents.formatAmount(Currency.USD)) }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.textSecondary,
                    )
                } else {
                    Text(
                        text = "No monthly budget set yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.textSecondary,
                    )
                }
            }
            TextButton(onClick = onEdit) { Text(if (budgetCents == null) "Set" else "Edit", color = AppColors.purple300) }
        }
        if (budgetCents != null && budgetCents > 0) {
            BudgetBar(progress = spentCents.toFloat() / budgetCents.toFloat(), color = budgetColor(percentage))
            Text(
                text = when {
                    percentage >= 100 -> buildAnnotatedString {
                        append("Overspent by ")
                        withStyle(SpanStyle(fontFamily = MonoFamily, fontFeatureSettings = "tnum")) { append(abs(remainingCents).formatAmount(Currency.USD)) }
                    }
                    percentage >= 80 -> buildAnnotatedString {
                        withStyle(SpanStyle(fontFamily = MonoFamily, fontFeatureSettings = "tnum")) { append(remainingCents.coerceAtLeast(0).formatAmount(Currency.USD)) }
                        append(" left before warning turns red")
                    }
                    else -> buildAnnotatedString {
                        withStyle(SpanStyle(fontFamily = MonoFamily, fontFeatureSettings = "tnum")) { append(remainingCents.formatAmount(Currency.USD)) }
                        append(" remaining")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = tone,
            )
        } else {
            Text("Tap to assign a recurring budget for this category.", style = MaterialTheme.typography.bodySmall, color = AppColors.textMuted)
        }
    }
}

@Composable
private fun SubscriptionCard(
    subscription: Subscription,
    accountName: String,
    categoryName: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SurfaceCard(modifier = modifier.clickable(onClick = onEdit)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(subscription.name, style = MaterialTheme.typography.titleMedium)
                    StatusBadge(if (subscription.isActive) "Active" else "Paused", if (subscription.isActive) AppColors.positive else AppColors.textSecondary)
                }
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontFamily = MonoFamily, fontFeatureSettings = "tnum")) {
                            append(subscription.amountCents.formatAmount(subscription.currency))
                        }
                        val rest = buildList {
                            add(subscription.billingCycle.displayName())
                            add(accountName)
                            categoryName?.let(::add)
                        }.joinToString(" • ")
                        append(" • $rest")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.textSecondary,
                )
                Text(
                    "Next billing ${formatMonthDay(subscription.nextBillingDate)}${if (subscription.autoLog) " • Auto-log on" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textMuted,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onEdit) { Text("Edit", color = AppColors.purple300) }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete subscription", tint = AppColors.negative)
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(label: String, color: Color) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(color.copy(alpha = 0.14f)).padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(label.uppercase(Locale.getDefault()), style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetEditorBottomSheet(
    category: Category,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var budgetInput by rememberSaveable(category.id) { mutableStateOf(category.budgetCents?.let { "%.2f".format(Locale.US, it / 100.0) }.orEmpty()) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppColors.bgElevated,
        contentColor = AppColors.textPrimary,
        tonalElevation = 0.dp,
        dragHandle = {
            Box(modifier = Modifier.padding(top = 12.dp).size(36.dp, 4.dp).clip(RoundedCornerShape(99.dp)).background(AppColors.borderStrong))
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Edit Budget", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text("Set a monthly limit for ${category.name}. Leave it blank to remove the budget.", style = MaterialTheme.typography.bodyMedium, color = AppColors.textSecondary)
            errorMessage?.let {
                SurfaceCard(backgroundColor = AppColors.negativeBg, borderColor = AppColors.borderNegative) {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = AppColors.negative)
                }
            }
            OutlinedTextField(value = budgetInput, onValueChange = { budgetInput = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Monthly budget (USD)") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.borderAccent, unfocusedBorderColor = AppColors.borderSubtle, focusedContainerColor = AppColors.bgSubtle, unfocusedContainerColor = AppColors.bgSubtle))
            Button(
                onClick = { onSave(budgetInput) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.purple500, contentColor = Color.White),
            ) { Text(if (isSaving) "Saving..." else "Save Budget") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubscriptionEditorBottomSheet(
    existingSubscription: Subscription?,
    accounts: List<Account>,
    categories: List<Category>,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String?, BillingCycle, String, Boolean, Boolean) -> Unit,
) {
    val selectableAccounts = remember(accounts, existingSubscription?.accountId) {
        buildList<Account> {
            addAll(accounts.filterNot { it.isHidden })
            val current = accounts.firstOrNull { it.id == existingSubscription?.accountId }
            if (current != null && none { it.id == current.id }) add(current)
        }.distinctBy { it.id }
    }
    var nameInput by rememberSaveable(existingSubscription?.id) { mutableStateOf(existingSubscription?.name.orEmpty()) }
    var amountInput by rememberSaveable(existingSubscription?.id) { mutableStateOf(existingSubscription?.let { "%.2f".format(Locale.US, it.amountCents / 100.0) }.orEmpty()) }
    var selectedAccountId by rememberSaveable(existingSubscription?.id) { mutableStateOf(existingSubscription?.accountId ?: selectableAccounts.firstOrNull()?.id.orEmpty()) }
    var selectedCategoryId by rememberSaveable(existingSubscription?.id) { mutableStateOf(existingSubscription?.categoryId.orEmpty()) }
    var selectedBillingCycle by rememberSaveable(existingSubscription?.id) { mutableStateOf((existingSubscription?.billingCycle ?: BillingCycle.MONTHLY).name) }
    var nextBillingDateInput by rememberSaveable(existingSubscription?.id) { mutableStateOf(existingSubscription?.nextBillingDate?.toString().orEmpty()) }
    var isActive by rememberSaveable(existingSubscription?.id) { mutableStateOf(existingSubscription?.isActive ?: true) }
    var autoLog by rememberSaveable(existingSubscription?.id) { mutableStateOf(existingSubscription?.autoLog ?: false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppColors.bgElevated,
        contentColor = AppColors.textPrimary,
        tonalElevation = 0.dp,
        dragHandle = {
            Box(modifier = Modifier.padding(top = 12.dp).size(36.dp, 4.dp).clip(RoundedCornerShape(99.dp)).background(AppColors.borderStrong))
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(if (existingSubscription == null) "New Subscription" else "Edit Subscription", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text("Recurring charges are tied to an account, optional category, and the next date they will hit.", style = MaterialTheme.typography.bodyMedium, color = AppColors.textSecondary)
            errorMessage?.let {
                SurfaceCard(backgroundColor = AppColors.negativeBg, borderColor = AppColors.borderNegative) {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = AppColors.negative)
                }
            }
            if (selectableAccounts.isEmpty()) {
                SurfaceCard {
                    EmptyState("No accounts available", "Create an account first, then come back to add subscriptions.", Icons.Rounded.Payments)
                }
            } else {
                OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Name") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.borderAccent, unfocusedBorderColor = AppColors.borderSubtle, focusedContainerColor = AppColors.bgSubtle, unfocusedContainerColor = AppColors.bgSubtle))
                OutlinedTextField(value = amountInput, onValueChange = { amountInput = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Amount") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.borderAccent, unfocusedBorderColor = AppColors.borderSubtle, focusedContainerColor = AppColors.bgSubtle, unfocusedContainerColor = AppColors.bgSubtle))
                BudgetSelectionField("Account", selectableAccounts.firstOrNull { it.id == selectedAccountId }?.let(::formatAccountOption) ?: "Choose account", selectableAccounts.map { it.id to formatAccountOption(it) }) { selectedAccountId = it }
                BudgetSelectionField("Category", categories.firstOrNull { it.id == selectedCategoryId }?.name ?: "No category", listOf("" to "No category") + categories.map { it.id to it.name }) { selectedCategoryId = it }
                BudgetSelectionField("Billing cycle", BillingCycle.valueOf(selectedBillingCycle).displayName(), BillingCycle.entries.map { it.name to it.displayName() }) { selectedBillingCycle = it }
                OutlinedTextField(value = nextBillingDateInput, onValueChange = { nextBillingDateInput = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Next billing date (YYYY-MM-DD)") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.borderAccent, unfocusedBorderColor = AppColors.borderSubtle, focusedContainerColor = AppColors.bgSubtle, unfocusedContainerColor = AppColors.bgSubtle))
                ToggleRow("Active", "Inactive subscriptions stay in history but stop showing as upcoming.", isActive) { isActive = it }
                ToggleRow("Auto-log", "Keep this on if recurring charges should be converted into transactions later.", autoLog) { autoLog = it }
                Button(
                    onClick = { onSave(nameInput, amountInput, selectedAccountId, selectedCategoryId.ifBlank { null }, BillingCycle.valueOf(selectedBillingCycle), nextBillingDateInput, isActive, autoLog) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.purple500, contentColor = Color.White),
                ) { Text(if (isSaving) "Saving..." else "Save Subscription") }
            }
        }
    }
}

@Composable
private fun BudgetSelectionField(label: String, selectedText: String, options: List<Pair<String, String>>, onSelect: (String) -> Unit) {
    SheetPickerField(
        label = label,
        selectedText = selectedText,
        options = options.map { PickerOption(it.first, it.second) },
        onSelect = onSelect,
    )
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AppColors.textSecondary)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun budgetTone(percentage: Int): Pair<String, Color> = when {
    percentage >= 100 -> "Over budget" to AppColors.negative
    percentage >= 80 -> "Warning zone" to AppColors.warning
    else -> "On track" to AppColors.positive
}

private fun monthRange(month: YearMonth) = LocalDate(month.year, month.monthValue, 1) to LocalDate(month.year, month.monthValue, month.lengthOfMonth())
private fun formatMonthLabel(month: YearMonth): String = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))
private fun formatMonthDay(date: LocalDate): String = java.time.LocalDate.of(date.year, date.monthNumber, date.dayOfMonth).format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
private fun BillingCycle.displayName(): String = name.lowercase(Locale.getDefault()).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
private fun formatAccountOption(account: Account): String = "${account.name} • ${account.currency.name}${if (account.isHidden) " • Hidden" else ""}"
