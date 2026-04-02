import { cx } from '../../lib/cx';

interface ProgressBarProps {
  value: number;
  status?: 'safe' | 'warning' | 'danger';
  className?: string;
}

export function ProgressBar({ value, status = 'safe', className }: ProgressBarProps) {
  const width = Math.max(0, Math.min(100, value));

  return (
    <div className={cx('h-1.5 w-full overflow-hidden rounded-full bg-subtle', className)}>
      <div
        className={cx(
          'h-full rounded-full transition-all',
          status === 'safe' && 'bg-positive',
          status === 'warning' && 'bg-warning',
          status === 'danger' && 'bg-negative',
        )}
        style={{ width: `${width}%` }}
      />
    </div>
  );
}
