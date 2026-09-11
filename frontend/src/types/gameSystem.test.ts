import { describe, it, expect } from 'vitest';
import { defaultWizardData, toRulesJson, fromRulesJson, attrPointCost, calcBudget, traitCost, danglingTraitRefs, traitSelectionErrors } from './gameSystem';

describe('gameSystem roundtrip', () => {
  it('toRulesJson/fromRulesJson preserves all ability fields', () => {
    const data = defaultWizardData();
    data.abilities = [
      {
        name: 'Feuerball',
        type: 'active',
        costType: 'AP',
        cost: 2,
        diceExpression: '2d6+intelligenz',
        effect: 'Feuerschaden',
        bonus: '',
        tags: ['attack', 'magic'],
        category: 'ability',
        actionCost: { type: 'action', amount: 1 },
        multiAttack: 2,
      },
      {
        name: 'Nachladen',
        type: 'passive',
        costType: '',
        cost: 0,
        diceExpression: '',
        effect: '',
        bonus: 'action_cost:-1',
        tags: ['ranged'],
      },
    ];

    const json = toRulesJson(data);
    const restored = fromRulesJson(json);

    expect(restored).not.toBeNull();
    const ability = restored!.abilities[0];
    expect(ability.name).toBe('Feuerball');
    expect(ability.tags).toEqual(['attack', 'magic']);
    expect(ability.category).toBe('ability');
    expect(ability.actionCost).toEqual({ type: 'action', amount: 1 });
    expect(ability.multiAttack).toBe(2);

    const passive = restored!.abilities[1];
    expect(passive.tags).toEqual(['ranged']);
    expect(passive.bonus).toBe('action_cost:-1');
  });

  it('roundtrip preserves combat critical/saving/resting', () => {
    const data = defaultWizardData();
    data.enableCombat = true;
    data.combat.criticalHit = { threshold: 19, multiplier: 3 };
    data.combat.savingThrows = { baseDc: 10, proficiencyBonus: 'floor((attr-10)/2)' };
    data.combat.resting = {
      shortRest: { healPercent: 0.25, recoverResources: false },
      longRest: { fullHeal: true, recoverAll: false },
    };

    const restored = fromRulesJson(toRulesJson(data));

    expect(restored!.combat.criticalHit).toEqual({ threshold: 19, multiplier: 3 });
    expect(restored!.combat.savingThrows?.baseDc).toBe(10);
    expect(restored!.combat.resting?.shortRest.healPercent).toBe(0.25);
    expect(restored!.combat.resting?.longRest.recoverAll).toBe(false);
  });

  it('roundtrip preserves derived values, skills and conditionals', () => {
    const data = defaultWizardData();
    data.derivedValues = [{ name: 'hp', formula: '10+konstitution' }];
    data.skills = [{ name: 'Athletik', attributes: ['staerke'], bonus: 2 }];
    data.conditionals = [
      { name: 'Stark', attribute: 'staerke', operator: 'gt', value: 14, bonus: '+2', target: 'schaden' },
    ];

    const restored = fromRulesJson(toRulesJson(data));

    expect(restored!.derivedValues).toEqual([{ name: 'hp', formula: '10+konstitution' }]);
    expect(restored!.skills).toEqual([{ name: 'Athletik', attributes: ['staerke'], bonus: 2 }]);
    expect(restored!.conditionals[0].operator).toBe('gt');
  });

  it('roundtrip preserves P28 blocks', () => {    const data = defaultWizardData();
    data.creationBudget = { ap: 1100, maxAttrTotal: 100, fatePoints: 3 };
    data.attributeCosts = { default: [{ upTo: 14, cost: 15 }] };
    data.packages = [{ name: 'Elf', kind: 'species', cost: 18 }];
    data.traits = [{ name: 'Glück', kind: 'advantage', costs: [{ tier: 'I', cost: 30 }] }];
    data.advancement = { columns: ['A', 'B'], table: [] };
    data.attributes = [{ name: 'staerke', type: 'INT', min: 1, max: 20, default: 10, costs: [{ upTo: 14, cost: 15 }] }];
    data.skills = [{ name: 'Athletik', attributes: ['staerke'], bonus: 0, costColumn: 'C', activationCost: 0 }];
    data.derivedValues = [{ name: 'sk', input: 'mut+klugheit', table: [{ min: 24, max: 26, value: 4 }], requiresTrait: 'Zauberer' }];

    const restored = fromRulesJson(toRulesJson(data));

    expect(restored!.creationBudget).toEqual({ ap: 1100, maxAttrTotal: 100, fatePoints: 3 });
    expect(restored!.attributeCosts).toEqual({ default: [{ upTo: 14, cost: 15 }] });
    expect(restored!.packages).toEqual([{ name: 'Elf', kind: 'species', cost: 18 }]);
    expect(restored!.traits).toEqual([{ name: 'Glück', kind: 'advantage', costs: [{ tier: 'I', cost: 30 }] }]);
    expect(restored!.advancement).toEqual({ columns: ['A', 'B'], table: [] });
    expect(restored!.attributes[0].costs).toEqual([{ upTo: 14, cost: 15 }]);
    expect(restored!.skills[0].costColumn).toBe('C');
    expect(restored!.derivedValues).toEqual([
      { name: 'sk', input: 'mut+klugheit', table: [{ min: 24, max: 26, value: 4 }], requiresTrait: 'Zauberer' },
    ]);
  });
});

