import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Heart, Shield, Zap, Sparkles, Check, X, Plus, Loader2 } from 'lucide-react';
import { apiClient } from '../../api/client';
import { useSheet, type SheetData } from '../../hooks/useSheet';
import { useToast } from '../../hooks/useToast';
import { ProbeRoller } from './ProbeRoller';

const VALUE_ICONS: Record<string, React.ReactNode> = {
  hp: <Heart size={16} className="text-danger" />,
  ac: <Shield size={16} className="text-accent" />,
  mp: <Zap size={16} className="text-blue-400" />,
  sanity: <Sparkles size={16} className="text-purple-400" />,
};

function AttrInput({ name, value, min, max, entityId, allAttributes, onSaved }: { name: string; value: number; min: number; max: number; entityId: string; allAttributes: Record<string, number>; onSaved: () => void }) {
  const toast = useToast();
  const [editing, setEditing] = useState(false);
  const [editVal, setEditVal] = useState(String(value));
  const [saving, setSaving] = useState(false);

  const save = async () => {
    const newVal = parseInt(editVal, 10);
    if (isNaN(newVal) || newVal === value) { setEditing(false); return; }
    if (newVal < min || newVal > max) { setEditVal(String(value)); setEditing(false); return; }
    setSaving(true);
    try {
      // Vollständiges Attributs-Map senden: Das Backend mergt nur, was es bekommt.
      // Würde man nur {[name]: newVal} senden, gingen geerbte Defaults (die nur
      // in der Sheet-Anzeige, nicht in der DB stehen) beim ersten Speichern verloren
      // und abgeleitete Formeln würden mit „(Fehler)" fehlschlagen.
      await apiClient.patch(`/entities/${entityId}/attributes`, { ...allAttributes, [name]: newVal });
      onSaved();
      setEditing(false);
    } catch { toast.error('Failed to save attribute'); setEditVal(String(value)); setEditing(false); }
    finally { setSaving(false); }
  };

  const cancel = () => { setEditVal(String(value)); setEditing(false); };

  return editing ? (
    <div className="flex items-center justify-center gap-1">
      <input
        type="number" value={editVal} autoFocus
        min={min} max={max}
        onChange={(e) => setEditVal(e.target.value)}
        onKeyDown={(e) => { if (e.key === 'Enter') save(); if (e.key === 'Escape') cancel(); }}
        className="w-16 rounded border border-accent bg-bg-primary px-1 py-0.5 text-lg font-heading text-center text-text-primary outline-none"
        disabled={saving}
      />
      <button onClick={save} className="text-success hover:text-success/60" disabled={saving}>
        {saving ? <Loader2 size={14} className="animate-spin" /> : <Check size={14} />}
      </button>
      <button onClick={cancel} className="text-danger hover:text-danger/60"><X size={14} /></button>
    </div>
  ) : (
    <button onClick={() => setEditing(true)} className="w-full text-center hover:bg-bg-elevated/30 rounded transition-colors">
      <p className="text-lg font-heading text-text-primary">{value}</p>
    </button>
  );
}

interface Props {
  entityId: string;
}

function XpInput({ value, entityId, onSaved }: { value: number; entityId: string; onSaved: () => void }) {
  const { t } = useTranslation('character');
  const toast = useToast();
  const [editing, setEditing] = useState(false);
  const [editVal, setEditVal] = useState(String(value));
  const [saving, setSaving] = useState(false);

  const save = async () => {
    const newVal = parseInt(editVal, 10);
    if (isNaN(newVal) || newVal === value) { setEditing(false); return; }
    setSaving(true);
    try {
      await apiClient.patch(`/entities/${entityId}/progression`, { experience_points: newVal });
      onSaved();
      setEditing(false);
    } catch { toast.error(t('sheet.xpSaveFailed')!); setEditVal(String(value)); setEditing(false); }
    finally { setSaving(false); }
  };

  return editing ? (
    <div className="flex items-center gap-1">
      <input type="number" value={editVal} autoFocus min={0} max={Math.max(value * 10, 1000000)}
        onChange={(e) => setEditVal(e.target.value)}
        onKeyDown={(e) => { if (e.key === 'Enter') save(); if (e.key === 'Escape') { setEditVal(String(value)); setEditing(false); }}}
        className="w-20 rounded border border-accent bg-bg-primary px-1 py-0.5 text-xs text-text-primary text-right outline-none"
        disabled={saving}
      />
      <button onClick={save} className="text-success" disabled={saving}>
        {saving ? <Loader2 size={12} className="animate-spin" /> : <Check size={12} />}
      </button>
      <button onClick={() => { setEditVal(String(value)); setEditing(false); }} className="text-danger"><X size={12} /></button>
    </div>
  ) : (
    <button onClick={() => setEditing(true)} className="text-xs text-text-secondary hover:text-accent">
      {value} XP
    </button>
  );
}

