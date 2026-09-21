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
      kind: 'attack', dice: [14], parts: [{ kind: 'mod', value: 3 }],
      total: 17, target: 12, comparison: 'gte',
    })).toBe('14 + 3 = 17 ≥ 12');
  });

  it('benennt Attribut- und Rüstungsposten', () => {
    expect(formatRollBreakdown({
      kind: 'damage', dice: [4],
      parts: [{ kind: 'attr', label: 'staerke', value: 2 }, { kind: 'armor', value: -1 }],
      total: 5,
    })).toBe('4 + 2 (staerke) - 1 = 5');
  });

  it('übersetzt Posten-Codes über den übergebenen Übersetzer', () => {
    const t = (key: string) => (key === 'combat.part_armor' ? 'Rüstung' : key);
    expect(formatRollBreakdown({
      kind: 'damage', dice: [4], parts: [{ kind: 'armor', value: -1 }],
      total: 3,
    }, t)).toBe('4 - 1 (Rüstung) = 3');
  });

  it('zeigt Zielschutz-Multiplikator als eigene Stufe', () => {
    expect(formatRollBreakdown({
      kind: 'damage', dice: [4], parts: [{ kind: 'armor', value: -1 }],
      subtotal: 3, total: 1, multiplier: 0.5,
    })).toBe('4 - 1 = 3 × 0.5 = 1');
  });
});
