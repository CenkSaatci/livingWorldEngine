import { describe, it, expect } from 'vitest';
import dsa5json from '../../../docs/examples/dsa5.json';
import { defaultWizardData, toRulesJson, fromRulesJson, attrPointCost, calcBudget, traitCost, danglingTraitRefs, traitSelectionErrors, advanceCost, skillAdvanceCost, wizardIssues, resolvePackageMods, packageCost, packageAutoTraits, packageSelectionIssues, packageSelectionWarnings, type PackageSelection } from './gameSystem';

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
        damageType: 'fire',
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
    expect(ability.damageType).toBe('fire');

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

describe('advancement (P28-T04)', () => {
  const adv = {
    columns: ['A', 'B', 'C', 'D'],
    table: [
      { from: 1, to: 12, costs: { A: 1, B: 2, C: 3, D: 4 } },
      { from: 13, to: 13, costs: { A: 2, B: 4, C: 6, D: 8 } },
    ],
    maxRule: 'highestAttributePlus2',
  };

  it('advanceCost finds row by target value and column', () => {
    expect(advanceCost(adv, 'A', 12)).toBe(1);
    expect(advanceCost(adv, 'C', 13)).toBe(6);
    expect(advanceCost(adv, 'A', 14)).toBeNull();
    expect(advanceCost({ columns: [], table: [] }, 'A', 5)).toBeNull();
    expect(advanceCost(undefined, 'A', 5)).toBeNull();
  });

  it('skillAdvanceCost covers activation and next step', () => {
    const spell = { name: 'Ignifaxius', attributes: ['klugheit'], bonus: 0, costColumn: 'C', activationCost: 3 };
    const talent = { name: 'Athletik', attributes: ['staerke'], bonus: 0, costColumn: 'B' };
    expect(skillAdvanceCost(adv, spell, 0)).toBe(3); // activation
    expect(skillAdvanceCost(adv, spell, 3)).toBe(3); // next step 4 → row 1-12, C=3
    expect(skillAdvanceCost(adv, talent, 12)).toBe(4); // next step 13 → B=4
    expect(skillAdvanceCost(adv, talent, 13)).toBeNull(); // beyond table
    expect(skillAdvanceCost(undefined, talent, 5)).toBeNull();
  });
});

describe('wizard save gate (P28 audit)', () => {
  it('wizardIssues finds blockers in order', () => {
    const data = defaultWizardData();
    expect(wizardIssues(data)).toEqual(['v_need_attributes']);

    data.attributes = [{ name: '', type: 'INT', min: 1, max: 20, default: 10 }];
    expect(wizardIssues(data)).toEqual(['v_empty_attribute_name']);

    data.attributes[0].name = 'staerke';
    data.traits = [{ name: '', kind: 'advantage' }];
    expect(wizardIssues(data)).toEqual(['v_empty_trait_name']);

    data.traits[0].name = 'Glück';
    expect(wizardIssues(data)).toEqual([]);
  });
});

