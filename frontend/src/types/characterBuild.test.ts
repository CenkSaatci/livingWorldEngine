import { describe, it, expect } from 'vitest';
import { defaultWizardData, buildCost, buildFinalAttributes, buildFinalTraits, buildIssues, type CharacterBuild } from './gameSystem';

describe('Charakter-Build (P30-T01)', () => {
  const base = () => {
    const data = defaultWizardData();
    data.creationBudget = { ap: 100, attrBase: 8, maxAttrTotal: 100 };
    data.attributeCosts = { default: [{ upTo: 14, cost: 1 }, { upTo: 19, cost: 2 }] };
    data.attributes = [
      { name: 'mut', type: 'INT', min: 1, max: 20, default: 8 },
      { name: 'klugheit', type: 'INT', min: 1, max: 20, default: 8 },
      { name: 'koerperkraft', type: 'INT', min: 1, max: 20, default: 8 },
    ];
    data.packages = [
      {
        name: 'Elf', kind: 'species', cost: 18,
        attributeMods: [{ attr: 'mut', value: 1 }, { choice: ['klugheit', 'koerperkraft'], value: -1 }],
        autoTraits: ['Nachtsicht'],
      },
    ];
    data.traits = [
      { name: 'Zauberer', kind: 'advantage', costs: [{ tier: 'I', cost: 25 }] },
      { name: 'Hohe Lebenskraft', kind: 'advantage', costs: [{ tier: 'I', cost: 6 }, { tier: 'II', cost: 12 }, { tier: 'III', cost: 18 }] },
      { name: 'Niedrige Lebenskraft', kind: 'disadvantage', costs: [{ tier: 'I', cost: -6 }], excludes: ['Hohe Lebenskraft'] },
    ];
    return data;
  };

  const build: CharacterBuild = {
    packageSelections: [{ name: 'Elf', choices: ['klugheit'] }],
    attributes: { mut: 10 },
    traits: [{ name: 'Zauberer' }],
  };

  it('rechnet Attribut-Kauf auf dem gekauften Wert, Paket-Mods danach', () => {
    const data = base();
    const attrs = buildFinalAttributes(data, build);
    const mut = attrs.find((a) => a.name === 'mut')!;
    expect(mut.purchased).toBe(10);
    expect(mut.mod).toBe(1);
    expect(mut.value).toBe(11);

    const kl = attrs.find((a) => a.name === 'klugheit')!;
    expect(kl.purchased).toBe(8);       // nicht gekauft → Default
    expect(kl.mod).toBe(-1);            // Elf-Choice
    expect(kl.value).toBe(7);           // 8 - 1
  });

  it('summiert AP: Attribute + Traits + Pakete', () => {
    const data = base();
    // mut 8→10 = 2 AP (1 je Schritt), Zauberer 25, Elf 18
    const cost = buildCost(data, build);
    expect(cost.attributes).toBe(2);
    expect(cost.traits).toBe(25);
    expect(cost.packages).toBe(18);
    expect(cost.total).toBe(45);
    expect(cost.budget).toBe(100);
    expect(cost.over).toBe(false);
  });

  it('zählt Trait-Tiers und Auto-Traits korrekt', () => {
    const data = base();
    const withTier: CharacterBuild = { ...build, traits: [{ name: 'Hohe Lebenskraft', tier: 'III' }] };
    expect(buildCost(data, withTier).traits).toBe(18);
    expect(buildFinalTraits(data, withTier)).toEqual(['Hohe Lebenskraft III', 'Nachtsicht']);
    // Auto-Trait dedupliziert, wenn auch manuell gewählt
    const manual: CharacterBuild = { ...build, traits: [{ name: 'Nachtsicht' }] };
    expect(buildFinalTraits(data, manual)).toEqual(['Nachtsicht']);
  });

  it('meldet Budget-Over, Bereichsfehler und Trait-Exklusionen', () => {
    const data = base();
    const over: CharacterBuild = { ...build, attributes: { mut: 18 } }; // 8→18 = 14 AP, total 57 > 40-Budget
    data.creationBudget = { ap: 40 };
    expect(buildIssues(data, over)).toContain('build_over_budget');

    const outOfRange: CharacterBuild = { ...build, attributes: { mut: 25 } };
    expect(buildIssues(data, outOfRange)).toContain('build_attr_range:mut');

    const unfit: CharacterBuild = {
      ...build,
      traits: [{ name: 'Zauberer' }, { name: 'Hohe Lebenskraft' }, { name: 'Niedrige Lebenskraft' }],
    };
    const issues = buildIssues(data, unfit);
    expect(issues.some((i) => i.startsWith('build_trait_excludes:'))).toBe(true);

    // Paket-Auswahl-Fehler werden durchgereicht
    const badPkg: CharacterBuild = { ...build, packageSelections: [{ name: 'Elf' }] };
    expect(buildIssues(data, badPkg)).toContain('choice_count:Elf');
  });
});
