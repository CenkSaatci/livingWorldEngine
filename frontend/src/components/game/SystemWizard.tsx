import { useState, useImperativeHandle, forwardRef } from 'react';
import { Plus, X, Check, Dice1 as Dice, ArrowLeft, ArrowRight, Save, Trash2 } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { apiClient } from '../../api/client';
import { useToast } from '../../hooks/useToast';

interface AttributeDef {
  name: string;
  type: 'INT' | 'STRING' | 'BOOL';
  min: number;
  max: number;
  default: number;
}

interface SkillDef {
  name: string;
  attributes: string[];
  bonus: number;
}

interface DiceCombat {
  initiative: string;
  damage: string;
  actionPoints: { standard: number; max: number };
}

interface SystemFeatures {
  magic: boolean;
  psionics: boolean;
  rangedCombat: boolean;
  criticalHits: boolean;
  armorPenalty: boolean;
}

interface DerivedValue {
  name: string;
  formula: string;
}

export interface WizardData {
  name: string;
  version: number;
  description: string;
  progressionType: 'level' | 'xp' | 'improvement' | null;
  features: SystemFeatures;
  derivedValues: DerivedValue[];
  attributes: AttributeDef[];
  skills: SkillDef[];
  probe: string;
  enableCombat: boolean;
  combat: DiceCombat;
}

const STEPS = ['step_label_0', 'step_label_1', 'step_label_2', 'step_label_dv', 'step_label_3', 'step_label_4', 'step_label_5'];

const DICE_PRESETS = [
  { v: '1d2', l: '1d2' }, { v: '1d3', l: '1d3' }, { v: '1d4', l: '1d4' }, { v: '1d6', l: '1d6' },
  { v: '1d8', l: '1d8' }, { v: '1d10', l: '1d10' }, { v: '1d12', l: '1d12' }, { v: '1d20', l: '1d20' },
  { v: '1d100', l: '1d100' }, { v: '2d6', l: '2d6' }, { v: '3d6', l: '3d6' }, { v: '4dF', l: '4dF' },
];

const INITIAL: WizardData = {
  name: '',
  version: 1,
  description: '',
  progressionType: null,
  features: {
    magic: false,
    psionics: false,
    rangedCombat: false,
    criticalHits: false,
    armorPenalty: false,
  },
  derivedValues: [],
  attributes: [],
  skills: [],
  probe: '1d20+mod',
  enableCombat: false,
  combat: {
    initiative: '1d20+geschick',
    damage: '1d8+staerke',
    actionPoints: { standard: 1, max: 2 },
  },
};

export interface SystemWizardHandle {
  buildRulesJson: () => string;
}

interface Props {
  onSaved: () => void;
  onClose: () => void;
  initialData?: WizardData;
  systemId?: string;
}

