import { useTranslation } from 'react-i18next';
import { useWorldStore } from '../../store/worldStore';

export function RollLog() {
  const { t } = useTranslation('chat');
  const worldEvents = useWorldStore((s) => s.worldEvents);

  const rolls = worldEvents
    .filter((e) => e.event_type === 'PROBE_ROLLED')
    .slice(-20)
    .reverse();

  return (
    <div className="h-full space-y-1 overflow-y-auto p-2">
      <h3 className="mb-2 text-xs font-semibold text-text-secondary uppercase tracking-wide">
        {t('roll_log')}
      </h3>
      {rolls.length === 0 && (
        <p className="text-xs text-text-secondary">{t('roll_command_hint')}</p>
      )}
      {rolls.map((r) => (
        <div key={r.id} className="rounded bg-bg-primary/50 px-2 py-1">
          <p className="text-xs font-mono text-text-primary">{JSON.stringify(r.payload)}</p>
        </div>
      ))}
    </div>
  );
}
