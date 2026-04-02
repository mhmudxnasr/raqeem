# Data Model — Raqeem
## PostgreSQL / Supabase Schema

---

## Principles

- All monetary amounts stored as **INTEGER in cents** (USD) to avoid floating
  point errors. EGP amounts stored as INTEGER in piastres (1 EGP = 100 piastres).
- All timestamps: `timestamptz` (UTC).
- `user_id` on every table references `auth.users(id)` — Supabase handles this.
- Row Level Security (RLS) enabled on every table.
- Soft deletes: `deleted_at timestamptz DEFAULT NULL`.

---

## Tables

### `accounts`

Tracks every financial account (wallet, savings, checking, crypto).

```sql
CREATE TABLE accounts (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id       UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  name          TEXT NOT NULL,
  type          TEXT NOT NULL CHECK (type IN ('cash','checking','saving','investment','crypto')),
  currency      TEXT NOT NULL DEFAULT 'USD' CHECK (currency IN ('USD','EGP')),
  initial_amount_cents INTEGER NOT NULL DEFAULT 0,
  balance_cents INTEGER NOT NULL DEFAULT 0,  -- computed & kept in sync
  is_hidden     BOOLEAN NOT NULL DEFAULT false,
  sort_order    SMALLINT NOT NULL DEFAULT 0,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  deleted_at    TIMESTAMPTZ
);

-- Index
CREATE INDEX accounts_user_id_idx ON accounts(user_id) WHERE deleted_at IS NULL;
```

**Pre-seeded accounts for this user:**
| name | type | currency |
|------|------|----------|
| Binance Free | checking | USD |
| Binance Savings | saving | USD |
| Wallet | cash | EGP |
| Charity | checking | USD |

---

### `categories`

Expense and income categories with monthly budgets.

```sql
CREATE TABLE categories (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id       UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  name          TEXT NOT NULL,
  type          TEXT NOT NULL CHECK (type IN ('income','expense')),
  icon          TEXT NOT NULL DEFAULT 'circle',   -- Lucide icon name
  color         TEXT NOT NULL DEFAULT '#8B5CF6',  -- hex
  budget_cents  INTEGER,                           -- monthly budget, NULL = no budget
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  deleted_at    TIMESTAMPTZ
);
```

**Pre-seeded expense categories:**
| name | icon | budget_cents |
|------|------|-------------|
| Food & Dining | utensils | 8000 (= $80) |
| Transportation | car | 3000 |
| Personal Care | heart | 2000 |
| Entertainment | tv | 2000 |
| Utilities | zap | 1500 |
| Shopping | shopping-bag | 5000 |
| Education | book | 3000 |
| Health | activity | 3000 |
| Miscellaneous | circle | 2000 |

**Pre-seeded income categories:**
| name | icon |
|------|------|
| Outlier | briefcase |
| Alignerr | briefcase |
| Freelance | code |
| Other | plus-circle |

---

### `transactions`

Every income and expense entry.

```sql
CREATE TABLE transactions (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  account_id      UUID NOT NULL REFERENCES accounts(id),
  category_id     UUID REFERENCES categories(id),
  type            TEXT NOT NULL CHECK (type IN ('income','expense')),
  amount_cents    INTEGER NOT NULL CHECK (amount_cents > 0),
  currency        TEXT NOT NULL DEFAULT 'USD' CHECK (currency IN ('USD','EGP')),
  note            TEXT,
  date            DATE NOT NULL,
  receipt_url     TEXT,   -- Supabase Storage path
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  deleted_at      TIMESTAMPTZ
);

-- Indexes
CREATE INDEX transactions_user_date_idx   ON transactions(user_id, date DESC) WHERE deleted_at IS NULL;
CREATE INDEX transactions_account_idx     ON transactions(account_id) WHERE deleted_at IS NULL;
CREATE INDEX transactions_category_idx    ON transactions(category_id) WHERE deleted_at IS NULL;
```

---

### `transfers`

Moving money between accounts. Also handles USD→EGP conversion for Wallet funding.

