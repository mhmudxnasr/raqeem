-- ============================================================
-- Raqeem — 003_balance_functions.sql
-- Auto-recalculate account balances on transaction/transfer changes.
-- ============================================================

-- Core balance computation function
CREATE OR REPLACE FUNCTION recalculate_account_balance(p_account_id UUID)
RETURNS void AS $$
  UPDATE accounts
  SET balance_cents = (
    SELECT initial_amount_cents +
      COALESCE(SUM(CASE WHEN t.type = 'income'  THEN t.amount_cents ELSE 0 END), 0) -
      COALESCE(SUM(CASE WHEN t.type = 'expense' THEN t.amount_cents ELSE 0 END), 0)
    FROM transactions t
    WHERE t.account_id = p_account_id
      AND t.deleted_at IS NULL
  ) + (
    COALESCE((
      SELECT SUM(to_amount_cents) FROM transfers
      WHERE to_account_id = p_account_id AND deleted_at IS NULL
    ), 0)
  ) - (
    COALESCE((
      SELECT SUM(from_amount_cents) FROM transfers
      WHERE from_account_id = p_account_id AND deleted_at IS NULL
    ), 0)
  ),
  updated_at = now()
  WHERE id = p_account_id;
$$ LANGUAGE sql;

-- Trigger function that calls recalculate for the right account(s)
CREATE OR REPLACE FUNCTION sync_account_balance()
RETURNS TRIGGER AS $$
DECLARE
  affected_account_id UUID;
BEGIN
  IF TG_TABLE_NAME = 'transactions' THEN
    affected_account_id := COALESCE(NEW.account_id, OLD.account_id);
    PERFORM recalculate_account_balance(affected_account_id);

    -- If account changed on UPDATE, also recalculate the old account
    IF TG_OP = 'UPDATE' AND OLD.account_id IS DISTINCT FROM NEW.account_id THEN
      PERFORM recalculate_account_balance(OLD.account_id);
    END IF;

  ELSIF TG_TABLE_NAME = 'transfers' THEN
    PERFORM recalculate_account_balance(COALESCE(NEW.from_account_id, OLD.from_account_id));
    PERFORM recalculate_account_balance(COALESCE(NEW.to_account_id, OLD.to_account_id));

    -- If accounts changed on UPDATE, also recalculate old accounts
    IF TG_OP = 'UPDATE' THEN
      IF OLD.from_account_id IS DISTINCT FROM NEW.from_account_id THEN
        PERFORM recalculate_account_balance(OLD.from_account_id);
      END IF;
      IF OLD.to_account_id IS DISTINCT FROM NEW.to_account_id THEN
        PERFORM recalculate_account_balance(OLD.to_account_id);
      END IF;
    END IF;
  END IF;

  RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger on transactions
CREATE TRIGGER trg_sync_balance_on_transaction
  AFTER INSERT OR UPDATE OR DELETE ON transactions
  FOR EACH ROW EXECUTE FUNCTION sync_account_balance();

-- Trigger on transfers
CREATE TRIGGER trg_sync_balance_on_transfer
  AFTER INSERT OR UPDATE OR DELETE ON transfers
  FOR EACH ROW EXECUTE FUNCTION sync_account_balance();

-- Function to update goal current_cents when a transfer funds a goal
CREATE OR REPLACE FUNCTION sync_goal_funding()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.goal_id IS NOT NULL THEN
    UPDATE goals
    SET current_cents = (
      SELECT COALESCE(SUM(
        CASE WHEN from_currency = 'USD' THEN from_amount_cents
             ELSE ROUND(from_amount_cents / exchange_rate)
        END
      ), 0)
      FROM transfers
      WHERE goal_id = NEW.goal_id AND deleted_at IS NULL
    ),
    updated_at = now()
    WHERE id = NEW.goal_id;
  END IF;

  -- Also handle old goal_id on UPDATE
  IF TG_OP = 'UPDATE' AND OLD.goal_id IS NOT NULL AND OLD.goal_id IS DISTINCT FROM NEW.goal_id THEN
    UPDATE goals
    SET current_cents = (
      SELECT COALESCE(SUM(
        CASE WHEN from_currency = 'USD' THEN from_amount_cents
             ELSE ROUND(from_amount_cents / exchange_rate)
        END
      ), 0)
      FROM transfers
      WHERE goal_id = OLD.goal_id AND deleted_at IS NULL
    ),
    updated_at = now()
    WHERE id = OLD.goal_id;
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER trg_sync_goal_funding
  AFTER INSERT OR UPDATE ON transfers
  FOR EACH ROW EXECUTE FUNCTION sync_goal_funding();

-- updated_at auto-update function
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply updated_at trigger to all tables
CREATE TRIGGER trg_accounts_updated_at BEFORE UPDATE ON accounts FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_categories_updated_at BEFORE UPDATE ON categories FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_transactions_updated_at BEFORE UPDATE ON transactions FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_transfers_updated_at BEFORE UPDATE ON transfers FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_goals_updated_at BEFORE UPDATE ON goals FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_subscriptions_updated_at BEFORE UPDATE ON subscriptions FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_settings_updated_at BEFORE UPDATE ON settings FOR EACH ROW EXECUTE FUNCTION set_updated_at();
