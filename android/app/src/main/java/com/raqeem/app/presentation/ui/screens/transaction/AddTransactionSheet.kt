package com.raqeem.app.presentation.ui.screens.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raqeem.app.domain.model.Account
import com.raqeem.app.domain.model.Category
import com.raqeem.app.domain.model.Currency
import com.raqeem.app.domain.model.TransactionType
import com.raqeem.app.presentation.ui.components.PickerOption
import com.raqeem.app.presentation.ui.components.PrimaryButton
import com.raqeem.app.presentation.ui.components.SurfaceCard
import com.raqeem.app.presentation.ui.theme.AppColors
import com.raqeem.app.presentation.ui.theme.MonoFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import java.time.Instant
import java.time.ZoneId

data class AddTransactionUiState(
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val type: TransactionType = TransactionType.EXPENSE,
    val amountRaw: String = "",
    val note: String = "",
    val selectedAccount: Account? = null,
    val selectedCategory: Category? = null,
    val date: LocalDate = defaultAddTransactionDate(),
    val formattedDate: String = "Today",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isSubmitEnabled: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    initialType: TransactionType = TransactionType.EXPENSE,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    viewModel: TransactionEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.addUiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    LaunchedEffect(initialType) {
        viewModel.startAddSession(initialType)
        delay(100)
        focusRequester.requestFocus()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.bgElevated,
        contentColor = AppColors.textPrimary,
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AppColors.borderDefault),
            )
        },
        windowInsets = WindowInsets(0, 0, 0, 0),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .imePadding()
                .navigationBarsPadding(),
        ) {
            AddTransactionContent(
                state = state,
                focusRequester = focusRequester,
                onTypeChange = viewModel::setType,
                onAmountChange = viewModel::setAmount,
                onAccountChange = viewModel::setAccount,
                onCategoryChange = viewModel::setCategory,
                onDateChange = viewModel::setDate,
                onNoteChange = viewModel::setNote,
                onDismiss = onDismiss,
                onSave = {
                    val saved = viewModel.submitAddTransaction()
                    if (saved) {
                        onSaved()
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionContent(
    state: AddTransactionUiState,
    focusRequester: androidx.compose.ui.focus.FocusRequester,
    onTypeChange: (TransactionType) -> Unit,
    onAmountChange: (String) -> Unit,
    onAccountChange: (Account) -> Unit,
    onCategoryChange: (Category) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onNoteChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: suspend () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var showAccountPicker by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val amountColor = if (state.type == TransactionType.EXPENSE) AppColors.negative else AppColors.positive
    val signLabel = if (state.type == TransactionType.EXPENSE) "−" else "+"
    val currencyLabel = when (state.selectedAccount?.currency ?: Currency.USD) {
        Currency.USD -> "$"
        Currency.EGP -> "EGP"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .weight(1f, fill = true)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TypeSelectorChip(
                    type = state.type,
                    onTypeChange = onTypeChange,
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = AppColors.textMuted,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            state.errorMessage?.let { message ->
                SurfaceCard(
                    backgroundColor = AppColors.negativeBg,
                    borderColor = AppColors.borderNegative,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.negative,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicTextField(
                    value = state.amountRaw,
                    onValueChange = onAmountChange,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 112.dp)
                        .testTag("transaction_editor_amount_input")
                        .focusRequester(focusRequester),
                    textStyle = TextStyle(
                        fontFamily = MonoFamily,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Medium,
                        color = amountColor,
                        textAlign = TextAlign.Center,
                        fontFeatureSettings = "tnum",
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { scope.launch { onSave() } },
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(amountColor),
                    decorationBox = { innerTextField ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = signLabel,
                                    style = TextStyle(
                                        fontFamily = MonoFamily,
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = amountColor,
                                    ),
                                )
                                Text(
                                    text = currencyLabel,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = AppColors.textMuted,
                                    ),
                                )
                                Box(contentAlignment = Alignment.Center) {
                                    if (state.amountRaw.isEmpty()) {
                                        Text(
                                            text = "0.00",
                                            style = TextStyle(
                                                fontFamily = MonoFamily,
                                                fontSize = 44.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = AppColors.textMuted,
                                                textAlign = TextAlign.Center,
                                                fontFeatureSettings = "tnum",
                                            ),
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        }
                    },
                )
            }

            HorizontalDivider(
                color = AppColors.borderSubtle,
                thickness = 0.5.dp,
            )
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SelectorChip(
                    icon = Icons.Rounded.AccountBalanceWallet,
                    label = state.selectedAccount?.name ?: "Select account",
                    onClick = { showAccountPicker = true },
                )
                SelectorChip(
                    icon = Icons.Rounded.Label,
                    label = state.selectedCategory?.name ?: "Select category",
                    onClick = { showCategoryPicker = true },
                )
                SelectorChip(
                    icon = Icons.Rounded.CalendarToday,
                    label = state.formattedDate,
                    onClick = { showDatePicker = true },
                    modifier = Modifier.testTag("transaction_editor_date_input"),
                )
                NoteField(
                    value = state.note,
                    onValueChange = onNoteChange,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        HorizontalDivider(
            color = AppColors.borderSubtle,
            thickness = 0.5.dp,
        )
        PrimaryButton(
            text = if (state.type == TransactionType.EXPENSE) "Add Expense" else "Add Income",
            onClick = { scope.launch { onSave() } },
            enabled = state.isSubmitEnabled,
            isLoading = state.isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 16.dp)
                .testTag("transaction_editor_save_button"),
        )
    }

    if (showAccountPicker) {
        SelectionSheet(
            title = "Select account",
            options = state.accounts.map { account ->
                PickerOption(
                    value = account.id,
                    label = account.name,
                    supportingText = "${account.type.name.lowercase().replaceFirstChar { it.uppercase() }} • ${account.currency.name}",
                )
            },
            selectedValue = state.selectedAccount?.id,
            onDismiss = { showAccountPicker = false },
            onSelect = { selectedId ->
                state.accounts.firstOrNull { it.id == selectedId }?.let(onAccountChange)
                showAccountPicker = false
            },
        )
    }

    if (showCategoryPicker) {
        SelectionSheet(
            title = "Select category",
            options = state.categories.map { category ->
                PickerOption(
                    value = category.id,
                    label = category.name,
                    supportingText = category.icon.ifBlank { null },
                )
            },
            selectedValue = state.selectedCategory?.id,
            onDismiss = { showCategoryPicker = false },
            onSelect = { selectedId ->
                state.categories.firstOrNull { it.id == selectedId }?.let(onCategoryChange)
                showCategoryPicker = false
            },
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date.toEpochMillisAtStartOfDay(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedMillis ->
                            onDateChange(selectedMillis.toLocalDate())
                        }
                        showDatePicker = false
                    },
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun TypeSelectorChip(
    type: TransactionType,
    onTypeChange: (TransactionType) -> Unit,
) {
    val isExpense = type == TransactionType.EXPENSE
    val backgroundColor = if (isExpense) AppColors.negativeBg else AppColors.positiveBg
    val borderColor = if (isExpense) AppColors.borderNegative else AppColors.borderPositive
    val textColor = if (isExpense) AppColors.negative else AppColors.positive
    val label = if (isExpense) "Expense" else "Income"
    val icon = if (isExpense) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable {
                onTypeChange(
                    if (isExpense) {
                        TransactionType.INCOME
                    } else {
                        TransactionType.EXPENSE
                    },
                )
            }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(color = textColor),
        )
        Icon(
            imageVector = Icons.Rounded.KeyboardArrowDown,
            contentDescription = null,
            tint = textColor.copy(alpha = 0.6f),
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
fun SelectorChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(AppColors.bgSubtle)
            .border(1.dp, AppColors.borderSubtle, RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppColors.textMuted,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(color = AppColors.textSecondary),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = AppColors.textMuted,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
fun NoteField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AppColors.bgSubtle)
            .border(1.dp, AppColors.borderSubtle, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp)
            .testTag("transaction_editor_note_input"),
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = AppColors.textPrimary),
        keyboardOptions = KeyboardOptions(
            imeAction = androidx.compose.ui.text.input.ImeAction.Default,
        ),
        maxLines = 2,
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Notes,
                    contentDescription = null,
                    tint = AppColors.textMuted,
                    modifier = Modifier.size(16.dp),
                )
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = "Add note...",
                            style = MaterialTheme.typography.bodyMedium.copy(color = AppColors.textMuted),
                        )
                    }
                    innerTextField()
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionSheet(
    title: String,
    options: List<PickerOption>,
    selectedValue: String?,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppColors.bgElevated,
        contentColor = AppColors.textPrimary,
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AppColors.borderDefault),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
            options.forEach { option ->
                SurfaceCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(option.value) },
                    backgroundColor = if (option.value == selectedValue) {
                        AppColors.purple500.copy(alpha = 0.12f)
                    } else {
                        AppColors.bgSubtle
                    },
                    borderColor = if (option.value == selectedValue) {
                        AppColors.borderAccent
                    } else {
                        AppColors.borderSubtle
                    },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
                ) {
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    option.supportingText?.let { supportingText ->
                        Text(
                            text = supportingText,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.textSecondary,
                        )
                    }
                }
            }
        }
    }
}

private fun LocalDate.toEpochMillisAtStartOfDay(): Long {
    return java.time.LocalDate.of(year, monthNumber, dayOfMonth)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}

private fun Long.toLocalDate(): LocalDate {
    val localDate = Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    return LocalDate(localDate.year, localDate.monthValue, localDate.dayOfMonth)
}

private fun defaultAddTransactionDate(): LocalDate {
    val today = java.time.LocalDate.now(ZoneId.systemDefault())
    return LocalDate(today.year, today.monthValue, today.dayOfMonth)
}
