import { describe, it, expect } from 'vitest';
import { formatDiceBreakdown, formatRollBreakdown } from './dice';

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

describe('formatRollBreakdown', () => {
  it('formatiert Angriff mit Vergleichsziel', () => {
    expect(formatRollBreakdown({
      kind: 'attack', dice: [14], parts: [{ label: 'Mod', value: 3 }],
      total: 17, target: 12, comparison: 'gte',
    })).toBe('14 + 3 = 17 ≥ 12');
  });

  it('formatiert Schaden mit Attribut und Rüstung', () => {
    expect(formatRollBreakdown({
      kind: 'damage', dice: [4], parts: [{ label: 'staerke', value: 2 }, { label: 'Rüstung', value: -1 }],
      total: 5,
    })).toBe('4 + 2 - 1 = 5');
  });

  it('zeigt Zielschutz-Multiplikator als eigene Stufe', () => {
    expect(formatRollBreakdown({
      kind: 'damage', dice: [4], parts: [{ label: 'Rüstung', value: -1 }],
      subtotal: 3, total: 1, multiplier: 0.5,
    })).toBe('4 - 1 = 3 × 0.5 = 1');
  });
});
