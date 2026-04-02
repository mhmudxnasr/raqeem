import type { SelectHTMLAttributes } from 'react';

import { cx } from '../../lib/cx';

interface SelectOption {
  label: string;
  value: string;
}

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  options: SelectOption[];
  error?: string;
}

export function Select({ label, options, error, className, id, ...props }: SelectProps) {
  const selectId = id ?? props.name;

  return (
    <label className="flex flex-col gap-2">
      {label ? <span className="section-label">{label}</span> : null}
      <select id={selectId} className={cx('input', className)} {...props}>
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      {error ? <span className="text-xs text-negative">{error}</span> : null}
    </label>
  );
}
