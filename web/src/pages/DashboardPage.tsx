import { ArrowRightLeft, TrendingDown, TrendingUp } from 'lucide-react';

import { buildLedgerEntries, getDashboardStats, getMonthlyTotals } from '../lib/analytics';
import { formatAmount, formatAppDate } from '../lib/format';
import { useFinanceStore } from '../store/useFinanceStore';
import { useAppShellContext } from '../components/layout/AppShell';
import { PageHeader } from '../components/layout/PageHeader';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';

export function DashboardPage() {
  const { openQuickAdd } = useAppShellContext();
  const accounts = useFinanceStore((state) => state.accounts);
  const categories = useFinanceStore((state) => state.categories);
  const transactions = useFinanceStore((state) => state.transactions);
  const transfers = useFinanceStore((state) => state.transfers);
  const goals = useFinanceStore((state) => state.goals);
  const subscriptions = useFinanceStore((state) => state.subscriptions);
  const settings = useFinanceStore((state) => state.settings);
  const selectedMonth = useFinanceStore((state) => state.selectedMonth);
  const isLoading = useFinanceStore((state) => state.isLoading);

  const snapshot = { accounts, categories, transactions, transfers, goals, subscriptions, settings };
  const recentEntries = buildLedgerEntries(snapshot).slice(0, 8);
  const totals = getMonthlyTotals(snapshot, selectedMonth);
  const dashboardStats = getDashboardStats(snapshot, selectedMonth);

  return (
    <>
      <PageHeader
        description={formatAppDate(new Date())}
        eyebrow="Home"
        title="Good morning"
        actions={
          <>
            <Button onClick={() => openQuickAdd('expense')}>Add expense</Button>
            <Button onClick={() => openQuickAdd('income')} variant="secondary">
              Add income
            </Button>
          </>
        }
      />

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1.3fr)_320px]">
        <div className="space-y-6">
          <div className="grid gap-4 md:grid-cols-2">
            <Card className="flex min-h-[180px] flex-col justify-between bg-gradient-to-br from-surface via-surface to-purple-900/15">
              <div className="space-y-2">
                <p className="section-label">Quick add</p>
                <h2 className="text-xl font-semibold text-[#F0F0F0]">Log an expense without losing flow.</h2>
                <p className="max-w-md text-sm text-[#A0A0A0]">Use the default account, keep the note short, and stay in the ledger.</p>
              </div>
              <Button className="w-fit" onClick={() => openQuickAdd('expense')}>
                Add expense
              </Button>
            </Card>

            <Card className="flex min-h-[180px] flex-col justify-between">
              <div className="space-y-2">
                <p className="section-label">Transfer</p>
                <h2 className="text-xl font-semibold text-[#F0F0F0]">Move money between accounts or goals.</h2>
                <p className="max-w-md text-sm text-[#A0A0A0]">Wallet conversions honor your saved USD/EGP rate automatically.</p>
              </div>
              <Button className="w-fit gap-2" onClick={() => openQuickAdd('transfer')} variant="secondary">
                <ArrowRightLeft className="h-4 w-4" />
                New transfer
              </Button>
            </Card>
          </div>

          <Card className="space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="section-label">Recent transactions</p>
                <h3 className="mt-2 text-lg font-semibold text-[#F0F0F0]">Latest activity</h3>
              </div>
              {isLoading ? <span className="text-xs text-[#5A5A5A]">Refreshing…</span> : null}
            </div>

            <div className="divide-y divide-white/5">
              {recentEntries.map((entry) => (
                <div key={entry.id} className="flex items-center justify-between gap-4 py-4">
                  <div className="space-y-1">
                    <p className="text-sm font-medium text-[#F0F0F0]">{entry.title}</p>
                    <p className="text-xs text-[#5A5A5A]">{entry.note ?? entry.subtitle}</p>
                  </div>
                  <div className="text-right">
                    <p className={`font-mono text-sm ${entry.amountCents >= 0 ? 'text-positive' : 'text-negative'}`}>
                      {formatAmount(entry.amountCents, entry.currency)}
                    </p>
                    <p className="text-xs text-[#5A5A5A]">{entry.date}</p>
                  </div>
                </div>
              ))}
            </div>
          </Card>
        </div>

        <div className="space-y-6">
          <Card className="space-y-4">
            <div className="flex items-center gap-2 text-positive">
              <TrendingUp className="h-4 w-4" />
              <p className="section-label text-positive">Income this month</p>
            </div>
            <p className="font-mono text-3xl text-[#F0F0F0]">{formatAmount(totals.incomeCents, 'USD', true)}</p>
            <p className="text-sm text-[#A0A0A0]">Expenses sit at {formatAmount(-totals.expenseCents, 'USD')} so far.</p>
          </Card>

          <Card className="space-y-4">
            <div className="flex items-center gap-2 text-warning">
              <TrendingDown className="h-4 w-4" />
              <p className="section-label text-warning">Budget pulse</p>
            </div>
            <p className="font-mono text-3xl text-[#F0F0F0]">{dashboardStats.budgetUsedPercent}%</p>
            <p className="text-sm text-[#A0A0A0]">
              {formatAmount(dashboardStats.totalSpentCents, 'USD')} spent out of {formatAmount(dashboardStats.totalBudgetCents, 'USD')}.
            </p>
            <p className="text-sm text-[#A0A0A0]">Today's spend is {formatAmount(dashboardStats.todaySpendCents, 'USD')}.</p>
          </Card>
        </div>
      </div>
    </>
  );
}
