package com.raqeem.app.presentation.ui.screens.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.raqeem.app.domain.model.Transaction
import com.raqeem.app.domain.usecase.DeleteTransactionUseCase
import com.raqeem.app.domain.usecase.GetAccountsUseCase
import com.raqeem.app.domain.usecase.GetCategoriesUseCase
import com.raqeem.app.domain.usecase.GetTransactionUseCase
import com.raqeem.app.presentation.ui.components.AmountText
import com.raqeem.app.presentation.ui.components.EmptyState
import com.raqeem.app.presentation.ui.components.SurfaceCard
import com.raqeem.app.presentation.ui.theme.AppColors
import com.raqeem.app.presentation.ui.theme.AppTypography
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

data class TransactionDetailUiState(
    val isLoading: Boolean = true,
    val isDeleting: Boolean = false,
    val transaction: Transaction? = null,
    val error: String? = null,
)

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val getTransaction: GetTransactionUseCase,
    getAccounts: GetAccountsUseCase,
    getCategories: GetCategoriesUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
) : ViewModel() {

    private val isDeleting = MutableStateFlow(false)
    private val actionError = MutableStateFlow<String?>(null)
    private val accounts = getAccounts()
    private val categories = getCategories()

    fun uiState(transactionId: String): Flow<TransactionDetailUiState> {
        return combine(
            getTransaction(transactionId),
            accounts,
            categories,
            isDeleting,
            actionError,
        ) { transaction, accounts, categories, deleting, error ->
            val accountsById = accounts.associateBy { it.id }
            val categoriesById = categories.associateBy { it.id }
            val enrichedTransaction = transaction?.copy(
                account = transaction.account ?: accountsById[transaction.accountId],
                category = transaction.categoryId?.let(categoriesById::get),
            )

            TransactionDetailUiState(
                isLoading = false,
                isDeleting = deleting,
                transaction = enrichedTransaction,
                error = error,
            )
        }
            .catch { throwable ->
                emit(
                    TransactionDetailUiState(
                        isLoading = false,
                        error = throwable.message ?: "Unable to load transaction details.",
                    ),
                )
            }
    }

    suspend fun delete(id: String): Boolean {
        isDeleting.value = true
        actionError.value = null
        val result = deleteTransaction(id)
        isDeleting.value = false

        return when (result) {
            is com.raqeem.app.domain.model.Result.Success -> true
            is com.raqeem.app.domain.model.Result.Error -> {
                actionError.value = result.message
                false
            }
            com.raqeem.app.domain.model.Result.Loading -> false
        }
    }

    fun clearError() {
        actionError.value = null
    }
}

@Composable
fun TransactionDetailScreen(
    transactionId: String,
    onBack: () -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel(),
) {
    val stateFlow = remember(transactionId) { viewModel.uiState(transactionId) }
    val uiState = stateFlow.collectAsStateWithLifecycle(
        initialValue = TransactionDetailUiState(),
    ).value
    val scope = rememberCoroutineScope()
    var showEditSheet by rememberSaveable(transactionId) { mutableStateOf(false) }
    var showDeleteConfirmation by rememberSaveable(transactionId) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.bgBase)
            .verticalScroll(rememberScrollState())
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
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Go back",
                        tint = AppColors.textSecondary,
                    )
                }
                Text(
                    text = "Transaction",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Row {
                IconButton(
                    onClick = {
                        viewModel.clearError()
                        showEditSheet = true
                    },
                    enabled = uiState.transaction != null,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit transaction",
                        tint = AppColors.textPrimary,
                    )
                }
                IconButton(
                    onClick = { showDeleteConfirmation = true },
                    enabled = uiState.transaction != null && !uiState.isDeleting,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete transaction",
                        tint = AppColors.negative,
                    )
                }
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

            uiState.transaction == null -> {
                SurfaceCard {
                    EmptyState(
                        title = "Transaction not found",
                        subtitle = "This entry may have been removed from the local ledger.",
                        icon = Icons.Rounded.Payments,
                    )
                }
            }

            else -> {
                TransactionDetailContent(transaction = uiState.transaction)
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

    if (showEditSheet) {
        TransactionEditorBottomSheet(
            mode = TransactionEditorMode.Edit,
            transactionId = transactionId,
            onDismiss = { showEditSheet = false },
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        val transaction = uiState.transaction ?: return@TextButton
                        scope.launch {
                            if (viewModel.delete(transaction.id)) {
                                withContext(Dispatchers.Main.immediate) {
                                    onBack()
                                }
                            }
                        }
                    },
                ) {
                    Text(if (uiState.isDeleting) "Deleting..." else "Delete")
                }
            },
            title = { Text("Delete transaction?") },
            text = { Text("This will remove the transaction from your visible ledger and recalculate balances.") },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun TransactionDetailContent(
    transaction: Transaction,
) {
    SurfaceCard {
        Text(
            text = transactionTitle(transaction),
            style = MaterialTheme.typography.titleLarge,
        )
        AmountText(
            amountCents = transaction.amountCents,
            currency = transaction.currency,
            transactionType = transaction.type,
            style = AppTypography.heroAmount,
        )
        Text(
            text = transaction.category?.name ?: transaction.type.name.lowercase(Locale.getDefault()).replaceFirstChar { char ->
                char.titlecase(Locale.getDefault())
            },
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.textSecondary,
        )
    }

    DetailFieldCard(label = "Account", value = transaction.account?.name ?: transaction.accountId)
    DetailFieldCard(label = "Category", value = transaction.category?.name ?: "Uncategorized")
    DetailFieldCard(label = "Date", value = transaction.date.toString())
    DetailFieldCard(label = "Currency", value = transaction.currency.name)
    DetailFieldCard(label = "Receipt", value = if (transaction.receiptUrl != null) "Attached" else "Not attached")
    DetailFieldCard(label = "Note", value = transaction.note?.takeIf { it.isNotBlank() } ?: "No note")
}

@Composable
private fun DetailFieldCard(
    label: String,
    value: String,
) {
    SurfaceCard {
        Text(
            text = label.uppercase(Locale.getDefault()),
            style = AppTypography.sectionLabel,
            color = AppColors.textMuted,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

private fun transactionTitle(transaction: Transaction): String {
    return transaction.note?.takeIf { it.isNotBlank() }
        ?: transaction.category?.name
        ?: transaction.type.name.lowercase(Locale.getDefault()).replaceFirstChar { char ->
            char.titlecase(Locale.getDefault())
        }
}