export const SystemWizard = forwardRef<SystemWizardHandle, Props>(function SystemWizard({ onSaved, onClose, initialData, systemId }, ref) {
  const { t } = useTranslation('systemWizard');
  const [step, setStep] = useState(0);
  const [data, setData] = useState<WizardData>(() => initialData ?? INITIAL);
  const [saving, setSaving] = useState(false);
  const toast = useToast();

  useImperativeHandle(ref, () => ({ buildRulesJson }), [data]);

  const update = <K extends keyof WizardData>(key: K, val: WizardData[K]) =>
    setData((prev) => ({ ...prev, [key]: val }));

  const buildRulesJson = () => {
    const rules: Record<string, unknown> = {
      version: data.version,
      progressionType: data.progressionType,
      features: data.features,
      derived_values: data.derivedValues,
      attributes: data.attributes,
      skills: data.skills,
      dice_mechanics: { probe: data.probe },
    };
    if (data.enableCombat) {
      (rules.dice_mechanics as Record<string, unknown>).combat = {
        initiative: data.combat.initiative,
        damage: data.combat.damage,
        action_points: data.combat.actionPoints,
      };
    }
    return JSON.stringify(rules, null, 2);
  };

  const handleSave = async () => {
    if (!data.name.trim()) return;
    setSaving(true);
    try {
      if (systemId) {
        await apiClient.patch(`/game-systems/${systemId}`, {
          name: data.name.trim(),
          version: data.version,
          rulesJson: buildRulesJson(),
        });
        toast.success('System updated');
      } else {
        await apiClient.post('/game-systems', {
          name: data.name.trim(),
          version: data.version,
          rulesJson: buildRulesJson(),
          schemaJson: '{}',
        });
        toast.success('System created');
      }
      onSaved();
    } catch {
      toast.error(systemId ? 'Failed to update' : 'Failed to create');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="rounded-lg border border-accent/20 bg-bg-surface p-5">
      {/* Steps indicator */}
      <div className="flex items-center gap-2 mb-6">
        {STEPS.map((s, i) => (
          <div
            key={s}
            className={`flex items-center gap-1.5 text-xs ${i === step ? 'text-accent' : i < step ? 'text-success' : 'text-text-secondary'}`}
          >
            <span
              className={`flex h-5 w-5 items-center justify-center rounded-full text-[10px] font-bold
              ${i === step ? 'bg-accent text-white' : i < step ? 'bg-success text-white' : 'bg-bg-elevated text-text-secondary'}`}
            >
              {i < step ? <Check size={10} /> : i + 1}
            </span>
            {t(s)}
            {i < STEPS.length - 1 && <span className="w-4 border-t border-bg-elevated" />}
          </div>
        ))}
      </div>

      {/* Step 0: System-Charakter */}
      {step === 0 && (
        <div className="space-y-5">
          <h3 className="font-heading text-text-primary">{t('s0_title')}</h3>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-2">{t('s0_progression_type')}</label>
            <div className="flex flex-wrap gap-3">
              {(['level', 'xp', 'improvement'] as const).map((type) => (
                <label key={type} className={`flex cursor-pointer items-center gap-2 rounded border px-4 py-3 text-sm transition-colors ${
                  data.progressionType === type
                    ? 'border-accent bg-accent/10 text-accent'
                    : 'border-bg-elevated bg-bg-primary text-text-secondary hover:border-accent/50'
                }`}>
                  <input
                    type="radio"
                    name="progressionType"
                    checked={data.progressionType === type}
                    onChange={() => update('progressionType', type)}
                    className="accent-accent"
                  />
                  {t(`s0_${type}`)}
                </label>
              ))}
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-2">{t('s0_features')}</label>
            <div className="grid grid-cols-2 gap-2">
              {(['magic', 'psionics', 'rangedCombat', 'criticalHits', 'armorPenalty'] as const).map((feat) => (
                <label key={feat} className="flex cursor-pointer items-center gap-2 rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-secondary hover:border-accent/50">
                  <input
                    type="checkbox"
                    checked={data.features[feat]}
                    onChange={(e) =>
                      setData((prev) => ({
                        ...prev,
                        features: { ...prev.features, [feat]: e.target.checked },
                      }))
                    }
                    className="accent-accent"
                  />
                  {t(`s0_${feat}`)}
                </label>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Step 1: Basic Information */}
      {step === 1 && (
        <div className="space-y-4">
          <h3 className="font-heading text-text-primary">{t('s1_title')}</h3>
          <div>
            <label className="block text-xs text-text-secondary mb-1">{t('s1_name')}</label>
            <input
              value={data.name}
              onChange={(e) => update('name', e.target.value)}
              className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs text-text-secondary mb-1">{t('s1_version')}</label>
              <input
                type="number"
                min={1}
                value={data.version}
                onChange={(e) => update('version', Number(e.target.value))}
                className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
              />
            </div>
          </div>
          <div>
            <label className="block text-xs text-text-secondary mb-1">{t('s1_description')}</label>
            <textarea
              value={data.description}
              onChange={(e) => update('description', e.target.value)}
              rows={3}
              className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent resize-none"
            />
          </div>
        </div>
      )}

      {step === 2 && (
        <div className="space-y-4">
          <h3 className="font-heading text-text-primary">{t('s2_title')}</h3>
          <p className="text-xs text-text-secondary" dangerouslySetInnerHTML={{ __html: t('s2_hint') }} />

          {/* Header */}
          <div className="flex items-center gap-2 px-2 text-[10px] text-text-secondary uppercase tracking-wider">
            <span className="w-24">{t('s2_header_name')}</span>
            <span className="w-[88px]">{t('s2_header_type')}</span>
            <span className="w-14 text-center">{t('s2_header_min')}</span>
            <span className="w-14 text-center">{t('s2_header_max')}</span>
            <span className="w-14 text-center">{t('s2_header_default')}</span>
            <span className="w-4">{t('s2_header_actions')}</span>
          </div>

          {data.attributes.map((attr, i) => (
            <div key={i} className="flex items-center gap-2 rounded bg-bg-primary/50 p-2">
              <input
                value={attr.name}
                onChange={(e) => {
                  const a = [...data.attributes];
                  a[i] = { ...a[i], name: e.target.value };
                  update('attributes', a);
                }}
                className="w-24 rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
                placeholder={t('s2_name_placeholder')}
              />
              <select
                value={attr.type}
                onChange={(e) => {
                  const a = [...data.attributes];
                  a[i] = { ...a[i], type: e.target.value as 'INT' | 'STRING' | 'BOOL' };
                  update('attributes', a);
                }}
                className="w-[88px] rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
              >
                <option value="INT">{t('s2_type_int')}</option>
                <option value="STRING">{t('s2_type_string')}</option>
                <option value="BOOL">{t('s2_type_bool')}</option>
              </select>
              <input
                type="number"
                value={attr.min}
                onChange={(e) => {
                  const a = [...data.attributes];
                  a[i] = { ...a[i], min: Number(e.target.value) };
                  update('attributes', a);
                }}
                className="w-14 rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
                placeholder={t('s2_min')}
              />
              <input
                type="number"
                value={attr.max}
                onChange={(e) => {
                  const a = [...data.attributes];
                  a[i] = { ...a[i], max: Number(e.target.value) };
                  update('attributes', a);
                }}
                className="w-14 rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
                placeholder={t('s2_max')}
              />
              <input
                type="number"
                value={attr.default}
                onChange={(e) => {
                  const a = [...data.attributes];
                  a[i] = { ...a[i], default: Number(e.target.value) };
                  update('attributes', a);
                }}
                className="w-14 rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
                placeholder={t('s2_default')}
              />
              <button
                onClick={() =>
                  update(
                    'attributes',
                    data.attributes.filter((_, j) => j !== i),
                  )
                }
                className="text-danger hover:text-danger/80"
              >
                <X size={14} />
              </button>
            </div>
          ))}
          <button
            onClick={() =>
              update('attributes', [
                ...data.attributes,
                { name: '', type: 'INT', min: 1, max: 20, default: 10 },
              ])
            }
            className="flex items-center gap-1 text-xs text-accent hover:text-accent/80"
          >
            <Plus size={14} /> {t('s2_add')}
          </button>
        </div>
      )}

      {/* Step 3: Derived Values */}
      {step === 3 && (
        <div className="space-y-4">
          <h3 className="font-heading text-text-primary">{t('sdv_title')}</h3>
          <p className="text-xs text-text-secondary" dangerouslySetInnerHTML={{ __html: t('sdv_hint') }} />

          {data.attributes.length > 0 && (
            <div className="rounded bg-bg-primary/30 p-2 text-xs text-text-secondary">
              <span className="font-semibold text-text-primary">Verfügbare Attribute: </span>
              {data.attributes.map((a, i) => (
                <span key={a.name}>
                  {i > 0 && <span className="mx-1">·</span>}
                  <code className="text-accent">{a.name}</code> ({a.min}–{a.max})
                </span>
              ))}
            </div>
          )}

          <div className="flex items-center gap-2 px-2 text-[10px] text-text-secondary uppercase tracking-wider">
            <span className="flex-1">{t('sdv_header_name')}</span>
            <span className="flex-[2]">{t('sdv_header_formula')}</span>
            <span className="w-4" />
          </div>

          {data.derivedValues.map((dv, i) => (
            <div key={i} className="flex items-center gap-2 rounded bg-bg-primary/50 p-2">
              <input
                value={dv.name}
                onChange={(e) => {
                  const a = [...data.derivedValues];
                  a[i] = { ...a[i], name: e.target.value };
                  update('derivedValues', a);
                }}
                className="flex-1 rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
                placeholder={t('sdv_name_placeholder')}
              />
              <input
                value={dv.formula}
                onChange={(e) => {
                  const a = [...data.derivedValues];
                  a[i] = { ...a[i], formula: e.target.value };
                  update('derivedValues', a);
                }}
                className="flex-[2] rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs font-mono text-text-primary outline-none focus:border-accent"
                placeholder={t('sdv_formula_placeholder')}
              />
              <button
                onClick={() =>
                  update('derivedValues', data.derivedValues.filter((_, j) => j !== i))
                }
                className="text-danger hover:text-danger/80"
              >
                <X size={14} />
              </button>
            </div>
          ))}

          <button
            onClick={() =>
              update('derivedValues', [...data.derivedValues, { name: '', formula: '' }])
            }
            className="flex items-center gap-1 text-xs text-accent hover:text-accent/80"
          >
            <Plus size={14} /> {t('sdv_add')}
          </button>
        </div>
      )}

      {step === 4 && (
        <div className="space-y-4">
          <h3 className="font-heading text-text-primary">Fertigkeiten</h3>
          <p className="text-xs text-text-secondary">
            Fertigkeiten (Skills) werden an Attribute gekoppelt. Wähle <strong>ein oder mehrere</strong> Attribute pro Fertigkeit aus — die Reihenfolge bestimmt die Gewichtung. Mit <em>+ Attribut</em> fügst du weitere hinzu.
          </p>
          {data.skills.map((skill, i) => (
            <div key={i} className="flex items-start gap-2 rounded bg-bg-primary/50 p-2">
              <input
                value={skill.name}
                onChange={(e) => {
                  const s = [...data.skills];
                  s[i] = { ...s[i], name: e.target.value };
                  update('skills', s);
                }}
                className="w-20 rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
                placeholder="Fertigkeit"
              />
              <div className="flex-1 space-y-1">
                {skill.attributes.map((attrName, ai) => (
                  <div key={ai} className="flex items-center gap-1">
                    <select
                      value={attrName}
                      onChange={(e) => {
                        const s = [...data.skills];
                        s[i].attributes[ai] = e.target.value;
                        update('skills', s);
                      }}
                      className="flex-1 rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
                    >
                      <option value="">— Attribut —</option>
                      {data.attributes.map((a) => (
                        <option key={a.name} value={a.name}>{a.name}</option>
                      ))}
                    </select>
                    <button
                      onClick={() => {
                        const s = [...data.skills];
                        s[i].attributes = skill.attributes.filter((_, j) => j !== ai);
                        update('skills', s);
                      }}
                      className="text-danger/60 hover:text-danger"
                    >
                      <X size={12} />
                    </button>
                  </div>
                ))}
                <button
                  onClick={() => {
                    const s = [...data.skills];
                    s[i].attributes = [...skill.attributes, ''];
                    update('skills', s);
                  }}
                  className="text-[11px] text-accent hover:text-accent/80"
                >
                  + Attribut
                </button>
              </div>
              <div>
                <label className="block text-[10px] text-text-secondary mb-1">Bonus</label>
                <input
                  type="number"
                  value={skill.bonus}
                  onChange={(e) => {
                    const s = [...data.skills];
                    s[i] = { ...s[i], bonus: Number(e.target.value) };
                    update('skills', s);
                  }}
                  className="w-14 rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
                />
              </div>
              <button
                onClick={() =>
                  update('skills', data.skills.filter((_, j) => j !== i))
                }
                className="mt-4 text-danger hover:text-danger/80"
              >
                <Trash2 size={14} />
              </button>
            </div>
          ))}
          <button
            onClick={() =>
              update('skills', [...data.skills, { name: '', attributes: [], bonus: 0 }])
            }
            className="flex items-center gap-1 text-xs text-accent hover:text-accent/80"
          >
            <Plus size={14} /> Add Skill
          </button>
        </div>
      )}

      {step === 5 && (
        <div className="space-y-4">
          <h3 className="font-heading text-text-primary">{t('s4_title')}</h3>
          <div className="rounded bg-bg-primary/30 p-3 text-xs text-text-secondary">
            <p dangerouslySetInnerHTML={{ __html: t('s4_probe_hint') }} />
          </div>
          <div>
            <label className="block text-xs text-text-secondary mb-1">Probe Expression</label>
            <div className="flex gap-2">
              <input
                value={data.probe}
                onChange={(e) => update('probe', e.target.value)}
                className="flex-1 rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm font-mono text-text-primary outline-none focus:border-accent"
                placeholder="1d20+mod"
              />
              <button
                onClick={async () => {
                  try {
                    const r = await apiClient.post('/rolls/free', { expression: data.probe });
                    toast.success(`Test: ${r.data.total}`);
                  } catch {
                    toast.error('Invalid expression');
                  }
                }}
                className="rounded bg-bg-elevated px-3 py-2 text-xs text-text-secondary hover:text-accent"
              >
                <Dice size={14} />
              </button>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <input
              type="checkbox"
              id="enableCombat"
              checked={data.enableCombat}
              onChange={(e) => update('enableCombat', e.target.checked)}
              className="accent-accent"
            />
            <label htmlFor="enableCombat" className="text-sm text-text-primary">
              Enable combat rules
            </label>
          </div>
          {data.enableCombat && (
            <div className="space-y-3 pl-4 border-l-2 border-accent/30">
              <CombatExpressionRow
                label="Initiative"
                value={data.combat.initiative}
                attributes={data.attributes}
                diceOptions={DICE_PRESETS}
                onChange={(v) => update('combat', { ...data.combat, initiative: v })}
                onRoll={async (expr) => {
                  try {
                    const r = await apiClient.post('/rolls/free', { expression: expr });
                    toast.success(`Initiative: ${r.data.total}`);
                  } catch { toast.error('Invalid'); }
                }}
              />
              <CombatExpressionRow
                label="Damage"
                value={data.combat.damage}
                attributes={data.attributes}
                diceOptions={DICE_PRESETS}
                onChange={(v) => update('combat', { ...data.combat, damage: v })}
                onRoll={async (expr) => {
                  try {
                    const r = await apiClient.post('/rolls/free', { expression: expr });
                    toast.success(`Damage: ${r.data.total}`);
                  } catch { toast.error('Invalid'); }
                }}
              />
              <div className="col-span-2 rounded bg-bg-primary/30 p-3 text-xs text-text-secondary mb-2">
                <strong>AP (Aktionspunkte):</strong> Jede Aktion im Kampf kostet AP. <em>AP Standard</em> = AP pro Runde, <em>AP Max</em> = maximal speicherbare AP (z.B. für Aufsparen). Typisch: 2 Standard / 4 Max.
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs text-text-secondary mb-1">AP pro Runde</label>
                  <input
                    type="number"
                    min={1}
                    value={data.combat.actionPoints.standard}
                    onChange={(e) =>
                      update('combat', {
                        ...data.combat,
                        actionPoints: {
                          ...data.combat.actionPoints,
                          standard: Number(e.target.value),
                        },
                      })
                    }
                    className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                  />
                </div>
                <div>
                  <label className="block text-xs text-text-secondary mb-1">AP Max (Pool)</label>
                  <input
                    type="number"
                    min={1}
                    value={data.combat.actionPoints.max}
                    onChange={(e) =>
                      update('combat', {
                        ...data.combat,
                        actionPoints: { ...data.combat.actionPoints, max: Number(e.target.value) },
                      })
                    }
                    className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                  />
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {step === 6 && (
        <div className="space-y-4">
          <h3 className="font-heading text-text-primary">{t('s5_title')}</h3>

          <div className="grid grid-cols-2 gap-3 text-sm">
            <div className="rounded bg-bg-primary/50 p-3">
              <p className="text-[10px] text-text-secondary uppercase tracking-wider">System</p>
              <p className="text-text-primary font-medium mt-1">{data.name || 'Unnamed'}</p>
              <p className="text-text-secondary text-xs">Version {data.version}</p>
              {data.description && (
                <p className="text-text-secondary text-xs mt-1">{data.description}</p>
              )}
            </div>
            <div className="rounded bg-bg-primary/50 p-3">
              <p className="text-[10px] text-text-secondary uppercase tracking-wider">Würfel</p>
              <p className="text-text-primary font-mono mt-1">{data.probe}</p>
              {data.enableCombat && (
                <p className="text-text-secondary text-xs mt-1">
                  Initiative: {data.combat.initiative} · Damage: {data.combat.damage}
                </p>
              )}
            </div>
          </div>

          {data.derivedValues.length > 0 && (
          <div className="rounded bg-bg-primary/50 p-3">
            <p className="text-[10px] text-text-secondary uppercase tracking-wider mb-2">
              Derived Values ({data.derivedValues.length})
            </p>
            <div className="flex flex-wrap gap-1">
              {data.derivedValues.map((dv) => (
                <span key={dv.name} className="rounded bg-bg-elevated px-2 py-0.5 text-xs text-text-primary">
                  {dv.name} = {dv.formula}
                </span>
              ))}
            </div>
          </div>
          )}

          <div className="rounded bg-bg-primary/50 p-3">
            <p className="text-[10px] text-text-secondary uppercase tracking-wider mb-2">
              Attribute ({data.attributes.length})
            </p>
            <div className="flex flex-wrap gap-1">
              {data.attributes.map((a) => (
                <span key={a.name} className="rounded bg-bg-elevated px-2 py-0.5 text-xs text-text-primary">
                  {a.name} ({a.min}–{a.max}, Ø{a.default})
                </span>
              ))}
              {data.attributes.length === 0 && (
                <span className="text-xs text-text-secondary">Keine Attribute definiert</span>
              )}
            </div>
          </div>

          <div className="rounded bg-bg-primary/50 p-3">
            <p className="text-[10px] text-text-secondary uppercase tracking-wider mb-2">
              Fertigkeiten ({data.skills.length})
            </p>
            <div className="space-y-1">
              {data.skills.map((s, i) => (
                <div key={i} className="flex items-center gap-2 text-xs">
                  <span className="text-text-primary w-20 truncate">{s.name}</span>
                  <span className="text-text-secondary">
                    {s.attributes.length > 0
                      ? s.attributes.join(', ')
                      : '—'}
                  </span>
                  {s.bonus !== 0 && (
                    <span className="text-accent">+{s.bonus}</span>
                  )}
                </div>
              ))}
              {data.skills.length === 0 && (
                <span className="text-xs text-text-secondary">Keine Fertigkeiten definiert</span>
              )}
            </div>
          </div>

          <details className="group">
            <summary className="cursor-pointer text-xs text-text-secondary hover:text-text-primary">
              Rules JSON anzeigen
            </summary>
            <pre className="mt-2 max-h-48 overflow-y-auto rounded border border-bg-elevated bg-bg-primary p-3 text-xs font-mono text-text-secondary">
              {buildRulesJson()}
            </pre>
          </details>
        </div>
      )}

      {/* Navigation */}
      <div className="flex items-center justify-between mt-6 pt-4 border-t border-bg-elevated">
        <button
          onClick={step === 0 ? onClose : () => setStep(step - 1)}
          className="flex items-center gap-1 text-xs text-text-secondary hover:text-text-primary"
        >
          <ArrowLeft size={14} /> {step === 0 ? t('nav_cancel') : t('nav_back')}
        </button>
        {step < STEPS.length - 1 ? (
          <button
            onClick={() => setStep(step + 1)}
            className="flex items-center gap-1 rounded bg-accent px-4 py-2 text-xs text-white hover:bg-accent/80"
          >
            {t('nav_next')} <ArrowRight size={14} />
          </button>
        ) : (
          <button
            onClick={handleSave}
            disabled={saving || !data.name.trim()}
            className="flex items-center gap-1 rounded bg-accent px-4 py-2 text-xs text-white hover:bg-accent/80 disabled:opacity-40"
          >
            <Save size={14} /> {saving ? t('nav_saving') : t('nav_save')}
          </button>
        )}
      </div>
    </div>
  );
});

function CombatExpressionRow({
  label, value, attributes, diceOptions, onChange, onRoll,
}: {
  label: string;
  value: string;
  attributes: AttributeDef[];
  diceOptions: { v: string; l: string }[];
  onChange: (v: string) => void;
  onRoll: (expr: string) => Promise<void>;
}) {
  const diceList = diceOptions.map((d) => d.v);
  const [selectedDice, setSelectedDice] = useState(() => {
    const m = value.match(/^(\d+d\d+)/);
    return m && diceList.includes(m[1]) ? m[1] : '1d20';
  });
  const [selectedAttr, setSelectedAttr] = useState(() => {
    const m = value.match(/\+(\w+)$/);
    return m && attributes.some((a) => a.name === m[1]) ? m[1] : '';
  });

  const rebuild = (dice: string, attr: string) => {
    const expr = attr ? `${dice}+${attr}` : dice;
    onChange(expr);
  };

  return (
    <div>
      <label className="block text-xs text-text-secondary mb-1">{label}</label>
      <div className="flex gap-2">
        <select
          value={selectedDice}
          onChange={(e) => {
            setSelectedDice(e.target.value);
            rebuild(e.target.value, selectedAttr);
          }}
          className="rounded border border-bg-elevated bg-bg-primary px-2 py-2 text-sm text-text-primary outline-none focus:border-accent"
        >
          {diceOptions.map((d) => (
            <option key={d.v} value={d.v}>{d.l}</option>
          ))}
        </select>
        <span className="self-center text-text-secondary text-sm">+</span>
        <select
          value={selectedAttr}
          onChange={(e) => {
            setSelectedAttr(e.target.value);
            rebuild(selectedDice, e.target.value);
          }}
          className="flex-1 rounded border border-bg-elevated bg-bg-primary px-2 py-2 text-sm text-text-primary outline-none focus:border-accent"
        >
          <option value="">— Attribute —</option>
          {attributes.map((a) => (
            <option key={a.name} value={a.name}>{a.name}</option>
          ))}
        </select>
        <input
          value={value}
          onChange={(e) => onChange(e.target.value)}
          className="w-28 rounded border border-bg-elevated bg-bg-primary px-2 py-2 text-sm font-mono text-text-primary outline-none focus:border-accent"
          placeholder="1d20+mod"
        />
        <button
          onClick={() => onRoll(value)}
          className="rounded bg-bg-elevated px-3 py-2 text-xs text-text-secondary hover:text-accent"
        >
          <Dice size={14} />
        </button>
      </div>
    </div>
  );
}
