package com.raqeem.app.presentation.ui.screens.transaction

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.raqeem.app.domain.model.LedgerEntry
import com.raqeem.app.domain.model.Result
import com.raqeem.app.domain.usecase.DeleteTransactionUseCase
import com.raqeem.app.domain.usecase.GetLedgerEntriesUseCase
import com.raqeem.app.presentation.ui.components.EmptyState
import com.raqeem.app.presentation.ui.components.LedgerEntryCard
import com.raqeem.app.presentation.ui.components.SectionLabel
import com.raqeem.app.presentation.ui.components.SkeletonBlock
import com.raqeem.app.presentation.ui.components.SurfaceCard
import com.raqeem.app.presentation.ui.components.groupLedgerEntries
import com.raqeem.app.presentation.ui.theme.AppColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class TransactionHistoryUiState(
    val isLoading: Boolean = true,
    val isDeleting: Boolean = false,
    val query: String = "",
    val entries: List<LedgerEntry> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class TransactionHistoryViewModel @Inject constructor(
    getLedgerEntries: GetLedgerEntriesUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val isDeleting = MutableStateFlow(false)
    private val actionError = MutableStateFlow<String?>(null)

    val uiState = combine(
        query.debounce(300).distinctUntilChanged(),
        getLedgerEntries(),
        isDeleting,
        actionError,
    ) { currentQuery, ledgerEntries, deleting, error ->
        val filtered = filterLedgerEntries(ledgerEntries, currentQuery)
        TransactionHistoryUiState(
            isLoading = false,
            isDeleting = deleting,
            query = currentQuery,
            entries = filtered,
            error = error,
        )
    }
        .catch { throwable ->
            emit(
                TransactionHistoryUiState(
                    isLoading = false,
                    error = throwable.message ?: "Unable to load ledger history.",
                ),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TransactionHistoryUiState(),
        )

    fun onQueryChange(value: String) {
        query.update { value }
    }

    suspend fun delete(id: String): Boolean {
        isDeleting.value = true
        actionError.value = null
        val result = deleteTransaction(id)
        isDeleting.value = false
        return when (result) {
            is Result.Success -> true
            is Result.Error -> {
                actionError.value = result.message
                false
            }
            Result.Loading -> false
        }
    }

    fun clearError() {
        actionError.value = null
    }

    private fun filterLedgerEntries(
        ledgerEntries: List<LedgerEntry>,
        query: String,
    ): List<LedgerEntry> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return ledgerEntries
        val normalized = trimmed.lowercase(Locale.getDefault())
        return ledgerEntries.filter { entry ->
            when (entry) {
                is LedgerEntry.TransactionEntry -> {
                    listOfNotNull(
                        entry.transaction.note,
                        entry.transaction.account?.name,
                        entry.transaction.category?.name,
                        (entry.transaction.amountCents / 100.0).toString(),
                    ).any { it.lowercase(Locale.getDefault()).contains(normalized) }
                }
                is LedgerEntry.TransferEntry -> {
                    listOfNotNull(
                        entry.transfer.note,
                        entry.transfer.fromAccount?.name,
                        entry.transfer.toAccount?.name,
                        entry.transfer.goal?.name,
                        (entry.transfer.fromAmountCents / 100.0).toString(),
                    ).any { it.lowercase(Locale.getDefault()).contains(normalized) }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionsScreen(
    onBack: () -> Unit,
    onOpenTransaction: (String) -> Unit,
    viewModel: TransactionHistoryViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val sections = remember(uiState.entries) { groupLedgerEntries(uiState.entries) }
    val scope = rememberCoroutineScope()
    var editingTransactionId by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingTransactionId by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.bgBase),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 20.dp,
            bottom = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "transactions_header", contentType = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TransactionsHeader(onBack = onBack)
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search note, account, category, transfer, or amount") },
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

        when {
            uiState.isLoading -> {
                items(5, key = { "loading_$it" }, contentType = { "loading" }) {
                    LoadingLedgerRow(modifier = Modifier.animateItemPlacement())
                }
            }

            sections.isEmpty() -> {
                item(key = "transactions_empty", contentType = "empty") {
                    SurfaceCard(modifier = Modifier.animateItemPlacement()) {
                        EmptyState(
                            title = if (uiState.query.isBlank()) "No ledger activity yet" else "No matching activity",
                            subtitle = if (uiState.query.isBlank()) {
                                "Transactions and transfers will both appear here once they are saved."
                            } else {
                                "Try a note keyword, account name, category name, or exact amount like 12.50."
                            },
                            icon = Icons.Rounded.Payments,
                        )
                    }
                }
            }

            else -> {
                sections.forEach { section ->
                    item(key = "section_${section.label}", contentType = "section_label") {
                        SectionLabel(
                            text = section.label,
                            modifier = Modifier.animateItemPlacement(),
                        )
                    }
                    items(
                        section.entries,
                        key = { entry -> "${entry::class.simpleName}:${entry.id}" },
                        contentType = { "ledger_card" }
                    ) { entry ->
                        LedgerEntryCard(
                            modifier = Modifier.animateItemPlacement(),
                            entry = entry,
                            onOpenTransaction = onOpenTransaction,
                            onEditTransaction = { transactionId ->
                                viewModel.clearError()
                                editingTransactionId = transactionId
                            },
                            onDeleteTransaction = { transactionId ->
                                viewModel.clearError()
                                deletingTransactionId = transactionId
                            },
                        )
                    }
                }
            }
        }

        uiState.error?.let { error ->
            item(key = "transactions_error", contentType = "error") {
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

    editingTransactionId?.let { transactionId ->
        TransactionEditorBottomSheet(
            mode = TransactionEditorMode.Edit,
            transactionId = transactionId,
            onDismiss = { editingTransactionId = null },
        )
    }

    deletingTransactionId?.let { transactionId ->
        AlertDialog(
            onDismissRequest = { deletingTransactionId = null },
            title = { Text("Delete transaction?") },
            text = { Text("This removes the transaction from the ledger and updates balances across synced clients.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            if (viewModel.delete(transactionId)) {
                                deletingTransactionId = null
                            }
                        }
                    },
                ) {
                    Text(if (uiState.isDeleting) "Deleting..." else "Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { deletingTransactionId = null },
                    enabled = !uiState.isDeleting,
                ) {
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
private fun TransactionsHeader(
    onBack: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.padding(start = 0.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Go back",
                tint = AppColors.textSecondary,
            )
        }
        Text(
            text = "Activity",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Browse, search, and manage transactions and transfers in one ledger view.",
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.textSecondary,
        )
    }
}

@Composable
private fun LoadingLedgerRow(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(16.dp),
            )
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(12.dp),
            )
        }
    }
}
