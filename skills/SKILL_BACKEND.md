---
name: SKILL_BACKEND
description: >
  Read this for ALL Supabase work: schema migrations, RLS policies,
  PostgreSQL functions, Edge Functions, Storage setup, and query conventions.
---

# Backend Skill — Raqeem
## Supabase / PostgreSQL

---

## Project Setup

```bash
# Install Supabase CLI
npm install -g supabase

# Init
supabase init
supabase start  # local dev

# Link to remote
supabase link --project-ref YOUR_PROJECT_REF
```

All schema changes go in `backend/supabase/migrations/` as numbered SQL files.
Never edit the schema in the Supabase dashboard UI — always use migrations.

---

## Migration File Convention

```
backend/supabase/migrations/
├── 001_initial_schema.sql
├── 002_rls_policies.sql
├── 003_balance_functions.sql
├── 004_seed_data.sql
```

---

## Full Initial Schema Migration (`001_initial_schema.sql`)

See `docs/DATA_MODEL.md` for the complete table definitions.
Copy them in order: accounts → categories → transactions → transfers → goals → subscriptions → settings.

---

## RLS Policies (`002_rls_policies.sql`)

```sql
-- Apply to ALL tables that have user_id

-- Accounts
ALTER TABLE accounts ENABLE ROW LEVEL SECURITY;
CREATE POLICY "owner_only" ON accounts
  USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

-- Same for: categories, transactions, transfers, goals, subscriptions
-- (repeat the pattern for each)

-- Settings (user_id IS the primary key)
ALTER TABLE settings ENABLE ROW LEVEL SECURITY;
CREATE POLICY "owner_only" ON settings
  USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
```

**Every single table must have RLS enabled + policy. No exceptions.**
If you add a new table, add RLS in the same migration.

---

## Balance Trigger (`003_balance_functions.sql`)

```sql
-- Recalculate balance after any transaction change
CREATE OR REPLACE FUNCTION sync_account_balance()
RETURNS TRIGGER AS $$
DECLARE
  affected_account_id UUID;
BEGIN
  -- Determine which account to update
  IF TG_TABLE_NAME = 'transactions' THEN
    affected_account_id := COALESCE(NEW.account_id, OLD.account_id);
  ELSIF TG_TABLE_NAME = 'transfers' THEN
    -- Handle both accounts
    PERFORM recalculate_account_balance(COALESCE(NEW.from_account_id, OLD.from_account_id));
    affected_account_id := COALESCE(NEW.to_account_id, OLD.to_account_id);
  END IF;
  
  PERFORM recalculate_account_balance(affected_account_id);
  RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Attach trigger to transactions
CREATE TRIGGER trg_sync_balance_on_transaction
  AFTER INSERT OR UPDATE OR DELETE ON transactions
  FOR EACH ROW EXECUTE FUNCTION sync_account_balance();

-- Attach trigger to transfers
CREATE TRIGGER trg_sync_balance_on_transfer
  AFTER INSERT OR UPDATE OR DELETE ON transfers
  FOR EACH ROW EXECUTE FUNCTION sync_account_balance();
```

The `recalculate_account_balance` function is in `docs/DATA_MODEL.md`.

---

## Seed Data (`004_seed_data.sql`)

```sql
-- Run once after auth user is created
-- Replace '{{USER_ID}}' with actual auth.uid() from Supabase dashboard

INSERT INTO accounts (id, user_id, name, type, currency, initial_amount_cents, sort_order) VALUES
  (gen_random_uuid(), '{{USER_ID}}', 'Binance Free',    'checking', 'USD', 0, 1),
  (gen_random_uuid(), '{{USER_ID}}', 'Binance Savings', 'saving',   'USD', 0, 2),
  (gen_random_uuid(), '{{USER_ID}}', 'Wallet',          'cash',     'EGP', 0, 3),
  (gen_random_uuid(), '{{USER_ID}}', 'Charity',         'checking', 'USD', 0, 4);

INSERT INTO settings (user_id, usd_to_egp_rate) VALUES ('{{USER_ID}}', 52.0);

-- Income categories
INSERT INTO categories (user_id, name, type, icon, color) VALUES
  ('{{USER_ID}}', 'Outlier',   'income', 'briefcase', '#8B5CF6'),
  ('{{USER_ID}}', 'Alignerr',  'income', 'briefcase', '#8B5CF6'),
  ('{{USER_ID}}', 'Freelance', 'income', 'code',      '#8B5CF6'),
  ('{{USER_ID}}', 'Other',     'income', 'plus-circle','#A0A0A0');

-- Expense categories
INSERT INTO categories (user_id, name, type, icon, color, budget_cents) VALUES
  ('{{USER_ID}}', 'Food & Dining',  'expense', 'utensils',     '#F87171', 12000),
  ('{{USER_ID}}', 'Transportation', 'expense', 'car',          '#FBBF24',  5000),
  ('{{USER_ID}}', 'Personal Care',  'expense', 'heart',        '#8B5CF6',  3000),
  ('{{USER_ID}}', 'Entertainment',  'expense', 'tv',           '#10B981',  3000),
  ('{{USER_ID}}', 'Utilities',      'expense', 'zap',          '#FBBF24',  2000),
  ('{{USER_ID}}', 'Shopping',       'expense', 'shopping-bag', '#F87171',  8000),
  ('{{USER_ID}}', 'Education',      'expense', 'book',         '#8B5CF6',  5000),
  ('{{USER_ID}}', 'Health',         'expense', 'activity',     '#10B981',  5000),
  ('{{USER_ID}}', 'Miscellaneous',  'expense', 'circle',       '#A0A0A0',  3000);
```

