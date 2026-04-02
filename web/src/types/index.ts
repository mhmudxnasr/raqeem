export type Currency = 'USD' | 'EGP';
export type AccountType = 'cash' | 'checking' | 'saving' | 'investment' | 'crypto';
export type TransactionType = 'income' | 'expense';
export type BillingCycle = 'weekly' | 'monthly' | 'yearly';
export type QuickAddMode = TransactionType | 'transfer';

export interface UserSummary {
  id: string;
  email: string;
  displayName: string;
}

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
  updatedAt: string;
  deletedAt: string | null;
}

export interface Category {
  id: string;
  name: string;
  type: TransactionType;
  icon: string;
  color: string;
  budgetCents: number | null;
  createdAt: string;
  updatedAt: string;
  deletedAt: string | null;
}

export interface Transaction {
  id: string;
  accountId: string;
  categoryId: string | null;
  type: TransactionType;
  amountCents: number;
  currency: Currency;
  note: string | null;
  date: string;
  receiptUrl: string | null;
  createdAt: string;
  updatedAt: string;
  deletedAt: string | null;
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
  updatedAt: string;
  deletedAt: string | null;
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
  note: string | null;
  createdAt: string;
  updatedAt: string;
  deletedAt: string | null;
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
  createdAt: string;
  updatedAt: string;
  deletedAt: string | null;
}

export interface Settings {
  usdToEgpRate: number;
  defaultAccountId: string | null;
  analyticsCurrency: Currency;
  createdAt: string;
  updatedAt: string;
}

export interface FinanceSnapshot {
  accounts: Account[];
  categories: Category[];
  transactions: Transaction[];
  transfers: Transfer[];
  goals: Goal[];
  subscriptions: Subscription[];
  settings: Settings;
}

export interface NewTransactionInput {
  type: TransactionType;
  amountCents: number;
  accountId: string;
  categoryId: string | null;
  currency: Currency;
  note: string | null;
  date: string;
}

export interface NewTransferInput {
  fromAccountId: string;
  toAccountId: string;
  fromAmountCents: number;
  toAmountCents: number;
  fromCurrency: Currency;
  toCurrency: Currency;
  exchangeRate: number;
  goalId: string | null;
  note: string | null;
  date: string;
}

export interface FinanceFilters {
  accountId: string;
  categoryId: string;
  entryType: 'all' | TransactionType | 'transfer';
  search: string;
  dateFrom: string;
  dateTo: string;
}

export interface LedgerTransactionEntry {
  kind: 'transaction';
  id: string;
  date: string;
  createdAt: string;
  type: TransactionType;
  title: string;
  subtitle: string;
  amountCents: number;
  currency: Currency;
  accountName: string;
  categoryName: string;
  note: string | null;
  raw: Transaction;
}

export interface LedgerTransferEntry {
  kind: 'transfer';
  id: string;
  date: string;
  createdAt: string;
  type: 'transfer';
  title: string;
  subtitle: string;
  amountCents: number;
  currency: Currency;
  accountName: string;
  counterpartyName: string;
  note: string | null;
  secondaryAmountCents: number;
  secondaryCurrency: Currency;
  raw: Transfer;
}

export type LedgerEntry = LedgerTransactionEntry | LedgerTransferEntry;

export interface CategorySpendPoint {
  categoryId: string;
  name: string;
  amountCents: number;
  color: string;
}

export interface BudgetSummary {
  categoryId: string;
  name: string;
  spentCents: number;
  budgetCents: number;
  percentUsed: number;
  status: 'safe' | 'warning' | 'danger';
  color: string;
}

export interface MonthlySeriesPoint {
  month: string;
  label: string;
  incomeCents: number;
  expenseCents: number;
  netWorthCents: number;
}

export interface AIChatMessage {
  role: 'user' | 'assistant';
  content: string;
}

export interface AIRequestPayload {
  type: 'monthly_insight' | 'chat';
  month?: string;
  message?: string;
  conversation?: AIChatMessage[];
}

export interface AIResponsePayload {
  content: string;
  source: 'mock' | 'supabase';
}
