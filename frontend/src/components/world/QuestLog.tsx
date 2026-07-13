import { useEffect, useState } from 'react';
import { ScrollText, CheckCircle, XCircle, Clock, Bot } from 'lucide-react';
import { apiClient } from '../../api/client';

interface Quest {
  id: string;
  title: string;
  description: string;
  type: string;
  status: string;
  objectives: string;
  rewards: string;
  ai_generated: boolean;
  created_at: string;
}

interface Props {
  worldId: string;
  onSelectQuest?: (id: string) => void;
}

export function QuestLog({ worldId, onSelectQuest }: Props) {
  const [quests, setQuests] = useState<Quest[]>([]);

  useEffect(() => {
    if (!worldId) return;
    apiClient.get('/quests', { params: { worldId } })
      .then((r) => setQuests(r.data)).catch(() => {});
  }, [worldId]);

  const active = quests.filter((q) => q.status === 'active');
  const pending = quests.filter((q) => q.status === 'pending');
  const completed = quests.filter((q) => q.status === 'completed');

  return (
    <div className="space-y-2">
      <h3 className="flex items-center gap-1.5 text-xs font-semibold text-text-secondary uppercase tracking-wide">
        <ScrollText size={14} /> Quests ({quests.length})
      </h3>

      {/* AI-Generated Pending */}
      {pending.filter(q => q.ai_generated).map((q) => (
        <div key={q.id} className="rounded border border-accent/30 bg-accent/5 p-3">
          <div className="flex items-start gap-2">
            <Bot size={16} className="text-accent shrink-0 mt-0.5" />
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium text-text-primary">{q.title}</p>
              <p className="text-xs text-text-secondary mt-0.5 line-clamp-2">{q.description}</p>
            </div>
          </div>
          <div className="mt-2 flex gap-2">
            <button
              onClick={() => apiClient.patch(`/quests/${q.id}/status`, { status: 'active' }).then(() => window.location.reload())}
              className="flex items-center gap-1 rounded bg-accent/20 px-2 py-1 text-xs text-accent hover:bg-accent/40"
            >
              <CheckCircle size={12} /> Approve
            </button>
            <button
              onClick={() => apiClient.patch(`/quests/${q.id}/status`, { status: 'cancelled' }).then(() => window.location.reload())}
              className="flex items-center gap-1 rounded bg-danger/20 px-2 py-1 text-xs text-danger hover:bg-danger/40"
            >
              <XCircle size={12} /> Reject
            </button>
          </div>
        </div>
      ))}

      {/* Active Quests */}
      {active.length === 0 && pending.length === 0 && (
        <p className="text-xs text-text-secondary">No quests</p>
      )}
      {active.map((q) => (
        <button
          key={q.id}
          onClick={() => onSelectQuest?.(q.id)}
          className="w-full rounded border border-bg-elevated bg-bg-surface/50 px-3 py-2 text-left hover:bg-bg-surface/80"
        >
          <div className="flex items-center gap-2">
            <Clock size={14} className="text-accent shrink-0" />
            <span className="text-sm text-text-primary truncate">{q.title}</span>
            {q.ai_generated && <span className="shrink-0 text-[10px] text-accent">AI</span>}
          </div>
          {q.description && (
            <p className="mt-0.5 text-xs text-text-secondary line-clamp-1">{q.description}</p>
          )}
        </button>
      ))}

      {/* Completed (collapsed) */}
      {completed.length > 0 && (
        <details className="text-xs text-text-secondary">
          <summary className="cursor-pointer hover:text-text-primary">
            Completed ({completed.length})
          </summary>
          <div className="mt-1 space-y-1 pl-1">
            {completed.map((q) => (
              <div key={q.id} className="flex items-center gap-2">
                <CheckCircle size={12} className="text-success shrink-0" />
                <span className="truncate">{q.title}</span>
              </div>
            ))}
          </div>
        </details>
      )}
    </div>
  );
}
