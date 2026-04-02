import { mockFinanceSnapshot } from './mock-data';
import { isSupabaseConfigured, supabase } from './supabase';
import type { FinanceSnapshot, NewTransactionInput, NewTransferInput, Settings, Transaction, Transfer } from '../types';
import type { Database } from '../types/supabase';

const DEMO_STORAGE_KEY = 'raqeem-web-demo-db-v1';

function cloneSnapshot<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T;
}

function nowIso(): string {
  return new Date().toISOString();
}

function normalizeSnapshot(snapshot: FinanceSnapshot): FinanceSnapshot {
  const normalized = cloneSnapshot(snapshot);
  const accountBalances = new Map(normalized.accounts.map((account) => [account.id, account.initialAmountCents]));
  const goalBalances = new Map(normalized.goals.map((goal) => [goal.id, 0]));

  for (const transaction of normalized.transactions) {
    if (transaction.deletedAt !== null) {
      continue;
    }

    const current = accountBalances.get(transaction.accountId) ?? 0;
    const next = transaction.type === 'income' ? current + transaction.amountCents : current - transaction.amountCents;
    accountBalances.set(transaction.accountId, next);
  }

  for (const transfer of normalized.transfers) {
    if (transfer.deletedAt !== null) {
      continue;
    }

    accountBalances.set(transfer.fromAccountId, (accountBalances.get(transfer.fromAccountId) ?? 0) - transfer.fromAmountCents);
    accountBalances.set(transfer.toAccountId, (accountBalances.get(transfer.toAccountId) ?? 0) + transfer.toAmountCents);

    if (transfer.goalId) {
      const fundedCents = transfer.fromCurrency === 'USD'
        ? transfer.fromAmountCents
        : Math.round(transfer.fromAmountCents / transfer.exchangeRate);
      goalBalances.set(transfer.goalId, (goalBalances.get(transfer.goalId) ?? 0) + fundedCents);
    }
  }

  normalized.accounts = normalized.accounts.map((account) => ({
    ...account,
    balanceCents: accountBalances.get(account.id) ?? account.initialAmountCents,
  }));

  normalized.goals = normalized.goals.map((goal) => {
    const currentCents = goalBalances.get(goal.id) ?? 0;
    return {
      ...goal,
      currentCents,
      isCompleted: currentCents >= goal.targetCents,
    };
  });

  return normalized;
}

function loadDemoSnapshot(): FinanceSnapshot {
  if (typeof window === 'undefined') {
    return normalizeSnapshot(mockFinanceSnapshot);
  }

  const stored = window.localStorage.getItem(DEMO_STORAGE_KEY);
  if (!stored) {
    return normalizeSnapshot(mockFinanceSnapshot);
  }

  try {
    return normalizeSnapshot(JSON.parse(stored) as FinanceSnapshot);
  } catch {
    return normalizeSnapshot(mockFinanceSnapshot);
  }
}

let demoSnapshot = loadDemoSnapshot();

function persistDemoSnapshot(snapshot: FinanceSnapshot): void {
  demoSnapshot = normalizeSnapshot(snapshot);
  if (typeof window !== 'undefined') {
    window.localStorage.setItem(DEMO_STORAGE_KEY, JSON.stringify(demoSnapshot));
  }
}

function mapAccount(row: Database['public']['Tables']['accounts']['Row']) {
  return {
    id: row.id,
    name: row.name,
    type: row.type,
    currency: row.currency,
    initialAmountCents: row.initial_amount_cents,
    balanceCents: row.balance_cents,
    isHidden: row.is_hidden,
    sortOrder: row.sort_order,
    createdAt: row.created_at,
    updatedAt: row.updated_at,
    deletedAt: row.deleted_at,
  };
}

function mapCategory(row: Database['public']['Tables']['categories']['Row']) {
  return {
    id: row.id,
    name: row.name,
    type: row.type,
    icon: row.icon,
    color: row.color,
    budgetCents: row.budget_cents,
    createdAt: row.created_at,
    updatedAt: row.updated_at,
    deletedAt: row.deleted_at,
  };
}

function mapTransaction(row: Database['public']['Tables']['transactions']['Row']): Transaction {
  return {
    id: row.id,
    accountId: row.account_id,
    categoryId: row.category_id,
    type: row.type,
    amountCents: row.amount_cents,
    currency: row.currency,
    note: row.note,
    date: row.date,
    receiptUrl: row.receipt_url,
    createdAt: row.created_at,
    updatedAt: row.updated_at,
    deletedAt: row.deleted_at,
  };
}

function mapTransfer(row: Database['public']['Tables']['transfers']['Row']): Transfer {
  return {
    id: row.id,
    fromAccountId: row.from_account_id,
    toAccountId: row.to_account_id,
    fromAmountCents: row.from_amount_cents,
    toAmountCents: row.to_amount_cents,
    fromCurrency: row.from_currency,
    toCurrency: row.to_currency,
    exchangeRate: Number(row.exchange_rate),
    isCurrencyConversion: row.is_currency_conversion,
    goalId: row.goal_id,
    note: row.note,
    date: row.date,
    createdAt: row.created_at,
    updatedAt: row.updated_at,
    deletedAt: row.deleted_at,
  };
}

