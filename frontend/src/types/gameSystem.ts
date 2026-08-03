// Zentrale Typen + JSON-Transformationen für Game-System-Konfiguration.
// Einzige Quelle der Wahrheit für das Mapping zwischen WizardData (camelCase, UI)
// und rulesJson (wire-Format, Backend).

export interface AttributeDef {
  name: string;
  type: 'INT' | 'STRING' | 'BOOL';
  min: number;
  max: number;
  default: number;
}

export interface SkillDef {
  name: string;
  attributes: string[];
  bonus: number;
}

export interface DiceCombat {
  initiative: string;
  damage: string;
  actionPoints: { standard: number; max: number };
  actionTypes: string[];
  actionsPerTurn: Record<string, number>;
  criticalHit?: { threshold: number; multiplier: number };
  savingThrows?: { baseDc: number; proficiencyBonus: string };
  resting?: { shortRest: { healPercent: number; recoverResources: boolean }; longRest: { fullHeal: boolean; recoverAll: boolean } };
}

export interface SystemFeatures {
  magic: boolean;
  psionics: boolean;
  rangedCombat: boolean;
  criticalHits: boolean;
  armorPenalty: boolean;
}

export interface DerivedValue {
  name: string;
  formula: string;
}

export interface AbilityDef {
  name: string;
  type: 'active' | 'passive';
  costType: 'AP' | 'MP' | '';
  cost: number;
  diceExpression: string;
  effect: string;
  bonus: string;
  tags?: string[];
  category?: string;
  actionCost?: { type: string; amount: number };
  multiAttack?: number;
}

export interface LevelEntry {
  level: number;
  xpRequired: number;
  features: string;
}

export interface XpCostEntry {
  name: string;
  cost: number;
}

export interface ImprovementEntry {
  name: string;
  count: number;
  dice: string;
  comparison: 'gte' | 'lte';
  target: number;
}

export interface MagicSystem {
  manaFormula: string;
  spellSlots: string;
  schools: string;
}

export interface PsionicsSystem {
  powerPoints: string;
  disciplines: string;
}

export interface ConditionalDef {
  name: string;
  attribute: string;
  operator: 'gt' | 'gte' | 'lt' | 'lte' | 'eq' | 'per_point';
  value: number;
  bonus: string;
  target: string;
}

export interface WizardData {
  name: string;
  version: number;
  description: string;
  progressionType: 'level' | 'xp' | 'improvement' | null;
  features: SystemFeatures;
  probeType: 'd20_target' | 'd100_threshold' | 'd20_3attr';
  derivedValues: DerivedValue[];
  abilities: AbilityDef[];
  progression: {
    levels: LevelEntry[];
    xpCosts: XpCostEntry[];
    improvements: ImprovementEntry[];
  };
  magic: MagicSystem;
  psionics: PsionicsSystem;
  conditionals: ConditionalDef[];
  attributes: AttributeDef[];
  skills: SkillDef[];
  probe: string;
  enableCombat: boolean;
  combat: DiceCombat;
}

export const DEFAULT_FEATURES: SystemFeatures = {
  magic: false,
  psionics: false,
  rangedCombat: false,
  criticalHits: false,
  armorPenalty: false,
};

export function defaultWizardData(): WizardData {
  return {
    name: '',
    version: 1,
    description: '',
    progressionType: null,
    features: { ...DEFAULT_FEATURES },
    probeType: 'd20_target',
    derivedValues: [],
    abilities: [],
    progression: {
      levels: [
        { level: 1, xpRequired: 0, features: '' },
        { level: 2, xpRequired: 300, features: '' },
        { level: 3, xpRequired: 900, features: '' },
      ],
      xpCosts: [{ name: '', cost: 0 }],
      improvements: [{ name: '', count: 1, dice: '1d100', comparison: 'gte', target: 0 }],
    },
    magic: { manaFormula: '', spellSlots: '', schools: '' },
    psionics: { powerPoints: '', disciplines: '' },
    conditionals: [],
    attributes: [],
    skills: [],
    probe: '1d20+mod',
    enableCombat: false,
    combat: {
      initiative: '1d20+geschick',
      damage: '1d8+staerke',
      actionPoints: { standard: 1, max: 2 },
      actionTypes: ['action'],
      actionsPerTurn: { action: 1 },
      criticalHit: { threshold: 20, multiplier: 2 },
      savingThrows: { baseDc: 8, proficiencyBonus: '' },
      resting: {
        shortRest: { healPercent: 0.5, recoverResources: true },
        longRest: { fullHeal: true, recoverAll: true },
      },
    },
  };
}

