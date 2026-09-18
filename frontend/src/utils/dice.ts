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

export interface RollPart {
  label: string;
  value: number;
}

/** Aufstellung eines Kampfwurfs (Backend: CombatService.RollBreakdown). */
export interface RollBreakdown {
  kind: 'attack' | 'damage';
  dice: number[];
  parts: RollPart[];
  subtotal?: number | null;
  total: number;
  multiplier?: number | null;
  target?: number | null;
  comparison?: 'gte' | 'lte' | null;
}

/**
 * Formatiert einen Kampfwurf als Aufstellung:
 * "14 + 3 = 17 ≥ 12" (Angriff), "4 + 2 - 1 = 5" (Schaden),
 * mit Zielschutz "4 + 2 - 1 = 3 × 0.5 = 1".
 */
export function formatRollBreakdown(rb: RollBreakdown): string {
  const addends = [...rb.dice, ...(rb.parts ?? []).map((p) => p.value)];
  const lhs = addends
    .map((v, i) => (i === 0 ? String(v) : v >= 0 ? `+ ${v}` : `- ${Math.abs(v)}`))
    .join(' ') || '0';
  let out = `${lhs} = ${rb.subtotal ?? rb.total}`;
  if (rb.multiplier != null && rb.multiplier !== 1) {
    out += ` × ${rb.multiplier} = ${rb.total}`;
  }
  if (rb.comparison && rb.target != null) {
    out += ` ${rb.comparison === 'lte' ? '≤' : '≥'} ${rb.target}`;
  }
  return out;
}
