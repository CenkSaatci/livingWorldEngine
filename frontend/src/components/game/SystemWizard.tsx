import { useState } from 'react';
import { Plus, X, Check, Dice1 as Dice, ArrowLeft, ArrowRight, Save } from 'lucide-react';
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
  attribute: string;
  bonus: number;
}

interface DiceCombat {
  initiative: string;
  damage: string;
  actionPoints: { standard: number; max: number };
}

interface WizardData {
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

interface Props {
  onSaved: () => void;
  onClose: () => void;
}

export function SystemWizard({ onSaved, onClose }: Props) {
  const [step, setStep] = useState(0);
  const [data, setData] = useState<WizardData>(INITIAL);
  const [saving, setSaving] = useState(false);
  const toast = useToast();

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
      await apiClient.post('/game-systems', {
        name: data.name.trim(),
        version: data.version,
        rulesJson: buildRulesJson(),
        schemaJson: '{}',
      });
      toast.success('System created');
      onSaved();
    } catch {
      toast.error('Failed to create');
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
          <h3 className="font-heading text-text-primary">Attributes</h3>
          <p className="text-xs text-text-secondary">
            Define the core attributes for your system (e.g., strength, dexterity).
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
          <h3 className="font-heading text-text-primary">Skills</h3>
          <p className="text-xs text-text-secondary">
            Skills are linked to attributes. Each skill uses an attribute for rolls.
          </p>
          {data.skills.map((skill, i) => (
            <div key={i} className="flex items-center gap-2 rounded bg-bg-primary/50 p-2">
              <input
                value={skill.name}
                onChange={(e) => {
                  const s = [...data.skills];
                  s[i] = { ...s[i], name: e.target.value };
                  update('skills', s);
                }}
                className="w-28 rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
                placeholder="Skill name"
              />
              <select
                value={skill.attribute}
                onChange={(e) => {
                  const s = [...data.skills];
                  s[i] = { ...s[i], attribute: e.target.value };
                  update('skills', s);
                }}
                className="flex-1 rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
              >
                <option value="">— Attribute —</option>
                {data.attributes.map((a) => (
                  <option key={a.name} value={a.name}>
                    {a.name}
                  </option>
                ))}
              </select>
              <span className="text-xs text-text-secondary">Bonus</span>
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
              <button
                onClick={() =>
                  update(
                    'skills',
                    data.skills.filter((_, j) => j !== i),
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
              update('skills', [...data.skills, { name: '', attribute: '', bonus: 0 }])
            }
            className="flex items-center gap-1 text-xs text-accent hover:text-accent/80"
          >
            <Plus size={14} /> Add Skill
          </button>
        </div>
      )}

      {step === 3 && (
        <div className="space-y-4">
          <h3 className="font-heading text-text-primary">Dice Mechanics</h3>
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
              <div>
                <label className="block text-xs text-text-secondary mb-1">Initiative</label>
                <div className="flex gap-2">
                  <input
                    value={data.combat.initiative}
                    onChange={(e) =>
                      update('combat', { ...data.combat, initiative: e.target.value })
                    }
                    className="flex-1 rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm font-mono text-text-primary outline-none focus:border-accent"
                  />
                  <button
                    onClick={async () => {
                      try {
                        const r = await apiClient.post('/rolls/free', {
                          expression: data.combat.initiative,
                        });
                        toast.success(`Initiative: ${r.data.total}`);
                      } catch {
                        toast.error('Invalid');
                      }
                    }}
                    className="rounded bg-bg-elevated px-3 py-2 text-xs text-text-secondary hover:text-accent"
                  >
                    <Dice size={14} />
                  </button>
                </div>
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">Damage</label>
                <div className="flex gap-2">
                  <input
                    value={data.combat.damage}
                    onChange={(e) => update('combat', { ...data.combat, damage: e.target.value })}
                    className="flex-1 rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm font-mono text-text-primary outline-none focus:border-accent"
                  />
                  <button
                    onClick={async () => {
                      try {
                        const r = await apiClient.post('/rolls/free', {
                          expression: data.combat.damage,
                        });
                        toast.success(`Damage: ${r.data.total}`);
                      } catch {
                        toast.error('Invalid');
                      }
                    }}
                    className="rounded bg-bg-elevated px-3 py-2 text-xs text-text-secondary hover:text-accent"
                  >
                    <Dice size={14} />
                  </button>
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs text-text-secondary mb-1">AP Standard</label>
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
                  <label className="block text-xs text-text-secondary mb-1">AP Max</label>
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
          <h3 className="font-heading text-text-primary">Review & Save</h3>
          <div className="text-sm text-text-primary space-y-1">
            <p>
              <span className="text-text-secondary">System:</span> {data.name || 'Unnamed'} v
              {data.version}
            </p>
            <p>
              <span className="text-text-secondary">Attributes:</span> {data.attributes.length} ·{' '}
              <span className="text-text-secondary">Skills:</span> {data.skills.length}
            </p>
            <p>
              <span className="text-text-secondary">Dice:</span> {data.probe}
              {data.enableCombat ? ` · Combat: ${data.combat.initiative}` : ''}
            </p>
          </div>
          <pre className="max-h-48 overflow-y-auto rounded border border-bg-elevated bg-bg-primary p-3 text-xs font-mono text-text-secondary">
            {buildRulesJson()}
          </pre>
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
}
