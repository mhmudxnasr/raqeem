import { compareDesc, eachMonthOfInterval, format, isWithinInterval, parseISO, startOfMonth, subDays, subMonths } from 'date-fns';

import { formatAmount, formatMonthLabel, monthKeyFromDate } from './format';
import type {
  Account,
  BudgetSummary,
  Category,
  CategorySpendPoint,
  FinanceFilters,
  FinanceSnapshot,
  LedgerEntry,
  MonthlySeriesPoint,
  Transaction,
} from '../types';

export function toUSD(cents: number, currency: 'USD' | 'EGP', usdToEgpRate: number): number {
  if (currency === 'USD') {
    return cents;
  }

  return Math.round(cents / usdToEgpRate);
}

export function getCurrentMonthKey(reference: Date = new Date()): string {
  return reference.toISOString().slice(0, 7);
}

export function calculateNetWorthCents(snapshot: FinanceSnapshot): number {
  return snapshot.accounts
    .filter((account) => account.deletedAt === null)
    .reduce((total, account) => total + toUSD(account.balanceCents, account.currency, snapshot.settings.usdToEgpRate), 0);
}

export function buildLedgerEntries(snapshot: FinanceSnapshot): LedgerEntry[] {
  const accountsById = new Map(snapshot.accounts.map((account) => [account.id, account]));
  const categoriesById = new Map(snapshot.categories.map((category) => [category.id, category]));

  const transactionEntries: LedgerEntry[] = snapshot.transactions
    .filter((transaction) => transaction.deletedAt === null)
    .map((transaction) => {
      const account = accountsById.get(transaction.accountId);
      const category = transaction.categoryId ? categoriesById.get(transaction.categoryId) : null;

      return {
        kind: 'transaction',
        id: transaction.id,
        date: transaction.date,
        createdAt: transaction.createdAt,
        type: transaction.type,
        title: category?.name ?? 'Uncategorized',
        subtitle: account?.name ?? 'Unknown account',
        amountCents: transaction.type === 'expense' ? -transaction.amountCents : transaction.amountCents,
        currency: transaction.currency,
        accountName: account?.name ?? 'Unknown account',
        categoryName: category?.name ?? 'Uncategorized',
        note: transaction.note,
        raw: transaction,
      };
    });

  const transferEntries: LedgerEntry[] = snapshot.transfers
    .filter((transfer) => transfer.deletedAt === null)
    .map((transfer) => {
      const fromAccount = accountsById.get(transfer.fromAccountId);
      const toAccount = accountsById.get(transfer.toAccountId);

      return {
        kind: 'transfer',
        id: transfer.id,
        date: transfer.date,
        createdAt: transfer.createdAt,
        type: 'transfer',
        title: `${fromAccount?.name ?? 'Unknown'} → ${toAccount?.name ?? 'Unknown'}`,
        subtitle: transfer.goalId ? 'Goal funding transfer' : 'Internal transfer',
        amountCents: -transfer.fromAmountCents,
        currency: transfer.fromCurrency,
        accountName: fromAccount?.name ?? 'Unknown account',
        counterpartyName: toAccount?.name ?? 'Unknown account',
        note: transfer.note,
        secondaryAmountCents: transfer.toAmountCents,
        secondaryCurrency: transfer.toCurrency,
        raw: transfer,
      };
    });

  return [...transactionEntries, ...transferEntries].sort((left, right) =>
    compareDesc(parseISO(left.date), parseISO(right.date)),
  );
}

export function getAvailableMonths(snapshot: FinanceSnapshot): string[] {
  const months = new Set<string>();

  for (const transaction of snapshot.transactions) {
    if (transaction.deletedAt === null) {
      months.add(monthKeyFromDate(transaction.date));
    }
  }

  for (const transfer of snapshot.transfers) {
    if (transfer.deletedAt === null) {
      months.add(monthKeyFromDate(transfer.date));
    }
  }

  return [...months].sort((left, right) => (left > right ? -1 : left < right ? 1 : 0));
}

