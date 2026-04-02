import type { LucideIcon } from 'lucide-react';

import { Button } from '../ui/Button';

interface EmptyStateProps {
  icon: LucideIcon;
  title: string;
  description: string;
  actionLabel?: string;
  onAction?: () => void;
}

export function EmptyState({ icon: Icon, title, description, actionLabel, onAction }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center gap-3 rounded-xl border border-dashed border-white/10 bg-surface px-6 py-16 text-center">
      <div className="rounded-full border border-white/10 bg-subtle p-3">
        <Icon className="h-5 w-5 text-[#5A5A5A]" />
      </div>
      <p className="text-base font-medium text-[#F0F0F0]">{title}</p>
      <p className="max-w-sm text-sm text-[#A0A0A0]">{description}</p>
      {actionLabel && onAction ? (
        <Button variant="secondary" onClick={onAction}>
          {actionLabel}
        </Button>
      ) : null}
    </div>
  );
}