---

## Supabase Storage

```sql
-- Create receipts bucket
INSERT INTO storage.buckets (id, name, public)
VALUES ('receipts', 'receipts', false);

-- RLS: only owner can access their receipts
CREATE POLICY "owner_access_receipts" ON storage.objects
  FOR ALL USING (
    bucket_id = 'receipts' AND
    auth.uid()::text = (storage.foldername(name))[1]
  );
```

**File path convention:** `receipts/{user_id}/{transaction_id}.jpg`

---

## Edge Function: AI Insights

```typescript
// backend/supabase/functions/ai-insights/index.ts
import { serve } from 'https://deno.land/std@0.168.0/http/server.ts';
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2';

serve(async (req) => {
  // CORS
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders });
  }
  
  // Auth check
  const authHeader = req.headers.get('Authorization');
  if (!authHeader) return new Response('Unauthorized', { status: 401 });
  
  const supabase = createClient(
    Deno.env.get('SUPABASE_URL')!,
    Deno.env.get('SUPABASE_ANON_KEY')!,
    { global: { headers: { Authorization: authHeader } } }
  );
  
  const { data: { user } } = await supabase.auth.getUser();
  if (!user) return new Response('Unauthorized', { status: 401 });
  
  const { type, month, message, conversation } = await req.json();
  
  // Fetch relevant data (scoped to user via RLS)
  const { data: transactions } = await supabase
    .from('transactions')
    .select('type, amount_cents, currency, date, note, categories(name)')
    .gte('date', `${month}-01`)
    .lte('date', `${month}-31`)
    .order('date', { ascending: true });
  
  // Build context and call Groq
  const groqKey = Deno.env.get('GROQ_API_KEY')!;
  const context = buildContext(transactions ?? []);
  const prompt = type === 'chat' ? buildChatPrompt(context, message, conversation) 
                                 : buildInsightPrompt(context);
  
  const groqResponse = await fetch('https://api.groq.com/openai/v1/chat/completions', {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${groqKey}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({
      model: 'llama3-70b-8192',
      messages: prompt,
      max_tokens: 800,
      temperature: 0.3,
    }),
  });
  
  const result = await groqResponse.json();
  const content = result.choices[0].message.content;
  
  return new Response(JSON.stringify({ content }), {
    headers: { ...corsHeaders, 'Content-Type': 'application/json' },
  });
});

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
};
```

**Environment variables (Supabase dashboard → Settings → Edge Functions):**
- `GROQ_API_KEY` — from console.groq.com

---

## Query Conventions

1. Always name columns: `select('id, name, type, amount_cents')` not `select('*')`
2. Always add `.limit()` to list queries — never unbounded
3. Always handle the `error` from Supabase responses
4. For paginated lists: use `.range(from, to)` with cursor from the client
5. For real-time, always unsubscribe in cleanup

```typescript
// Correct pattern
const { data, error } = await supabase
  .from('transactions')
  .select('id, account_id, type, amount_cents, currency, date, note')
  .eq('user_id', userId)
  .is('deleted_at', null)
  .order('date', { ascending: false })
  .limit(50);

if (error) throw new Error(`Supabase error: ${error.message}`);
```

---

## Soft Delete Convention

Never hard-delete records. Always:
```sql
UPDATE transactions SET deleted_at = now() WHERE id = $1;
```

All queries must include `.is('deleted_at', null)` filter.
Add a helper:
```typescript
const activeRecords = (query: any) => query.is('deleted_at', null);
```
