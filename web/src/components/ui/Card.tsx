import type { HTMLAttributes, ReactNode } from 'react';

import { cx } from '../../lib/cx';

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
  elevated?: boolean;
}

export function Card({ children, className, elevated = false, ...props }: CardProps) {
  return (
    <div className={cx(elevated ? 'card-elevated' : 'card', className)} {...props}>
      {children}
    </div>
  );
}