export function filterLedgerEntries(entries: LedgerEntry[], filters: FinanceFilters): LedgerEntry[] {
  const search = filters.search.trim().toLowerCase();

  return entries.filter((entry) => {
    if (filters.entryType !== 'all' && entry.type !== filters.entryType) {
      return false;
    }

    if (filters.accountId.length > 0) {
      const accountId = entry.kind === 'transaction' ? entry.raw.accountId : entry.raw.fromAccountId;
      if (accountId !== filters.accountId) {
        return false;
      }
    }

    if (filters.categoryId.length > 0) {
      if (entry.kind === 'transfer' || entry.raw.categoryId !== filters.categoryId) {
        return false;
      }
    }

    if (filters.dateFrom.length > 0 && entry.date < filters.dateFrom) {
      return false;
    }

    if (filters.dateTo.length > 0 && entry.date > filters.dateTo) {
      return false;
    }

    if (search.length > 0) {
      const haystack = [
        entry.title,
        entry.subtitle,
        entry.accountName,
        entry.note ?? '',
        entry.kind === 'transaction' ? entry.categoryName : entry.counterpartyName,
      ]
        .join(' ')
        .toLowerCase();

      if (!haystack.includes(search) && !formatAmount(entry.amountCents, entry.currency).toLowerCase().includes(search)) {
        return false;
      }
    }

    return true;
  });
}

export function getMonthTransactions(snapshot: FinanceSnapshot, month: string): Transaction[] {
  return snapshot.transactions.filter(
    (transaction) => transaction.deletedAt === null && monthKeyFromDate(transaction.date) === month,
  );
}

export function getBudgetSummaries(snapshot: FinanceSnapshot, month: string): BudgetSummary[] {
  const monthTransactions = getMonthTransactions(snapshot, month);

  return snapshot.categories
    .filter((category) => category.deletedAt === null && category.type === 'expense' && category.budgetCents !== null)
    .map((category) => {
      const spentCents = monthTransactions
        .filter((transaction) => transaction.type === 'expense' && transaction.categoryId === category.id)
        .reduce((sum, transaction) => sum + toUSD(transaction.amountCents, transaction.currency, snapshot.settings.usdToEgpRate), 0);
      const budgetCents = category.budgetCents ?? 0;
      const percentUsed = budgetCents === 0 ? 0 : Math.round((spentCents / budgetCents) * 100);

      let status: BudgetSummary['status'] = 'safe';
      if (percentUsed >= 100) {
        status = 'danger';
      } else if (percentUsed >= 80) {
        status = 'warning';
      }

      return {
        categoryId: category.id,
        name: category.name,
        spentCents,
        budgetCents,
        percentUsed,
        status,
        color: category.color,
      };
    })
    .sort((left, right) => right.spentCents - left.spentCents);
}

export function getSpendingByCategory(snapshot: FinanceSnapshot, month: string): CategorySpendPoint[] {
  return getBudgetSummaries(snapshot, month)
    .filter((summary) => summary.spentCents > 0)
    .map((summary) => ({
      categoryId: summary.categoryId,
      name: summary.name,
      amountCents: summary.spentCents,
      color: summary.color,
    }));
}

export function getMonthlyTotals(snapshot: FinanceSnapshot, month: string): {
  incomeCents: number;
  expenseCents: number;
  savingsCents: number;
  savingsRate: number;
} {
  const monthTransactions = getMonthTransactions(snapshot, month);
  const incomeCents = monthTransactions
    .filter((transaction) => transaction.type === 'income')
    .reduce((sum, transaction) => sum + toUSD(transaction.amountCents, transaction.currency, snapshot.settings.usdToEgpRate), 0);
  const expenseCents = monthTransactions
    .filter((transaction) => transaction.type === 'expense')
    .reduce((sum, transaction) => sum + toUSD(transaction.amountCents, transaction.currency, snapshot.settings.usdToEgpRate), 0);
  const savingsCents = incomeCents - expenseCents;

  return {
    incomeCents,
    expenseCents,
    savingsCents,
    savingsRate: incomeCents === 0 ? 0 : Math.max(0, Math.round((savingsCents / incomeCents) * 100)),
  };
}

