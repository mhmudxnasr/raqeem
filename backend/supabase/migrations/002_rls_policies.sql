-- ============================================================
-- Raqeem — 002_rls_policies.sql
-- Row Level Security: every table locked to owner only.
-- ============================================================

-- accounts
ALTER TABLE accounts ENABLE ROW LEVEL SECURITY;
CREATE POLICY "owner_only" ON accounts
  USING (auth.uid() = user_id)
  WITH CHECK (auth.uid() = user_id);

-- categories
ALTER TABLE categories ENABLE ROW LEVEL SECURITY;
CREATE POLICY "owner_only" ON categories
  USING (auth.uid() = user_id)
  WITH CHECK (auth.uid() = user_id);

-- transactions
ALTER TABLE transactions ENABLE ROW LEVEL SECURITY;
CREATE POLICY "owner_only" ON transactions
  USING (auth.uid() = user_id)
  WITH CHECK (auth.uid() = user_id);

-- transfers
ALTER TABLE transfers ENABLE ROW LEVEL SECURITY;
CREATE POLICY "owner_only" ON transfers
  USING (auth.uid() = user_id)
  WITH CHECK (auth.uid() = user_id);

-- goals
ALTER TABLE goals ENABLE ROW LEVEL SECURITY;
CREATE POLICY "owner_only" ON goals
  USING (auth.uid() = user_id)
  WITH CHECK (auth.uid() = user_id);

-- subscriptions
ALTER TABLE subscriptions ENABLE ROW LEVEL SECURITY;
CREATE POLICY "owner_only" ON subscriptions
  USING (auth.uid() = user_id)
  WITH CHECK (auth.uid() = user_id);

-- settings (user_id is the primary key)
ALTER TABLE settings ENABLE ROW LEVEL SECURITY;
CREATE POLICY "owner_only" ON settings
  USING (auth.uid() = user_id)
  WITH CHECK (auth.uid() = user_id);