```sql
CREATE TABLE transfers (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id           UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  from_account_id   UUID NOT NULL REFERENCES accounts(id),
  to_account_id     UUID NOT NULL REFERENCES accounts(id),
  from_amount_cents INTEGER NOT NULL CHECK (from_amount_cents > 0),
  to_amount_cents   INTEGER NOT NULL CHECK (to_amount_cents > 0),
  from_currency     TEXT NOT NULL CHECK (from_currency IN ('USD','EGP')),
  to_currency       TEXT NOT NULL CHECK (to_currency IN ('USD','EGP')),
  exchange_rate     NUMERIC(10,4) NOT NULL DEFAULT 1.0,
                    -- e.g. 52.0000 means 1 USD = 52 EGP
  is_currency_conversion BOOLEAN NOT NULL DEFAULT false,
  goal_id           UUID REFERENCES goals(id),  -- if funding a goal
  note              TEXT,
  date              DATE NOT NULL,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  deleted_at        TIMESTAMPTZ
);
```

---

### `goals`

Savings goals funded by transfers.

```sql
CREATE TABLE goals (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id           UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  name              TEXT NOT NULL,
  target_cents      INTEGER NOT NULL CHECK (target_cents > 0),
  current_cents     INTEGER NOT NULL DEFAULT 0,
  currency          TEXT NOT NULL DEFAULT 'USD',
  deadline          DATE,
  is_completed      BOOLEAN NOT NULL DEFAULT false,
  icon              TEXT NOT NULL DEFAULT 'flag',
  note              TEXT,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  deleted_at        TIMESTAMPTZ
);
```

---

### `subscriptions`

Recurring charges tracked separately.

```sql
CREATE TABLE subscriptions (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  account_id      UUID NOT NULL REFERENCES accounts(id),
  category_id     UUID REFERENCES categories(id),
  name            TEXT NOT NULL,
  amount_cents    INTEGER NOT NULL CHECK (amount_cents > 0),
  currency        TEXT NOT NULL DEFAULT 'USD',
  billing_cycle   TEXT NOT NULL CHECK (billing_cycle IN ('weekly','monthly','yearly')),
  next_billing_date DATE NOT NULL,
  is_active       BOOLEAN NOT NULL DEFAULT true,
  auto_log        BOOLEAN NOT NULL DEFAULT false,  -- auto-create transaction on billing date
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  deleted_at      TIMESTAMPTZ
);
```

---

### `settings`

Single row per user for app-wide preferences.

```sql
CREATE TABLE settings (
  user_id             UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  usd_to_egp_rate     NUMERIC(8,4) NOT NULL DEFAULT 52.0,
  default_account_id  UUID REFERENCES accounts(id),
  analytics_currency  TEXT NOT NULL DEFAULT 'USD',
  created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

---

## RLS Policies

Apply this pattern to EVERY table:

```sql
-- Enable RLS
ALTER TABLE accounts ENABLE ROW LEVEL SECURITY;

-- Single policy: only your rows
CREATE POLICY "user owns rows" ON accounts
  USING (auth.uid() = user_id)
  WITH CHECK (auth.uid() = user_id);
```

Repeat for: `categories`, `transactions`, `transfers`, `goals`, `subscriptions`.
`settings` uses `user_id` as primary key so the same pattern applies.

---

## Balance Computation

Account balances are maintained in real-time via PostgreSQL functions:

```sql
-- Called after INSERT/UPDATE/DELETE on transactions
CREATE OR REPLACE FUNCTION recalculate_account_balance(p_account_id UUID)
RETURNS void AS $$
  UPDATE accounts
  SET balance_cents = (
    SELECT initial_amount_cents +
      COALESCE(SUM(CASE WHEN t.type = 'income'  THEN  t.amount_cents ELSE 0 END), 0) -
      COALESCE(SUM(CASE WHEN t.type = 'expense' THEN  t.amount_cents ELSE 0 END), 0)
    FROM transactions t
    WHERE t.account_id = p_account_id
      AND t.deleted_at IS NULL
  ) + (
    -- Add incoming transfers
    COALESCE((
      SELECT SUM(to_amount_cents) FROM transfers
      WHERE to_account_id = p_account_id AND deleted_at IS NULL
    ), 0)
  ) - (
    -- Subtract outgoing transfers
    COALESCE((
      SELECT SUM(from_amount_cents) FROM transfers
      WHERE from_account_id = p_account_id AND deleted_at IS NULL
    ), 0)
  ),
  updated_at = now()
  WHERE id = p_account_id;
