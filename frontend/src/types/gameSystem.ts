// Zentrale Typen + JSON-Transformationen für Game-System-Konfiguration.
// Einzige Quelle der Wahrheit für das Mapping zwischen WizardData (camelCase, UI)
// und rulesJson (wire-Format, Backend).

export interface AttributeDef {
  name: string;
  type: 'INT' | 'STRING' | 'BOOL';
  min: number;
  max: number;
  default: number;
  costs?: AttributeCostTier[];
}

export interface SkillDef {
  name: string;
  attributes: string[];
  bonus: number;
  costColumn?: string;
  activationCost?: number;
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
  formula?: string;
  input?: string;
  table?: { min: number; max: number; value: number }[];
  requiresTrait?: string;
}

export interface AbilityDef {
  name: string;
  type: 'active' | 'passive';
  costType: 'AP' | 'MP' | '';
  cost: number;
  diceExpression: string;
  damageType?: string;
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

// P28-Blöcke. Absichtlich schlank typisiert — Verschärfung (Pflichtfelder,
// Wertebereiche, Effekt-Semantik) erfolgt in T02–T05 mit den Features.
export interface AttributeCostTier {
  upTo: number;
  cost: number;
}

export interface CreationBudget {
  ap: number;
  apCarryoverMax?: number;
  fatePoints?: number;
  // Ab diesem Attributwert kosten Punkte (DSA: 8, d. h. 8 ist gratis).
  // Fehlt er, zählt ab Attribut-min.
  attrBase?: number;
  maxAttrTotal?: number;
  maxAttrValue?: number;
  maxSkillValue?: number;
  maxCombatValue?: number;
  maxSpells?: number;
  maxAdvantageAp?: number;
}

export interface PkgDef {
  name: string;
  kind: string;
  cost?: number;
}

export interface TraitCost {
  tier: string;
  cost: number;
}

export interface TraitEffect {
  target: string;
  op: string;
  value: number;
}

export interface TraitDef {
  name: string;
  kind: string;
  costs?: TraitCost[];
  requires?: string[];
  excludes?: string[];
  effects?: TraitEffect[];
}

export interface AdvancementDef {
  columns?: string[];
  table?: { from: number; to: number; costs: Record<string, number> }[];
  maxRule?: string;
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
  // P28-Blöcke, alle optional (alte Systeme und defaultWizardData bleiben gültig).
  creationBudget?: CreationBudget;
  attributeCosts?: { default: AttributeCostTier[] };
  packages?: PkgDef[];
  traits?: TraitDef[];
  advancement?: AdvancementDef;
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

/**
 * Ersetzt den `mod`-Platzhalter (steht für den jeweils relevanten Modifikator)
 * durch 0, damit Test-Würfe als reine Syntax-/Würfelprobe funktionieren.
 * Echte Proben laufen immer über /rolls bzw. /rolls/probe mit Modifikator.
 * Kampf-Expressionen (Initiative/Damage) referenzieren Attributnamen
 * (z.B. `1d20+geschick`), die /rolls/free (nur NdM±Zahl) ablehnen würde —
 * daher werden auch `±attribut`-Suffixe zu `+0` neutralisiert.
 */
export function testExpression(expression: string): string {
  return expression
    .replace(/\bmod\b/g, '0')
    .replace(/([+-])\s*[A-Za-z_][A-Za-z0-9_]*/g, (_m, sign: string) => `${sign}0`);
}

/**
 * Kosten für Attribut-Steigerung von `from` (exklusiv) bis `to` (inklusiv)
 * entlang der Staffel. Leer/fehlend = gratis, Senken = gratis, über letzte
 * Stufe hinaus = letzte Stufe. Reine Funktion für UI + Tests.
 */
export function attrPointCost(from: number, to: number, tiers: AttributeCostTier[]): number {
  if (!tiers.length || to <= from) return 0;
  const sorted = [...tiers].sort((a, b) => a.upTo - b.upTo);
  let total = 0;
  for (let v = from + 1; v <= to; v++) {
    total += (sorted.find((t) => v <= t.upTo) ?? sorted[sorted.length - 1]).cost;
  }
  return total;
}

export interface BudgetResult {
  spend: number;
  attrTotal: number;
  ap: number | null;
  over: boolean;
  perAttr: Record<string, number>;
}

/** Summiert Attribut-Ausgaben (ab attrBase bzw. min) und prüft Budget/Caps. */
// ponytail: zählt nur Attribute — Skills/Traits/Zauber kommen mit T03/T04 in die Summe.
export function calcBudget(data: WizardData): BudgetResult {
  const perAttr: Record<string, number> = {};
  let spend = 0;
  let attrTotal = 0;
  for (const a of data.attributes) {
    const from = data.creationBudget?.attrBase ?? a.min;
    const tiers = a.costs ?? data.attributeCosts?.default ?? [];
    const c = attrPointCost(from, a.default, tiers);
    perAttr[a.name] = c;
    spend += c;
    attrTotal += a.default;
  }
  const ap = data.creationBudget?.ap ?? null;
  const maxTotal = data.creationBudget?.maxAttrTotal;
  return {
    spend,
    attrTotal,
    ap,
    over: (ap != null && spend > ap) || (maxTotal != null && attrTotal > maxTotal),
    perAttr,
  };
}

// --- Traits (P28-T03): reine Helper für UI + spätere Heldenauswahl ---

export function traitCost(def: TraitDef, tier?: string): number {
  const costs = def.costs ?? [];
  if (!costs.length) return 0;
  return (tier ? costs.find((c) => c.tier === tier) : costs[0])?.cost ?? 0;
}

export function danglingTraitRefs(traits: TraitDef[]): { name: string; missing: string[] }[] {
  const names = new Set(traits.map((t) => t.name));
  const out: { name: string; missing: string[] }[] = [];
  for (const t of traits) {
    const missing = [...new Set([...(t.requires ?? []), ...(t.excludes ?? [])].filter((n) => !names.has(n)))];
    if (missing.length) out.push({ name: t.name, missing });
  }
  return out;
}

export interface TraitIssue {
  trait: string;
  issue: 'unknown' | 'requires' | 'excludes';
  detail: string;
}

/** Prüft eine Trait-Auswahl (Held): unbekannt, fehlende Voraussetzung, Ausschluss.
 *  Tier-Suffixe ("Glück II") werden wie im Backend ignoriert. */
export function traitSelectionErrors(traits: TraitDef[], selected: string[]): TraitIssue[] {
  const issues: TraitIssue[] = [];
  const matches = (name: string, candidate: string) =>
    candidate === name || candidate.startsWith(name + ' ');
  const defOf = (sel: string) => traits.find((t) => matches(t.name, sel));
  for (const sel of selected) {
    const def = defOf(sel);
    if (!def) {
      issues.push({ trait: sel, issue: 'unknown', detail: '' });
      continue;
    }
    for (const req of def.requires ?? []) {
      if (!selected.some((s) => matches(req, s))) {
        issues.push({ trait: def.name, issue: 'requires', detail: req });
      }
    }
  }
  for (const def of traits) {
    if (!selected.some((s) => matches(def.name, s))) continue;
    for (const ex of def.excludes ?? []) {
      if (selected.some((s) => matches(ex, s))) {
        issues.push({ trait: def.name, issue: 'excludes', detail: ex });
      }
    }
  }
  return issues;
}

// --- Advancement (P28-T04): Kosten der nächsten Steigerung ---
/** Kosten, um `target` (Zielwert) in `column` zu erreichen. null = außerhalb der Matrix. */
export function advanceCost(
  advancement: AdvancementDef | undefined,
  column: string,
  target: number,
): number | null {
  const row = advancement?.table?.find((r) => target >= r.from && target <= r.to);
  if (!row) return null;
  const cost = row.costs?.[column];
  return typeof cost === 'number' ? cost : null;
}

/** Kosten des nächsten Schritts für einen Skill: Aktivierung oder Matrix-Zeile. */
export function skillAdvanceCost(
  advancement: AdvancementDef | undefined,
  skill: SkillDef,
  fromValue: number,
): number | null {
  if (skill.activationCost != null && fromValue <= 0) return skill.activationCost;
  if (!skill.costColumn) return null;
  return advanceCost(advancement, skill.costColumn, fromValue + 1);
}

/**
 * Blocker für den Save-Gate (Audit P28): liefert i18n-Keys statt Text.
 * Backend-Schema würde sonst mit generischem 400 antworten.
 */
export function wizardIssues(data: WizardData): string[] {
  if (data.attributes.length === 0) return ['v_need_attributes'];
  if (data.attributes.some((a) => !a.name.trim())) return ['v_empty_attribute_name'];
  if ((data.traits ?? []).some((tr) => !tr.name.trim())) return ['v_empty_trait_name'];
  return [];
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
    // progressionType nur setzen, wenn gewählt — das Backend-Schema verlangt
    // einen String und würde `null` ablehnen.
    ...(data.progressionType ? { progressionType: data.progressionType } : {}),
    // P28-Blöcke: nur setzen, wenn vorhanden (abwärtskompatibel).
    ...(data.creationBudget ? { creationBudget: data.creationBudget } : {}),
    ...(data.attributeCosts ? { attributeCosts: data.attributeCosts } : {}),
    ...(data.packages ? { packages: data.packages } : {}),
    ...(data.traits ? { traits: data.traits } : {}),
    ...(data.advancement ? { advancement: data.advancement } : {}),
    features: data.features,
    derived_values: data.derivedValues,
    abilities: data.abilities,
    // Backend-Wire-Format für Level: {level, xp, attribute_points, ability_slots}.
    // (Wizard-intern heißt das XP-Feld xpRequired — hier wird gemappt.)
    progression: {
      levels: data.progression.levels.map((lv) => ({
        level: lv.level,
        xp: lv.xpRequired,
        attribute_points: 0,
        ability_slots: 0,
      })),
      xpCosts: data.progression.xpCosts,
      improvements: data.progression.improvements,
    },
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
        short_rest: {
          heal_percent: data.combat.resting.shortRest.healPercent,
          recover_resources: data.combat.resting.shortRest.recoverResources,
        },
        long_rest: {
          full_heal: data.combat.resting.longRest.fullHeal,
          recover_all: data.combat.resting.longRest.recoverAll,
        },
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
        ...(a.costs !== undefined ? { costs: a.costs as AttributeCostTier[] } : {}),
      }),
    );
    const skills: SkillDef[] = ((parsed.skills as Record<string, unknown>[] | undefined) ?? []).map((s: Record<string, unknown>) => ({
      name: (s.name as string) ?? '',
      attributes: (s.attributes as string[]) ?? ((s.attribute as string) ? [s.attribute as string] : []),
      bonus: (s.bonus as number) ?? 0,
      ...(s.costColumn !== undefined ? { costColumn: s.costColumn as string } : {}),
      ...(s.activationCost !== undefined ? { activationCost: s.activationCost as number } : {}),
    }));
    const dice = (parsed.dice_mechanics ?? {}) as Record<string, unknown>;
    const combat = (dice.combat ?? {}) as Record<string, any>;

    return {
      name: (parsed.name as string) ?? '',
      version: (parsed.version as number) ?? 1,
      description: (parsed.description as string) ?? '',
      probeType: (parsed.probeType as WizardData['probeType']) ?? 'd20_target',
      // P28-Blöcke: fehlen → undefined (alte JSONs bleiben unverändert lesbar).
      creationBudget: parsed.creationBudget as CreationBudget | undefined,
      attributeCosts: parsed.attributeCosts as { default: AttributeCostTier[] } | undefined,
      packages: parsed.packages as PkgDef[] | undefined,
      traits: parsed.traits as TraitDef[] | undefined,
      advancement: parsed.advancement as AdvancementDef | undefined,
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
          ...(dv.formula !== undefined ? { formula: dv.formula as string } : {}),
          ...(dv.input !== undefined ? { input: dv.input as string } : {}),
          ...(dv.table !== undefined
            ? { table: dv.table as { min: number; max: number; value: number }[] }
            : {}),
          ...(dv.requiresTrait !== undefined ? { requiresTrait: dv.requiresTrait as string } : {}),
        }),
      ),
      abilities: ((parsed.abilities as Record<string, unknown>[]) ?? []).map(
        (a: Record<string, unknown>) => ({
          name: (a.name as string) ?? '',
          type: (a.type as 'active' | 'passive') ?? 'active',
          costType: (a.costType as 'AP' | 'MP' | '') ?? 'AP',
          cost: (a.cost as number) ?? 0,
          diceExpression: (a.diceExpression as string) ?? '',
          damageType: a.damageType as string | undefined,
          effect: (a.effect as string) ?? '',
          bonus: (a.bonus as string) ?? '',
          tags: a.tags as string[] | undefined,
          category: a.category as string | undefined,
          actionCost: a.actionCost as { type: string; amount: number } | undefined,
          multiAttack: a.multiAttack as number | undefined,
        }),
      ),
      progression: {
        // Wire-Format (xp) und Wizard-Format (xpRequired) werden beide gelesen.
        levels: ((parsed.progression as any)?.levels ?? []).map((lv: Record<string, unknown>) => ({
          level: (lv.level as number) ?? 0,
          xpRequired: ((lv.xp ?? lv.xpRequired) as number) ?? 0,
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
