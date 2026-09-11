import { useState, useImperativeHandle, forwardRef } from 'react';
import { Plus, X, Check, Dice1 as Dice, ArrowLeft, ArrowRight, Save, Trash2 } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { apiClient } from '../../api/client';
import { useToast } from '../../hooks/useToast';
import { FormulaBuilder } from '../ui/FormulaBuilder';
import {
  defaultWizardData,
  testExpression,
  toRulesJson,
  type AttributeDef,
  type ConditionalDef,
  type WizardData,
} from '../../types/gameSystem';

export type { WizardData };

const STEPS = ['step_label_0', 'step_label_1', 'step_label_2', 'step_label_dv', 'step_label_3', 'step_label_5a', 'step_label_6', 'step_label_7', 'step_label_8', 'step_label_4', 'step_label_5'];

const DICE_PRESETS = [
  { v: '1d2', l: '1d2' }, { v: '1d3', l: '1d3' }, { v: '1d4', l: '1d4' }, { v: '1d6', l: '1d6' },
  { v: '1d8', l: '1d8' }, { v: '1d10', l: '1d10' }, { v: '1d12', l: '1d12' }, { v: '1d20', l: '1d20' },
  { v: '1d100', l: '1d100' }, { v: '2d6', l: '2d6' }, { v: '3d6', l: '3d6' }, { v: '4dF', l: '4dF' },
];

