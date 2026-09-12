import { describe, it, expect } from 'vitest';
import { RULES_KEYS, isBareRulesFormat, sanitizeRulesForImport } from './importRules';

describe('sanitizeRulesForImport', () => {
  it('behält alle backend-gültigen Top-Level-Keys (kein stiller Verlust)', () => {
    const full: Record<string, unknown> = {};
    for (const k of RULES_KEYS) full[k] = k === 'version' ? 1 : [];
    const parsed = JSON.parse(sanitizeRulesForImport(full));
    expect(Object.keys(parsed).sort()).toEqual([...RULES_KEYS].sort());
  });

  it('streicht Doku-Felder aus Beispiel-Dateien', () => {
    const parsed = JSON.parse(
      sanitizeRulesForImport({ name: 'DSA', description: 'x', _comment: 'y', version: 1, attributes: [] }),
    );
    expect(parsed).toEqual({ version: 1, attributes: [] });
  });

  it('erkennt Bare-Rules- vs. Wire-Format', () => {
    expect(isBareRulesFormat({ attributes: [] })).toBe(true);
    expect(isBareRulesFormat({ rulesJson: '{}' })).toBe(false);
    expect(isBareRulesFormat({ rules_json: '{}' })).toBe(false);
    expect(isBareRulesFormat(null)).toBe(false);
  });
});
