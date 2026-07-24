import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Heart, Shield, Zap, Sparkles } from 'lucide-react';
import { useSheet } from '../../hooks/useSheet';
import { ProbeRoller } from './ProbeRoller';

const VALUE_ICONS: Record<string, React.ReactNode> = {
  hp: <Heart size={16} className="text-danger" />,
  ac: <Shield size={16} className="text-accent" />,
  mp: <Zap size={16} className="text-blue-400" />,
  sanity: <Sparkles size={16} className="text-purple-400" />,
};

interface Props {
  entityId: string;
  worldId: string;
}

export function CharacterSheet({ entityId }: Props) {
  const { t } = useTranslation('character');
  const { data, loading, error } = useSheet(entityId);
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

      {/* Attributes */}
      <div className="rounded-lg border border-bg-elevated bg-bg-surface p-3">
        <h3 className="mb-2 text-xs font-semibold text-text-secondary uppercase tracking-wider">
          {t('sheet.attributes')}
        </h3>
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
          {data.attributes.map((attr) => (
            <div key={attr.name} className="rounded bg-bg-primary/50 p-2 text-center">
              <p className="text-[10px] text-text-secondary uppercase">{attr.name}</p>
              <p className="text-lg font-heading text-text-primary">{attr.value}</p>
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
                <ProbeRoller skillName={skill.name} skillTotal={skill.total} />
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
