/**
 * Parst einen Dice-Expression-String in count + sides + modifier.
 *
 * Beispiele:
 *   "1d20+3" → { count: 1, sides: 20, modifier: 3 }
 *   "2d6-1"  → { count: 2, sides: 6, modifier: -1 }
 *   "1d20"   → { count: 1, sides: 20, modifier: 0 }
 */
export function parseExpression(expr: string): { count: number; sides: number; modifier: number } {
  const match = expr.match(/^(\d+)d(\d+)([+-]\d+)?$/);
  if (!match) return { count: 1, sides: 20, modifier: 0 };
  return {
    count: Number(match[1]),
    sides: Number(match[2]),
    modifier: match[3] ? Number(match[3]) : 0,
  };
}
