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
  /** Stabiler Code (Frontend-i18n), z. B. flat/attr/armor/condition/trait/malus/mod/value/maneuver. */
  kind: string;
  /** Optionaler Eigenname (Attribut-/Manövername). */
  label?: string | null;
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
 * "14 + 3 = 17 ≥ 12" (Angriff), "4 + 2 (staerke) - 1 (Rüstung) = 5" (Schaden),
 * mit Zielschutz "4 + 2 - 1 = 3 × 0.5 = 1".
 * @param t optionaler Übersetzer für die Posten-Codes (`combat.part_*`).
 */
export function formatRollBreakdown(
  rb: RollBreakdown,
  t?: (key: string) => string,
): string {
  const labelFor = (p: RollPart): string => {
    if (p.label) return p.label;
    if (!t) return '';
    return t(`combat.part_${p.kind}`);
  };
  const addends = [
    ...rb.dice,
    ...(rb.parts ?? []).map((p) => ({ value: p.value, label: labelFor(p) })),
  ];
  const lhs = addends
    .map((a, i) => {
      const n = typeof a === 'number' ? a : a.value;
      const label = typeof a === 'number' ? '' : a.label;
      const sign = i === 0 ? '' : n >= 0 ? '+ ' : '- ';
      const amount = i === 0 ? String(n) : String(Math.abs(n));
      return `${sign}${amount}${label ? ` (${label})` : ''}`;
    })
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
