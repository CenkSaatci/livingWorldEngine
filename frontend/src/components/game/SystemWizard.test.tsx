import { describe, it, expect } from 'vitest';
import { render } from '@testing-library/react';
import { SystemWizard, type WizardData } from './SystemWizard';

vi.mock('../../api/client', () => ({
  apiClient: { post: vi.fn().mockResolvedValue({ data: { id: 'gs1' } }), patch: vi.fn() },
}));

function baseData(overrides: Partial<WizardData> = {}): WizardData {
  return {
    name: 'Test', version: 1, description: '',
    probeType: 'd20_target',
    progressionType: null,
    features: { magic: false, psionics: false, rangedCombat: false, criticalHits: false, armorPenalty: false },
    derivedValues: [], abilities: [],
    progression: { levels: [], xpCosts: [], improvements: [] },
    magic: { manaFormula: '', spellSlots: '', schools: '' },
    psionics: { powerPoints: '', disciplines: '' },
    conditionals: [],
    attributes: [{ name: 'staerke', type: 'INT', min: 3, max: 20, default: 10 }],
    skills: [],
    probe: '1d20+mod', enableCombat: false,
    combat: { initiative: '1d20', damage: '1d8', actionPoints: { standard: 1, max: 2 }, actionTypes: ['action'], actionsPerTurn: { action: 1 } },
    ...overrides,
  };
}

describe('SystemWizard', () => {
  it('loads d20_target initialData without error', () => {
    expect(() => render(<SystemWizard onSaved={() => {}} onClose={() => {}} initialData={baseData()} />)).not.toThrow();
  });

  it('loads d100_threshold initialData without error', () => {
    expect(() => render(<SystemWizard onSaved={() => {}} onClose={() => {}} initialData={baseData({ probeType: 'd100_threshold' })} />)).not.toThrow();
  });

  it('loads d20_3attr with 3-attribute skill without error', () => {
    expect(() => render(<SystemWizard onSaved={() => {}} onClose={() => {}} initialData={baseData({
      probeType: 'd20_3attr',
      attributes: [
        { name: 'mut', type: 'INT', min: 1, max: 21, default: 8 },
        { name: 'klugheit', type: 'INT', min: 1, max: 21, default: 8 },
      ],
      skills: [{ name: 'Fliegen', attributes: ['mut', 'klugheit'], bonus: 0 }],
    })} />)).not.toThrow();
  });

  it('loads actionTypes config without error', () => {
    expect(() => render(<SystemWizard onSaved={() => {}} onClose={() => {}} initialData={baseData({
      enableCombat: true,
      combat: { initiative: '1d20', damage: '1d8', actionPoints: { standard: 1, max: 2 }, actionTypes: ['action', 'bonus_action'], actionsPerTurn: { action: 1, bonus_action: 1 } },
      abilities: [{ name: 'Angriff', type: 'active', costType: 'AP', cost: 1, diceExpression: '1d20', effect: '', bonus: '', tags: ['attack'], category: 'ability' }],
    })} />)).not.toThrow();
  });

  it('loads tags and category on ability without error', () => {
    expect(() => render(<SystemWizard onSaved={() => {}} onClose={() => {}} initialData={baseData({
      abilities: [{ name: 'Feat', type: 'passive', costType: '', cost: 0, diceExpression: '', effect: '', bonus: '+2', tags: ['defensive'], category: 'advantage' }],
    })} />)).not.toThrow();
  });
});
