package com.raqeem.app.presentation.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.raqeem.app.domain.model.SyncStatus
import com.raqeem.app.domain.model.Transaction
import com.raqeem.app.domain.model.TransactionType
import com.raqeem.app.domain.usecase.GetAccountsUseCase
import com.raqeem.app.domain.usecase.GetCategoriesUseCase
import com.raqeem.app.domain.usecase.GetRecentTransactionsUseCase
import com.raqeem.app.presentation.ui.components.AmountText
import com.raqeem.app.presentation.ui.components.HeaderIconButton
import com.raqeem.app.presentation.ui.components.PageHeader
import com.raqeem.app.presentation.ui.components.SectionLabel
import com.raqeem.app.presentation.ui.components.SkeletonBlock
import com.raqeem.app.presentation.ui.components.SurfaceCard
import com.raqeem.app.presentation.ui.theme.AppColors
import com.raqeem.app.presentation.ui.theme.AppTypography
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate as KotlinLocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val transactions: List<Transaction> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    getRecentTransactions: GetRecentTransactionsUseCase,
    getAccounts: GetAccountsUseCase,
    getCategories: GetCategoriesUseCase,
) : ViewModel() {

    val uiState = combine(
        getRecentTransactions(limit = 20),
        getAccounts(),
        getCategories(),
    ) { transactions, accounts, categories ->
        val accountsById = accounts.associateBy { it.id }
        val categoriesById = categories.associateBy { it.id }

        val enrichedTransactions = transactions.map { transaction ->
            transaction.copy(
                account = accountsById[transaction.accountId],
                category = transaction.categoryId?.let(categoriesById::get),
            )
        }

        HomeUiState(
            isLoading = false,
            transactions = enrichedTransactions,
        )
    }
        .catch { throwable ->
            emit(
                HomeUiState(
                    isLoading = false,
                    error = throwable.message ?: "Unable to load recent transactions.",
                ),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    syncStatus: SyncStatus,
    onSyncStatusClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onViewAllTransactions: () -> Unit,
    onOpenTransaction: (String) -> Unit,
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val groupedTransactions = remember(uiState.transactions) {
        groupTransactions(uiState.transactions)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_transactions_list")
            .background(AppColors.bgBase),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 20.dp,
            bottom = 120.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item(key = "header", contentType = "header") {
            HomeHeader(
                syncStatus = syncStatus,
                onSyncStatusClick = onSyncStatusClick,
                onOpenSettings = onOpenSettings,
            )
            Spacer(Modifier.height(20.dp))
        }

        item(key = "quick_add", contentType = "quick_add") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                QuickAddCard(
                    modifier = Modifier.weight(1f),
                    label = "Expense",
                    type = TransactionType.EXPENSE,
                    onClick = onAddExpense,
                )
                QuickAddCard(
                    modifier = Modifier.weight(1f),
                    label = "Income",
                    type = TransactionType.INCOME,
                    onClick = onAddIncome,
                )
            }
            Spacer(Modifier.height(28.dp))
        }

        when {
            uiState.isLoading -> {
                item(key = "loading_today", contentType = "section_label") { SectionLabel("Today") }
                items(3, key = { "loading_$it" }, contentType = { "loading_card" }) {
                    LoadingTransactionCard()
                }
            }

            uiState.transactions.isEmpty() -> {
                item(key = "empty_state", contentType = "empty") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp, bottom = 0.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = AppColors.textMuted,
                        )
                        Text(
                            text = "No transactions yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.textSecondary,
                        )
                        Text(
                            text = "Tap + to add your first one",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.textMuted,
                        )
                    }
                }
            }

            else -> {
                groupedTransactions.forEach { (label, transactions) ->
                    item(key = "section_$label", contentType = "section_label") {
                        SectionLabel(label, modifier = Modifier.animateItemPlacement().padding(top = 20.dp, bottom = 8.dp))
                    }
                    items(
                        transactions,
                        key = { transaction -> transaction.id },
                        contentType = { "transaction" }
                    ) { transaction ->
                        TransactionCard(
                            transaction = transaction,
                            onClick = { onOpenTransaction(transaction.id) },
                            modifier = Modifier.animateItemPlacement()
                        )
                    }
                }

                item(key = "view_all", contentType = "view_all") {
                    Row(
                        modifier = Modifier.fillMaxWidth().animateItemPlacement(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(
                            onClick = onViewAllTransactions,
                            modifier = Modifier.testTag("home_view_all_transactions_button"),
                        ) {
                            Text(
                                text = "See all transactions",
                                color = AppColors.purple300,
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = AppColors.purple300,
                            )
                        }
                    }
                }
            }
        }

        uiState.error?.let { error ->
            item(key = "error", contentType = "error") {
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
}

@Composable
private fun HomeHeader(
    syncStatus: SyncStatus,
    onSyncStatusClick: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.US)
    val greeting = when (LocalTime.now().hour) {
        in 0..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }

    PageHeader(
        title = greeting,
        modifier = modifier,
        supportingText = now.format(formatter),
        trailing = {
            SyncStatusDot(
                syncStatus = syncStatus,
                onClick = onSyncStatusClick,
            )
            HeaderIconButton(
                icon = Icons.Rounded.Settings,
                contentDescription = "Open settings",
                onClick = onOpenSettings,
                tint = AppColors.textMuted,
            )
        },
    )
}

