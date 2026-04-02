import { AlertTriangle } from 'lucide-react';

import { formatAmount } from '../../lib/format';
import { ProgressBar } from '../ui/ProgressBar';
import type { BudgetSummary } from '../../types';

interface BudgetProgressProps {
  summary: BudgetSummary;
}

export function BudgetProgress({ summary }: BudgetProgressProps) {
  return (
    <div className="space-y-3 rounded-xl border border-white/5 bg-surface/70 p-4">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-sm font-medium text-[#F0F0F0]">{summary.name}</p>
          <p className="mt-1 font-mono text-sm text-[#A0A0A0]">
            {formatAmount(summary.spentCents, 'USD')} / {formatAmount(summary.budgetCents, 'USD')}
          </p>
        </div>
        {summary.status !== 'safe' ? <AlertTriangle className="h-4 w-4 text-warning" /> : null}
      </div>
      <ProgressBar status={summary.status} value={summary.percentUsed} />
      <p className="text-xs text-[#5A5A5A]">{summary.percentUsed}% used</p>
    </div>
  );
}