export function getDashboardStats(snapshot: FinanceSnapshot, month: string): {
  todaySpendCents: number;
  budgetUsedPercent: number;
  totalBudgetCents: number;
  totalSpentCents: number;
} {
  const today = new Date().toISOString().slice(0, 10);
  const todaySpendCents = snapshot.transactions
    .filter((transaction) => transaction.deletedAt === null && transaction.type === 'expense' && transaction.date === today)
    .reduce((sum, transaction) => sum + toUSD(transaction.amountCents, transaction.currency, snapshot.settings.usdToEgpRate), 0);

  const budgets = getBudgetSummaries(snapshot, month);
  const totalBudgetCents = budgets.reduce((sum, summary) => sum + summary.budgetCents, 0);
  const totalSpentCents = budgets.reduce((sum, summary) => sum + summary.spentCents, 0);

  return {
    todaySpendCents,
    budgetUsedPercent: totalBudgetCents === 0 ? 0 : Math.round((totalSpentCents / totalBudgetCents) * 100),
    totalBudgetCents,
    totalSpentCents,
  };
}

export function getIncomeExpenseSeries(snapshot: FinanceSnapshot, months = 6): MonthlySeriesPoint[] {
  const currentMonth = startOfMonth(new Date());
  const monthRange = eachMonthOfInterval({
    start: subMonths(currentMonth, months - 1),
    end: currentMonth,
  });

  const currentNetWorth = calculateNetWorthCents(snapshot);
  const monthlyChange = new Map<string, number>();
  const series = monthRange.map((date) => {
    const month = date.toISOString().slice(0, 7);
    const totals = getMonthlyTotals(snapshot, month);
    monthlyChange.set(month, totals.savingsCents);

    return {
      month,
      label: format(date, 'MMM'),
      incomeCents: totals.incomeCents,
      expenseCents: totals.expenseCents,
      netWorthCents: 0,
    };
  });

  let runningNetWorth = currentNetWorth;
  for (const point of [...series].reverse()) {
    point.netWorthCents = runningNetWorth;
    runningNetWorth -= monthlyChange.get(point.month) ?? 0;
  }

  return series;
}

export function getAccountMonthlyStats(snapshot: FinanceSnapshot, accountId: string, month: string): {
  incomeCents: number;
  expenseCents: number;
} {
  const transactions = getMonthTransactions(snapshot, month).filter((transaction) => transaction.accountId === accountId);

  return {
    incomeCents: transactions
      .filter((transaction) => transaction.type === 'income')
      .reduce((sum, transaction) => sum + transaction.amountCents, 0),
    expenseCents: transactions
      .filter((transaction) => transaction.type === 'expense')
      .reduce((sum, transaction) => sum + transaction.amountCents, 0),
  };
}

export function getAccountSparkline(snapshot: FinanceSnapshot, account: Account, days = 30): number[] {
  const end = new Date();
  const points: number[] = [];
  let running = 0;

  for (let index = 0; index < days; index += 1) {
    const day = subDays(end, days - index - 1);
    const dayStart = new Date(day);
    dayStart.setHours(0, 0, 0, 0);
    const dayEnd = new Date(day);
    dayEnd.setHours(23, 59, 59, 999);

    const transactionDelta = snapshot.transactions
      .filter(
        (transaction) =>
          transaction.deletedAt === null &&
          transaction.accountId === account.id &&
          isWithinInterval(parseISO(transaction.date), { start: dayStart, end: dayEnd }),
      )
      .reduce((sum, transaction) => sum + (transaction.type === 'income' ? transaction.amountCents : -transaction.amountCents), 0);

    const incomingTransfers = snapshot.transfers
      .filter(
        (transfer) =>
          transfer.deletedAt === null &&
          transfer.toAccountId === account.id &&
          isWithinInterval(parseISO(transfer.date), { start: dayStart, end: dayEnd }),
      )
      .reduce((sum, transfer) => sum + transfer.toAmountCents, 0);

    const outgoingTransfers = snapshot.transfers
      .filter(
        (transfer) =>
          transfer.deletedAt === null &&
          transfer.fromAccountId === account.id &&
          isWithinInterval(parseISO(transfer.date), { start: dayStart, end: dayEnd }),
      )
      .reduce((sum, transfer) => sum + transfer.fromAmountCents, 0);

    running += transactionDelta + incomingTransfers - outgoingTransfers;
    points.push(running);
  }

  return points;
}

export function getSubscriptionMonthlyTotal(snapshot: FinanceSnapshot): number {
  return snapshot.subscriptions
    .filter((subscription) => subscription.deletedAt === null && subscription.isActive)
    .reduce((sum, subscription) => {
      if (subscription.billingCycle === 'yearly') {
        return sum + Math.round(subscription.amountCents / 12);
      }

      if (subscription.billingCycle === 'weekly') {
        return sum + Math.round((subscription.amountCents * 52) / 12);
      }

      return sum + subscription.amountCents;
    }, 0);
}

