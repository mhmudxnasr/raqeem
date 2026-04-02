package com.raqeem.app.presentation.ui.screens.goals

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.raqeem.app.domain.model.Goal
import com.raqeem.app.domain.model.Result
import com.raqeem.app.domain.usecase.AddGoalUseCase
import com.raqeem.app.domain.usecase.CompleteGoalUseCase
import com.raqeem.app.domain.usecase.DeleteGoalUseCase
import com.raqeem.app.domain.usecase.GetGoalsUseCase
import com.raqeem.app.domain.usecase.GetSettingsUseCase
import com.raqeem.app.domain.usecase.UpdateGoalUseCase
import com.raqeem.app.presentation.ui.components.BudgetBar
import com.raqeem.app.presentation.ui.components.EmptyState
import com.raqeem.app.presentation.ui.components.HeaderIconButton
import com.raqeem.app.presentation.ui.components.PageHeader
import com.raqeem.app.presentation.ui.components.SurfaceCard
import com.raqeem.app.presentation.ui.theme.AppColors
import com.raqeem.app.presentation.ui.theme.MonoFamily
import com.raqeem.app.util.formatAmount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlin.math.roundToInt

data class GoalsUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val userId: String = LOCAL_USER_ID,
    val goals: List<Goal> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    getGoals: GetGoalsUseCase,
    getSettings: GetSettingsUseCase,
    private val addGoal: AddGoalUseCase,
    private val updateGoal: UpdateGoalUseCase,
    private val completeGoal: CompleteGoalUseCase,
    private val deleteGoal: DeleteGoalUseCase,
) : ViewModel() {

    private val isSaving = MutableStateFlow(false)
    private val actionError = MutableStateFlow<String?>(null)

    val uiState = combine(
        getGoals(),
        getSettings(),
        isSaving,
        actionError,
    ) { goals, settings, saving, error ->
        GoalsUiState(
            isLoading = false,
            isSaving = saving,
            userId = settings.userId.ifBlank { LOCAL_USER_ID },
            goals = goals,
            error = error,
        )
    }
        .catch { throwable ->
            emit(
                GoalsUiState(
                    isLoading = false,
                    error = throwable.message ?: "Unable to load goals.",
                ),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GoalsUiState(),
        )

    fun clearError() {
        actionError.value = null
    }

    suspend fun saveGoal(
        existingGoal: Goal?,
        name: String,
        targetInput: String,
        deadlineInput: String,
        note: String,
    ): Boolean {
        val targetCents = parseAmountToCents(targetInput)
            ?: return setError("Target amount must be greater than 0.")
        val deadline = deadlineInput.trim().takeIf { it.isNotBlank() }?.let {
            runCatching { LocalDate.parse(it) }.getOrNull()
        }
        if (deadlineInput.isNotBlank() && deadline == null) {
            return setError("Deadline must use YYYY-MM-DD.")
        }

        isSaving.value = true
        actionError.value = null
        val now = Clock.System.now()
        val goal = (existingGoal ?: Goal(
            id = UUID.randomUUID().toString(),
            userId = uiState.value.userId,
            name = name.trim(),
            targetCents = targetCents,
            createdAt = now,
            updatedAt = now,
        )).copy(
            name = name.trim(),
            targetCents = targetCents,
            deadline = deadline,
            note = note.trim().ifBlank { null },
            updatedAt = now,
        )

        val result = if (existingGoal == null) addGoal(goal) else updateGoal(goal)
        isSaving.value = false
        return when (result) {
            is Result.Success -> true
            is Result.Error -> setError(result.message)
            Result.Loading -> false
        }
    }

    fun completeGoal(goalId: String) {
        viewModelScope.launch {
            completeGoal(goalId)
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            deleteGoal(goalId)
        }
    }

    private fun setError(message: String): Boolean {
        actionError.value = message
        return false
    }

    private fun parseAmountToCents(input: String): Int? {
        val amount = input.trim().replace(",", "").toDoubleOrNull() ?: return null
        if (amount <= 0) return null
        return (amount * 100).roundToInt()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GoalsScreen(
    onOpenSettings: () -> Unit,
    onAddFunds: (String) -> Unit,
    viewModel: GoalsViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val scope = rememberCoroutineScope()
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editingGoalId by rememberSaveable { mutableStateOf<String?>(null) }
    val editingGoal = uiState.goals.firstOrNull { it.id == editingGoalId }
    val activeGoals = uiState.goals.filterNot { it.isCompleted }
    val completedGoals = uiState.goals.filter { it.isCompleted }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.bgBase)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item("goals_header", contentType = "header") {
            PageHeader(
                title = "Goals",
                supportingText = "Long-term targets funded through transfers.",
                trailing = {
                    HeaderIconButton(
                        icon = Icons.Rounded.Add,
                        contentDescription = "Add goal",
                        onClick = {
                            editingGoalId = null
                            viewModel.clearError()
                            showEditor = true
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

        if (uiState.goals.isEmpty() && !uiState.isLoading) {
            item("goals_empty", contentType = "empty") {
                SurfaceCard(modifier = Modifier.animateItemPlacement()) {
                    EmptyState(
                        title = "No goals yet",
                        subtitle = "Create a savings goal, then fund it from the transfer flow.",
                        icon = Icons.Rounded.Flag,
                        actionLabel = "New Goal",
                        onAction = {
                            editingGoalId = null
                            showEditor = true
                        },
                    )
                }
            }
        } else {
            items(
                activeGoals,
                key = { it.id },
                contentType = { "goal_card" }
            ) { goal ->
                GoalCard(
                    modifier = Modifier.animateItemPlacement(),
                    goal = goal,
                    onAddFunds = { onAddFunds(goal.id) },
                    onEdit = {
                        editingGoalId = goal.id
                        showEditor = true
                    },
                    onComplete = { viewModel.completeGoal(goal.id) },
                    onDelete = { viewModel.deleteGoal(goal.id) },
                )
            }
            if (completedGoals.isNotEmpty()) {
                item("goals_completed_label", contentType = "section_label") {
                    Text(
                        text = "Completed",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.textMuted,
                        modifier = Modifier.animateItemPlacement(),
                    )
                }
                items(
                    completedGoals,
                    key = { it.id },
                    contentType = { "goal_card" }
                ) { goal ->
                    GoalCard(
                        modifier = Modifier.animateItemPlacement(),
                        goal = goal,
                        onAddFunds = { onAddFunds(goal.id) },
                        onEdit = {
                            editingGoalId = goal.id
                            showEditor = true
                        },
                        onComplete = { viewModel.completeGoal(goal.id) },
                        onDelete = { viewModel.deleteGoal(goal.id) },
                    )
                }
            }
        }

        uiState.error?.let { error ->
            item("goals_error", contentType = "error") {
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

    if (showEditor) {
        GoalEditorBottomSheet(
            existingGoal = editingGoal,
            isSaving = uiState.isSaving,
            onDismiss = { showEditor = false },
            onSave = { name, target, deadline, note ->
                scope.launch {
                    if (viewModel.saveGoal(editingGoal, name, target, deadline, note)) {
                        showEditor = false
                    }
                }
            },
        )
    }
}

@Composable
private fun GoalCard(
    goal: Goal,
    onAddFunds: () -> Unit,
    onEdit: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = goal.progressPercent.coerceIn(0, 100)

    SurfaceCard(
        modifier = modifier.clickable(onClick = onEdit),
        backgroundColor = if (goal.isCompleted) AppColors.bgSurface else AppColors.bgElevated,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = goal.name,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = if (goal.isCompleted) "DONE" else "${progress}%",
                style = MaterialTheme.typography.labelLarge,
                color = if (goal.isCompleted) AppColors.positive else AppColors.textMuted,
            )
        }
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontFamily = MonoFamily, fontFeatureSettings = "tnum")) { append(goal.currentCents.formatAmount(goal.currency)) }
                append(" / ")
                withStyle(SpanStyle(fontFamily = MonoFamily, fontFeatureSettings = "tnum")) { append(goal.targetCents.formatAmount(goal.currency)) }
            },
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.textSecondary,
        )
        BudgetBar(
            progress = progress / 100f,
            color = if (goal.isCompleted) AppColors.positive else AppColors.purple500,
        )
        Text(
            text = goal.deadline?.let(::formatDeadline) ?: "No deadline",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.textMuted,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onAddFunds,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.purple500,
                    contentColor = Color.White,
                ),
            ) {
                Text("Add funds")
            }
            if (!goal.isCompleted) {
                Button(
                    onClick = onComplete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.bgSubtle,
                        contentColor = AppColors.textPrimary,
                    ),
                ) {
                    Text("Complete")
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete goal",
                    tint = AppColors.negative,
                )
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun GoalEditorBottomSheet(
    existingGoal: Goal?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit,
) {
    var nameInput by rememberSaveable(existingGoal?.id) { mutableStateOf(existingGoal?.name.orEmpty()) }
    var targetInput by rememberSaveable(existingGoal?.id) {
        mutableStateOf(existingGoal?.let { "%.2f".format(Locale.US, it.targetCents / 100.0) }.orEmpty())
    }
    var deadlineInput by rememberSaveable(existingGoal?.id) { mutableStateOf(existingGoal?.deadline?.toString().orEmpty()) }
    var noteInput by rememberSaveable(existingGoal?.id) { mutableStateOf(existingGoal?.note.orEmpty()) }

    androidx.compose.material3.ModalBottomSheet(
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
                text = if (existingGoal == null) "New Goal" else "Edit Goal",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Name") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.borderAccent,
                    unfocusedBorderColor = AppColors.borderSubtle,
                    focusedContainerColor = AppColors.bgSubtle,
                    unfocusedContainerColor = AppColors.bgSubtle,
                ),
            )
            OutlinedTextField(
                value = targetInput,
                onValueChange = { targetInput = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Target amount") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.borderAccent,
                    unfocusedBorderColor = AppColors.borderSubtle,
                    focusedContainerColor = AppColors.bgSubtle,
                    unfocusedContainerColor = AppColors.bgSubtle,
                ),
            )
            OutlinedTextField(
                value = deadlineInput,
                onValueChange = { deadlineInput = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Deadline (YYYY-MM-DD)") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.borderAccent,
                    unfocusedBorderColor = AppColors.borderSubtle,
                    focusedContainerColor = AppColors.bgSubtle,
                    unfocusedContainerColor = AppColors.bgSubtle,
                ),
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
            Button(
                onClick = { onSave(nameInput, targetInput, deadlineInput, noteInput) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.purple500,
                    contentColor = Color.White,
                ),
            ) {
                Text(if (isSaving) "Saving..." else "Save Goal")
            }
        }
    }
}

private fun formatDeadline(date: LocalDate): String {
    val javaDate = java.time.LocalDate.of(date.year, date.monthNumber, date.dayOfMonth)
    return "Deadline: ${javaDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))}"
}