@Composable
private fun SyncStatusDot(
    syncStatus: SyncStatus,
    onClick: () -> Unit,
) {
    val color = when (syncStatus) {
        is SyncStatus.Synced -> if (syncStatus.failedCount > 0) AppColors.negative else AppColors.positive
        is SyncStatus.Syncing -> AppColors.warning
        is SyncStatus.Offline -> AppColors.textMuted
        is SyncStatus.Failed -> AppColors.negative
        is SyncStatus.Idle -> return
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.bgSurface)
            .border(1.dp, AppColors.borderSubtle, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true, radius = 18.dp),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
    }
}

@Composable
private fun QuickAddCard(
    label: String,
    type: TransactionType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor = if (type == TransactionType.EXPENSE) AppColors.negativeBg else AppColors.positiveBg
    val borderColor = if (type == TransactionType.EXPENSE) AppColors.borderNegative else AppColors.borderPositive
    val accentColor = if (type == TransactionType.EXPENSE) AppColors.negative else AppColors.positive
    val icon = if (type == TransactionType.EXPENSE) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward
    val shape = RoundedCornerShape(14.dp)

    Row(
        modifier = modifier
            .height(72.dp)
            .clip(shape)
            .background(bgColor)
            .border(1.dp, borderColor, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(color = accentColor.copy(alpha = 0.15f)),
                onClick = onClick,
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = "+ $label",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = AppColors.textPrimary,
        )
    }
}


@Composable
private fun TransactionCard(
    transaction: Transaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = transaction.note?.takeIf { it.isNotBlank() }
                    ?: transaction.category?.name
                    ?: transaction.type.name.lowercase().replaceFirstChar { char ->
                        char.titlecase(Locale.getDefault())
                    },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = transaction.account?.name ?: formatCompactDate(transaction.date),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textSecondary,
            )
        }
        AmountText(
            amountCents = transaction.amountCents,
            currency = transaction.currency,
            transactionType = transaction.type,
            style = AppTypography.largeAmount,
        )
    }
}

@Composable
private fun LoadingTransactionCard() {
    SurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(16.dp),
            )
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(12.dp),
            )
        }
    }
}

private fun groupTransactions(transactions: List<Transaction>): List<Pair<String, List<Transaction>>> {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val yesterday = today.minus(DatePeriod(days = 1))

    return transactions
        .groupBy { transaction ->
            when (transaction.date) {
                today -> "Today"
                yesterday -> "Yesterday"
                else -> formatSectionDate(transaction.date)
            }
        }
        .toList()
}

private fun formatSectionDate(date: KotlinLocalDate): String {
    val javaDate = LocalDate.of(date.year, date.monthNumber, date.dayOfMonth)
    return javaDate.format(DateTimeFormatter.ofPattern("MMMM d", Locale.US))
}

private fun formatCompactDate(date: KotlinLocalDate): String {
    val javaDate = LocalDate.of(date.year, date.monthNumber, date.dayOfMonth)
    return javaDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
}
