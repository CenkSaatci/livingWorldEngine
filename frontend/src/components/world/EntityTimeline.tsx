import { useTranslation } from 'react-i18next';
import { Clock, AlertTriangle, CheckCircle, Info } from 'lucide-react';
import { useApiGet } from '../../hooks/useApiGet';

interface EntityEvent {
  id: number;
  entityType: string;
  eventType: string;
  title: string;
  description: string;
  importance: number;
  createdAt: string;
}

interface Props {
  entityType: string;
  entityId: string;
}

function eventIcon(eventType: string | undefined) {
  const t = eventType ?? '';
  if (t.includes('RAID') || t.includes('ATTACK') || t.includes('DISASTER'))
    return <AlertTriangle size={14} className="text-danger" />;
  if (t.includes('COMPLETED') || t.includes('CREATED') || t.includes('VISIT'))
    return <CheckCircle size={14} className="text-success" />;
  return <Info size={14} className="text-accent" />;
}

function eventBg(eventType: string | undefined) {
  const t = eventType ?? '';
  if (t.includes('RAID') || t.includes('ATTACK') || t.includes('DISASTER'))
    return 'bg-danger/10 border-danger/20';
  if (t.includes('COMPLETED') || t.includes('CREATED') || t.includes('VISIT'))
    return 'bg-success/10 border-success/20';
  return 'bg-accent/5 border-bg-elevated';
}

function useFormatTime() {
  const { t } = useTranslation('common');
  return (iso: string) => {
    const diff = Date.now() - new Date(iso).getTime();
    const days = Math.floor(diff / 86400000);
    if (days === 0) return t('entityTimeline.today');
    if (days === 1) return t('entityTimeline.yesterday');
    return t('entityTimeline.daysAgo', { count: days });
  };
}

export function EntityTimeline({ entityType, entityId }: Props) {
  const { t } = useTranslation('common');
  const formatTime = useFormatTime();
  const { data: _events } = useApiGet<EntityEvent[]>(
    `/entity-events?entityType=${entityType}&entityId=${entityId}&limit=10`,
    [entityType, entityId],
  );
  const events = _events ?? [];

  return (
    <div className="space-y-2">
      <h3 className="flex items-center gap-1.5 text-xs font-semibold text-text-secondary uppercase tracking-wide">
        <Clock size={14} /> {t('entityTimeline.events')}
      </h3>

      {events.length === 0 && (
        <p className="text-xs text-text-secondary">{t('entityTimeline.noEvents')}</p>
      )}

      {events.map((ev) => (
        <div key={ev.id} className={`rounded border px-3 py-2 ${eventBg(ev.eventType)}`}>
          <div className="flex items-start gap-2">
            {eventIcon(ev.eventType)}
            <div className="flex-1 min-w-0">
              <div className="flex items-center justify-between gap-2">
                <p className="text-xs font-medium text-text-primary truncate">{ev.title}</p>
                <span className="shrink-0 text-[10px] text-text-secondary">
                  {formatTime(ev.createdAt)}
                </span>
              </div>
              {ev.description && (
                <p className="mt-0.5 text-[11px] text-text-secondary line-clamp-2">
                  {ev.description}
                </p>
              )}
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
