import { format, isToday, isYesterday, parseISO } from 'date-fns';

import type { Currency } from '../types';

export function getCurrencySymbol(currency: Currency): string {
  return currency === 'USD' ? '$' : 'EGP ';
}

export function formatAmount(cents: number, currency: Currency, showSign = false): string {
  const absoluteAmount = Math.abs(cents) / 100;
  const sign = cents < 0 ? '−' : showSign && cents > 0 ? '+' : '';
  return `${sign}${getCurrencySymbol(currency)}${absoluteAmount.toFixed(2)}`;
}

export function formatCompactAmount(cents: number, currency: Currency): string {
  const absoluteAmount = Math.abs(cents) / 100;
  const formatter = new Intl.NumberFormat('en-US', {
    notation: absoluteAmount >= 1000 ? 'compact' : 'standard',
    maximumFractionDigits: absoluteAmount >= 1000 ? 1 : 2,
    minimumFractionDigits: absoluteAmount >= 1000 ? 0 : 2,
  });

  return `${getCurrencySymbol(currency)}${formatter.format(absoluteAmount)}`;
}

export function parseAmountInput(value: string): number | null {
  const normalized = value.trim().replace(/,/g, '');
  if (normalized.length === 0) {
    return null;
  }

  const parsed = Number(normalized);
  if (!Number.isFinite(parsed) || parsed <= 0) {
    return null;
  }

  return Math.round(parsed * 100);
}

export function formatDateLabel(date: string): string {
  const parsed = parseISO(date);
  if (isToday(parsed)) {
    return `Today · ${format(parsed, 'MMM d')}`;
  }

  if (isYesterday(parsed)) {
    return `Yesterday · ${format(parsed, 'MMM d')}`;
  }

  return format(parsed, 'EEE, MMM d');
}

export function formatMonthLabel(month: string): string {
  return format(parseISO(`${month}-01`), 'MMMM yyyy');
}

export function formatInputDate(date: string): string {
  return format(parseISO(date), 'yyyy-MM-dd');
}

export function formatAppDate(date: Date): string {
  return format(date, 'EEEE, MMMM d');
}

export function monthKeyFromDate(date: string): string {
  return date.slice(0, 7);
}

export function maskAmount(currency: Currency): string {
  return currency === 'USD' ? '$••••••' : 'EGP ••••••';
}
