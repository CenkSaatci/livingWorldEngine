import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, ArrowRight, Check, X } from 'lucide-react';
import { apiClient } from '../../api/client';
import { useToast } from '../../hooks/useToast';
import {
  buildCost,
  buildFinalAttributes,
  buildFinalTraits,
  buildIssues,
  findPackage,
  isChoiceMod,
  packageSelectionWarnings,
  type CharacterBuild,
  type PkgDef,
  type WizardData,
} from '../../types/gameSystem';

interface Props {
  worldId: string;
  rules: WizardData;
  onCreated: (entityId: string) => void;
  onClose: () => void;
}

const KINDS = ['species', 'culture', 'profession'] as const;

export function CharacterWizard({ worldId, rules, onCreated, onClose }: Props) {
  const { t } = useTranslation('character');
  const toast = useToast();
  const [step, setStep] = useState(0);
  const [name, setName] = useState('');
  const [saving, setSaving] = useState(false);
  const [build, setBuild] = useState<CharacterBuild>({
    packageSelections: [],
    attributes: {},
    traits: [],
  });

  const dialogRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    dialogRef.current?.focus();
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !saving) onClose();
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [saving, onClose]);

  const cost = buildCost(rules, build);
  const issues = buildIssues(rules, build);
  const warnings = packageSelectionWarnings(rules, build.packageSelections);
  const finalAttrs = buildFinalAttributes(rules, build);
  const finalTraits = buildFinalTraits(rules, build);

  const setSelection = (kind: string, pkgName: string) => {
    const options = (rules.packages ?? []).filter((p) => p.kind === kind);
    const rest = build.packageSelections.filter(
      (sel) => !options.some((o) => o.name === sel.name),
    );
    const selected = options.find((o) => o.name === pkgName);
    setBuild({
      ...build,
      packageSelections: selected ? [...rest, { name: selected.name, choices: [] }] : rest,
    });
  };

  const setChoice = (pkgName: string, index: number, value: string) => {
    setBuild({
      ...build,
      packageSelections: build.packageSelections.map((sel) => {
        if (sel.name !== pkgName) return sel;
        const choices = [...(sel.choices ?? [])];
        choices[index] = value;
        return { ...sel, choices };
      }),
    });
  };

  const purchase = (attrName: string, value: number) => {
    setBuild({ ...build, attributes: { ...build.attributes, [attrName]: value } });
  };

  const toggleTrait = (defName: string) => {
    const exists = build.traits.some((tr) => tr.name === defName);
    const def = (rules.traits ?? []).find((tr) => tr.name === defName);
    const tiers = def?.costs ?? [];
    setBuild({
      ...build,
      traits: exists
        ? build.traits.filter((tr) => tr.name !== defName)
        : [...build.traits, { name: defName, ...(tiers.length > 1 ? { tier: tiers[0].tier } : {}) }],
    });
  };

  const setTier = (defName: string, tier: string) => {
    setBuild({
      ...build,
      traits: build.traits.map((tr) => (tr.name === defName ? { ...tr, tier } : tr)),
    });
  };

  const handleSave = async () => {
    if (!name.trim() || issues.length > 0) return;
    setSaving(true);
    try {
      const attributesJson = JSON.stringify(
        Object.fromEntries(finalAttrs.map((a) => [a.name, a.value])),
      );
      const metadataJson = JSON.stringify({
        traits: finalTraits,
        package_selections: build.packageSelections,
        ...(rules.creationBudget?.fatePoints != null
          ? { fate_points: rules.creationBudget.fatePoints }
          : {}),
      });
      const res = await apiClient.post(`/worlds/${worldId}/entities`, {
        entityType: 'PC',
        name: name.trim(),
        attributesJson,
        metadataJson,
      });
      onCreated(res.data.id);
      onClose();
    } catch {
      toast.error(t('wizard.saveFailed'));
    } finally {
      setSaving(false);
    }
  };

  const CODE_KEYS: Record<string, string> = {
    build_over_budget: 'codeBuildOverBudget',
    build_attr_range: 'codeAttrRange',
    build_attr_cap: 'codeAttrCap',
    build_attr_total_cap: 'codeAttrTotalCap',
    build_advantage_cap: 'codeAdvantageCap',
    build_trait_excludes: 'codeTraitExcludes',
    build_trait_requires: 'codeTraitRequires',
    choice_count: 'codeChoiceCount',
    choice_invalid: 'codeChoiceInvalid',
    duplicate_kind: 'codeDuplicateKind',
    restricted: 'codeRestricted',
    unknown: 'codeUnknown',
    recommended: 'codeRecommended',
  };

  const codeText = (code: string) => {
    const [key, a = '', b = ''] = code.split(':');
    const mapped = CODE_KEYS[key];
    return mapped ? t(`wizard.${mapped}`, { a, b }) : code;
  };

  const budgetLabel = cost.budget != null
    ? `${cost.total} / ${cost.budget} ${t('wizard.ap')}`
    : `${cost.total} ${t('wizard.ap')}`;

  return (
    <div
      className="modal-overlay fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/60 pt-10"
      onClick={() => { if (!saving) onClose(); }}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-label={t('wizard.title')}
        ref={dialogRef}
        tabIndex={-1}
        className="w-full max-w-2xl rounded-lg border border-bg-elevated bg-bg-surface p-5 outline-none"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="mb-4 flex items-center justify-between">
          <h2 className="font-heading text-lg text-text-primary">{t('wizard.title')}</h2>
          <button
            onClick={onClose}
            disabled={saving}
            aria-label={t('wizard.close')}
            className="text-text-secondary hover:text-danger disabled:opacity-40"
          >
            <X size={18} />
          </button>
        </div>

        {/* Stepper */}
        <div className="mb-4 flex items-center gap-2 text-xs">
          {[t('wizard.stepPackages'), t('wizard.stepAttributes'), t('wizard.stepTraits'), t('wizard.stepSummary')].map((label, i) => (
            <span
              key={label}
              className={`rounded px-2 py-1 ${i === step ? 'bg-accent/15 text-accent' : 'text-text-secondary'}`}
            >
              {i + 1}. {label}
            </span>
          ))}
        </div>

        {/* Budget */}
        <div className="mb-4">
          <div className="mb-1 flex items-center justify-between text-xs">
            <span className="text-text-secondary">{t('wizard.budget')}</span>
            <span className={cost.over ? 'text-danger' : 'text-text-primary'}>{budgetLabel}</span>
          </div>
          {cost.budget != null && (
            <div className="h-2 overflow-hidden rounded-full bg-bg-elevated">
              <div
                className={`h-full rounded-full ${cost.over ? 'bg-danger' : 'bg-accent'}`}
                style={{ width: `${Math.min(100, (cost.total / Math.max(1, cost.budget)) * 100)}%` }}
              />
            </div>
          )}
        </div>

        {step === 0 && (
          <div className="space-y-3">
            {KINDS.map((kind) => {
              const options = (rules.packages ?? []).filter((p) => p.kind === kind && p.name);
              if (options.length === 0) return null;
              const current = build.packageSelections.find((sel) =>
                options.some((o) => o.name === sel.name),
              );
              return (
                <div key={kind} className="space-y-2">
                  <label className="block text-xs text-text-secondary">
                    {t(`wizard.${kind}`)}
                    <select
                      value={current?.name ?? ''}
                      onChange={(e) => setSelection(kind, e.target.value)}
                      className="ml-2 rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
                    >
                      <option value="">-</option>
                      {options.map((o) => (
                        <option key={o.name} value={o.name}>{o.name} ({o.cost ?? 0} AP)</option>
                      ))}
                    </select>
                  </label>
                  {current && (() => {
                    const def = findPackage(rules.packages ?? [], current.name) as PkgDef;
                    const choiceMods = (def.attributeMods ?? []).filter(isChoiceMod);
                    if (choiceMods.length === 0) return null;
                    return (
                      <div className="flex flex-wrap items-center gap-2 pl-4 text-xs">
                        {choiceMods.map((m, ci) => {
                          const allowed: string[] = m.choice === '*'
                            ? rules.attributes.map((a) => a.name)
                            : Array.isArray(m.choice) ? m.choice
                              : typeof m.choice === 'string' ? [m.choice] : [];
                          return (
                            <select
                              key={ci}
                              aria-label={`${current.name} ${t('wizard.choice')}`}
                              value={current.choices?.[ci] ?? ''}
                              onChange={(e) => setChoice(current.name, ci, e.target.value)}
                              className="rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
                            >
                              <option value="">-</option>
                              {allowed.map((a) => <option key={a} value={a}>{a} {m.value > 0 ? '+' : ''}{m.value}</option>)}
                            </select>
                          );
                        })}
                      </div>
                    );
                  })()}
                </div>
              );
            })}
            {(rules.packages ?? []).length === 0 && (
              <p className="text-xs text-text-secondary">{t('wizard.noPackages')}</p>
            )}
          </div>
        )}

        {step === 1 && (
          <div className="space-y-2">
            {finalAttrs.map((a) => {
              const def = rules.attributes.find((x) => x.name === a.name)!;
              return (
                <div key={a.name} className="flex items-center gap-2 text-sm">
                  <span className="w-32 text-text-primary">{a.name}</span>
                  <button
                    aria-label={`${a.name} -`}
                    onClick={() => purchase(a.name, Math.max(def.min, a.purchased - 1))}
                    className="rounded border border-bg-elevated px-2 text-text-secondary hover:text-accent"
                  >
                    −
                  </button>
                  <span className="w-8 text-center font-mono text-text-primary">{a.purchased}</span>
                  <button
                    aria-label={`${a.name} +`}
                    onClick={() => purchase(a.name, Math.min(def.max, rules.creationBudget?.maxAttrValue ?? def.max, a.purchased + 1))}
                    className="rounded border border-bg-elevated px-2 text-text-secondary hover:text-accent"
                  >
                    +
                  </button>
                  <span className="text-xs text-text-secondary">
                    {a.mod !== 0 && <>({a.mod > 0 ? '+' : ''}{a.mod}) → </>}
                    <span className="font-mono text-text-primary">{a.value}</span>
                  </span>
                  <span className="ml-auto text-xs text-text-secondary">
                    {t('wizard.cost')}: {(() => {
                      const from = rules.creationBudget?.attrBase ?? def.min;
                      const tiers = def.costs ?? rules.attributeCosts?.default ?? [];
                      let sum = 0;
                      const sorted = [...tiers].sort((x, y) => x.upTo - y.upTo);
                      for (let v = from + 1; v <= a.purchased; v++) {
                        sum += (sorted.find((x) => v <= x.upTo) ?? sorted[sorted.length - 1])?.cost ?? 0;
                      }
                      return sum;
                    })()}
                  </span>
                </div>
              );
            })}
          </div>
        )}

        {step === 2 && (
          <div className="space-y-2">
            {(rules.traits ?? []).map((def) => {
              const selected = build.traits.find((tr) => tr.name === def.name);
              const tiers = def.costs ?? [];
              return (
                <div key={def.name} className="flex items-center gap-2 text-sm">
                  <label className="flex flex-1 items-center gap-2">
                    <input
                      type="checkbox"
                      checked={!!selected}
                      onChange={() => toggleTrait(def.name)}
                    />
                    <span className="text-text-primary">{def.name}</span>
                    <span className="text-xs text-text-secondary capitalize">{def.kind}</span>
                  </label>
                  {selected && tiers.length > 1 && (
                    <select
                      aria-label={`${def.name} ${t('wizard.tier')}`}
                      value={selected.tier ?? tiers[0].tier}
                      onChange={(e) => setTier(def.name, e.target.value)}
                      className="rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
                    >
                      {tiers.map((c) => (
                        <option key={c.tier} value={c.tier}>{c.tier} ({c.cost} AP)</option>
                      ))}
                    </select>
                  )}
                </div>
              );
            })}
            {(rules.traits ?? []).length === 0 && (
              <p className="text-xs text-text-secondary">{t('wizard.noTraits')}</p>
            )}
            {finalTraits.length > 0 && (
              <div className="pt-2">
                <p className="mb-1 text-[10px] uppercase text-text-secondary">{t('wizard.finalTraits')}</p>
                <div className="flex flex-wrap gap-1">
                  {finalTraits.map((tr) => (
                    <span key={tr} className="rounded bg-accent/10 px-2 py-0.5 text-xs text-accent">{tr}</span>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        {step === 3 && (
          <div className="space-y-3">
            <label className="block text-xs text-text-secondary">
              {t('wizard.name')}
              <input
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="mt-1 w-full rounded border border-bg-elevated bg-bg-primary px-2 py-1.5 text-sm text-text-primary outline-none focus:border-accent"
              />
            </label>
            <div className="grid grid-cols-2 gap-3 text-xs">
              <div className="rounded bg-bg-primary/50 p-2">
                <p className="mb-1 font-medium text-text-primary">{t('wizard.finalAttributes')}</p>
                {finalAttrs.map((a) => (
                  <p key={a.name} className="text-text-secondary">
                    {a.name}: <span className="font-mono text-text-primary">{a.value}</span>
                  </p>
                ))}
              </div>
              <div className="rounded bg-bg-primary/50 p-2">
                <p className="mb-1 font-medium text-text-primary">{t('wizard.costs')}</p>
                <p className="text-text-secondary">{t('wizard.attrCost')}: {cost.attributes}</p>
                <p className="text-text-secondary">{t('wizard.traitCost')}: {cost.traits}</p>
                <p className="text-text-secondary">{t('wizard.packageCost')}: {cost.packages}</p>
                <p className={`font-medium ${cost.over ? 'text-danger' : 'text-text-primary'}`}>
                  {t('wizard.total')}: {cost.total} / {cost.budget ?? '∞'}
                </p>
              </div>
            </div>
            {issues.map((code) => (
              <p key={code} className="text-xs text-danger">{t('wizard.issue')}: {codeText(code)}</p>
            ))}
            {warnings.map((code) => (
              <p key={code} className="text-xs text-warning">{t('wizard.warning')}: {codeText(code)}</p>
            ))}
          </div>
        )}

        {/* Navigation */}
        <div className="mt-5 flex items-center justify-between">
          <button
            onClick={() => (step === 0 ? onClose() : setStep(step - 1))}
            disabled={saving}
            className="flex items-center gap-1 rounded border border-bg-elevated px-3 py-1.5 text-sm text-text-secondary hover:text-text-primary disabled:opacity-40"
          >
            <ArrowLeft size={14} /> {step === 0 ? t('wizard.cancel') : t('wizard.back')}
          </button>
          {step < 3 ? (
            <button
              onClick={() => setStep(step + 1)}
              className="flex items-center gap-1 rounded bg-accent px-3 py-1.5 text-sm text-white hover:bg-accent/80"
            >
              {t('wizard.next')} <ArrowRight size={14} />
            </button>
          ) : (
            <button
              onClick={handleSave}
              disabled={saving || !name.trim() || issues.length > 0}
              className="flex items-center gap-1 rounded bg-accent px-3 py-1.5 text-sm text-white hover:bg-accent/80 disabled:opacity-40"
            >
              <Check size={14} /> {t('wizard.save')}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