function mapGoal(row: Database['public']['Tables']['goals']['Row']) {
  return {
    id: row.id,
    name: row.name,
    targetCents: row.target_cents,
    currentCents: row.current_cents,
    currency: row.currency,
    deadline: row.deadline,
    isCompleted: row.is_completed,
    icon: row.icon,
    note: row.note,
    createdAt: row.created_at,
    updatedAt: row.updated_at,
    deletedAt: row.deleted_at,
  };
}

function mapSubscription(row: Database['public']['Tables']['subscriptions']['Row']) {
  return {
    id: row.id,
    accountId: row.account_id,
    categoryId: row.category_id,
    name: row.name,
    amountCents: row.amount_cents,
    currency: row.currency,
    billingCycle: row.billing_cycle,
    nextBillingDate: row.next_billing_date,
    isActive: row.is_active,
    autoLog: row.auto_log,
    createdAt: row.created_at,
    updatedAt: row.updated_at,
    deletedAt: row.deleted_at,
  };
}

function mapSettings(row: Database['public']['Tables']['settings']['Row']): Settings {
  return {
    usdToEgpRate: Number(row.usd_to_egp_rate),
    defaultAccountId: row.default_account_id,
    analyticsCurrency: row.analytics_currency,
    createdAt: row.created_at,
    updatedAt: row.updated_at,
  };
}

function getClient() {
  if (!isSupabaseConfigured || !supabase) {
    return null;
  }

  return supabase;
}

async function getCurrentUserId(): Promise<string> {
  if (!supabase) {
    throw new Error('Supabase is not configured.');
  }

  const {
    data: { user },
    error,
  } = await supabase.auth.getUser();

  if (error || !user) {
    throw new Error('Your session has expired. Please sign in again.');
  }

  return user.id;
}

export async function fetchFinanceSnapshot(): Promise<FinanceSnapshot> {
  const client = getClient();
  if (!client) {
    return normalizeSnapshot(demoSnapshot);
  }

  const [
    accountsResult,
    categoriesResult,
    transactionsResult,
    transfersResult,
    goalsResult,
    subscriptionsResult,
    settingsResult,
  ] = await Promise.all([
    client
      .from('accounts')
      .select('id, user_id, name, type, currency, initial_amount_cents, balance_cents, is_hidden, sort_order, created_at, updated_at, deleted_at')
      .is('deleted_at', null)
      .order('sort_order', { ascending: true }),
    client
      .from('categories')
      .select('id, user_id, name, type, icon, color, budget_cents, created_at, updated_at, deleted_at')
      .is('deleted_at', null)
      .order('name', { ascending: true }),
    client
      .from('transactions')
      .select('id, user_id, account_id, category_id, type, amount_cents, currency, note, date, receipt_url, created_at, updated_at, deleted_at')
      .is('deleted_at', null)
      .order('date', { ascending: false }),
    client
      .from('transfers')
      .select('id, user_id, from_account_id, to_account_id, from_amount_cents, to_amount_cents, from_currency, to_currency, exchange_rate, is_currency_conversion, goal_id, note, date, created_at, updated_at, deleted_at')
      .is('deleted_at', null)
      .order('date', { ascending: false }),
    client
      .from('goals')
      .select('id, user_id, name, target_cents, current_cents, currency, deadline, is_completed, icon, note, created_at, updated_at, deleted_at')
      .is('deleted_at', null)
      .order('created_at', { ascending: true }),
    client
      .from('subscriptions')
      .select('id, user_id, account_id, category_id, name, amount_cents, currency, billing_cycle, next_billing_date, is_active, auto_log, created_at, updated_at, deleted_at')
      .is('deleted_at', null)
      .order('next_billing_date', { ascending: true }),
    client
      .from('settings')
      .select('user_id, usd_to_egp_rate, default_account_id, analytics_currency, created_at, updated_at')
      .single(),
  ]);

  if (accountsResult.error) throw new Error('Failed to load accounts.');
  if (categoriesResult.error) throw new Error('Failed to load categories.');
  if (transactionsResult.error) throw new Error('Failed to load transactions.');
  if (transfersResult.error) throw new Error('Failed to load transfers.');
  if (goalsResult.error) throw new Error('Failed to load goals.');
  if (subscriptionsResult.error) throw new Error('Failed to load subscriptions.');
  if (settingsResult.error) throw new Error('Failed to load settings.');

  return normalizeSnapshot({
    accounts: (accountsResult.data ?? []).map(mapAccount),
    categories: (categoriesResult.data ?? []).map(mapCategory),
    transactions: (transactionsResult.data ?? []).map(mapTransaction),
    transfers: (transfersResult.data ?? []).map(mapTransfer),
    goals: (goalsResult.data ?? []).map(mapGoal),
    subscriptions: (subscriptionsResult.data ?? []).map(mapSubscription),
    settings: mapSettings(settingsResult.data),
  });
}

