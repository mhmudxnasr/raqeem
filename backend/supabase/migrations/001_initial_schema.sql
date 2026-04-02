-- ============================================================
-- Raqeem — 001_initial_schema.sql
-- All core tables for the personal finance tracker.
-- Amounts stored as INTEGER in cents (USD) or piastres (EGP).
-- ============================================================

-- accounts
CREATE TABLE accounts (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  name            TEXT NOT NULL,
  type            TEXT NOT NULL CHECK (type IN ('cash','checking','saving','investment','crypto')),
  currency        TEXT NOT NULL DEFAULT 'USD' CHECK (currency IN ('USD','EGP')),
  initial_amount_cents INTEGER NOT NULL DEFAULT 0,
  balance_cents   INTEGER NOT NULL DEFAULT 0,
  is_hidden       BOOLEAN NOT NULL DEFAULT false,
  sort_order      SMALLINT NOT NULL DEFAULT 0,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  deleted_at      TIMESTAMPTZ
);

CREATE INDEX accounts_user_id_idx ON accounts(user_id) WHERE deleted_at IS NULL;

-- categories
CREATE TABLE categories (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  name            TEXT NOT NULL,
  type            TEXT NOT NULL CHECK (type IN ('income','expense')),
  icon            TEXT NOT NULL DEFAULT 'circle',
  color           TEXT NOT NULL DEFAULT '#8B5CF6',
  budget_cents    INTEGER,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  deleted_at      TIMESTAMPTZ
);

CREATE INDEX categories_user_id_idx ON categories(user_id) WHERE deleted_at IS NULL;

-- goals (must exist before transfers, which references goals)
CREATE TABLE goals (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  name            TEXT NOT NULL,
  target_cents    INTEGER NOT NULL CHECK (target_cents > 0),
  current_cents   INTEGER NOT NULL DEFAULT 0,
  currency        TEXT NOT NULL DEFAULT 'USD',
  deadline        DATE,
  is_completed    BOOLEAN NOT NULL DEFAULT false,
  icon            TEXT NOT NULL DEFAULT 'flag',
  note            TEXT,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  deleted_at      TIMESTAMPTZ
);

CREATE INDEX goals_user_id_idx ON goals(user_id) WHERE deleted_at IS NULL;

-- transactions
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
  receipt_url     TEXT,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  deleted_at      TIMESTAMPTZ
);

CREATE INDEX transactions_user_date_idx ON transactions(user_id, date DESC) WHERE deleted_at IS NULL;
CREATE INDEX transactions_account_idx   ON transactions(account_id) WHERE deleted_at IS NULL;
CREATE INDEX transactions_category_idx  ON transactions(category_id) WHERE deleted_at IS NULL;

-- transfers
CREATE TABLE transfers (
  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id             UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  from_account_id     UUID NOT NULL REFERENCES accounts(id),
  to_account_id       UUID NOT NULL REFERENCES accounts(id),
  from_amount_cents   INTEGER NOT NULL CHECK (from_amount_cents > 0),
  to_amount_cents     INTEGER NOT NULL CHECK (to_amount_cents > 0),
  from_currency       TEXT NOT NULL CHECK (from_currency IN ('USD','EGP')),
  to_currency         TEXT NOT NULL CHECK (to_currency IN ('USD','EGP')),
  exchange_rate       NUMERIC(10,4) NOT NULL DEFAULT 1.0,
  is_currency_conversion BOOLEAN NOT NULL DEFAULT false,
  goal_id             UUID REFERENCES goals(id),
  note                TEXT,
  date                DATE NOT NULL,
  created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
  deleted_at          TIMESTAMPTZ
);

CREATE INDEX transfers_user_date_idx ON transfers(user_id, date DESC) WHERE deleted_at IS NULL;
CREATE INDEX transfers_from_account_idx ON transfers(from_account_id) WHERE deleted_at IS NULL;
CREATE INDEX transfers_to_account_idx   ON transfers(to_account_id) WHERE deleted_at IS NULL;

-- subscriptions
CREATE TABLE subscriptions (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id           UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  account_id        UUID NOT NULL REFERENCES accounts(id),
  category_id       UUID REFERENCES categories(id),
  name              TEXT NOT NULL,
  amount_cents      INTEGER NOT NULL CHECK (amount_cents > 0),
  currency          TEXT NOT NULL DEFAULT 'USD',
  billing_cycle     TEXT NOT NULL CHECK (billing_cycle IN ('weekly','monthly','yearly')),
  next_billing_date DATE NOT NULL,
  is_active         BOOLEAN NOT NULL DEFAULT true,
  auto_log          BOOLEAN NOT NULL DEFAULT false,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  deleted_at        TIMESTAMPTZ
);

CREATE INDEX subscriptions_user_id_idx ON subscriptions(user_id) WHERE deleted_at IS NULL;

-- settings (single row per user)
CREATE TABLE settings (
  user_id             UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  usd_to_egp_rate     NUMERIC(8,4) NOT NULL DEFAULT 52.0,
  default_account_id  UUID REFERENCES accounts(id),
  analytics_currency  TEXT NOT NULL DEFAULT 'USD',
  created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