const INITIAL: WizardData = defaultWizardData();

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

  const buildRulesJson = () => toRulesJson(data);

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
        toast.success(t('msg_updated'));
      } else {
        await apiClient.post('/game-systems', {
          name: data.name.trim(),
          version: data.version,
          rulesJson: buildRulesJson(),
          schemaJson: '{}',
        });
        toast.success(t('msg_created'));
      }
      onSaved();
    } catch {
      toast.error(systemId ? t('msg_update_failed') : t('msg_create_failed'));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="rounded-lg border border-accent/20 bg-bg-surface p-5 max-h-[90vh] flex flex-col">
      {/* Steps indicator */}
      <div className="flex items-center gap-2 mb-4 shrink-0 overflow-x-auto pb-1">
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

      <div className="flex-1 overflow-y-auto min-h-0">
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
              <div className="flex-[2]">
                <label className="block text-[10px] text-text-secondary mb-1">{t('sdv_header_formula')}</label>
                <FormulaBuilder
                  value={dv.formula}
                  onChange={(v) => {
                    const a = [...data.derivedValues];
                    a[i] = { ...a[i], formula: v };
                    update('derivedValues', a);
                  }}
                  attributes={data.attributes}
                />
              </div>
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
          <h3 className="font-heading text-text-primary">{t('s3_title')}</h3>
          <p className="text-xs text-text-secondary" dangerouslySetInnerHTML={{ __html: t('s3_hint') }} />
          {data.probeType === 'd20_3attr' && (
            <p className="text-xs text-warning/80">DSA-Modus: Jedes Talent hat genau 3 Attribute.</p>
          )}
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
                placeholder={t('s3_placeholder')}
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
                    {data.probeType !== 'd20_3attr' && (
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
                    )}
                  </div>
                ))}
                {data.probeType !== 'd20_3attr' && (
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
                )}
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
              update('skills', [...data.skills, {
                name: '',
                attributes: data.probeType === 'd20_3attr' ? ['', '', ''] : [],
                bonus: 0,
              }])
            }
            className="flex items-center gap-1 text-xs text-accent hover:text-accent/80"
          >
            <Plus size={14} /> {t('s3_add_skill')}
          </button>
        </div>
      )}

      {/* Step 5: Abilities */}
      {step === 5 && (
        <div className="space-y-4">
          <h3 className="font-heading text-text-primary">{t('s5a_title')}</h3>
          <p className="text-xs text-text-secondary" dangerouslySetInnerHTML={{ __html: t('s5a_hint') }} />

          <div className="flex items-center gap-2 px-2 text-[10px] text-text-secondary uppercase tracking-wider">
            <span className="w-24">{t('s5a_header_name')}</span>
            <span className="w-14">{t('s5a_header_type')}</span>
            <span className="w-20">{t('s5a_header_cost')}</span>
            {data.combat.actionTypes.length > 0 && <span className="w-16">{t('s5a_header_action')}</span>}
            <span className="w-28">{t('s5a_header_dice')}</span>
            <span className="flex-1">{t('s5a_header_effect')}</span>
            <span className="w-4" />
          </div>

          {data.abilities.map((ability, i) => (
            <div key={i} className="flex flex-wrap items-start gap-2 rounded bg-bg-primary/50 p-2">
              <input
                value={ability.name}
                onChange={(e) => {
                  const a = [...data.abilities];
                  a[i] = { ...a[i], name: e.target.value };
                  update('abilities', a);
                }}
                className="w-24 rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
                placeholder={t('s5a_name_placeholder')}
              />
              <select
                value={ability.type}
                onChange={(e) => {
                  const a = [...data.abilities];
                  a[i] = { ...a[i], type: e.target.value as 'active' | 'passive' };
                  update('abilities', a);
                }}
                className="w-14 rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
              >
                <option value="active">{t('s5a_active')}</option>
                <option value="passive">{t('s5a_passive')}</option>
              </select>

              {ability.type === 'active' ? (
                <>
                  <div className="flex w-20 gap-1">
                    <input
                      type="number"
                      min={0}
                      value={ability.cost}
                      onChange={(e) => {
                        const a = [...data.abilities];
                        a[i] = { ...a[i], cost: Number(e.target.value) };
                        update('abilities', a);
                      }}
                      className="w-10 rounded border border-bg-elevated bg-bg-primary px-1 py-1 text-xs text-text-primary outline-none focus:border-accent"
                    />
                    <select
                      value={ability.costType}
                      onChange={(e) => {
                        const a = [...data.abilities];
                        a[i] = { ...a[i], costType: e.target.value as 'AP' | 'MP' | '' };
                        update('abilities', a);
                      }}
                      className="w-10 rounded border border-bg-elevated bg-bg-primary px-1 py-1 text-xs text-text-primary outline-none focus:border-accent"
                    >
                      <option value="">-</option>
                      <option value="AP">{t('s5a_cost_ap')}</option>
                      <option value="MP">{t('s5a_cost_mp')}</option>
                    </select>
                  </div>
                  {data.combat.actionTypes.length > 0 && (
                    <select
                      value={ability.actionCost?.type ?? ''}
                      onChange={(e) => {
                        const a = [...data.abilities];
                        a[i] = { ...a[i], actionCost: { ...(a[i].actionCost ?? { type: '', amount: 0 }), type: e.target.value } };
                        update('abilities', a);
                      }}
                      className="w-16 rounded border border-bg-elevated bg-bg-primary px-1 py-1 text-xs text-text-primary outline-none focus:border-accent"
                    >
                      <option value="">-</option>
                      {data.combat.actionTypes.map((at) => (
                        <option key={at} value={at}>{t(`s4_action_${at}`)}</option>
                      ))}
                    </select>
                  )}
                  <input
                    value={ability.diceExpression}
                    onChange={(e) => {
                      const a = [...data.abilities];
                      a[i] = { ...a[i], diceExpression: e.target.value };
                      update('abilities', a);
                    }}
                    className="w-28 rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs font-mono text-text-primary outline-none focus:border-accent"
                    placeholder={t('s5a_dice_placeholder')}
                  />
                  <input
                    value={ability.effect}
                    onChange={(e) => {
                      const a = [...data.abilities];
                      a[i] = { ...a[i], effect: e.target.value };
                      update('abilities', a);
                    }}
                    className="flex-1 rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
                    placeholder={t('s5a_effect_placeholder')}
                  />
                </>
              ) : (
                <>
                  <div className="w-20" />
                  <div className="w-28" />
                  <input
                    value={ability.bonus}
                    onChange={(e) => {
                      const a = [...data.abilities];
                      a[i] = { ...a[i], bonus: e.target.value };
                      update('abilities', a);
                    }}
                    className="flex-1 rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
                    placeholder={t('s5a_bonus_placeholder')}
                  />
                </>
              )}

              {/* Tags + Category row */}
              <div className="w-full flex gap-2 mt-1">
                <input
                  value={(ability.tags ?? []).join(', ')}
                  onChange={(e) => {
                    const a = [...data.abilities];
                    a[i] = { ...a[i], tags: e.target.value.split(',').map((t) => t.trim()).filter(Boolean) };
                    update('abilities', a);
                  }}
                  className="flex-1 rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-[10px] text-text-secondary outline-none focus:border-accent"
                  placeholder={t('s5a_tags_placeholder')}
                />
                <select
                  value={ability.category ?? 'ability'}
                  onChange={(e) => {
                    const a = [...data.abilities];
                    a[i] = { ...a[i], category: e.target.value };
                    update('abilities', a);
                  }}
                  className="w-24 rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-[10px] text-text-secondary outline-none focus:border-accent"
                >
                  <option value="ability">{t('s5a_cat_ability')}</option>
                  <option value="advantage">{t('s5a_cat_advantage')}</option>
                  <option value="perk">{t('s5a_cat_perk')}</option>
                </select>
              </div>

              <button
                onClick={() =>
                  update('abilities', data.abilities.filter((_, j) => j !== i))
                }
                className="text-danger hover:text-danger/80"
              >
                <X size={14} />
              </button>
            </div>
          ))}

          <button
            onClick={() =>
              update('abilities', [
                ...data.abilities,
                { name: '', type: 'active', costType: 'AP', cost: 1, diceExpression: '', effect: '', bonus: '', tags: [], category: 'ability' },
              ])
            }
            className="flex items-center gap-1 text-xs text-accent hover:text-accent/80"
          >
            <Plus size={14} /> {t('s5a_add')}
          </button>
        </div>
      )}

      {/* Step 6: Progression */}
      {step === 6 && (
        <div className="space-y-4">
          <h3 className="font-heading text-text-primary">{t('s6_title')}</h3>
          <p className="text-xs text-text-secondary" dangerouslySetInnerHTML={{ __html: t('s6_hint') }} />

          {data.progressionType === 'level' && (
            <>
              <p className="text-xs font-semibold text-text-primary">{t('s6_level_title')}</p>
              <p className="text-xs text-text-secondary" dangerouslySetInnerHTML={{ __html: t('s6_level_hint') }} />
              <div className="flex items-center gap-2 px-2 text-[10px] text-text-secondary uppercase tracking-wider">
                <span className="w-12">{t('s6_level_header_level')}</span>
                <span className="w-20">{t('s6_level_header_xp')}</span>
                <span className="flex-1">{t('s6_level_header_features')}</span>
                <span className="w-4" />
              </div>
              {data.progression.levels.map((lv, i) => (
                <div key={i} className="flex items-center gap-2 rounded bg-bg-primary/50 p-2">
                  <span className="w-12 text-xs text-text-primary">{lv.level}</span>
                  <input
                    type="number" min={0}
                    value={lv.xpRequired}
                    onChange={(e) => {
                      const a = [...data.progression.levels];
                      a[i] = { ...a[i], xpRequired: Number(e.target.value) };
                      update('progression', { ...data.progression, levels: a });
                    }}
                    className="w-20 rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
                  />
                  <input
                    value={lv.features}
                    onChange={(e) => {
                      const a = [...data.progression.levels];
                      a[i] = { ...a[i], features: e.target.value };
                      update('progression', { ...data.progression, levels: a });
                    }}
                    className="flex-1 rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
                    placeholder={t('s4_level_features_placeholder')}
                  />
                  <button
                    onClick={() =>
                      update('progression', {
                        ...data.progression,
                        levels: data.progression.levels.filter((_, j) => j !== i),
                      })
                    }
                    className="text-danger hover:text-danger/80"
                  >
                    <X size={14} />
                  </button>
                </div>
              ))}
              <button
                onClick={() =>
                  update('progression', {
                    ...data.progression,
                    levels: [...data.progression.levels, { level: data.progression.levels.length + 1, xpRequired: 0, features: '' }],
                  })
                }
                className="flex items-center gap-1 text-xs text-accent hover:text-accent/80"
              >
                <Plus size={14} /> {t('s6_level_add')}
              </button>
            </>
          )}

          {data.progressionType === 'xp' && (
            <>
              <p className="text-xs font-semibold text-text-primary">{t('s6_xp_title')}</p>
              <p className="text-xs text-text-secondary" dangerouslySetInnerHTML={{ __html: t('s6_xp_hint') }} />
              <div className="flex items-center gap-2 px-2 text-[10px] text-text-secondary uppercase tracking-wider">
                <span className="flex-1">{t('s6_xp_header_name')}</span>
                <span className="w-20">{t('s6_xp_header_cost')}</span>
                <span className="w-4" />
              </div>
              {data.progression.xpCosts.map((xc, i) => (
                <div key={i} className="flex items-center gap-2 rounded bg-bg-primary/50 p-2">
                  <input
                    value={xc.name}
                    onChange={(e) => {
                      const a = [...data.progression.xpCosts];
                      a[i] = { ...a[i], name: e.target.value };
                      update('progression', { ...data.progression, xpCosts: a });
                    }}
                    className="flex-1 rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
                  />
                  <input
                    type="number" min={0}
                    value={xc.cost}
                    onChange={(e) => {
                      const a = [...data.progression.xpCosts];
                      a[i] = { ...a[i], cost: Number(e.target.value) };
                      update('progression', { ...data.progression, xpCosts: a });
                    }}
                    className="w-20 rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
                  />
                  <button
                    onClick={() =>
                      update('progression', {
                        ...data.progression,
                        xpCosts: data.progression.xpCosts.filter((_, j) => j !== i),
                      })
                    }
                    className="text-danger hover:text-danger/80"
                  >
                    <X size={14} />
                  </button>
                </div>
              ))}
              <button
                onClick={() =>
                  update('progression', {
                    ...data.progression,
                    xpCosts: [...data.progression.xpCosts, { name: '', cost: 0 }],
                  })
                }
                className="flex items-center gap-1 text-xs text-accent hover:text-accent/80"
              >
                <Plus size={14} /> {t('s6_xp_add')}
              </button>
            </>
          )}

          {data.progressionType === 'improvement' && (
            <>
              <p className="text-xs font-semibold text-text-primary">{t('s6_imp_title')}</p>
              <p className="text-xs text-text-secondary" dangerouslySetInnerHTML={{ __html: t('s6_imp_hint') }} />
              <div className="flex items-center gap-2 px-2 text-[10px] text-text-secondary uppercase tracking-wider">
                <span className="w-24">{t('s6_imp_header_name')}</span>
                <span className="w-12">{t('s6_imp_header_count')}</span>
                <span className="w-16">{t('s6_imp_header_dice')}</span>
                <span className="w-12">{t('s6_imp_header_comparison')}</span>
                <span className="w-16">{t('s6_imp_header_target')}</span>
                <span className="w-4" />
              </div>
              {data.progression.improvements.map((imp, i) => (
                <div key={i} className="flex items-center gap-2 rounded bg-bg-primary/50 p-2">
                  <input
                    value={imp.name}
                    onChange={(e) => {
                      const a = [...data.progression.improvements];
                      a[i] = { ...a[i], name: e.target.value };
                      update('progression', { ...data.progression, improvements: a });
                    }}
                    className="w-24 rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
                  />
                  <input
                    type="number" min={1} value={imp.count}
                    onChange={(e) => {
                      const a = [...data.progression.improvements];
                      a[i] = { ...a[i], count: Number(e.target.value) };
                      update('progression', { ...data.progression, improvements: a });
                    }}
                    className="w-12 rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
                  />
                  <select
                    value={imp.dice}
                    onChange={(e) => {
                      const a = [...data.progression.improvements];
                      a[i] = { ...a[i], dice: e.target.value };
                      update('progression', { ...data.progression, improvements: a });
                    }}
                    className="w-16 rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
                  >
                    {DICE_PRESETS.map((d) => (
                      <option key={d.v} value={d.v}>{d.l}</option>
                    ))}
                  </select>
                  <select
                    value={imp.comparison}
                    onChange={(e) => {
                      const a = [...data.progression.improvements];
                      a[i] = { ...a[i], comparison: e.target.value as 'gte' | 'lte' };
                      update('progression', { ...data.progression, improvements: a });
                    }}
                    className="w-12 rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
                  >
                    <option value="gte">≥</option>
                    <option value="lte">≤</option>
                  </select>
                  <input
                    type="number" min={0} value={imp.target}
                    onChange={(e) => {
                      const a = [...data.progression.improvements];
                      a[i] = { ...a[i], target: Number(e.target.value) };
                      update('progression', { ...data.progression, improvements: a });
                    }}
                    className="w-16 rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
                  />
                  <button
                    onClick={() =>
                      update('progression', {
                        ...data.progression,
                        improvements: data.progression.improvements.filter((_, j) => j !== i),
                      })
                    }
                    className="text-danger hover:text-danger/80"
                  >
                    <X size={14} />
                  </button>
                </div>
              ))}
              <button
                onClick={() =>
                  update('progression', {
                    ...data.progression,
                    improvements: [...data.progression.improvements, { name: '', count: 1, dice: '1d100', comparison: 'gte', target: 0 }],
                  })
                }
                className="flex items-center gap-1 text-xs text-accent hover:text-accent/80"
              >
                <Plus size={14} /> {t('s6_imp_add')}
              </button>
            </>
          )}

          {!data.progressionType && (
            <p className="text-xs text-text-secondary">Bitte wähle zuerst unter System-Charakter einen Aufstiegs-Typ.</p>
          )}
        </div>
      )}

      {/* Step 7: Specials */}
      {step === 7 && (
        <div className="space-y-4">
          <h3 className="font-heading text-text-primary">{t('s7_title')}</h3>

          {data.features.magic && (
            <div className="space-y-3 rounded border border-accent/20 bg-bg-primary/30 p-3">
              <p className="text-xs font-semibold text-text-primary">{t('s7_magic_title')}</p>
              <p className="text-xs text-text-secondary" dangerouslySetInnerHTML={{ __html: t('s7_magic_hint') }} />
              <div>
                <label className="block text-xs text-text-secondary mb-1">{t('s7_mana_formula')}</label>
                <input
                  value={data.magic.manaFormula}
                  onChange={(e) => update('magic', { ...data.magic, manaFormula: e.target.value })}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm font-mono text-text-primary outline-none focus:border-accent"
                  placeholder={t('s7_mana_placeholder')}
                />
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">{t('s7_spell_slots')}</label>
                <input
                  value={data.magic.spellSlots}
                  onChange={(e) => update('magic', { ...data.magic, spellSlots: e.target.value })}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                  placeholder={t('s7_spell_slots_placeholder')}
                />
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">{t('s7_schools')}</label>
                <input
                  value={data.magic.schools}
                  onChange={(e) => update('magic', { ...data.magic, schools: e.target.value })}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                  placeholder={t('s7_schools_placeholder')}
                />
              </div>
            </div>
          )}

          {data.features.psionics && (
            <div className="space-y-3 rounded border border-accent/20 bg-bg-primary/30 p-3">
              <p className="text-xs font-semibold text-text-primary">{t('s7_psionics_title')}</p>
              <p className="text-xs text-text-secondary" dangerouslySetInnerHTML={{ __html: t('s7_psionics_hint') }} />
              <div>
                <label className="block text-xs text-text-secondary mb-1">{t('s7_power_points')}</label>
                <input
                  value={data.psionics.powerPoints}
                  onChange={(e) => update('psionics', { ...data.psionics, powerPoints: e.target.value })}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm font-mono text-text-primary outline-none focus:border-accent"
                  placeholder={t('s7_power_points_placeholder')}
                />
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">{t('s7_disciplines')}</label>
                <input
                  value={data.psionics.disciplines}
                  onChange={(e) => update('psionics', { ...data.psionics, disciplines: e.target.value })}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                  placeholder={t('s7_disciplines_placeholder')}
                />
              </div>
            </div>
          )}

          {!data.features.magic && !data.features.psionics && (
            <p className="text-xs text-text-secondary">Aktiviere Magie oder Psionik unter System-Charakter (Step 0), um hier Einstellungen vorzunehmen.</p>
          )}
        </div>
      )}

      {/* Step 8: Conditions */}
      {step === 8 && (
        <div className="space-y-4">
          <h3 className="font-heading text-text-primary">{t('s8_title')}</h3>
          <p className="text-xs text-text-secondary" dangerouslySetInnerHTML={{ __html: t('s8_hint') }} />

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
            <span className="w-20">{t('s8_header_name')}</span>
            <span className="w-16">{t('s8_header_attr')}</span>
            <span className="w-14">{t('s8_header_op')}</span>
            <span className="w-14">{t('s8_header_val')}</span>
            <span className="w-14">{t('s8_header_bonus')}</span>
            <span className="flex-1">{t('s8_header_target')}</span>
            <span className="w-4" />
          </div>

          {data.conditionals.map((c, i) => (
            <div key={i} className="flex items-center gap-2 rounded bg-bg-primary/50 p-2">
              <input
                value={c.name} onChange={(e) => {
                  const a = [...data.conditionals]; a[i] = { ...a[i], name: e.target.value }; update('conditionals', a);
                }}
                className="w-20 rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
                placeholder={t('s8_header_name')}
              />
              <select
                value={c.attribute} onChange={(e) => {
                  const a = [...data.conditionals]; a[i] = { ...a[i], attribute: e.target.value }; update('conditionals', a);
                }}
                className="w-16 rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
              >
                <option value="">—</option>
                {data.attributes.map((a) => (
                  <option key={a.name} value={a.name}>{a.name}</option>
                ))}
              </select>
              <select
                value={c.operator} onChange={(e) => {
                  const a = [...data.conditionals]; a[i] = { ...a[i], operator: e.target.value as ConditionalDef['operator'] }; update('conditionals', a);
                }}
                className="w-14 rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
              >
                <option value="gt">{t('s8_op_gt')}</option>
                <option value="gte">{t('s8_op_gte')}</option>
                <option value="lt">{t('s8_op_lt')}</option>
                <option value="lte">{t('s8_op_lte')}</option>
                <option value="eq">{t('s8_op_eq')}</option>
                <option value="per_point">{t('s8_op_per_point')}</option>
              </select>
              <input
                type="number" value={c.value} onChange={(e) => {
                  const a = [...data.conditionals]; a[i] = { ...a[i], value: Number(e.target.value) }; update('conditionals', a);
                }}
                className="w-14 rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
              />
              <input
                value={c.bonus} onChange={(e) => {
                  const a = [...data.conditionals]; a[i] = { ...a[i], bonus: e.target.value }; update('conditionals', a);
                }}
                className="w-14 rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
                placeholder={t('s8_bonus_placeholder')}
              />
              <input
                value={c.target} onChange={(e) => {
                  const a = [...data.conditionals]; a[i] = { ...a[i], target: e.target.value }; update('conditionals', a);
                }}
                className="flex-1 rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
                placeholder={t('s8_target_placeholder')}
              />
              <button onClick={() => update('conditionals', data.conditionals.filter((_, j) => j !== i))}
                className="text-danger hover:text-danger/80"><X size={14} /></button>
            </div>
          ))}

          <button
            onClick={() => update('conditionals', [...data.conditionals, { name: '', attribute: '', operator: 'gte', value: 0, bonus: '', target: '' }])}
            className="flex items-center gap-1 text-xs text-accent hover:text-accent/80"
          >
            <Plus size={14} /> {t('s8_add')}
          </button>
        </div>
      )}

      {step === 9 && (
        <div className="space-y-4">
          <h3 className="font-heading text-text-primary">{t('s4_title')}</h3>
          <div className="rounded bg-bg-primary/30 p-3 text-xs text-text-secondary">
            <p dangerouslySetInnerHTML={{ __html: t('s4_probe_hint') }} />
          </div>
          <div>
            <label className="block text-xs text-text-secondary mb-1">{t('s4_probe_type')}</label>
            <select
              value={data.probeType}
              onChange={(e) => {
                const pt = e.target.value as 'd20_target' | 'd100_threshold' | 'd20_3attr';
                update('probeType', pt);
                update('probe', pt === 'd20_target' ? '1d20+mod' : pt === 'd100_threshold' ? '1d100' : '3d20');
              }}
              className="rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
            >
              <option value="d20_target">{t('s4_probe_d20')}</option>
              <option value="d100_threshold">{t('s4_probe_d100')}</option>
              <option value="d20_3attr">{t('s4_probe_3d20')}</option>
            </select>
          </div>
          <div>
            <label className="block text-xs text-text-secondary mb-1">{t('s4_probe_label')}</label>
            <div className="flex gap-2">
              <input
                value={data.probe}
                onChange={(e) => update('probe', e.target.value)}
                className="flex-1 rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm font-mono text-text-primary outline-none focus:border-accent"
                placeholder={t('s4_probe_placeholder')}
              />
              <button
                onClick={async () => {
                  try {
                    const r = await apiClient.post('/rolls/free', { expression: testExpression(data.probe) });
                    toast.success(`${t('s4_test_result')} ${r.data.total}`);
                  } catch {
                    toast.error(t('s4_invalid'));
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
              {t('s4_enable_combat')}
            </label>
          </div>
          {data.enableCombat && (
            <div className="space-y-3 pl-4 border-l-2 border-accent/30">
              <CombatExpressionRow
                label={t('s4_initiative')}
                value={data.combat.initiative}
                attributes={data.attributes}
                diceOptions={DICE_PRESETS}
                onChange={(v) => update('combat', { ...data.combat, initiative: v })}
                onRoll={async (expr) => {
                  try {
                    const r = await apiClient.post('/rolls/free', { expression: testExpression(expr) });
                    toast.success(`${t('s4_initiative')}: ${r.data.total}`);
                  } catch { toast.error(t('s4_invalid')); }
                }}
              />
              <CombatExpressionRow
                label={t('s4_damage')}
                value={data.combat.damage}
                attributes={data.attributes}
                diceOptions={DICE_PRESETS}
                onChange={(v) => update('combat', { ...data.combat, damage: v })}
                onRoll={async (expr) => {
                  try {
                    const r = await apiClient.post('/rolls/free', { expression: testExpression(expr) });
                    toast.success(`${t('s4_damage')}: ${r.data.total}`);
                  } catch { toast.error(t('s4_invalid')); }
                }}
              />
              <div className="rounded bg-bg-primary/30 p-3 text-xs text-text-secondary mb-2">
                <p dangerouslySetInnerHTML={{ __html: t('s4_ap_hint') }} />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs text-text-secondary mb-1">{t('s4_ap_per_round')}</label>
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
                  <label className="block text-xs text-text-secondary mb-1">{t('s4_ap_max')}</label>
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

              {/* Action Types */}
              <div>
                <label className="block text-xs text-text-secondary mb-2">{t('s4_action_types')}</label>
                <div className="flex flex-wrap gap-2">
                  {['action', 'bonus_action', 'reaction'].map((at) => (
                    <label key={at} className="flex cursor-pointer items-center gap-1.5 rounded border border-bg-elevated bg-bg-primary px-3 py-1.5 text-xs text-text-secondary hover:border-accent/50">
                      <input
                        type="checkbox"
                        checked={data.combat.actionTypes.includes(at)}
                        onChange={(e) => {
                          const types = e.target.checked
                            ? [...data.combat.actionTypes, at]
                            : data.combat.actionTypes.filter((t) => t !== at);
                          const perTurn = { ...data.combat.actionsPerTurn };
                          if (!types.includes(at)) delete perTurn[at];
                          update('combat', { ...data.combat, actionTypes: types, actionsPerTurn: perTurn });
                        }}
                        className="accent-accent"
                      />
                      {t(`s4_action_${at}`)}
                    </label>
                  ))}
                </div>
                {data.combat.actionTypes.length > 0 && (
                  <div className="mt-2 grid grid-cols-3 gap-2">
                    {data.combat.actionTypes.map((at) => (
                      <div key={at}>
                        <label className="block text-[10px] text-text-secondary mb-0.5">
                          {t(`s4_action_${at}`)} {t('s4_action_per_turn')}
                        </label>
                        <input
                          type="number" min={0} max={10}
                          value={data.combat.actionsPerTurn[at] ?? 0}
                          onChange={(e) =>
                            update('combat', {
                              ...data.combat,
                              actionsPerTurn: { ...data.combat.actionsPerTurn, [at]: Number(e.target.value) },
                            })
                          }
                          className="w-16 rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
                        />
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Critical Hits */}
              <details className="rounded border border-bg-elevated bg-bg-primary/20">
                <summary className="cursor-pointer px-3 py-2 text-xs font-medium text-text-secondary hover:text-text-primary">
                  {t('s4_critical_hit')}
                </summary>
                <div className="space-y-3 px-3 pb-3">
                  <div>
                    <label className="block text-xs text-text-secondary mb-1">{t('s4_crit_threshold')}</label>
                    <input type="number" min={1} max={20}
                      value={data.combat.criticalHit?.threshold ?? 20}
                      onChange={(e) => update('combat', { ...data.combat, criticalHit: { ...data.combat.criticalHit ?? { threshold: 20, multiplier: 2 }, threshold: Number(e.target.value) } })}
                      className="w-20 rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                    />
                  </div>
                  <div>
                    <label className="block text-xs text-text-secondary mb-1">{t('s4_crit_multiplier')}</label>
                    <input type="number" min={1} max={10}
                      value={data.combat.criticalHit?.multiplier ?? 2}
                      onChange={(e) => update('combat', { ...data.combat, criticalHit: { ...data.combat.criticalHit ?? { threshold: 20, multiplier: 2 }, multiplier: Number(e.target.value) } })}
                      className="w-20 rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                    />
                  </div>
                </div>
              </details>

              {/* Saving Throws */}
              <details className="rounded border border-bg-elevated bg-bg-primary/20">
                <summary className="cursor-pointer px-3 py-2 text-xs font-medium text-text-secondary hover:text-text-primary">
                  {t('s4_saving_throws')}
                </summary>
                <div className="space-y-3 px-3 pb-3">
                  <div>
                    <label className="block text-xs text-text-secondary mb-1">{t('s4_save_base_dc')}</label>
                    <input type="number" min={1} max={30}
                      value={data.combat.savingThrows?.baseDc ?? 8}
                      onChange={(e) => update('combat', { ...data.combat, savingThrows: { ...data.combat.savingThrows ?? { baseDc: 8, proficiencyBonus: '' }, baseDc: Number(e.target.value) } })}
                      className="w-20 rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                    />
                  </div>
                  <CombatExpressionRow
                    label={t('s4_save_prof_bonus')}
                    value={data.combat.savingThrows?.proficiencyBonus ?? ''}
                    attributes={data.attributes}
                    diceOptions={[]}
                    onRoll={async () => {}}
                    onChange={(v) => update('combat', { ...data.combat, savingThrows: { ...data.combat.savingThrows ?? { baseDc: 8, proficiencyBonus: '' }, proficiencyBonus: v } })}
                  />
                </div>
              </details>

              {/* Resting */}
              <details className="rounded border border-bg-elevated bg-bg-primary/20">
                <summary className="cursor-pointer px-3 py-2 text-xs font-medium text-text-secondary hover:text-text-primary">
                  {t('s4_resting')}
                </summary>
                <div className="space-y-3 px-3 pb-3">
                  <div className="rounded bg-bg-primary/30 p-2">
                    <p className="text-xs font-medium text-text-primary mb-2">{t('s4_short_rest')}</p>
                    <div className="grid grid-cols-2 gap-3">
                      <div>
                        <label className="block text-[10px] text-text-secondary mb-0.5">{t('s4_rest_heal_pct')}</label>
                        <input type="number" min={0} max={1} step={0.1}
                          value={data.combat.resting?.shortRest.healPercent ?? 0.5}
                          onChange={(e) => update('combat', {
                            ...data.combat,
                            resting: { ...data.combat.resting ?? { shortRest: { healPercent: 0.5, recoverResources: true }, longRest: { fullHeal: true, recoverAll: true } },
                              shortRest: { ...(data.combat.resting?.shortRest ?? { healPercent: 0.5, recoverResources: true }), healPercent: Number(e.target.value) } },
                          })}
                          className="w-20 rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
                        />
                      </div>
                      <label className="flex items-center gap-1.5 text-xs text-text-secondary">
                        <input type="checkbox" className="accent-accent"
                          checked={data.combat.resting?.shortRest.recoverResources ?? true}
                          onChange={(e) => update('combat', {
                            ...data.combat,
                            resting: { ...data.combat.resting ?? { shortRest: { healPercent: 0.5, recoverResources: true }, longRest: { fullHeal: true, recoverAll: true } },
                              shortRest: { ...(data.combat.resting?.shortRest ?? { healPercent: 0.5, recoverResources: true }), recoverResources: e.target.checked } },
                          })}
                        />
                        {t('s4_rest_recover')}
                      </label>
                    </div>
                  </div>
                  <div className="rounded bg-bg-primary/30 p-2">
                    <p className="text-xs font-medium text-text-primary mb-2">{t('s4_long_rest')}</p>
                    <div className="flex flex-wrap gap-3">
                      <label className="flex items-center gap-1.5 text-xs text-text-secondary">
                        <input type="checkbox" className="accent-accent"
                          checked={data.combat.resting?.longRest.fullHeal ?? true}
                          onChange={(e) => update('combat', {
                            ...data.combat,
                            resting: { ...data.combat.resting ?? { shortRest: { healPercent: 0.5, recoverResources: true }, longRest: { fullHeal: true, recoverAll: true } },
                              longRest: { ...(data.combat.resting?.longRest ?? { fullHeal: true, recoverAll: true }), fullHeal: e.target.checked } },
                          })}
                        />
                        {t('s4_rest_full_heal')}
                      </label>
                      <label className="flex items-center gap-1.5 text-xs text-text-secondary">
                        <input type="checkbox" className="accent-accent"
                          checked={data.combat.resting?.longRest.recoverAll ?? true}
                          onChange={(e) => update('combat', {
                            ...data.combat,
                            resting: { ...data.combat.resting ?? { shortRest: { healPercent: 0.5, recoverResources: true }, longRest: { fullHeal: true, recoverAll: true } },
                              longRest: { ...(data.combat.resting?.longRest ?? { fullHeal: true, recoverAll: true }), recoverAll: e.target.checked } },
                          })}
                        />
                        {t('s4_rest_recover_all')}
                      </label>
                    </div>
                  </div>
                </div>
              </details>
            </div>
          )}
        </div>
      )}

      {step === 10 && (
        <div className="space-y-4">
          <h3 className="font-heading text-text-primary">{t('s5_title')}</h3>

          <div className="grid grid-cols-2 gap-3 text-sm">
            <div className="rounded bg-bg-primary/50 p-3">
              <p className="text-[10px] text-text-secondary uppercase tracking-wider">{t('s5_system')}</p>
              <p className="text-text-primary font-medium mt-1">{data.name || t('s1_unnamed')}</p>
              <p className="text-text-secondary text-xs">{t('s1_version')} {data.version}</p>
              {data.description && (
                <p className="text-text-secondary text-xs mt-1">{data.description}</p>
              )}
            </div>
            <div className="rounded bg-bg-primary/50 p-3">
              <p className="text-[10px] text-text-secondary uppercase tracking-wider">{t('s5_dice')}</p>
              <p className="text-text-primary font-mono mt-1">{data.probe}</p>
              {data.enableCombat && (
                <p className="text-text-secondary text-xs mt-1">
                  {t('s4_initiative')}: {data.combat.initiative} · {t('s4_damage')}: {data.combat.damage}
                </p>
              )}
            </div>
          </div>

          {data.derivedValues.length > 0 && (
          <div className="rounded bg-bg-primary/50 p-3">
            <p className="text-[10px] text-text-secondary uppercase tracking-wider mb-2">
              {t('sdv_title')} ({data.derivedValues.length})
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
              {t('s5_attributes', { count: data.attributes.length })}
            </p>
            <div className="flex flex-wrap gap-1">
              {data.attributes.map((a) => (
                <span key={a.name} className="rounded bg-bg-elevated px-2 py-0.5 text-xs text-text-primary">
                  {a.name} ({a.min}–{a.max}, Ø{a.default})
                </span>
              ))}
              {data.attributes.length === 0 && (
                <span className="text-xs text-text-secondary">{t('s5_no_attributes')}</span>
              )}
            </div>
          </div>

          {data.progressionType && (
          <div className="rounded bg-bg-primary/50 p-3">
            <p className="text-[10px] text-text-secondary uppercase tracking-wider mb-2">
              {t('s5_progression')} ({data.progressionType})
            </p>
            {data.progressionType === 'level' && (
              <div className="flex flex-wrap gap-1">
                {data.progression.levels.map((lv) => (
                  <span key={lv.level} className="rounded bg-bg-elevated px-2 py-0.5 text-xs text-text-primary">
                    Lv.{lv.level} — {lv.xpRequired} XP
                  </span>
                ))}
              </div>
            )}
            {data.progressionType === 'xp' && (
              <div className="space-y-1">
                {data.progression.xpCosts.map((xc, i) => (
                  <div key={i} className="text-xs text-text-secondary">{xc.name || '—'}: {xc.cost} XP</div>
                ))}
              </div>
            )}
            {data.progressionType === 'improvement' && (
              <div className="space-y-1">
                {data.progression.improvements.map((imp, i) => (
                  <div key={i} className="text-xs text-text-secondary">{imp.name || '—'} ({imp.dice})</div>
                ))}
              </div>
            )}
          </div>
          )}

          {data.conditionals.length > 0 && (
          <div className="rounded bg-bg-primary/50 p-3">
            <p className="text-[10px] text-text-secondary uppercase tracking-wider mb-2">
              Bedingungen ({data.conditionals.length})
            </p>
            <div className="space-y-1">
              {data.conditionals.map((c, i) => (
                <div key={i} className="flex items-center gap-2 text-xs">
                  <span className="text-text-primary w-20 truncate">{c.name || '—'}</span>
                  <span className="text-text-secondary">{c.attribute} {c.operator} {c.value} → {c.bonus} auf {c.target}</span>
                </div>
              ))}
            </div>
          </div>
          )}

          {data.abilities.length > 0 && (
          <div className="rounded bg-bg-primary/50 p-3">
            <p className="text-[10px] text-text-secondary uppercase tracking-wider mb-2">
              Fähigkeiten ({data.abilities.length})
            </p>
            <div className="space-y-1">
              {data.abilities.map((a, i) => (
                <div key={i} className="flex items-center gap-2 text-xs">
                  <span className="text-text-primary w-20 truncate">{a.name}</span>
                  {a.type === 'active' ? (
                    <span className="text-text-secondary">
                      {a.cost > 0 ? `${a.cost} ${a.costType}` : '—'} · {a.diceExpression || '—'} · {a.effect || '—'}
                    </span>
                  ) : (
                    <span className="text-accent">passiv: {a.bonus || '—'}</span>
                  )}
                </div>
              ))}
            </div>
          </div>
          )}

          <div className="rounded bg-bg-primary/50 p-3">
            <p className="text-[10px] text-text-secondary uppercase tracking-wider mb-2">
              {t('s5_skills', { count: data.skills.length })}
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
                <span className="text-xs text-text-secondary">{t('s5_no_skills')}</span>
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

      </div>

      {/* Navigation */}
      <div className="shrink-0 flex items-center justify-between mt-4 pt-4 border-t border-bg-elevated">
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
  const { t } = useTranslation('systemWizard');
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
          placeholder={t('s4_probe_placeholder')}
        />
        {diceOptions.length > 0 && (
        <button
          onClick={() => onRoll(value)}
          className="rounded bg-bg-elevated px-3 py-2 text-xs text-text-secondary hover:text-accent"
        >
          <Dice size={14} />
        </button>
        )}
      </div>
    </div>
  );
}
