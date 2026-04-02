import { describe, expect, it } from 'vitest';

import { toUSD } from '../analytics';
import { formatAmount, parseAmountInput } from '../format';

describe('formatAmount', () => {
  it('formats USD with signs', () => {
    expect(formatAmount(1000, 'USD', true)).toBe('+$10.00');
    expect(formatAmount(-1000, 'USD')).toBe('−$10.00');
    expect(formatAmount(0, 'USD')).toBe('$0.00');
  });

  it('formats EGP correctly', () => {
    expect(formatAmount(52000, 'EGP')).toBe('EGP 520.00');
  });
});

describe('parseAmountInput', () => {
  it('parses decimal strings into cents', () => {
    expect(parseAmountInput('10.25')).toBe(1025);
    expect(parseAmountInput('0')).toBeNull();
    expect(parseAmountInput('abc')).toBeNull();
  });
});

describe('toUSD', () => {
  it('converts EGP piastres to USD cents', () => {
    expect(toUSD(52000, 'EGP', 52)).toBe(1000);
    expect(toUSD(1000, 'USD', 52)).toBe(1000);
  });
});
