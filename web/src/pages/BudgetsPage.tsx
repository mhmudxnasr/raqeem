import { BudgetProgress } from '../components/features/BudgetProgress';
import { PageHeader } from '../components/layout/PageHeader';
import { Card } from '../components/ui/Card';
import { Select } from '../components/ui/Select';
import { getAvailableMonths, getBudgetSummaries, getSubscriptionMonthlyTotal } from '../lib/analytics';
import { formatAmount, formatMonthLabel } from '../lib/format';
import { useFinanceStore } from '../store/useFinanceStore';

export function BudgetsPage() {
  const accounts = useFinanceStore((state) => state.accounts);
  const categories = useFinanceStore((state) => state.categories);
  const transactions = useFinanceStore((state) => state.transactions);
  const transfers = useFinanceStore((state) => state.transfers);
  const goals = useFinanceStore((state) => state.goals);
  const subscriptions = useFinanceStore((state) => state.subscriptions);
  const settings = useFinanceStore((state) => state.settings);
  const selectedMonth = useFinanceStore((state) => state.selectedMonth);
  const setSelectedMonth = useFinanceStore((state) => state.setSelectedMonth);

  const snapshot = { accounts, categories, transactions, transfers, goals, subscriptions, settings };
  const budgets = getBudgetSummaries(snapshot, selectedMonth);
  const totalSpent = budgets.reduce((sum, summary) => sum + summary.spentCents, 0);
  const totalBudget = budgets.reduce((sum, summary) => sum + summary.budgetCents, 0);
  const months = getAvailableMonths(snapshot).map((month) => ({ value: month, label: formatMonthLabel(month) }));

  return (
    <>
      <PageHeader
        eyebrow="Budgets"
        title="Budgets and subscriptions"
        description="Every category budget is normalized into USD so Wallet expenses stay comparable."
        actions={<Select label="" onChange={(event) => setSelectedMonth(event.target.value)} options={months} value={selectedMonth} />}
      />

      <div className="grid gap-6 xl:grid-cols-[340px_minmax(0,1fr)]">
        <div className="space-y-6">
          <Card className="space-y-4">
            <p className="section-label">Total spent</p>
            <p className="font-mono text-4xl text-[#F0F0F0]">{formatAmount(totalSpent, 'USD')}</p>
            <p className="text-sm text-[#A0A0A0]">Against a total category budget of {formatAmount(totalBudget, 'USD')}.</p>
          </Card>

          <Card className="space-y-4">
            <p className="section-label">Subscriptions</p>
            <p className="font-mono text-4xl text-[#F0F0F0]">{formatAmount(getSubscriptionMonthlyTotal(snapshot), 'USD')}</p>
            <p className="text-sm text-[#A0A0A0]">Monthly run-rate across all active recurring services.</p>
          </Card>
        </div>

        <div className="space-y-6">
          <Card className="space-y-4">
            <p className="section-label">By category</p>
            <div className="space-y-3">
              {budgets.map((summary) => (
                <BudgetProgress key={summary.categoryId} summary={summary} />
              ))}
            </div>
          </Card>

          <Card className="space-y-4">
            <p className="section-label">Upcoming subscriptions</p>
            <div className="divide-y divide-white/5">
              {subscriptions
                .filter((subscription) => subscription.deletedAt === null && subscription.isActive)
                .map((subscription) => (
                  <div key={subscription.id} className="flex items-center justify-between gap-4 py-4">
                    <div>
                      <p className="text-sm font-medium text-[#F0F0F0]">{subscription.name}</p>
                      <p className="text-xs text-[#5A5A5A]">Next billing {subscription.nextBillingDate}</p>
                    </div>
                    <div className="text-right">
                      <p className="font-mono text-sm text-[#F0F0F0]">{formatAmount(subscription.amountCents, subscription.currency)}</p>
                      <p className="text-xs uppercase tracking-[0.08em] text-[#5A5A5A]">{subscription.billingCycle}</p>
                    </div>
                  </div>
                ))}
            </div>
          </Card>
        </div>
      </div>
    </>
  );
}
