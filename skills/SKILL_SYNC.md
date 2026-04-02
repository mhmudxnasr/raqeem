---
name: SKILL_SYNC
description: >
  Read this for anything related to data synchronization between Android
  (offline-first) and Web (realtime). Covers conflict resolution, sync queue,
  WorkManager, and Supabase Realtime setup.
---

# Sync Skill — Raqeem

## Two Platforms, Two Sync Strategies

| Platform | Strategy | Local DB | Real-time |
|----------|----------|----------|-----------|
| Android | Offline-first | Room (SQLite) | WorkManager background sync |
| Web | Online-first | None | Supabase Realtime subscriptions |

---

## Android: Offline-First

### The Contract

Every user action (add/edit/delete) MUST:
1. Write to Room immediately → UI reflects change instantly
2. Enqueue in `sync_queue` → background sync will handle Supabase
3. Return success to the user regardless of network state

The user should never wait for a network call to see their change.

### Sync Queue Entity

```kotlin
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
  @PrimaryKey val id: String = UUID.randomUUID().toString(),
  val tableName: String,
  val recordId: String,
  val operation: String,    // "upsert" | "delete"
  val payload: String,      // JSON-serialized record
  val createdAt: Long = System.currentTimeMillis(),
  val attempts: Int = 0,
  val lastError: String? = null,
  val isSynced: Boolean = false,
)
```

### SyncWorker

```kotlin
@HiltWorker
class SyncWorker @AssistedInject constructor(
  @Assisted context: Context,
  @Assisted params: WorkerParameters,
  private val syncQueueDao: SyncQueueDao,
  private val supabaseClient: SupabaseClient,
) : CoroutineWorker(context, params) {

  override suspend fun doWork(): Result {
    val pending = syncQueueDao.getPendingItems(limit = 50)
    if (pending.isEmpty()) return Result.success()

    var hasFailures = false

    for (item in pending) {
      try {
        when (item.operation) {
          "upsert" -> upsertToSupabase(item)
          "delete" -> softDeleteInSupabase(item)
        }
        syncQueueDao.markSynced(item.id)
      } catch (e: Exception) {
        syncQueueDao.incrementAttempts(item.id, e.message)
        if (item.attempts >= 5) {
          // Give up on this item after 5 attempts — log it
          syncQueueDao.markFailed(item.id)
        }
        hasFailures = true
      }
    }

    return if (hasFailures) Result.retry() else Result.success()
  }

  private suspend fun upsertToSupabase(item: SyncQueueEntity) {
    val payload = Json.parseToJsonElement(item.payload).jsonObject
    supabaseClient.from(item.tableName).upsert(payload)
  }
}
```

### WorkManager Registration

```kotlin
// In Application.onCreate()
val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
  .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
  .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
  .addTag("raqeem_sync")
  .build()

WorkManager.getInstance(this).enqueueUniquePeriodicWork(
  "raqeem_sync",
  ExistingPeriodicWorkPolicy.KEEP,
  syncRequest,
)
```

Also trigger immediate sync on:
- App comes to foreground (via LifecycleObserver)
- Network becomes available (NetworkCallback)

### Conflict Resolution

**Strategy: Last-write-wins using `updated_at`.**

When syncing to Supabase, use UPSERT with `ON CONFLICT (id) DO UPDATE` only if
the incoming `updated_at` is newer than what's in Supabase.

```sql
INSERT INTO transactions (..., updated_at)
VALUES (...)
ON CONFLICT (id) DO UPDATE SET
  amount_cents = EXCLUDED.amount_cents,
  note = EXCLUDED.note,
  -- ... other fields
  updated_at = EXCLUDED.updated_at
WHERE transactions.updated_at < EXCLUDED.updated_at;
```

The Kotlin-side upsert: always include `updated_at = Instant.now()` when modifying a record.

### Full Sync on First Launch

When the app is first installed (or after sign-in):
1. Pull all records from Supabase for the user
2. Bulk-insert into Room
3. Set a flag `is_initial_sync_done = true` in DataStore

```kotlin
class InitialSyncUseCase @Inject constructor(
  private val supabaseClient: SupabaseClient,
  private val db: AppDatabase,
  private val dataStore: DataStore<Preferences>,
) {
  suspend operator fun invoke() {
    val accounts = supabaseClient.from("accounts").select { ... }.decodeList<AccountDto>()
    val transactions = supabaseClient.from("transactions").select { ... }.decodeList<TransactionDto>()
    // ... etc
    
    db.withTransaction {
      db.accountDao().insertAll(accounts.map { it.toEntity() })
      db.transactionDao().insertAll(transactions.map { it.toEntity() })
    }
    
    dataStore.edit { it[IS_SYNCED] = true }
  }
}
```

---

## Web: Realtime

The web app has no local database. It reads from Supabase and subscribes to changes.

### Realtime Channel Setup

```typescript
// hooks/useRealtime.ts — call once in App.tsx
export function useRealtime() {
  useEffect(() => {
    const channel = supabase.channel('app-changes')
      .on('postgres_changes', { event: '*', schema: 'public', table: 'transactions' },
        (payload) => {
          // Invalidate and refetch
          useTransactionStore.getState().handleRealtimeEvent(payload);
        }
      )
      .on('postgres_changes', { event: '*', schema: 'public', table: 'accounts' },
        () => useAccountStore.getState().fetch()
      )
      .on('postgres_changes', { event: '*', schema: 'public', table: 'goals' },
        () => useGoalStore.getState().fetch()
      )
      .subscribe((status) => {
        if (status === 'SUBSCRIBED') {
          console.log('Realtime connected');
        }
      });

    return () => supabase.removeChannel(channel);
  }, []);
}
```

### Handling Realtime Events in Store

```typescript
// In useTransactionStore:
handleRealtimeEvent: (payload) => {
  set((state) => {
    const { eventType, new: newRecord, old: oldRecord } = payload;
    
    switch (eventType) {
      case 'INSERT':
        return { transactions: [mapRow(newRecord), ...state.transactions] };
      case 'UPDATE':
        return { transactions: state.transactions.map(t =>
          t.id === newRecord.id ? mapRow(newRecord) : t
        )};
      case 'DELETE':
        return { transactions: state.transactions.filter(t => t.id !== oldRecord.id) };
      default:
        return state;
    }
  });
}
```

---

## Sync Status Indicator (Android)

Show a small indicator in the app:
- Green dot: all synced
- Yellow dot: syncing in progress  
- Grey dot: offline (pending changes)
- Red dot: sync errors (tap to retry)

```kotlin
// Expose sync status from a SyncStatusRepository
sealed class SyncStatus {
  object Synced : SyncStatus()
  object Syncing : SyncStatus()
  data class Offline(val pendingCount: Int) : SyncStatus()
  data class Error(val failedCount: Int) : SyncStatus()
}
```

---

## What Syncs, What Doesn't

| Data | Syncs | Notes |
|------|-------|-------|
| Transactions | ✅ Yes | Core data |
| Accounts | ✅ Yes | Balance recomputed server-side |
| Categories | ✅ Yes | |
| Transfers | ✅ Yes | |
| Goals | ✅ Yes | |
| Subscriptions | ✅ Yes | |
| Settings | ✅ Yes | Currency rate, default account |
| AI chat history | ❌ No | Ephemeral, not stored |
| Biometric/PIN | ❌ No | Local device only |
| Receipts (images) | ✅ Yes | Via Supabase Storage, async |
