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
          <div className="grid gap-6 md:grid-cols-2">
            <Card className="flex min-h-[180px] flex-col justify-between">
              <div className="space-y-2">
                <p className="section-label">Quick Action</p>
                <h2 className="text-lg font-semibold tracking-tight text-white">Log an expense without losing flow.</h2>
                <p className="text-sm leading-relaxed text-[#A0A0A0]">Use the default account, keep the note short, and stay in the flow of your ledger.</p>
              </div>
              <Button className="w-fit" onClick={() => openQuickAdd('expense')}>
                Add expense
              </Button>
            </Card>

            <Card className="flex min-h-[180px] flex-col justify-between">
              <div className="space-y-2">
                <p className="section-label">Managed Move</p>
                <h2 className="text-lg font-semibold tracking-tight text-white">Move money between accounts or goals.</h2>
                <p className="text-sm leading-relaxed text-[#A0A0A0]">Wallet conversions honor your saved USD/EGP rate automatically for precise tracking.</p>
              </div>
              <Button className="w-fit gap-2" onClick={() => openQuickAdd('transfer')} variant="secondary">
                <ArrowRightLeft className="h-4 w-4" />
                New transfer
              </Button>
            </Card>
          </div>

          <Card className="p-0 overflow-hidden">
            <div className="flex items-center justify-between border-b border-white/5 px-5 py-4">
              <div>
                <p className="section-label">Activity</p>
                <h3 className="mt-1 text-base font-semibold tracking-tight text-white">Recent Transactions</h3>
              </div>
              {isLoading && (
                <div className="flex items-center gap-2">
                   <div className="h-2 w-2 animate-pulse rounded-full bg-purple-500"></div>
                   <span className="section-label">Refreshing</span>
                </div>
              )}
            </div>

            <div className="divide-y divide-white/5">
              {recentEntries.length > 0 ? (
                recentEntries.map((entry) => (
                  <div key={entry.id} className="flex items-center justify-between gap-4 px-5 py-4 transition-colors hover:bg-elevated/50">
                    <div className="space-y-0.5">
                      <p className="text-sm font-medium text-white">{entry.title}</p>
                      <p className="text-xs text-[#5A5A5A]">{entry.note ?? entry.subtitle}</p>
                    </div>
                    <div className="text-right">
                      <p className={`font-mono text-sm font-medium ${entry.amountCents >= 0 ? 'text-positive' : 'text-negative'}`}>
                        {formatAmount(entry.amountCents, entry.currency)}
                      </p>
                      <p className="section-label mt-0.5">{entry.date}</p>
                    </div>
                  </div>
                ))
              ) : (
                <div className="p-12 text-center">
                  <p className="text-sm text-[#5A5A5A]">No recent activity found.</p>
                </div>
              )}
            </div>
          </Card>
        </div>

        <div className="space-y-6">
          <Card>
            <div className="flex items-center gap-2 text-positive">
              <TrendingUp className="h-4 w-4" />
              <p className="section-label text-positive">Monthly Yield</p>
            </div>
            <p className="mt-4 font-mono text-3xl font-medium text-white">{formatAmount(totals.incomeCents, 'USD', true)}</p>
            <p className="mt-2 text-xs text-[#A0A0A0]">
              Expenses sit at <span className="text-negative">{formatAmount(-totals.expenseCents, 'USD')}</span> so far this month.
            </p>
          </Card>

          <Card>
            <div className="flex items-center gap-2 text-purple-400">
              <TrendingDown className="h-4 w-4" />
              <p className="section-label text-purple-400">Budget Pulse</p>
            </div>
            <p className="mt-4 font-mono text-3xl font-medium text-white">{dashboardStats.budgetUsedPercent}%</p>
            <div className="mt-4 space-y-2">
              <div className="h-1.5 w-full rounded-full bg-subtle">
                <div 
                  className="h-full rounded-full bg-purple-500" 
                  style={{ width: `${Math.min(dashboardStats.budgetUsedPercent, 100)}%` }}
                ></div>
              </div>
              <p className="section-label">
                {formatAmount(dashboardStats.totalSpentCents, 'USD')} of {formatAmount(dashboardStats.totalBudgetCents, 'USD')}
              </p>
            </div>
          </Card>
        </div>
      </div>
    </>
  );
}
