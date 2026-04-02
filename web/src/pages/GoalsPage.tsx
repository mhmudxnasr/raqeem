import { GoalCard } from '../components/features/GoalCard';
import { PageHeader } from '../components/layout/PageHeader';
import { Card } from '../components/ui/Card';
import { calculateNetWorthCents } from '../lib/analytics';
import { formatAmount } from '../lib/format';
import { useFinanceStore } from '../store/useFinanceStore';
import { useAppShellContext } from '../components/layout/AppShell';

export function GoalsPage() {
  const { openQuickAdd } = useAppShellContext();
  const accounts = useFinanceStore((state) => state.accounts);
  const categories = useFinanceStore((state) => state.categories);
  const transactions = useFinanceStore((state) => state.transactions);
  const transfers = useFinanceStore((state) => state.transfers);
  const goals = useFinanceStore((state) => state.goals);
  const subscriptions = useFinanceStore((state) => state.subscriptions);
  const settings = useFinanceStore((state) => state.settings);

  const snapshot = { accounts, categories, transactions, transfers, goals, subscriptions, settings };
  const activeGoals = goals.filter((goal) => goal.deletedAt === null);
  const totalFunded = activeGoals.reduce((sum, goal) => sum + goal.currentCents, 0);

  return (
    <>
      <PageHeader eyebrow="Goals" title="Savings targets" description="Goals are funded through transfers so balances, net worth, and progress stay in sync." />

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_320px]">
        <div className="grid gap-6 md:grid-cols-2">
          {activeGoals.map((goal) => (
            <GoalCard key={goal.id} goal={goal} onAddFunds={(goalId) => openQuickAdd('transfer', goalId)} />
          ))}
        </div>

        <div className="space-y-6">
          <Card className="space-y-1">
            <p className="section-label">Total Funded</p>
            <p className="font-mono text-3xl font-medium text-white">{formatAmount(totalFunded, 'USD')}</p>
            <p className="mt-2 text-sm text-[#A0A0A0]">Current net worth is {formatAmount(calculateNetWorthCents(snapshot), 'USD')}.</p>
          </Card>
        </div>
      </div>
    </>
  );
}
