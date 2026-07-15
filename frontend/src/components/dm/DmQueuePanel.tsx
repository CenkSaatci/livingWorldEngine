import { useEffect, useState, useCallback } from 'react';
import { Check, X, Bot } from 'lucide-react';
import { apiClient } from '../../api/client';
import { useToast } from '../../hooks/useToast';

interface NpcIntent {
  id: string;
  world_id: string;
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
  const toast = useToast();
  const [intents, setIntents] = useState<NpcIntent[]>([]);

  const fetchIntents = useCallback(async () => {
    try {
      const res = await apiClient.get('/npc-intents', {
        params: { worldId, status: 'pending' },
      });
      setIntents(res.data ?? []);
    } catch {
      toast.error('Failed to fetch intents');
    }
  }, [worldId, toast]);

  useEffect(() => {
    fetchIntents();
    const interval = setInterval(fetchIntents, 5000);
    return () => clearInterval(interval);
  }, [fetchIntents]);

  const handleAction = async (id: string, action: 'approve' | 'reject') => {
    try {
      await apiClient.post(`/npc-intents/${id}/${action}`);
      setIntents((prev) => prev.filter((i) => i.id !== id));
    } catch {
      toast.error('Failed to approve/reject intent');
    }
  };

  if (intents.length === 0) return null;

  return (
    <div className="rounded-lg border border-accent/20 bg-accent/5 p-3">
      <h3 className="flex items-center gap-1.5 text-xs font-semibold text-accent uppercase tracking-wide mb-2">
        <Bot size={14} /> KI-Intents ({intents.length})
      </h3>

      <div className="space-y-2">
        {intents.map((intent) => (
          <div key={intent.id} className="rounded border border-bg-elevated bg-bg-surface p-2">
            <div className="flex items-start justify-between gap-2">
              <div className="min-w-0">
                <p className="text-xs font-medium text-text-primary truncate">
                  {intent.intent_type}
                </p>
                <p className="text-[10px] text-text-secondary mt-0.5 line-clamp-2">
                  {intent.reasoning || '—'}
                </p>
              </div>
              <div className="flex shrink-0 gap-1">
                <button
                  onClick={() => handleAction(intent.id, 'approve')}
                  className="rounded bg-success/20 p-1 text-success hover:bg-success/30"
                  title="Approve"
                >
                  <Check size={14} />
                </button>
                <button
                  onClick={() => handleAction(intent.id, 'reject')}
                  className="rounded bg-danger/20 p-1 text-danger hover:bg-danger/30"
                  title="Reject"
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
