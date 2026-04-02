import { cx } from '../../lib/cx';

interface SkeletonProps {
  className?: string;
}

export function Skeleton({ className }: SkeletonProps) {
  return <div className={cx('animate-pulseSoft rounded-md bg-subtle', className)} />;
}
