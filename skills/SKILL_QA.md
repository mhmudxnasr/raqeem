---
name: SKILL_QA
description: >
  Read this for testing strategy on both Android (JUnit/Espresso) and
  Web (Vitest/Testing Library). Covers what to test, what not to,
  error handling conventions, and edge cases specific to this finance app.
---

# QA Skill — Raqeem

## Testing Philosophy

Test behavior, not implementation.
The user cares that "$10 expense reduces account balance by $10."
They don't care that `balanceCents` is updated by calling `recalculate()`.

Focus testing effort where it matters most:
1. **Financial calculations** — must be bulletproof
2. **Currency conversion logic** — USD/EGP, rounding
3. **Sync queue** — offline writes, retry logic
4. **Data validation** — amounts, dates, categories

---

## Android Testing

### Unit Tests (JUnit 5 + MockK)

**Priority: Use cases and domain logic**

```kotlin
// test/domain/usecase/AddTransactionUseCaseTest.kt
class AddTransactionUseCaseTest {

  private val mockRepo = mockk<TransactionRepository>()
  private val useCase = AddTransactionUseCase(mockRepo)

  @Test
  fun `adding expense decrements account balance`() = runTest {
    val tx = buildTestTransaction(type = EXPENSE, amountCents = 1000, accountId = "acc-1")
    coEvery { mockRepo.add(tx) } returns Result.Success(Unit)
    
    val result = useCase(tx)
    
    assertTrue(result is Result.Success)
    coVerify(exactly = 1) { mockRepo.add(tx) }
  }

  @Test
  fun `negative amount is rejected`() = runTest {
    val tx = buildTestTransaction(amountCents = -500)
    val result = useCase(tx)
    assertTrue(result is Result.Error)
  }
}
```

**Priority: Currency conversion**

```kotlin
// test/utils/AmountUtilsTest.kt
class AmountUtilsTest {

  @Test
  fun `USD to EGP conversion at fixed rate`() {
    val usdCents = 1000  // $10.00
    val rate = 52.0
    val egpPiastres = convertUsdToEgp(usdCents, rate)
    assertEquals(52000, egpPiastres)  // EGP 520.00
  }

  @Test
  fun `amount formatting shows correct sign`() {
    assertEquals("+$10.00", 1000.formatAmount(Currency.USD, showSign = true))
    assertEquals("−$10.00", (-1000).formatAmount(Currency.USD))
    assertEquals("EGP 520.00", 52000.formatAmount(Currency.EGP))
  }

  @Test
  fun `zero amount formats correctly`() {
    assertEquals("$0.00", 0.formatAmount(Currency.USD))
  }
}
```

### Integration Tests (Room in-memory DB)

```kotlin
// test/data/TransactionDaoTest.kt
@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {

  private lateinit var db: AppDatabase
  private lateinit var dao: TransactionDao

  @Before
  fun setup() {
    db = Room.inMemoryDatabaseBuilder(
      ApplicationProvider.getApplicationContext(),
      AppDatabase::class.java,
    ).build()
    dao = db.transactionDao()
  }

  @After
  fun teardown() = db.close()

  @Test
  fun insertAndRetrieveTransaction() = runTest {
    val tx = buildTestTransactionEntity(id = "tx-1", amountCents = 5000)
    dao.insert(tx)
    
    val retrieved = dao.getById("tx-1").first()
    assertEquals(5000, retrieved?.amountCents)
  }
}
```

### UI Tests (Espresso — minimal, only critical flows)

Test only these 3 critical flows:
1. Add expense → appears in transaction list
2. Transfer USD → Wallet → shows EGP conversion prompt
3. Login → biometric gate → home screen visible

---

## Web Testing

### Unit Tests (Vitest)