export function buildMonthlyInsights(snapshot: FinanceSnapshot, month: string): string[] {
  const budgets = getBudgetSummaries(snapshot, month);
  const totals = getMonthlyTotals(snapshot, month);
  const topCategory = budgets[0];
  const wallet = snapshot.accounts.find((account) => account.name === 'Wallet');
  const walletUsd = wallet ? toUSD(wallet.balanceCents, wallet.currency, snapshot.settings.usdToEgpRate) : 0;
  const items: string[] = [];

  if (topCategory) {
    items.push(
      `${topCategory.name} is the biggest line item at ${formatAmount(topCategory.spentCents, 'USD')} (${topCategory.percentUsed}% of budget).`,
    );
  }

  items.push(
    `Income is ${formatAmount(totals.incomeCents, 'USD')} against ${formatAmount(-totals.expenseCents, 'USD')} in spend, leaving a ${totals.savingsRate}% savings rate.`,
  );

  const overBudget = budgets.find((summary) => summary.status === 'danger');
  if (overBudget) {
    items.push(`${overBudget.name} is already over budget, so that category needs the next correction.`);
  } else {
    const nearLimit = budgets.find((summary) => summary.status === 'warning');
    if (nearLimit) {
      items.push(`${nearLimit.name} is close to the limit, so the rest of the month should stay light there.`);
    }
  }

  if (wallet) {
    items.push(`Wallet cash sits at ${formatAmount(wallet.balanceCents, 'EGP')} (~${formatAmount(walletUsd, 'USD')}) after recent transport spend.`);
  }

  return items.slice(0, 4);
}

export function answerFinanceQuestion(snapshot: FinanceSnapshot, month: string, message: string): string {
  const lower = message.toLowerCase();
  const budgets = getBudgetSummaries(snapshot, month);
  const totals = getMonthlyTotals(snapshot, month);

  if (lower.includes('food')) {
    const food = budgets.find((summary) => summary.name.toLowerCase().includes('food'));
    if (!food) {
      return `I don't have a Food & Dining budget for ${formatMonthLabel(month)}.`;
    }

    return `Food & Dining is at ${formatAmount(food.spentCents, 'USD')} for ${formatMonthLabel(month)}. That's ${food.percentUsed}% of the ${formatAmount(food.budgetCents, 'USD')} budget.`;
  }

  if (lower.includes('savings rate')) {
    return `Your savings rate for ${formatMonthLabel(month)} is ${totals.savingsRate}%. Income is ${formatAmount(totals.incomeCents, 'USD')} and expenses are ${formatAmount(-totals.expenseCents, 'USD')}.`;
  }

  if (lower.includes('most') && lower.includes('spend')) {
    const topCategory = budgets[0];
    if (!topCategory) {
      return `There isn't enough expense data yet for ${formatMonthLabel(month)}.`;
    }

    return `Your biggest spend in ${formatMonthLabel(month)} is ${topCategory.name} at ${formatAmount(topCategory.spentCents, 'USD')}.`;
  }

  if (lower.includes('goal') || lower.includes('track')) {
    const lines = snapshot.goals
      .filter((goal) => goal.deletedAt === null)
      .map((goal) => `${goal.name}: ${formatAmount(goal.currentCents, goal.currency)} of ${formatAmount(goal.targetCents, goal.currency)}`);

    return lines.length > 0 ? `Current goal progress:\n${lines.join('\n')}` : 'There are no active goals yet.';
  }

  const lead = buildMonthlyInsights(snapshot, month)[0];
  return lead ?? `For ${formatMonthLabel(month)}, income is ${formatAmount(totals.incomeCents, 'USD')} and spend is ${formatAmount(-totals.expenseCents, 'USD')}.`;
}

export function getCategoryById(categories: Category[], categoryId: string | null): Category | null {
  if (!categoryId) {
    return null;
  }

  return categories.find((category) => category.id === categoryId) ?? null;
}

export function getAccountById(accounts: Account[], accountId: string): Account | null {
  return accounts.find((account) => account.id === accountId) ?? null;
}
