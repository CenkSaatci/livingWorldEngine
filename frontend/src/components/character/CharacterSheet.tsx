import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Heart, Shield, Zap, Sparkles, Check, X } from 'lucide-react';
import { apiClient } from '../../api/client';
import { useSheet } from '../../hooks/useSheet';
import { ProbeRoller } from './ProbeRoller';

const VALUE_ICONS: Record<string, React.ReactNode> = {
  hp: <Heart size={16} className="text-danger" />,
  ac: <Shield size={16} className="text-accent" />,
  mp: <Zap size={16} className="text-blue-400" />,
  sanity: <Sparkles size={16} className="text-purple-400" />,
};

function AttrInput({ name, value, min, max, entityId, onSaved }: { name: string; value: number; min: number; max: number; entityId: string; onSaved: () => void }) {
  const [editing, setEditing] = useState(false);
  const [editVal, setEditVal] = useState(String(value));
  const [saving, setSaving] = useState(false);

  const save = async () => {
    const newVal = parseInt(editVal, 10);
    if (isNaN(newVal) || newVal === value) { setEditing(false); return; }
    if (newVal < min || newVal > max) { setEditVal(String(value)); setEditing(false); return; }
    setSaving(true);
    try {
      await apiClient.patch(`/entities/${entityId}/attributes`, { [name]: newVal });
      onSaved();
      setEditing(false);
    } catch { setEditVal(String(value)); setEditing(false); }
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
      <button onClick={save} className="text-success hover:text-success/60" disabled={saving}><Check size={14} /></button>
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
  worldId: string;
}

function XpInput({ value, entityId, onSaved }: { value: number; entityId: string; onSaved: () => void }) {
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
    } catch { setEditVal(String(value)); setEditing(false); }
    finally { setSaving(false); }
  };

  return editing ? (
    <div className="flex items-center gap-1">
      <input type="number" value={editVal} autoFocus min={0}
        onChange={(e) => setEditVal(e.target.value)}
        onKeyDown={(e) => { if (e.key === 'Enter') save(); if (e.key === 'Escape') { setEditVal(String(value)); setEditing(false); }}}
        className="w-20 rounded border border-accent bg-bg-primary px-1 py-0.5 text-xs text-text-primary text-right outline-none"
        disabled={saving}
      />
      <button onClick={save} className="text-success" disabled={saving}><Check size={12} /></button>
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
              <AttrInput name={attr.name} value={attr.value} min={attr.min} max={attr.max} entityId={entityId} onSaved={refetch} />
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
            <div key={skill.name} className="flex items-center justify-between rounded bg-bg-primary/30 px-2 py-1">
              <span className="text-xs text-text-primary">{skill.name}</span>
              <div className="flex items-center gap-2">
                <span className="text-xs font-mono text-text-secondary">
                  {skill.total > 0 ? '+' : ''}{skill.total}
                </span>
                <ProbeRoller entityId={data.entity.id} skillName={skill.name} skillTotal={skill.total} />
              </div>
            </div>
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

      {/* Abilities placeholder */}
      <div className="rounded-lg border border-bg-elevated bg-bg-surface p-3">
        <h3 className="mb-2 text-xs font-semibold text-text-secondary uppercase tracking-wider">
          {t('sheet.abilities')}
        </h3>
        <p className="text-xs text-text-secondary">{t('sheet.abilitiesComingSoon')}</p>
      </div>
    </div>
  );
}