```typescript
// src/lib/__tests__/format.test.ts
import { describe, it, expect } from 'vitest';
import { formatAmount, toUSD } from '../format';

describe('formatAmount', () => {
  it('formats USD with sign', () => {
    expect(formatAmount(1000, 'USD', true)).toBe('+$10.00');
    expect(formatAmount(-1000, 'USD')).toBe('−$10.00');
    expect(formatAmount(0, 'USD')).toBe('$0.00');
  });

  it('formats EGP correctly', () => {
    expect(formatAmount(52000, 'EGP')).toBe('EGP 520.00');
  });
});

describe('toUSD', () => {
  it('converts EGP piastres to USD cents at given rate', () => {
    expect(toUSD(52000, 'EGP', 52)).toBe(1000);   // EGP 520 = $10
    expect(toUSD(1000, 'USD', 52)).toBe(1000);    // USD passes through
  });
});
```

### Component Tests (Vitest + Testing Library)

```typescript
// src/components/features/__tests__/TransactionItem.test.tsx
import { render, screen } from '@testing-library/react';
import { TransactionItem } from '../TransactionItem';

const mockExpense: Transaction = {
  id: '1', type: 'expense', amountCents: 1250, currency: 'USD',
  date: '2024-04-01', note: 'Lunch', categoryId: null, accountId: 'acc-1',
  receiptUrl: null, createdAt: '2024-04-01T12:00:00Z',
};

it('shows negative amount for expense', () => {
  render(<TransactionItem transaction={mockExpense} />);
  expect(screen.getByText('−$12.50')).toBeInTheDocument();
});

it('shows correct category label', () => {
  const withCategory = { ...mockExpense, category: { name: 'Food & Dining' } };
  render(<TransactionItem transaction={withCategory} />);
  expect(screen.getByText('Food & Dining')).toBeInTheDocument();
});
```

### Form Validation Tests

```typescript
describe('AddTransactionForm validation', () => {
  it('rejects zero amount', async () => {
    // submit with amountCents = 0, expect error message
  });

  it('requires account selection', async () => {
    // submit without accountId, expect error
  });

  it('auto-applies EGP currency when Wallet account selected', async () => {
    // select Wallet account, expect currency to switch to EGP
  });
});
```

---

## Critical Edge Cases to Test

### Financial Logic
- [ ] Transfer from USD account to Wallet → EGP conversion shown
- [ ] EGP expense on Wallet → balance decrements in EGP, not USD
- [ ] Goal funding via transfer → goal `current_cents` increments
- [ ] Deleting a transaction → account balance recalculates
- [ ] Budget utilization at exactly 100% → shows red, not orange
- [ ] Budget utilization at 0% → shows green
- [ ] Amount of 0 → rejected at form level
- [ ] Amount with more than 2 decimal places → rounded or rejected

### Sync
- [ ] App goes offline → add transaction → goes back online → transaction syncs
- [ ] Same record edited on web and Android in quick succession → last-write-wins
- [ ] Sync queue with 50+ pending items → processes in batches without timeout

### Display
- [ ] Very large amounts (e.g., $999,999.99) → no layout overflow
- [ ] Long category name → truncated with ellipsis, not wrapping
- [ ] Empty transaction list → empty state shown (not blank space)
- [ ] Balance hidden → shows bullet dots, not 0

---

## Error Handling Convention

Every operation that can fail must:
1. Show a toast/snackbar with a user-friendly message
2. NOT crash the app or leave the UI in a broken state
3. Offer retry where appropriate

```kotlin
// Android snackbar convention
viewModel.error.collect { message ->
  if (message != null) {
    Snackbar.make(view, message, Snackbar.LENGTH_LONG)
      .setAction("Retry") { viewModel.retry() }
      .show()
  }
}
```

```typescript
// Web toast convention (use a simple toast library or custom)
try {
  await store.add(transaction);
  toast.success('Transaction added');
} catch {
  toast.error('Failed to save. Please try again.');
}
```

---

## What NOT to Test

- Supabase internals (trust the library)
- UI pixel positions or exact colors
- Third-party component library behavior
- Groq AI response content (non-deterministic)
- Animation timing