describe('budget (P28-T02)', () => {
  const DSA = [
    { upTo: 14, cost: 15 },
    { upTo: 15, cost: 30 },
    { upTo: 16, cost: 45 },
  ];

  it('attrPointCost follows tier curve', () => {
    expect(attrPointCost(8, 14, DSA)).toBe(90); // 6 x 15
    expect(attrPointCost(14, 15, DSA)).toBe(30);
    expect(attrPointCost(10, 10, DSA)).toBe(0);
    expect(attrPointCost(14, 8, DSA)).toBe(0); // lowering costs nothing
    expect(attrPointCost(8, 10, [])).toBe(0); // no tiers = free
  });

  it('calcBudget totals spend and flags overrun', () => {
    const data = defaultWizardData();
    data.creationBudget = { ap: 100, maxAttrTotal: 100 };
    data.attributes = [
      { name: 'a', type: 'INT', min: 1, max: 20, default: 14, costs: DSA },
      { name: 'b', type: 'INT', min: 1, max: 20, default: 8, costs: DSA },
    ];
    // ohne attrBase: ab min => a: 1->14, b: 1->8
    const res = calcBudget(data);
    expect(res.spend).toBeGreaterThan(0);
    expect(res.over).toBe(res.spend > 100);
    expect(res.perAttr.a).toBe(attrPointCost(1, 14, DSA));
  });

  it('calcBudget counts from attrBase when set (DSA: 8 gratis)', () => {
    const data = defaultWizardData();
    data.creationBudget = { ap: 1100, attrBase: 8, maxAttrTotal: 100 };
    data.attributes = [
      { name: 'a', type: 'INT', min: 1, max: 20, default: 14, costs: DSA },
    ];
    const res = calcBudget(data);
    expect(res.perAttr.a).toBe(90); // 6 x 15
    expect(res.spend).toBe(90);
    expect(res.over).toBe(false);
  });

  it('calcBudget without budget returns nullish spend-only', () => {
    const data = defaultWizardData();
    const res = calcBudget(data);
    expect(res.over).toBe(false);
  });
});

describe('traits (P28-T03)', () => {
  const catalog = [
    { name: 'Zauberer', kind: 'advantage', requires: ['Tradition'] },
    { name: 'Tradition', kind: 'advantage' },
    { name: 'Glück', kind: 'advantage', costs: [{ tier: 'I', cost: 30 }, { tier: 'II', cost: 60 }], excludes: ['Pech'] },
    { name: 'Pech', kind: 'disadvantage' },
  ];

  it('traitCost picks tier or first, 0 without costs', () => {
    const glueck = catalog[2];
    expect(traitCost(glueck)).toBe(30);
    expect(traitCost(glueck, 'II')).toBe(60);
    expect(traitCost(glueck, 'III')).toBe(0);
    expect(traitCost(catalog[0])).toBe(0);
  });

  it('danglingTraitRefs finds unknown references', () => {
    const dangling = danglingTraitRefs([
      ...catalog,
      { name: 'Riese', kind: 'advantage', requires: ['Feenblut'], excludes: ['Pech'] },
    ]);
    expect(dangling).toEqual([{ name: 'Riese', missing: ['Feenblut'] }]);
  });

  it('traitSelectionErrors catches unknown, requires, excluded', () => {
    expect(traitSelectionErrors(catalog, ['Zauberer'])).toEqual([
      { trait: 'Zauberer', issue: 'requires', detail: 'Tradition' },
    ]);
    expect(traitSelectionErrors(catalog, ['Glück', 'Pech'])).toEqual([
      { trait: 'Glück', issue: 'excludes', detail: 'Pech' },
    ]);
    expect(traitSelectionErrors(catalog, ['Nix'])).toEqual([
      { trait: 'Nix', issue: 'unknown', detail: '' },
    ]);
    expect(traitSelectionErrors(catalog, ['Glück II', 'Tradition'])).toEqual([]);
  });
});
