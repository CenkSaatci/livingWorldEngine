import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useApiGet } from '../../hooks/useApiGet';

export interface ActorOption {
  id: string;
  name: string;
  entityType: string;
}

/** Charakter-Auswahl für Aktionen/Handel (ADR-015): eigene PCs der Welt. */
export function ActorSelect({
  worldId,
  value,
  onChange,
}: {
  worldId: string;
  value: string;
  onChange: (id: string) => void;
}) {
  const { t } = useTranslation('common');
  const { data } = useApiGet<ActorOption[]>(worldId ? `/worlds/${worldId}/entities` : '', [worldId]);
  const actors = (data ?? []).filter((e) => e.entityType === 'PC');

  useEffect(() => {
    if (!value && actors.length > 0) onChange(actors[0].id);
  }, [actors, value, onChange]);

  if (actors.length === 0) return null;

  return (
    <label className="flex items-center gap-2 text-xs text-text-secondary">
      {t('poi.actor')}
      <select
        value={value}
        onChange={(e) => onChange(e.target.value)}
        aria-label={t('poi.actor')}
        className="rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
      >
        {actors.map((a) => (
          <option key={a.id} value={a.id}>
            {a.name}
          </option>
        ))}
      </select>
    </label>
  );
}
