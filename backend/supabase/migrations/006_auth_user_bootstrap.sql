-- ============================================================
-- Raqeem — 006_auth_user_bootstrap.sql
-- Automatically provision default accounts, categories, and
-- settings for every new Supabase auth user.
-- ============================================================

CREATE OR REPLACE FUNCTION public.handle_new_raqeem_user()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  INSERT INTO public.accounts (user_id, name, type, currency, initial_amount_cents, balance_cents, sort_order)
  VALUES
    (NEW.id, 'Binance Free', 'checking', 'USD', 0, 0, 1),
    (NEW.id, 'Binance Savings', 'saving', 'USD', 0, 0, 2),
    (NEW.id, 'Wallet', 'cash', 'EGP', 0, 0, 3),
    (NEW.id, 'Charity', 'checking', 'USD', 0, 0, 4)
  ON CONFLICT DO NOTHING;

  INSERT INTO public.categories (user_id, name, type, icon, color, budget_cents)
  VALUES
    (NEW.id, 'Outlier', 'income', 'briefcase', '#8B5CF6', NULL),
    (NEW.id, 'Alignerr', 'income', 'briefcase', '#8B5CF6', NULL),
    (NEW.id, 'Freelance', 'income', 'code', '#8B5CF6', NULL),
    (NEW.id, 'Other', 'income', 'plus-circle', '#A0A0A0', NULL),
    (NEW.id, 'Food & Dining', 'expense', 'utensils', '#F87171', 12000),
    (NEW.id, 'Transportation', 'expense', 'car', '#FBBF24', 5000),
    (NEW.id, 'Personal Care', 'expense', 'heart', '#8B5CF6', 3000),
    (NEW.id, 'Entertainment', 'expense', 'tv', '#10B981', 3000),
    (NEW.id, 'Utilities', 'expense', 'zap', '#FBBF24', 2000),
    (NEW.id, 'Shopping', 'expense', 'shopping-bag', '#F87171', 8000),
    (NEW.id, 'Education', 'expense', 'book', '#8B5CF6', 5000),
    (NEW.id, 'Health', 'expense', 'activity', '#10B981', 5000),
    (NEW.id, 'Miscellaneous', 'expense', 'circle', '#A0A0A0', 3000)
  ON CONFLICT DO NOTHING;

  INSERT INTO public.settings (user_id, usd_to_egp_rate, analytics_currency)
  VALUES (NEW.id, 52.0, 'USD')
  ON CONFLICT (user_id) DO NOTHING;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_auth_user_bootstrap ON auth.users;

CREATE TRIGGER trg_auth_user_bootstrap
AFTER INSERT ON auth.users
FOR EACH ROW
EXECUTE FUNCTION public.handle_new_raqeem_user();