export function CharacterSheet({ entityId }: Props) {
  const { t } = useTranslation('character');
  const { data, loading, error, refetch } = useSheet(entityId);
  const [skillFilter, setSkillFilter] = useState('');
  const [skillOverrides, setSkillOverrides] = useState<Record<string, number>>({});

  useEffect(() => {
    if (data?.skills) {
      const map: Record<string, number> = {};
      data.skills.forEach((s) => {
        if (s.perCharacterValue != null) map[s.name] = s.perCharacterValue;
      });
      // Nur setzen, wenn sich die Werte tatsächlich geändert haben (verhindert unnötige Re-Renders)
      setSkillOverrides((prev) => {
        const prevKeys = Object.keys(prev);
        const newKeys = Object.keys(map);
        if (prevKeys.length !== newKeys.length) return map;
        for (const k of newKeys) {
          if (prev[k] !== map[k]) return map;
        }
        return prev;
      });
    }
  }, [data?.skills]);

  if (loading) return <div className="p-4 text-sm text-text-secondary">{t('sheet.loading')}</div>;
  if (error) return <div className="p-4 text-sm text-danger">{error}</div>;
  if (!data) return null;

  const filteredSkills = data.skills.filter((s) =>
    s.name.toLowerCase().includes(skillFilter.toLowerCase())
  );

  return (
    <div className="space-y-4 p-4">
      {/* Entity Info */}
      <div className="rounded-lg border border-bg-elevated bg-bg-surface p-3">
        <h2 className="font-heading text-lg text-text-primary">{data.entity.name}</h2>
        <p className="text-xs text-text-secondary">{data.entity.entityType}</p>
      </div>

      {/* XP Bar */}
      <div className="rounded-lg border border-bg-elevated bg-bg-surface p-3">
        <div className="flex items-center gap-3 text-xs">
          <Sparkles size={14} className="text-warning" />
          <span className="font-medium text-text-primary">{t('sheet.level')} {data.level}</span>
          <div className="flex-1 h-2 rounded-full bg-bg-elevated overflow-hidden">
            <div className="h-full rounded-full bg-warning" style={{ width: `${Math.min(100, (data.experiencePoints % 1000) / 10)}%` }} />
          </div>
          <XpInput value={data.experiencePoints} entityId={entityId} onSaved={refetch} />
        </div>
      </div>

      {/* Attributes */}
      <div className="rounded-lg border border-bg-elevated bg-bg-surface p-3">
        <h3 className="mb-2 text-xs font-semibold text-text-secondary uppercase tracking-wider">
          {t('sheet.attributes')}
        </h3>
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
          {data.attributes.map((attr) => (
            <div key={attr.name} className="rounded bg-bg-primary/50 p-2 text-center">
              <p className="text-[10px] text-text-secondary uppercase">{attr.name}</p>
              <AttrInput name={attr.name} value={attr.value} min={attr.min} max={attr.max} entityId={entityId}
                allAttributes={Object.fromEntries(data.attributes.map((a) => [a.name, a.value]))} onSaved={refetch} />
              {attr.modifier !== 0 && (
                <p className="text-xs text-accent">
                  {attr.modifier > 0 ? '+' : ''}{Math.round(attr.modifier * 10) / 10}
                </p>
              )}
            </div>
          ))}
        </div>
      </div>

      {/* Derived Values */}
      {data.derivedValues.length > 0 && (
        <div className="rounded-lg border border-bg-elevated bg-bg-surface p-3">
          <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
            {data.derivedValues.map((dv) => (
              <div key={dv.name} className="flex items-center gap-2 rounded bg-bg-primary/50 p-2">
                {VALUE_ICONS[dv.name] ?? <Zap size={16} className="text-text-secondary" />}
                <div>
                  <p className="text-[10px] text-text-secondary uppercase">{dv.name}</p>
                  <p className="text-sm font-heading text-text-primary">{Math.round(dv.value * 10) / 10}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Skills */}
      <div className="rounded-lg border border-bg-elevated bg-bg-surface p-3">
        <div className="flex items-center justify-between mb-2">
          <h3 className="text-xs font-semibold text-text-secondary uppercase tracking-wider">
            {t('sheet.skills')}
          </h3>
          <input
            value={skillFilter}
            onChange={(e) => setSkillFilter(e.target.value)}
            placeholder={t('sheet.searchSkills')!}
            className="w-32 rounded border border-bg-elevated bg-bg-primary px-2 py-0.5 text-[10px] text-text-primary outline-none focus:border-accent"
          />
        </div>
        <div className="space-y-1 max-h-64 overflow-y-auto">
          {filteredSkills.map((skill) => (
            <SkillRow key={skill.name} skill={skill} entityId={entityId}
              skillOverrides={skillOverrides} setSkillOverrides={setSkillOverrides}
              onSaved={refetch} />
          ))}
          {filteredSkills.length === 0 && (
            <p className="text-xs text-text-secondary">{t('sheet.noSkills')}</p>
          )}
        </div>
      </div>

      {/* Conditionals */}
      {data.conditionals.length > 0 && (
        <div className="rounded-lg border border-bg-elevated bg-bg-surface p-3">
          <h3 className="mb-2 text-xs font-semibold text-text-secondary uppercase tracking-wider">
            {t('sheet.conditionals')}
          </h3>
          <div className="space-y-1">
            {data.conditionals.map((c) => (
              <div
                key={c.name}
                className={`flex items-center gap-2 rounded px-2 py-1 text-xs ${
                  c.active ? 'bg-success/10 text-success' : 'bg-bg-primary/30 text-text-secondary'
                }`}
              >
                <span className={`h-1.5 w-1.5 rounded-full ${c.active ? 'bg-success' : 'bg-bg-elevated'}`} />
                <span>{c.name}</span>
                {c.active && c.description && (
                  <span className="text-[10px] opacity-70">{c.description}</span>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Formula Overrides */}
      <FormulaOverrides entityId={entityId} onSaved={refetch} />

      {/* Abilities */}
      <div className="rounded-lg border border-bg-elevated bg-bg-surface p-3">
        <h3 className="mb-2 text-xs font-semibold text-text-secondary uppercase tracking-wider">
          {t('sheet.abilities')}
        </h3>
        {data.abilities.length === 0 ? (
          <p className="text-xs text-text-secondary">{t('sheet.noAbilities')}</p>
        ) : (
          <div className="space-y-1">
            {data.abilities.map((a) => (
              <AbilityRow key={a.name} ability={a} attributes={data.attributes} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function SkillRow({ skill, entityId, skillOverrides, setSkillOverrides, onSaved }: {
  skill: SheetData['skills'][0]; entityId: string;
  skillOverrides: Record<string, number>; setSkillOverrides: (v: Record<string, number>) => void;
  onSaved: () => void;
}) {
  const toast = useToast();
  const [editing, setEditing] = useState(false);
  const [editVal, setEditVal] = useState(String(skill.total));
  const [saving, setSaving] = useState(false);

  const save = async () => {
    const newVal = parseInt(editVal, 10);
    if (isNaN(newVal) || newVal === skill.total) { setEditing(false); return; }
    setSaving(true);
    try {
      const updated = { ...skillOverrides, [skill.name]: newVal };
      await apiClient.patch(`/entities/${entityId}/skills`, updated);
      setSkillOverrides(updated);
      onSaved();
      setEditing(false);
    } catch { toast.error('Failed to save skill'); setEditVal(String(skill.total)); setEditing(false); }
    finally { setSaving(false); }
  };

  return (
    <div className="flex items-center justify-between rounded bg-bg-primary/30 px-2 py-1">
      <span className="text-xs text-text-primary">{skill.name}</span>
      <div className="flex items-center gap-2">
        {editing ? (
          <div className="flex items-center gap-1">
            <input type="number" value={editVal} autoFocus min={0} max={99}
              onChange={(e) => setEditVal(e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter') save(); if (e.key === 'Escape') { setEditVal(String(skill.total)); setEditing(false); }}}
              className="w-14 rounded border border-accent bg-bg-primary px-1 py-0.5 text-xs text-text-primary text-right outline-none"
              disabled={saving}
            />
            <button onClick={save} className="text-success" disabled={saving}>
              {saving ? <Loader2 size={12} className="animate-spin" /> : <Check size={12} />}
            </button>
            <button onClick={() => { setEditVal(String(skill.total)); setEditing(false); }} className="text-danger"><X size={12} /></button>
          </div>
        ) : (
          <button onClick={() => setEditing(true)} className="group flex items-center gap-1 hover:bg-bg-elevated/30 rounded px-1 transition-colors">
            <span className={`text-xs font-mono ${skill.perCharacterValue != null ? 'text-accent' : 'text-text-secondary'}`}>
              {skill.total > 0 ? '+' : ''}{skill.total}
            </span>
            {skill.perCharacterValue != null && (
              <span className="text-[9px] text-accent/60 italic">override</span>
            )}
          </button>
        )}
        <ProbeRoller entityId={entityId} skillName={skill.name} skillTotal={skill.total} />
      </div>
    </div>
  );
}

function AbilityRow({ ability, attributes }: {
  ability: SheetData['abilities'][0];
  attributes: SheetData['attributes'];
}) {
  const { t } = useTranslation('character');
  const toast = useToast();
  const [result, setResult] = useState<number | null>(null);
  const [showDetail, setShowDetail] = useState(false);
  const [failed, setFailed] = useState(false);

  const handleUse = async () => {
    // Attribut-Referenzen in der diceExpression auflösen (z.B. "2d6+intelligenz" → "2d6+14")
    let expr = ability.diceExpression || '1d20';
    for (const attr of attributes) {
      expr = expr.split(attr.name).join(String(attr.value));
    }
    setFailed(false);
    try {
      const res = await apiClient.post('/rolls/free', { expression: expr });
      setResult(res.data.total ?? 0);
    } catch {
      // Kein lokaler Fallback-Wurf: Ein fehlgeschlagener Server-Wurf darf nicht
      // wie ein echtes Ergebnis aussehen.
      setResult(null);
      setFailed(true);
      toast.error(t('sheet.abilityFailed')!);
    }
    setShowDetail(true);
    setTimeout(() => setShowDetail(false), 3000);
  };

  if (ability.type !== 'active') {
    return (
      <div className="flex items-center justify-between rounded bg-bg-primary/30 px-2 py-1 text-xs">
        <div className="flex items-center gap-2">
          <span className="text-text-primary font-medium">{ability.name}</span>
          <span className="text-[9px] uppercase text-text-secondary">passive</span>
        </div>
        {ability.effect && (
          <span className="text-[10px] text-text-secondary truncate max-w-[200px]">{ability.effect}</span>
        )}
      </div>
    );
  }

  return (
    <div className="flex items-center justify-between rounded bg-accent/10 px-2 py-1 text-xs">
      <div className="flex items-center gap-2">
        <span className="text-text-primary font-medium">{ability.name}</span>
        <span className="text-[9px] uppercase text-accent">active</span>
        {ability.apCost > 0 && (
          <span className="text-[10px] text-text-secondary">AP: {ability.apCost}</span>
        )}
      </div>
      <div className="flex items-center gap-2">
        {ability.effect && (
          <span className="text-[10px] text-text-secondary truncate max-w-[140px]">{ability.effect}</span>
        )}
        <button onClick={handleUse}
          className="rounded bg-accent px-2 py-0.5 text-[10px] text-white hover:bg-accent/80 disabled:opacity-40"
        >
          Use
        </button>
          {showDetail && result !== null && (
            <span className="text-xs font-mono text-accent font-bold">{result}</span>
          )}
          {showDetail && failed && (
            <span className="text-xs font-mono text-danger font-bold">!</span>
          )}
      </div>
    </div>
  );
}

function FormulaOverrides({ entityId, onSaved }: { entityId: string; onSaved: () => void }) {
  const { t } = useTranslation('character');
  const toast = useToast();
  const [editName, setEditName] = useState('');
  const [editValue, setEditValue] = useState('');
  const [isAdding, setIsAdding] = useState(false);
  const [overrides, setOverrides] = useState<Record<string, number>>({});

  // Load current overrides from entity metadata
  useEffect(() => {
    apiClient.get(`/entities/${entityId}`).then((res) => {
      try {
        const meta = JSON.parse(res.data.metadataJson ?? '{}');
        setOverrides(meta.formula_overrides ?? {});
      } catch { setOverrides({}); }
    }).catch(() => {});
  }, [entityId]);

  const handleAdd = async () => {
    if (!editName || !editValue) return;
    const newOverrides = { ...overrides, [editName]: Number(editValue) };
    try {
      await apiClient.patch(`/entities/${entityId}/override`, newOverrides);
      setOverrides(newOverrides);
      setEditName('');
      setEditValue('');
      setIsAdding(false);
      onSaved();
    } catch { toast.error(t('sheet.overrideSaveFailed')!); }
  };

  const handleRemove = async (name: string) => {
    const { [name]: _, ...rest } = overrides;
    try {
      await apiClient.patch(`/entities/${entityId}/override`, rest);
      setOverrides(rest);
      onSaved();
    } catch { toast.error(t('sheet.overrideSaveFailed')!); }
  };

  const entries = Object.entries(overrides);
  if (entries.length === 0 && !isAdding) {
    return (
      <div className="rounded-lg border border-bg-elevated bg-bg-surface p-3">
        <div className="flex items-center justify-between">
          <h3 className="text-xs font-semibold text-text-secondary uppercase tracking-wider">
            {t('sheet.overrides')}
          </h3>
          <button onClick={() => setIsAdding(true)} className="text-accent hover:text-accent/60">
            <Plus size={14} />
          </button>
        </div>
        {isAdding && (
          <OverrideInput
            name={editName}
            value={editValue}
            onNameChange={setEditName}
            onValueChange={setEditValue}
            onAdd={handleAdd}
            onCancel={() => setIsAdding(false)}
          />
        )}
      </div>
    );
  }

  return (
    <div className="rounded-lg border border-bg-elevated bg-bg-surface p-3">
      <div className="flex items-center justify-between mb-2">
        <h3 className="text-xs font-semibold text-text-secondary uppercase tracking-wider">
          {t('sheet.overrides')}
        </h3>
        <button onClick={() => setIsAdding(true)} className="text-accent hover:text-accent/60">
          <Plus size={14} />
        </button>
      </div>
      {isAdding && (
        <OverrideInput
          name={editName}
          value={editValue}
          onNameChange={setEditName}
          onValueChange={setEditValue}
          onAdd={handleAdd}
          onCancel={() => setIsAdding(false)}
        />
      )}
      {entries.length > 0 && (
        <div className="space-y-1">
          {entries.map(([name, val]) => (
            <div key={name} className="flex items-center justify-between rounded bg-bg-primary/30 px-2 py-1 text-xs">
              <span className="text-text-primary">{name}: +{val}</span>
              <button onClick={() => handleRemove(name)} className="text-danger hover:text-danger/60">
                <X size={12} />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function OverrideInput({ name, value, onNameChange, onValueChange, onAdd, onCancel }: {
  name: string;
  value: string;
  onNameChange: (v: string) => void;
  onValueChange: (v: string) => void;
  onAdd: () => void;
  onCancel: () => void;
}) {
  const { t } = useTranslation('character');
  return (
    <div className="flex items-center gap-2 mt-2">
      <input value={name} onChange={(e) => onNameChange(e.target.value)}
        placeholder={t('sheet.overridesName')!} className="w-20 rounded border border-bg-elevated bg-bg-primary px-1.5 py-0.5 text-xs text-text-primary outline-none" />
      <input value={value} onChange={(e) => onValueChange(e.target.value)} type="number"
        placeholder={t('sheet.overridesValue')!} className="w-16 rounded border border-bg-elevated bg-bg-primary px-1.5 py-0.5 text-xs text-text-primary outline-none" />
      <button onClick={onAdd} className="text-success hover:text-success/60"><Check size={14} /></button>
      <button onClick={onCancel} className="text-danger hover:text-danger/60"><X size={14} /></button>
    </div>
  );
}