/** WizardData → rulesJson (Backend-Wire-Format). */
export function toRulesJson(data: WizardData): string {
  const probeExpr =
    data.probeType === 'd20_target'
      ? '1d20+mod'
      : data.probeType === 'd100_threshold'
        ? '1d100'
        : '3d20';

  const rules: Record<string, unknown> = {
    version: data.version,
    probeType: data.probeType,
    progressionType: data.progressionType,
    features: data.features,
    derived_values: data.derivedValues,
    abilities: data.abilities,
    progression: data.progression,
    magic: data.magic,
    psionics: data.psionics,
    conditionals: data.conditionals,
    attributes: data.attributes,
    skills: data.skills,
    dice_mechanics: { probe: probeExpr },
  };
  if (data.enableCombat) {
    const combat: Record<string, unknown> = {
      initiative: data.combat.initiative,
      damage: data.combat.damage,
      action_points: data.combat.actionPoints,
      action_types: data.combat.actionTypes,
      actions_per_turn: data.combat.actionsPerTurn,
    };
    if (data.combat.criticalHit) combat.critical_hit = data.combat.criticalHit;
    if (data.combat.savingThrows) {
      combat.saving_throws = {
        base_dc: data.combat.savingThrows.baseDc,
        proficiency_bonus: data.combat.savingThrows.proficiencyBonus,
      };
    }
    if (data.combat.resting) {
      combat.resting = {
        short_rest: data.combat.resting.shortRest,
        long_rest: data.combat.resting.longRest,
      };
    }
    (rules.dice_mechanics as Record<string, unknown>).combat = combat;
  }
  return JSON.stringify(rules, null, 2);
}

