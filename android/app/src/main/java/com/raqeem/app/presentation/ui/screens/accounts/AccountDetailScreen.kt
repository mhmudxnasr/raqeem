package com.raqeem.app.presentation.ui.screens.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.raqeem.app.domain.model.Account
import com.raqeem.app.domain.model.LedgerEntry
import com.raqeem.app.domain.model.TransactionType
import com.raqeem.app.domain.usecase.GetAccountUseCase
import com.raqeem.app.domain.usecase.GetLedgerEntriesForAccountUseCase
import com.raqeem.app.presentation.ui.components.AmountText
import com.raqeem.app.presentation.ui.components.EmptyState
import com.raqeem.app.presentation.ui.components.LedgerEntryCard
import com.raqeem.app.presentation.ui.components.MiniSparkline
import com.raqeem.app.presentation.ui.components.SectionLabel
import com.raqeem.app.presentation.ui.components.SurfaceCard
import com.raqeem.app.presentation.ui.components.groupLedgerEntries
import com.raqeem.app.presentation.ui.theme.AppColors
import com.raqeem.app.presentation.ui.theme.AppTypography
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import java.util.Locale
import javax.inject.Inject

data class AccountDetailUiState(
    val isLoading: Boolean = true,
    val account: Account? = null,
    val monthlyIncomeCents: Int = 0,
    val monthlyExpenseCents: Int = 0,
    val ledgerEntries: List<LedgerEntry> = emptyList(),
    val sparklinePoints: List<Int> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class AccountDetailViewModel @Inject constructor(
    private val getAccount: GetAccountUseCase,
    private val getLedgerEntriesForAccount: GetLedgerEntriesForAccountUseCase,
) : ViewModel() {

    fun uiState(accountId: String) = combine(
        getAccount(accountId),
        getLedgerEntriesForAccount(accountId),
    ) { account, ledgerEntries ->
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val currentMonthEntries = ledgerEntries.filter { entry ->
            entry.date.year == today.year && entry.date.monthNumber == today.monthNumber
        }
        AccountDetailUiState(
            isLoading = false,
            account = account,
            monthlyIncomeCents = currentMonthEntries.sumOf { entry ->
                when (entry) {
                    is LedgerEntry.TransactionEntry -> {
                        if (entry.transaction.type.name == "INCOME") entry.transaction.amountCents else 0
                    }
                    is LedgerEntry.TransferEntry -> {
                        if (entry.transfer.toAccountId == accountId) entry.transfer.toAmountCents else 0
                    }
                }
            },
            monthlyExpenseCents = currentMonthEntries.sumOf { entry ->
                when (entry) {
                    is LedgerEntry.TransactionEntry -> {
                        if (entry.transaction.type.name == "EXPENSE") entry.transaction.amountCents else 0
                    }
                    is LedgerEntry.TransferEntry -> {
                        if (entry.transfer.fromAccountId == accountId) entry.transfer.fromAmountCents else 0
                    }
                }
            },
            ledgerEntries = ledgerEntries,
            sparklinePoints = buildSparklinePoints(accountId, ledgerEntries),
        )
    }
        .catch { throwable ->
            emit(
                AccountDetailUiState(
                    isLoading = false,
                    error = throwable.message ?: "Unable to load account details.",
                ),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AccountDetailUiState(),
        )

    private fun buildSparklinePoints(
        accountId: String,
        entries: List<LedgerEntry>,
    ): List<Int> {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val range = (0..29).map { offset -> today.minus(kotlinx.datetime.DatePeriod(days = 29 - offset)) }
        return range.map { date ->
            entries.filter { it.date == date }.sumOf { entry ->
                when (entry) {
                    is LedgerEntry.TransactionEntry -> {
                        if (entry.transaction.type.name == "INCOME") {
                            entry.transaction.amountCents
                        } else {
                            -entry.transaction.amountCents
                        }
                    }
                    is LedgerEntry.TransferEntry -> when (accountId) {
                        entry.transfer.fromAccountId -> -entry.transfer.fromAmountCents
                        entry.transfer.toAccountId -> entry.transfer.toAmountCents
                        else -> 0
                    }
                }
            }
        }
    }
}

@Composable
fun AccountDetailScreen(
    accountId: String,
    onBack: () -> Unit,
    onOpenTransaction: (String) -> Unit,
    viewModel: AccountDetailViewModel = hiltViewModel(),
) {
    val stateFlow = remember(accountId) { viewModel.uiState(accountId) }
    val uiState = stateFlow.collectAsStateWithLifecycle(initialValue = AccountDetailUiState()).value
    val sections = remember(uiState.ledgerEntries) { groupLedgerEntries(uiState.ledgerEntries) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.bgBase)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Go back",
                tint = AppColors.textSecondary,
            )
        }

        when {
            uiState.isLoading -> {
                SurfaceCard {
                    Text("Loading account...", color = AppColors.textSecondary)
                }
            }
            uiState.account == null -> {
                SurfaceCard {
                    EmptyState(
                        title = "Account not found",
                        subtitle = "This account may have been deleted or hidden from the active ledger.",
                        icon = Icons.Rounded.AccountBalanceWallet,
                    )
                }
            }
            else -> {
                val account = uiState.account
                SurfaceCard {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    AmountText(
                        amountCents = account.balanceCents,
                        currency = account.currency,
                        style = AppTypography.heroAmount,
                    )
                    Text(
                        text = "${account.type.name.lowercase().replaceFirstChar { char -> char.titlecase(Locale.getDefault()) }} • ${account.currency.name}",
                        color = AppColors.textSecondary,
                    )
                }

                SurfaceCard {
                    SectionLabel("This Month")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Income",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.textMuted,
                        )
                        AmountText(
                            amountCents = uiState.monthlyIncomeCents,
                            currency = account.currency,
                            transactionType = TransactionType.INCOME,
                            style = AppTypography.largeAmount,
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Expenses",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.textMuted,
                        )
                        AmountText(
                            amountCents = uiState.monthlyExpenseCents,
                            currency = account.currency,
                            transactionType = TransactionType.EXPENSE,
                            style = AppTypography.largeAmount,
                        )
                    }
                }

                SurfaceCard {
                    SectionLabel("30d Flow")
                    MiniSparkline(points = uiState.sparklinePoints)
                }

                SectionLabel("Activity")
                if (sections.isEmpty()) {
                    SurfaceCard {
                        EmptyState(
                            title = "No activity yet",
                            subtitle = "Transactions and transfers for this account will appear here.",
                            icon = Icons.Rounded.AccountBalanceWallet,
                        )
                    }
                } else {
                    for (section in sections) {
                        SectionLabel(section.label)
                        for (entry in section.entries) {
                            LedgerEntryCard(
                                entry = entry,
                                onOpenTransaction = onOpenTransaction,
                            )
                        }
                    }
                }
            }
        }

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
}
