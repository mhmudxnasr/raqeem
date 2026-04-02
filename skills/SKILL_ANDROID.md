---
name: SKILL_ANDROID
description: >
  Read this for ALL Android work: architecture, Compose patterns, Room, Hilt,
  navigation, offline-first sync, Supabase Kotlin client, and conventions.
  Read SKILL_DESIGN.md first, then this.
---

# Android Skill — Raqeem

## Architecture: MVVM + Clean Architecture

```
presentation/     ← Composables + ViewModels
domain/           ← Use cases, interfaces, pure models
data/             ← Room, Supabase, repositories
```

**Rule:** Domain layer has ZERO Android imports. It's pure Kotlin.
ViewModels depend on UseCases. UseCases depend on Repository interfaces.
Repository implementations are in the `data` layer.

---

## Dependencies (build.gradle.kts)

```kotlin
// Jetpack Compose BOM
implementation(platform("androidx.compose:compose-bom:2024.05.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.ui:ui-tooling-preview")

// Navigation
implementation("androidx.navigation:navigation-compose:2.7.7")

// ViewModel
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

// Hilt
implementation("com.google.dagger:hilt-android:2.51")
kapt("com.google.dagger:hilt-compiler:2.51")
implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

// Room
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")

// Supabase Kotlin
implementation(platform("io.github.jan-tennert.supabase:bom:2.4.0"))
implementation("io.github.jan-tennert.supabase:postgrest-kt")
implementation("io.github.jan-tennert.supabase:realtime-kt")
implementation("io.github.jan-tennert.supabase:auth-kt")
implementation("io.github.jan-tennert.supabase:storage-kt")

// Ktor (Supabase engine)
implementation("io.ktor:ktor-client-android:2.3.10")

// Charts
implementation("com.patrykandpatrick.vico:compose-m3:1.15.0")

// Biometric
implementation("androidx.biometric:biometric:1.2.0-alpha05")

// Google Fonts
implementation("androidx.compose.ui:ui-text-google-fonts:1.6.7")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

// DateTime
implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
```

---

## Room Setup

```kotlin
// data/local/AppDatabase.kt
@Database(
  entities = [AccountEntity::class, TransactionEntity::class, TransferEntity::class,
              CategoryEntity::class, GoalEntity::class, SubscriptionEntity::class,
              SyncQueueEntity::class],
  version = 1,
  exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
  abstract fun accountDao(): AccountDao
  abstract fun transactionDao(): TransactionDao
  abstract fun transferDao(): TransferDao
  abstract fun categoryDao(): CategoryDao
  abstract fun goalDao(): GoalDao
  abstract fun subscriptionDao(): SubscriptionDao
  abstract fun syncQueueDao(): SyncQueueDao
}
```

**Type converters needed:**
- `Instant` ↔ `Long` (epoch millis)
- `LocalDate` ↔ `String` (ISO date)

---

## Offline-First Sync Architecture

```kotlin
// The flow:
// 1. User action → ViewModel → UseCase → Repository
// 2. Repository writes to Room immediately (returns success)
// 3. Repository enqueues sync item in sync_queue table
// 4. SyncWorker runs in background (WorkManager), picks up queue
// 5. SyncWorker sends to Supabase, marks queue item done
// 6. On conflict: last-write-wins using updated_at

// SyncQueueEntity
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
  @PrimaryKey val id: String = UUID.randomUUID().toString(),
  val tableName: String,    // "transactions", "accounts", etc.
  val recordId: String,
  val operation: String,    // "upsert" | "delete"
  val payload: String,      // JSON of the record
  val createdAt: Long = System.currentTimeMillis(),
  val attempts: Int = 0,
)
```

**WorkManager config:**
```kotlin
val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
  .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
  .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
  .build()
```

---

## Compose Navigation

5 bottom tabs + nested screens:

```kotlin
// NavGraph.kt
sealed class Screen(val route: String) {
  object Home : Screen("home")
  object Accounts : Screen("accounts")
  object Analytics : Screen("analytics")
  object Budgets : Screen("budgets")
  object Goals : Screen("goals")
  object Settings : Screen("settings")
  object AccountDetail : Screen("account/{accountId}") {
    fun createRoute(id: String) = "account/$id"
  }
  object AddTransaction : Screen("add_transaction?type={type}") {
    fun createRoute(type: String = "expense") = "add_transaction?type=$type"
  }
  object AIBot : Screen("ai_bot")
  object TransactionDetail : Screen("transaction/{id}") {
    fun createRoute(id: String) = "transaction/$id"
  }
}
```

