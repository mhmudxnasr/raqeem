import type { ReactNode } from 'react';

import { cx } from '../../lib/cx';

interface BadgeProps {
  children: ReactNode;
  tone?: 'default' | 'positive' | 'warning' | 'negative' | 'accent';
}

export function Badge({ children, tone = 'default' }: BadgeProps) {
  return (
    <span
      className={cx(
        'inline-flex items-center rounded-full border px-2.5 py-1 text-[11px] font-medium uppercase tracking-[0.08em]',
        tone === 'default' && 'border-white/10 bg-white/5 text-[#A0A0A0]',
        tone === 'positive' && 'border-emerald-400/25 bg-emerald-400/10 text-positive',
        tone === 'warning' && 'border-amber-400/25 bg-amber-400/10 text-warning',
        tone === 'negative' && 'border-rose-400/25 bg-rose-400/10 text-negative',
        tone === 'accent' && 'border-purple-400/30 bg-purple-500/10 text-purple-300',
      )}
    >
      {children}
    </span>
  );
}
