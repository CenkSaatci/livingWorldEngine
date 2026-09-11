import { useEffect, useState, useCallback } from 'react';
import { Check, X, Bot } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { apiClient } from '../../api/client';

interface NpcIntent {
  id: string;
  worldId: string;
  npcId: string;
  intentType: string;
  reasoning: string;
  status: string;
  createdAt: string;
}

interface Props {
  worldId: string;
}

export function DmQueuePanel({ worldId }: Props) {
  const { t } = useTranslation('dm');
  const [intents, setIntents] = useState<NpcIntent[]>([]);
  const [forbidden, setForbidden] = useState(false);
  const [typeFilter, setTypeFilter] = useState('');
  const [selected, setSelected] = useState<Set<string>>(new Set());

  const fetchIntents = useCallback(async () => {
    if (forbidden) return;
    try {
      const res = await apiClient.get('/npc-intents', {
        params: { worldId, status: 'pending', type: typeFilter || undefined },
      });
      setIntents(res.data ?? []);
      setSelected(new Set());
    } catch (e) {
      // P27-T06: Queue ist DM-only — 403 blendet das Panel aus (kein Dauer-Polling).
      if ((e as { response?: { status?: number } })?.response?.status === 403) {
        setForbidden(true);
        setIntents([]);
      }
    }
  }, [worldId, forbidden, typeFilter]);

  useEffect(() => {
    fetchIntents();
    const interval = setInterval(fetchIntents, 5000);
    // T33-08: WS-Event (useWorldSocket) loest sofortigen Refetch aus.
    const onChanged = () => fetchIntents();
    window.addEventListener('lwe:npc-intents-changed', onChanged);
    return () => {
      clearInterval(interval);
      window.removeEventListener('lwe:npc-intents-changed', onChanged);
    };
  }, [fetchIntents]);

  const handleAction = async (id: string, action: 'approve' | 'reject') => {
    try {
      await apiClient.post(`/npc-intents/${id}/${action}`, action === 'reject' ? { reason: '' } : undefined);
      setIntents((prev) => prev.filter((i) => i.id !== id));
    } catch {
      /* ignore */
    }
  };

  const handleBulk = async (action: 'approve' | 'reject') => {
    if (selected.size === 0) return;
    try {
      await apiClient.post('/npc-intents/bulk', {
        ids: [...selected],
        action,
        reason: action === 'reject' ? '' : undefined,
      });
      setSelected(new Set());
      fetchIntents();
    } catch {
      /* ignore */
    }
  };

  if (forbidden || intents.length === 0) return null;

  return (
    <div className="rounded-lg border border-accent/20 bg-accent/5 p-3">
      <h3 className="flex items-center gap-1.5 text-xs font-semibold text-accent uppercase tracking-wide mb-2">
        <Bot size={14} /> {t('queue.title')} ({intents.length})
      </h3>

      <div className="mb-2 flex flex-wrap items-center gap-2 text-xs">
        <select
          value={typeFilter}
          onChange={(e) => setTypeFilter(e.target.value)}
          aria-label="Filter by type"
          className="rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-text-primary"
        >
          <option value="">All types</option>
          {[...new Set(intents.map((i) => i.intentType))].map((tp) => (
            <option key={tp} value={tp}>{tp}</option>
          ))}
        </select>
        <button
          onClick={() => handleBulk('approve')}
          disabled={selected.size === 0}
          className="rounded bg-success/20 px-2 py-1 text-success disabled:opacity-40"
        >
          Approve selected ({selected.size})
        </button>
        <button
          onClick={() => handleBulk('reject')}
          disabled={selected.size === 0}
          className="rounded bg-danger/20 px-2 py-1 text-danger disabled:opacity-40"
        >
          Reject selected
        </button>
      </div>

      <div className="space-y-2">
        {intents.map((intent) => (
          <div key={intent.id} className="rounded border border-bg-elevated bg-bg-surface p-2">
            <label className="mr-2 inline-flex items-center">
              <input
                type="checkbox"
                checked={selected.has(intent.id)}
                onChange={() => {
                  setSelected((prev) => {
                    const next = new Set(prev);
                    if (next.has(intent.id)) next.delete(intent.id);
                    else next.add(intent.id);
                    return next;
                  });
                }}
                aria-label={`Select ${intent.id}`}
              />
            </label>
            <div className="flex items-start justify-between gap-2">
              <div className="min-w-0">
                <p className="text-xs font-medium text-text-primary truncate">
                  {intent.intentType}
                </p>
                <p className="text-[10px] text-text-secondary mt-0.5 line-clamp-2">
                  {intent.reasoning || '—'}
                </p>
              </div>
              <div className="flex shrink-0 gap-1">
                <button
                  onClick={() => handleAction(intent.id, 'approve')}
                  className="rounded bg-success/20 p-1 text-success hover:bg-success/30"
                  title={t('queue.approve')}
                >
                  <Check size={14} />
                </button>
                <button
                  onClick={() => handleAction(intent.id, 'reject')}
                  className="rounded bg-danger/20 p-1 text-danger hover:bg-danger/30"
                  title={t('queue.reject')}
                >
                  <X size={14} />
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
