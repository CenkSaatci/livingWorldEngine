import { useEffect, useState } from 'react';
import { Clock, AlertTriangle, CheckCircle, Info } from 'lucide-react';
import { apiClient } from '../../api/client';

interface EntityEvent {
  id: number;
  entity_type: string;
  event_type: string;
  title: string;
  description: string;
  importance: number;
  created_at: string;
}

interface Props {
  entityType: string;
  entityId: string;
}

function eventIcon(eventType: string) {
  if (eventType.includes('RAID') || eventType.includes('ATTACK') || eventType.includes('DISASTER'))
    return <AlertTriangle size={14} className="text-danger" />;
  if (eventType.includes('COMPLETED') || eventType.includes('CREATED') || eventType.includes('VISIT'))
    return <CheckCircle size={14} className="text-success" />;
  return <Info size={14} className="text-accent" />;
}

function eventBg(eventType: string) {
  if (eventType.includes('RAID') || eventType.includes('ATTACK') || eventType.includes('DISASTER'))
    return 'bg-danger/10 border-danger/20';
  if (eventType.includes('COMPLETED') || eventType.includes('CREATED') || eventType.includes('VISIT'))
    return 'bg-success/10 border-success/20';
  return 'bg-accent/5 border-bg-elevated';
}

function formatTime(iso: string) {
  const diff = Date.now() - new Date(iso).getTime();
  const days = Math.floor(diff / 86400000);
  if (days === 0) return 'heute';
  if (days === 1) return 'gestern';
  return `vor ${days} Tagen`;
}

export function EntityTimeline({ entityType, entityId }: Props) {
  const [events, setEvents] = useState<EntityEvent[]>([]);

  useEffect(() => {
    if (!entityId) return;
    apiClient.get('/entity-events', {
      params: { entityType, entityId, limit: 10 },
    }).then((r) => setEvents(r.data)).catch(() => {});
  }, [entityType, entityId]);

  return (
    <div className="space-y-2">
      <h3 className="flex items-center gap-1.5 text-xs font-semibold text-text-secondary uppercase tracking-wide">
        <Clock size={14} /> Events
      </h3>

      {events.length === 0 && (
        <p className="text-xs text-text-secondary">No events yet</p>
      )}

      {events.map((ev) => (
        <div
          key={ev.id}
          className={`rounded border px-3 py-2 ${eventBg(ev.event_type)}`}
        >
          <div className="flex items-start gap-2">
            {eventIcon(ev.event_type)}
            <div className="flex-1 min-w-0">
              <div className="flex items-center justify-between gap-2">
                <p className="text-xs font-medium text-text-primary truncate">{ev.title}</p>
                <span className="shrink-0 text-[10px] text-text-secondary">{formatTime(ev.created_at)}</span>
              </div>
              {ev.description && (
                <p className="mt-0.5 text-[11px] text-text-secondary line-clamp-2">{ev.description}</p>
              )}
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
