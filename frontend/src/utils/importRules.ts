/**
 * Import-Helfer für Regelsystem-JSON.
 * RULES_KEYS spiegelt die Top-Level-Keys aus RuleSchemaValidator (Backend).
 * Der Import darf keine backend-gültigen Keys verlieren (Befund Playtest:
 * traits/packages/conditions/advancement/creationBudget/attributeCosts
 * wurden stillschweigend gestrichen).
 */
export const RULES_KEYS = [
  'version', 'probeType', 'progressionType', 'features', 'modifierFormula',
  'creationBudget', 'attributeCosts', 'packages', 'traits', 'advancement',
  'derived_values', 'abilities', 'progression', 'magic', 'psionics',
  'conditionals', 'conditions', 'attributes', 'skills', 'dice_mechanics',
];

export function isBareRulesFormat(data: unknown): data is Record<string, unknown> {
  return (
    !!data && typeof data === 'object' && !('rulesJson' in (data as object)) &&
    !('rules_json' in (data as object)) &&
    ((data as Record<string, unknown>).attributes !== undefined ||
      (data as Record<string, unknown>).dice_mechanics !== undefined ||
      (data as Record<string, unknown>).progressionType !== undefined)
  );
}

export function sanitizeRulesForImport(data: Record<string, unknown>): string {
  return JSON.stringify(
    Object.fromEntries(Object.entries(data).filter(([k]) => RULES_KEYS.includes(k))),
  );
}
