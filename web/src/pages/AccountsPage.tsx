import { Eye, EyeOff } from 'lucide-react';
import { useState } from 'react';

import { AccountCard } from '../components/features/AccountCard';
import { PageHeader } from '../components/layout/PageHeader';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { calculateNetWorthCents, getAccountSparkline, getAccountMonthlyStats } from '../lib/analytics';
import { formatAmount } from '../lib/format';
import { useFinanceStore } from '../store/useFinanceStore';

export function AccountsPage() {
  const accounts = useFinanceStore((state) => state.accounts);
  const categories = useFinanceStore((state) => state.categories);
  const transactions = useFinanceStore((state) => state.transactions);
  const transfers = useFinanceStore((state) => state.transfers);
  const goals = useFinanceStore((state) => state.goals);
  const subscriptions = useFinanceStore((state) => state.subscriptions);
  const settings = useFinanceStore((state) => state.settings);
  const selectedMonth = useFinanceStore((state) => state.selectedMonth);
  const [revealedIds, setRevealedIds] = useState<string[]>([]);
  const [showNetWorth, setShowNetWorth] = useState<boolean>(false);

  const snapshot = { accounts, categories, transactions, transfers, goals, subscriptions, settings };
  const netWorth = calculateNetWorthCents(snapshot);

  return (
    <>
      <PageHeader eyebrow="Accounts" title="Balances and account flows" description="Every balance stays hidden until you decide to reveal it." />

      <Card className="flex flex-col justify-between gap-4 md:flex-row md:items-center">
        <div className="space-y-2">
          <p className="section-label">Net worth</p>
          <p className="font-mono text-4xl text-[#F0F0F0]">{showNetWorth ? formatAmount(netWorth, 'USD') : '$••••••'}</p>
        </div>
        <Button
          onClick={() => setShowNetWorth((current) => !current)}
          type="button"
          variant="secondary"
        >
          {showNetWorth ? <EyeOff className="mr-2 h-4 w-4" /> : <Eye className="mr-2 h-4 w-4" />}
          {showNetWorth ? 'Hide net worth' : 'Reveal net worth'}
        </Button>
      </Card>

      <div className="grid gap-6 xl:grid-cols-2">
        {accounts
          .filter((account) => account.deletedAt === null)
          .map((account) => {
            const stats = getAccountMonthlyStats(snapshot, account.id, selectedMonth);
            const isRevealed = revealedIds.includes(account.id);

            return (
              <AccountCard
                key={account.id}
                account={account}
                expenseCents={stats.expenseCents}
                incomeCents={stats.incomeCents}
                isRevealed={isRevealed}
                onToggleReveal={() =>
                  setRevealedIds((current) =>
                    current.includes(account.id) ? current.filter((item) => item !== account.id) : [...current, account.id],
                  )
                }
                sparkline={getAccountSparkline(snapshot, account)}
              />
            );
          })}
      </div>
    </>
  );
}
