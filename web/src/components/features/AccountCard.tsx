import { Eye, EyeOff } from 'lucide-react';

import { formatAmount, maskAmount } from '../../lib/format';
import { Badge } from '../ui/Badge';
import { Card } from '../ui/Card';
import type { Account } from '../../types';

interface AccountCardProps {
  account: Account;
  incomeCents: number;
  expenseCents: number;
  sparkline: number[];
  isRevealed: boolean;
  onToggleReveal: () => void;
}

function buildSparklinePath(points: number[]): string {
  if (points.length === 0) {
    return '';
  }

  const width = 220;
  const height = 50;
  const min = Math.min(...points);
  const max = Math.max(...points);
  const range = max - min || 1;

  return points
    .map((point, index) => {
      const x = (index / Math.max(points.length - 1, 1)) * width;
      const y = height - ((point - min) / range) * height;
      return `${index === 0 ? 'M' : 'L'} ${x.toFixed(2)} ${y.toFixed(2)}`;
    })
    .join(' ');
}

export function AccountCard({
  account,
  incomeCents,
  expenseCents,
  sparkline,
  isRevealed,
  onToggleReveal,
}: AccountCardProps) {
  return (
    <Card className="space-y-5">
      <div className="flex items-start justify-between gap-4">
        <div className="space-y-2">
          <p className="text-base font-medium text-[#F0F0F0]">{account.name}</p>
          <Badge tone="default">{account.type}</Badge>
        </div>
        <button
          className="rounded-lg border border-white/10 p-2 text-[#A0A0A0] transition-colors hover:bg-subtle hover:text-white"
          onClick={onToggleReveal}
          type="button"
        >
          {isRevealed ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
        </button>
      </div>

      <div>
        <p className="section-label">Balance</p>
        <p className="mt-3 font-mono text-3xl text-[#F0F0F0]">
          {isRevealed ? formatAmount(account.balanceCents, account.currency) : maskAmount(account.currency)}
        </p>
      </div>

      <div className="space-y-3">
        <div className="flex items-center justify-between text-sm text-[#A0A0A0]">
          <span>Last 30 days</span>
          <span className="font-mono">
            +{formatAmount(incomeCents, account.currency).replace(/^[^0-9A-Z$]+/, '')} / {formatAmount(-expenseCents, account.currency)}
          </span>
        </div>
        <svg className="h-14 w-full" preserveAspectRatio="none" viewBox="0 0 220 50">
          <path d={buildSparklinePath(sparkline)} fill="none" stroke="var(--purple-400)" strokeWidth="2.5" />
        </svg>
      </div>
    </Card>
  );
}
