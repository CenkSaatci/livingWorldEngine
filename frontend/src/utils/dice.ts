/**
 * Formatiert einen Wurf als Einzelaufstellung: "4 + 2 + 3 = 9".
 * Negative Boni werden als "- n" angehängt (nicht "+ -n").
 */
export function formatDiceBreakdown(dice: number[], modifier: number, total: number): string {
  let lhs = dice.map((d) => String(d)).join(' + ');
  if (modifier !== 0) {
    lhs = lhs.length > 0
      ? `${lhs} ${modifier > 0 ? '+' : '-'} ${Math.abs(modifier)}`
      : String(modifier);
  }
  return `${lhs} = ${total}`;
}
