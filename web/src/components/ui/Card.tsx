import type { HTMLAttributes, ReactNode } from 'react';

import { cx } from '../../lib/cx';

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
}

export function Card({ children, className, ...props }: CardProps) {
  return (
    <div className={cx('card', className)} {...props}>
      {children}
    </div>
  );
}