$$ LANGUAGE sql;
```

---

## TypeScript Types (shared)

```typescript
// types/index.ts

export type Currency = 'USD' | 'EGP';
export type AccountType = 'cash' | 'checking' | 'saving' | 'investment' | 'crypto';
export type TransactionType = 'income' | 'expense';
export type BillingCycle = 'weekly' | 'monthly' | 'yearly';

export interface Account {
  id: string;
  name: string;
  type: AccountType;
  currency: Currency;
  initialAmountCents: number;
  balanceCents: number;
  isHidden: boolean;
  sortOrder: number;
  createdAt: string;
}

export interface Category {
  id: string;
  name: string;
  type: TransactionType;
  icon: string;
  color: string;
  budgetCents: number | null;
}

export interface Transaction {
  id: string;
  accountId: string;
  categoryId: string | null;
  type: TransactionType;
  amountCents: number;
  currency: Currency;
  note: string | null;
  date: string;            // ISO date string YYYY-MM-DD
  receiptUrl: string | null;
  createdAt: string;
  // Joined
  account?: Account;
  category?: Category;
}

export interface Transfer {
  id: string;
  fromAccountId: string;
  toAccountId: string;
  fromAmountCents: number;
  toAmountCents: number;
  fromCurrency: Currency;
  toCurrency: Currency;
  exchangeRate: number;
  isCurrencyConversion: boolean;
  goalId: string | null;
  note: string | null;
  date: string;
  createdAt: string;
}

export interface Goal {
  id: string;
  name: string;
  targetCents: number;
  currentCents: number;
  currency: Currency;
  deadline: string | null;
  isCompleted: boolean;
  icon: string;
}

export interface Subscription {
  id: string;
  accountId: string;
  categoryId: string | null;
  name: string;
  amountCents: number;
  currency: Currency;
  billingCycle: BillingCycle;
  nextBillingDate: string;
  isActive: boolean;
  autoLog: boolean;
}

export interface Settings {
  usdToEgpRate: number;
  defaultAccountId: string | null;
  analyticsCurrency: Currency;
}
```

---

## Kotlin Data Classes (Android)

```kotlin
// domain/model/Account.kt
data class Account(
  val id: String,
  val name: String,
  val type: AccountType,
  val currency: Currency,
  val initialAmountCents: Int,
  val balanceCents: Int,
  val isHidden: Boolean,
  val sortOrder: Int,
  val createdAt: Instant,
)

enum class AccountType { CASH, CHECKING, SAVING, INVESTMENT, CRYPTO }
enum class Currency { USD, EGP }
enum class TransactionType { INCOME, EXPENSE }

data class Transaction(
  val id: String,
  val accountId: String,
  val categoryId: String?,
  val type: TransactionType,
  val amountCents: Int,
  val currency: Currency,
  val note: String?,
  val date: LocalDate,
  val receiptUrl: String?,
  val createdAt: Instant,
  // joined
  val account: Account? = null,
  val category: Category? = null,
)
```

---

## Utility: Amount Formatting

```kotlin
// Android
fun Int.formatAsCurrency(currency: Currency, showSign: Boolean = false): String {
  val amount = this / 100.0
  val symbol = if (currency == Currency.USD) "$" else "EGP "
  val sign = if (showSign && this >= 0) "+" else if (this < 0) "−" else ""
  return "$sign$symbol${"%.2f".format(kotlin.math.abs(amount))}"
}
// 12450 -> "$124.50"
// +12450 (showSign) -> "+$124.50"
```

```typescript
// Web
export function formatAmount(cents: number, currency: Currency, showSign = false): string {
  const abs = Math.abs(cents) / 100;
  const symbol = currency === 'USD' ? '$' : 'EGP ';
  const sign = showSign && cents >= 0 ? '+' : cents < 0 ? '−' : '';
  return `${sign}${symbol}${abs.toFixed(2)}`;
}
```