**Bottom sheets** use `ModalBottomSheet` from Material3.
**Never use Dialog for the Add Transaction / Add Transfer flows** — always bottom sheet.

---

## Theme Setup

```kotlin
// ui/theme/Theme.kt
@Composable
fun RaqeemTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = darkColorScheme(
      primary       = AppColors.purple500,
      onPrimary     = Color.White,
      background    = AppColors.bgBase,
      surface       = AppColors.bgSurface,
      surfaceVariant= AppColors.bgElevated,
      onBackground  = AppColors.textPrimary,
      onSurface     = AppColors.textPrimary,
      error         = AppColors.negative,
    ),
    typography = RaqeemTypography,
    content = content,
  )
}
```

---

## ViewModel Pattern

```kotlin
// presentation/viewmodel/HomeViewModel.kt
@HiltViewModel
class HomeViewModel @Inject constructor(
  private val getRecentTransactions: GetRecentTransactionsUseCase,
) : ViewModel() {

  private val _uiState = MutableStateFlow(HomeUiState())
  val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

  init { loadRecentTransactions() }

  private fun loadRecentTransactions() {
    viewModelScope.launch {
      getRecentTransactions().collect { result ->
        _uiState.update { it.copy(
          transactions = result,
          isLoading = false,
        )}
      }
    }
  }
}

data class HomeUiState(
  val transactions: List<Transaction> = emptyList(),
  val isLoading: Boolean = true,
  val error: String? = null,
)
```

**Rule:** Every screen has its own `UiState` data class. Never pass raw domain
models directly into composables — wrap in UI state.

---

## Biometric Gate

```kotlin
// Shown on app foreground if > 5 minutes elapsed since last unlock
class BiometricGate {
  fun prompt(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onFallback: () -> Unit, // show PIN entry
  ) {
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
      .setTitle("Unlock Raqeem")
      .setSubtitle("Use biometric or PIN")
      .setNegativeButtonText("Use PIN")
      .build()

    BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
      override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        onSuccess()
      }
      override fun onAuthenticationError(code: Int, msg: CharSequence) {
        if (code == BiometricPrompt.ERROR_NEGATIVE_BUTTON) onFallback()
      }
    }).authenticate(promptInfo)
  }
}
```

---

## Key Compose Patterns

```kotlin
// Loading + error pattern for every screen
@Composable
fun TransactionListContent(state: TransactionListUiState) {
  when {
    state.isLoading -> TransactionListSkeleton()
    state.error != null -> ErrorState(message = state.error)
    state.transactions.isEmpty() -> EmptyState(message = "No transactions yet")
    else -> LazyColumn {
      items(state.transactions, key = { it.id }) { tx ->
        TransactionListItem(transaction = tx)
      }
    }
  }
}

// Always use key= in LazyColumn for performance
// Always use const constructors where possible
// Never put business logic in Composables
```

---

## Amount Formatting Extension

```kotlin
// Put in utils/AmountUtils.kt
fun Int.formatAmount(currency: Currency, showSign: Boolean = false): String {
  val abs = kotlin.math.abs(this) / 100.0
  val symbol = if (currency == Currency.USD) "$" else "EGP "
  val formatted = "%.2f".format(abs)
  val sign = when {
    showSign && this > 0 -> "+"
    this < 0 -> "−"
    else -> ""
  }
  return "$sign$symbol$formatted"
}

fun Int.toAmountColor(type: TransactionType): Color = when (type) {
  TransactionType.INCOME  -> AppColors.positive
  TransactionType.EXPENSE -> AppColors.negative
}
```

---

## Error Handling Convention

```kotlin
// Never crash silently. Use a result wrapper.
sealed class Result<out T> {
  data class Success<T>(val data: T) : Result<T>()
  data class Error(val message: String, val cause: Throwable? = null) : Result<Nothing>()
  object Loading : Result<Nothing>()
}

// In Repository:
override suspend fun addTransaction(tx: Transaction): Result<Unit> {
  return try {
    transactionDao.insert(tx.toEntity())
    syncQueue.enqueue(tx)
    Result.Success(Unit)
  } catch (e: Exception) {
    Result.Error("Failed to save transaction", e)
  }
}
```

---

## What NOT to Do

- Don't use `rememberCoroutineScope` to launch business logic — use ViewModel
- Don't call repository directly from Composable — always through ViewModel
- Don't use `LaunchedEffect` for anything that should survive recomposition — use ViewModel init
- Don't share ViewModels between screens — each screen gets its own
- Don't use `GlobalScope` anywhere
- Don't block the main thread — everything async with coroutines
