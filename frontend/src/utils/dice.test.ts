import { describe, it, expect } from 'vitest';
import { formatDiceBreakdown } from './dice';

describe('formatDiceBreakdown', () => {
  it('listet Einzelwürfe und die Summe', () => {
    expect(formatDiceBreakdown([4, 2, 3], 0, 9)).toBe('4 + 2 + 3 = 9');
  });

  it('hängt positive und negative Boni korrekt an', () => {
    expect(formatDiceBreakdown([5], 3, 8)).toBe('5 + 3 = 8');
    expect(formatDiceBreakdown([5], -2, 3)).toBe('5 - 2 = 3');
    expect(formatDiceBreakdown([1, 6, 2], -3, 6)).toBe('1 + 6 + 2 - 3 = 6');
  });
});