/** rulesJson (Backend-Wire-Format) → WizardData. */
export function fromRulesJson(json: string): WizardData | null {
  try {
    const parsed = JSON.parse(json) as Record<string, unknown>;
    const attrs: AttributeDef[] = ((parsed.attributes as Record<string, unknown>[] | undefined) ?? []).map(
      (a: Record<string, unknown>) => ({
        name: (a.name as string) ?? '',
        type: (['INT', 'STRING', 'BOOL'].includes(a.type as string) ? a.type : 'INT') as 'INT' | 'STRING' | 'BOOL',
        min: (a.min as number) ?? 1,
        max: (a.max as number) ?? 20,
        default: (a.default as number) ?? 10,
      }),
    );
    const skills: SkillDef[] = ((parsed.skills as Record<string, unknown>[] | undefined) ?? []).map((s: Record<string, unknown>) => ({
      name: (s.name as string) ?? '',
      attributes: (s.attributes as string[]) ?? ((s.attribute as string) ? [s.attribute as string] : []),
      bonus: (s.bonus as number) ?? 0,
    }));
    const dice = (parsed.dice_mechanics ?? {}) as Record<string, unknown>;
    const combat = (dice.combat ?? {}) as Record<string, any>;

    return {
      name: (parsed.name as string) ?? '',
      version: (parsed.version as number) ?? 1,
      description: (parsed.description as string) ?? '',
      probeType: (parsed.probeType as WizardData['probeType']) ?? 'd20_target',
      attributes: attrs,
      skills,
      probe: (dice.probe as string) ?? '1d20+mod',
      enableCombat: !!dice.combat,
      combat: {
        initiative: combat.initiative ?? '1d20+geschick',
        damage: combat.damage ?? '1d8+staerke',
        actionPoints: {
          standard: combat.action_points?.standard ?? 1,
          max: combat.action_points?.max ?? 2,
        },
        actionTypes: combat.action_types ?? ['action'],
        actionsPerTurn: combat.actions_per_turn ?? { action: 1 },
        criticalHit: {
          threshold: combat.critical_hit?.threshold ?? 20,
          multiplier: combat.critical_hit?.multiplier ?? 2,
        },
        savingThrows: {
          baseDc: combat.saving_throws?.base_dc ?? 8,
          proficiencyBonus: combat.saving_throws?.proficiency_bonus ?? '',
        },
        resting: {
          shortRest: {
            healPercent: combat.resting?.short_rest?.heal_percent ?? 0.5,
            recoverResources: combat.resting?.short_rest?.recover_resources ?? true,
          },
          longRest: {
            fullHeal: combat.resting?.long_rest?.full_heal ?? true,
            recoverAll: combat.resting?.long_rest?.recover_all ?? true,
          },
        },
      },
      progressionType: (parsed.progressionType as WizardData['progressionType']) ?? null,
      features: {
        magic: (parsed.features as any)?.magic ?? false,
        psionics: (parsed.features as any)?.psionics ?? false,
        rangedCombat: (parsed.features as any)?.rangedCombat ?? false,
        criticalHits: (parsed.features as any)?.criticalHits ?? false,
        armorPenalty: (parsed.features as any)?.armorPenalty ?? false,
      },
      derivedValues: ((parsed.derived_values as Record<string, unknown>[]) ?? []).map(
        (dv: Record<string, unknown>) => ({
          name: (dv.name as string) ?? '',
          formula: (dv.formula as string) ?? '',
        }),
      ),
      abilities: ((parsed.abilities as Record<string, unknown>[]) ?? []).map(
        (a: Record<string, unknown>) => ({
          name: (a.name as string) ?? '',
          type: (a.type as 'active' | 'passive') ?? 'active',
          costType: (a.costType as 'AP' | 'MP' | '') ?? 'AP',
          cost: (a.cost as number) ?? 0,
          diceExpression: (a.diceExpression as string) ?? '',
          effect: (a.effect as string) ?? '',
          bonus: (a.bonus as string) ?? '',
        }),
      ),
      progression: {
        levels: ((parsed.progression as any)?.levels ?? []).map((lv: Record<string, unknown>) => ({
          level: (lv.level as number) ?? 0,
          xpRequired: (lv.xpRequired as number) ?? 0,
          features: (lv.features as string) ?? '',
        })),
        xpCosts: ((parsed.progression as any)?.xpCosts ?? []).map((xc: Record<string, unknown>) => ({
          name: (xc.name as string) ?? '',
          cost: (xc.cost as number) ?? 0,
        })),
        improvements: ((parsed.progression as any)?.improvements ?? []).map(
          (imp: Record<string, unknown>) => ({
            name: (imp.name as string) ?? '',
            count: (imp.count as number) ?? 1,
            dice: (imp.dice as string) ?? '1d6',
            comparison: (imp.comparison as 'gte' | 'lte') ?? 'gte',
            target: (imp.target as number) ?? 0,
          }),
        ),
      },
      magic: {
        manaFormula: (parsed.magic as any)?.manaFormula ?? '',
        spellSlots: (parsed.magic as any)?.spellSlots ?? '',
        schools: (parsed.magic as any)?.schools ?? '',
      },
      psionics: {
        powerPoints: (parsed.psionics as any)?.powerPoints ?? '',
        disciplines: (parsed.psionics as any)?.disciplines ?? '',
      },
      conditionals: ((parsed.conditionals as Record<string, unknown>[]) ?? []).map(
        (c: Record<string, unknown>) => ({
          name: (c.name as string) ?? '',
          attribute: (c.attribute as string) ?? '',
          operator: (c.operator as ConditionalDef['operator']) ?? 'gte',
          value: (c.value as number) ?? 0,
          bonus: (c.bonus as string) ?? '',
          target: (c.target as string) ?? '',
        }),
      ),
    };
  } catch {
    return null;
  }
}