export async function createTransaction(input: NewTransactionInput): Promise<void> {
  const client = getClient();
  if (!client) {
    const snapshot = cloneSnapshot(demoSnapshot);
    snapshot.transactions.unshift({
      id: crypto.randomUUID(),
      accountId: input.accountId,
      categoryId: input.categoryId,
      type: input.type,
      amountCents: input.amountCents,
      currency: input.currency,
      note: input.note,
      date: input.date,
      receiptUrl: null,
      createdAt: nowIso(),
      updatedAt: nowIso(),
      deletedAt: null,
    });
    persistDemoSnapshot(snapshot);
    return;
  }

  const userId = await getCurrentUserId();
  const { error } = await client.from('transactions').insert({
    user_id: userId,
    account_id: input.accountId,
    category_id: input.categoryId,
    type: input.type,
    amount_cents: input.amountCents,
    currency: input.currency,
    note: input.note,
    date: input.date,
  });

  if (error) {
    throw new Error('Failed to create the transaction.');
  }
}

export async function updateTransaction(input: NewTransactionInput & { id: string }): Promise<void> {
  const client = getClient();
  if (!client) {
    const snapshot = cloneSnapshot(demoSnapshot);
    snapshot.transactions = snapshot.transactions.map((transaction) =>
      transaction.id === input.id
        ? {
            ...transaction,
            accountId: input.accountId,
            categoryId: input.categoryId,
            type: input.type,
            amountCents: input.amountCents,
            currency: input.currency,
            note: input.note,
            date: input.date,
            updatedAt: nowIso(),
          }
        : transaction,
    );
    persistDemoSnapshot(snapshot);
    return;
  }

  const { error } = await client
    .from('transactions')
    .update({
      account_id: input.accountId,
      category_id: input.categoryId,
      type: input.type,
      amount_cents: input.amountCents,
      currency: input.currency,
      note: input.note,
      date: input.date,
    })
    .eq('id', input.id);

  if (error) {
    throw new Error('Failed to update the transaction.');
  }
}

export async function softDeleteTransaction(id: string): Promise<void> {
  const client = getClient();
  if (!client) {
    const snapshot = cloneSnapshot(demoSnapshot);
    snapshot.transactions = snapshot.transactions.map((transaction) =>
      transaction.id === id ? { ...transaction, deletedAt: nowIso(), updatedAt: nowIso() } : transaction,
    );
    persistDemoSnapshot(snapshot);
    return;
  }

  const { error } = await client.from('transactions').update({ deleted_at: nowIso() }).eq('id', id);
  if (error) {
    throw new Error('Failed to delete the transaction.');
  }
}

export async function createTransfer(input: NewTransferInput): Promise<void> {
  const client = getClient();
  if (!client) {
    const snapshot = cloneSnapshot(demoSnapshot);
    snapshot.transfers.unshift({
      id: crypto.randomUUID(),
      fromAccountId: input.fromAccountId,
      toAccountId: input.toAccountId,
      fromAmountCents: input.fromAmountCents,
      toAmountCents: input.toAmountCents,
      fromCurrency: input.fromCurrency,
      toCurrency: input.toCurrency,
      exchangeRate: input.exchangeRate,
      isCurrencyConversion: input.fromCurrency !== input.toCurrency,
      goalId: input.goalId,
      note: input.note,
      date: input.date,
      createdAt: nowIso(),
      updatedAt: nowIso(),
      deletedAt: null,
    });
    persistDemoSnapshot(snapshot);
    return;
  }

  const userId = await getCurrentUserId();
  const { error } = await client.from('transfers').insert({
    user_id: userId,
    from_account_id: input.fromAccountId,
    to_account_id: input.toAccountId,
    from_amount_cents: input.fromAmountCents,
    to_amount_cents: input.toAmountCents,
    from_currency: input.fromCurrency,
    to_currency: input.toCurrency,
    exchange_rate: input.exchangeRate,
    is_currency_conversion: input.fromCurrency !== input.toCurrency,
    goal_id: input.goalId,
    note: input.note,
    date: input.date,
  });

  if (error) {
    throw new Error('Failed to create the transfer.');
  }
}

export async function updateSettings(changes: Partial<Settings>): Promise<void> {
  const client = getClient();
  if (!client) {
    const snapshot = cloneSnapshot(demoSnapshot);
    snapshot.settings = {
      ...snapshot.settings,
      ...changes,
      updatedAt: nowIso(),
    };
    persistDemoSnapshot(snapshot);
    return;
  }

  const userId = await getCurrentUserId();
  const payload: Database['public']['Tables']['settings']['Update'] = {};

  if (typeof changes.usdToEgpRate === 'number') payload.usd_to_egp_rate = changes.usdToEgpRate;
  if (typeof changes.defaultAccountId !== 'undefined') payload.default_account_id = changes.defaultAccountId;
  if (typeof changes.analyticsCurrency !== 'undefined') payload.analytics_currency = changes.analyticsCurrency;

  const { error } = await client.from('settings').update(payload).eq('user_id', userId);
  if (error) {
    throw new Error('Failed to update settings.');
  }
}
