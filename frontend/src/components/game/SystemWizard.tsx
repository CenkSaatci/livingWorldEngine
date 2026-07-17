import { useState, useImperativeHandle, forwardRef } from 'react';
import { Plus, X, Check, Dice1 as Dice, ArrowLeft, ArrowRight, Save, Trash2 } from 'lucide-react';
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

export interface WizardData {
  name: string;
  version: number;
  description: string;
  attributes: AttributeDef[];
  skills: SkillDef[];
  probe: string;
  enableCombat: boolean;
  combat: DiceCombat;
}

const STEPS = ['Basic', 'Attributes', 'Skills', 'Dice', 'Review'];

const DICE_PRESETS = [
  { v: '1d2', l: '1d2' }, { v: '1d3', l: '1d3' }, { v: '1d4', l: '1d4' }, { v: '1d6', l: '1d6' },
  { v: '1d8', l: '1d8' }, { v: '1d10', l: '1d10' }, { v: '1d12', l: '1d12' }, { v: '1d20', l: '1d20' },
  { v: '1d100', l: '1d100' }, { v: '2d6', l: '2d6' }, { v: '3d6', l: '3d6' }, { v: '4dF', l: '4dF' },
];

const INITIAL: WizardData = {
  name: '',
  version: 1,
  description: '',
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
            {s}
            {i < STEPS.length - 1 && <span className="w-4 border-t border-bg-elevated" />}
          </div>
        ))}
      </div>

      {step === 0 && (
        <div className="space-y-4">
          <h3 className="font-heading text-text-primary">Basic Information</h3>
          <div>
            <label className="block text-xs text-text-secondary mb-1">System Name *</label>
            <input
              value={data.name}
              onChange={(e) => update('name', e.target.value)}
              className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs text-text-secondary mb-1">Version</label>
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
            <label className="block text-xs text-text-secondary mb-1">Description</label>
            <textarea
              value={data.description}
              onChange={(e) => update('description', e.target.value)}
              rows={3}
              className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent resize-none"
            />
          </div>
        </div>
      )}

      {step === 1 && (
        <div className="space-y-4">
          <h3 className="font-heading text-text-primary">Attribute</h3>
          <p className="text-xs text-text-secondary">
            Definiere die Kerneigenschaften deines Systems (z.B. <em>Stärke</em>, <em>Geschicklichkeit</em>). Attributnamen werden <strong>klein</strong> geschrieben (z.B. <code>staerke</code>) und später in Würfelausdrücken wie <code>1d20+staerke</code> verwendet.
          </p>
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
                placeholder="Name"
              />
              <select
                value={attr.type}
                onChange={(e) => {
                  const a = [...data.attributes];
                  a[i] = { ...a[i], type: e.target.value as 'INT' | 'STRING' | 'BOOL' };
                  update('attributes', a);
                }}
                className="rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
              >
                <option value="INT">INT</option>
                <option value="STRING">STRING</option>
                <option value="BOOL">BOOL</option>
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
                placeholder="Min"
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
                placeholder="Max"
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
                placeholder="Default"
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
            <Plus size={14} /> Add Attribute
          </button>
        </div>
      )}

      {step === 2 && (
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

      {step === 3 && (
        <div className="space-y-4">
          <h3 className="font-heading text-text-primary">Würfelmechanik</h3>
          <div className="rounded bg-bg-primary/30 p-3 text-xs text-text-secondary">
            <p><strong>Probe (Probe Expression):</strong> Der Würfelausdruck für Fertigkeitsproben, z.B. <code>1d20+mod</code> (1W20 + Modifikator), <code>2d6+mod</code> (2W6), <code>4dF</code> (Fudge-Würfel). <code>mod</code> wird später durch den Fertigkeitswert des Charakters ersetzt. Statt <code>mod</code> kann auch ein Attributsname stehen wie <code>1d20+staerke</code>.</p>
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

      {step === 4 && (
        <div className="space-y-4">
          <h3 className="font-heading text-text-primary">Übersicht & Speichern</h3>

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
          <ArrowLeft size={14} /> {step === 0 ? 'Cancel' : 'Back'}
        </button>
        {step < STEPS.length - 1 ? (
          <button
            onClick={() => setStep(step + 1)}
            className="flex items-center gap-1 rounded bg-accent px-4 py-2 text-xs text-white hover:bg-accent/80"
          >
            Next <ArrowRight size={14} />
          </button>
        ) : (
          <button
            onClick={handleSave}
            disabled={saving || !data.name.trim()}
            className="flex items-center gap-1 rounded bg-accent px-4 py-2 text-xs text-white hover:bg-accent/80 disabled:opacity-40"
          >
            <Save size={14} /> {saving ? 'Creating…' : 'Save System'}
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
