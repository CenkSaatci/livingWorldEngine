import { describe, it, expect } from 'vitest';
import { defaultWizardData, toRulesJson, fromRulesJson } from './gameSystem';

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

  it('roundtrip preserves P28 blocks', () => {
    const data = defaultWizardData();
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
