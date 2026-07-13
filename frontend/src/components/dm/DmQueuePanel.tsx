import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Check, X } from 'lucide-react';
import { apiClient } from '../../api/client';

interface NpcIntent {
  id: string;
  npc_id: string;
  intent_type: string;
  reasoning: string;
  status: string;
  created_at: string;
}

interface Props {
  worldId: string;
}

export function DmQueuePanel({ worldId }: Props) {
  const { t } = useTranslation('dm');
  const [intents, setIntents] = useState<NpcIntent[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchIntents = async () => {
    setLoading(true);
    try {
      const res = await apiClient.get(`/npc-intents`, {
        params: { worldId, status: 'pending' },
      });
      setIntents(res.data ?? []);
    } catch {
      // silent
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchIntents();
    const interval = setInterval(fetchIntents, 5000);
    return () => clearInterval(interval);
  }, [worldId]);

  const handleApprove = async (id: string) => {
    await apiClient.post(`/npc-intents/${id}/approve`);
    setIntents((prev) => prev.filter((i) => i.id !== id));
  };

  const handleReject = async (id: string) => {
    await apiClient.post(`/npc-intents/${id}/reject`, { reason: 'DM rejected' });
    setIntents((prev) => prev.filter((i) => i.id !== id));
  };

  return (
    <div className="space-y-2">
      <h3 className="flex items-center gap-2 text-sm font-semibold text-text-secondary uppercase tracking-wide">
        {t('queue.title')}
        {intents.length > 0 && (
          <span className="rounded-full bg-accent/20 px-2 py-0.5 text-xs text-accent">
            {intents.length}
          </span>
        )}
      </h3>

      {loading && intents.length === 0 && (
        <p className="text-xs text-text-secondary">{t('queue.title')}…</p>
      )}

      {intents.length === 0 && !loading && (
        <p className="text-xs text-text-secondary">{t('queue.empty')}</p>
      )}

      {intents.map((intent) => (
        <div key={intent.id} className="rounded border border-bg-elevated bg-bg-surface p-3">
          <div className="mb-1 flex items-center gap-2">
            <span className="text-xs font-bold text-accent uppercase">{intent.intent_type}</span>
            <span className="text-xs text-text-secondary">{intent.npc_id.slice(0, 8)}…</span>
          </div>
          {intent.reasoning && (
            <p className="mb-2 text-xs text-text-primary">{intent.reasoning}</p>
          )}
          <div className="flex gap-2">
            <button
              onClick={() => handleApprove(intent.id)}
              className="flex items-center gap-1 rounded bg-accent/20 px-2 py-1 text-xs text-accent hover:bg-accent/40"
            >
              <Check size={14} /> {t('queue.approve')}
            </button>
            <button
              onClick={() => handleReject(intent.id)}
              className="flex items-center gap-1 rounded bg-danger/20 px-2 py-1 text-xs text-danger hover:bg-danger/40"
            >
              <X size={14} /> {t('queue.reject')}
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}