describe('packages (P29-T05)', () => {
  const withPackages = () => {
    const data = defaultWizardData();
    data.attributes = [
      { name: 'MU', type: 'INT', min: 1, max: 20, default: 10 },
      { name: 'KK', type: 'INT', min: 1, max: 20, default: 10 },
      { name: 'KO', type: 'INT', min: 1, max: 20, default: 10 },
    ];
    data.packages = [
      {
        name: 'Elf', kind: 'species', cost: 18,
        attributeMods: [{ attr: 'MU', value: 1 }, { choice: ['KK', 'KO'], value: -1 }],
        autoTraits: ['Nachtsicht'],
        recommended: ['Waldelf'],
      },
      { name: 'Waldelf', kind: 'culture', cost: 0, restricted: ['Zwerg'] },
      { name: 'Zwerg', kind: 'species', cost: 12 },
      { name: 'Söldner', kind: 'profession', cost: 5, attributeMods: [{ choice: '*', value: 1 }] },
    ];
    return data;
  };

  it('resolves fixed and choice attribute mods (Elf-Äquivalent)', () => {
    const data = withPackages();
    const sel: PackageSelection[] = [{ name: 'Elf', choices: ['KK'] }];
    const mods = resolvePackageMods(data.packages!, sel);
    expect(mods.get('MU')).toBe(1);
    expect(mods.get('KK')).toBe(-1);
    expect(mods.has('KO')).toBe(false);
    expect(packageCost(data.packages!, sel)).toBe(18);
    expect(packageAutoTraits(data.packages!, sel)).toEqual(['Nachtsicht']);
  });

  it('roundtrips packages through toRulesJson/fromRulesJson', () => {
    const data = withPackages();
    const restored = fromRulesJson(toRulesJson(data));
    expect(restored!.packages).toEqual(data.packages);
  });

  it('validates choice groups exactly once and flags untypical combos', () => {
    const data = withPackages();
    const ok: PackageSelection[] = [{ name: 'Elf', choices: ['KK'] }, { name: 'Waldelf' }];
    expect(packageSelectionIssues(data, ok)).toEqual([]);
    // Elf empfiehlt Waldelf → gewählt: keine Warnung
    expect(packageSelectionWarnings(data, ok)).toEqual([]);

    const missing = packageSelectionIssues(data, [{ name: 'Elf' }]);
    expect(missing).toContain('choice_count:Elf');
    const badChoice = packageSelectionIssues(data, [{ name: 'Elf', choices: ['GE'] }]);
    expect(badChoice).toContain('choice_invalid:Elf:GE');
    const star = packageSelectionIssues(data, [{ name: 'Söldner', choices: ['KO'] }]);
    expect(star).toEqual([]);
    const starBad = packageSelectionIssues(data, [{ name: 'Söldner', choices: ['XX'] }]);
    expect(starBad).toContain('choice_invalid:Söldner:XX');

    const restricted = packageSelectionIssues(data, [{ name: 'Zwerg' }, { name: 'Waldelf' }]);
    expect(restricted).toContain('restricted:Waldelf:Zwerg');

    const duplicate = packageSelectionIssues(data, [{ name: 'Elf', choices: ['KK'] }, { name: 'Zwerg' }]);
    expect(duplicate).toContain('duplicate_kind:species');

    const noCulture = packageSelectionWarnings(data, [{ name: 'Elf', choices: ['KK'] }]);
    expect(noCulture).toContain('recommended:Elf:Waldelf');
  });
});

describe('DSA5-Referenz (P29-T06)', () => {
  it('parses the reference system and applies an Elf package selection', () => {
    const data = fromRulesJson(JSON.stringify(dsa5json))!;
    expect(data.packages).toHaveLength(4);
    expect(data.packages!.map((p) => p.name)).toEqual(['Elf', 'Waldelf', 'Zwerg', 'Jäger']);

    const sel: PackageSelection[] = [
      { name: 'Elf', choices: ['klugheit'] },
      { name: 'Waldelf' },
      { name: 'Jäger', choices: ['mut'] },
    ];
    const mods = resolvePackageMods(data.packages!, sel);
    expect(mods.get('mut')).toBe(2);          // Elf +1, Jäger +1
    expect(mods.get('gewandtheit')).toBe(1);  // Elf +1
    expect(mods.get('klugheit')).toBe(-1);    // Elf wählt KL -1
    expect(packageCost(data.packages!, sel)).toBe(118); // 18 + 0 + 100
    expect(packageAutoTraits(data.packages!, sel)).toEqual(['Nachtsicht']);
    expect(packageSelectionIssues(data, sel)).toEqual([]);
    expect(packageSelectionWarnings(data, sel)).toEqual([]);
  });

  it('flags restricted species combos and missing culture', () => {
    const data = fromRulesJson(JSON.stringify(dsa5json))!;
    const bad: PackageSelection[] = [{ name: 'Zwerg' }, { name: 'Elf', choices: ['intuition'] }];
    expect(packageSelectionIssues(data, bad)).toContain('restricted:Zwerg:Elf');
    expect(packageSelectionIssues(data, bad)).toContain('duplicate_kind:species');

    const noCulture: PackageSelection[] = [{ name: 'Elf', choices: ['intuition'] }];
    expect(packageSelectionWarnings(data, noCulture)).toContain('recommended:Elf:Waldelf');
  });
});
