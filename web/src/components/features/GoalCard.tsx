import { Target } from 'lucide-react';

import { formatAmount } from '../../lib/format';
import { ProgressBar } from '../ui/ProgressBar';
import { Button } from '../ui/Button';
import { Card } from '../ui/Card';
import type { Goal } from '../../types';

interface GoalCardProps {
  goal: Goal;
  onAddFunds: (goalId: string) => void;
}

export function GoalCard({ goal, onAddFunds }: GoalCardProps) {
  const progress = Math.min(100, Math.round((goal.currentCents / goal.targetCents) * 100));

  return (
    <Card className="space-y-5">
      <div className="flex items-start justify-between gap-4">
        <div className="space-y-2">
          <div className="flex items-center gap-2">
            <Target className="h-4 w-4 text-purple-300" />
            <p className="text-base font-medium text-[#F0F0F0]">{goal.name}</p>
          </div>
          <p className="text-sm text-[#A0A0A0]">{goal.deadline ? `Target date ${goal.deadline}` : 'No deadline set'}</p>
        </div>
        <Button onClick={() => onAddFunds(goal.id)} variant="secondary">
          Add funds
        </Button>
      </div>

      <div className="space-y-2">
        <p className="font-mono text-2xl text-[#F0F0F0]">
          {formatAmount(goal.currentCents, goal.currency)} / {formatAmount(goal.targetCents, goal.currency)}
        </p>
        <ProgressBar status={progress >= 100 ? 'danger' : 'safe'} value={progress} />
        <p className="text-xs text-[#5A5A5A]">{progress}% funded</p>
      </div>
    </Card>
  );
}
