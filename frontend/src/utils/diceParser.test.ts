import { describe, it, expect } from 'vitest';
import { parseExpression } from './diceParser';

describe('parseExpression', () => {
  it('parses simple expression', () => {
    const r = parseExpression('2d6');
    expect(r.count).toBe(2);
    expect(r.sides).toBe(6);
    expect(r.modifier).toBe(0);
  });

  it('parses expression with modifier', () => {
    const r = parseExpression('1d20+3');
    expect(r.modifier).toBe(3);
  });

  it('parses expression with negative modifier', () => {
    const r = parseExpression('1d20-2');
    expect(r.modifier).toBe(-2);
  });

  it('returns default for invalid input', () => {
    const r = parseExpression('invalid');
    expect(r.count).toBe(1);
    expect(r.sides).toBe(20);
  });
});
