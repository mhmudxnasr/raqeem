import type { InputHTMLAttributes } from 'react';

import { cx } from '../../lib/cx';

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
}

export function Input({ label, error, className, id, ...props }: InputProps) {
  const inputId = id ?? props.name;

  return (
    <label className="flex flex-col gap-2">
      {label ? <span className="section-label">{label}</span> : null}
      <input id={inputId} className={cx('input', className)} {...props} />
      {error ? <span className="text-xs text-negative">{error}</span> : null}
    </label>
  );
}
