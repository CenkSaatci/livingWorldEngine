import { ScrollText, CheckCircle, XCircle, Clock, Bot, Star, Coins } from 'lucide-react';
import { apiClient } from '../../api/client';
import { useApiGet } from '../../hooks/useApiGet';

interface QuestRewards {
  xp?: number;
  gold?: number;
}

interface QuestObjective {
  type?: string;
  target?: string;
  count?: number;
}

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

function parseRewards(raw: string): QuestRewards | null {
  try {
    return JSON.parse(raw) as QuestRewards;
  } catch {
    return null;
  }
}

function parseObjectives(raw: string): QuestObjective[] {
  try {
    return JSON.parse(raw) as QuestObjective[];
  } catch {
    return [];
  }
}

export function QuestLog({ worldId, onSelectQuest }: Props) {
  const { data: _quests, refetch } = useApiGet<Quest[]>(`/quests?worldId=${worldId}`, [worldId]);
  const quests = _quests ?? [];

  const updateStatus = async (id: string, status: string) => {
    try {
      await apiClient.patch(`/quests/${id}/status`, { status });
      refetch();
    } catch {
      /* */
    }
  };

  const active = quests.filter((q) => q.status === 'active');
  const pending = quests.filter((q) => q.status === 'pending');
  const completed = quests.filter((q) => q.status === 'completed');

  return (
    <div className="space-y-2">
      <h3 className="flex items-center gap-1.5 text-xs font-semibold text-text-secondary uppercase tracking-wide">
        <ScrollText size={14} /> Quests ({quests.length})
      </h3>

      {/* AI-Generated Pending */}
      {pending
        .filter((q) => q.ai_generated)
        .map((q) => (
          <div key={q.id} className="rounded border border-accent/30 bg-accent/5 p-3">
            <div className="flex items-start gap-2">
              <Bot size={16} className="text-accent shrink-0 mt-0.5" />
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-text-primary">{q.title}</p>
                <p className="text-xs text-text-secondary mt-0.5 line-clamp-2">{q.description}</p>
                {renderRewards(q.rewards)}
              </div>
            </div>
            <div className="mt-2 flex gap-2">
              <button
                onClick={() => updateStatus(q.id, 'active')}
                className="flex items-center gap-1 rounded bg-accent/20 px-2 py-1 text-xs text-accent hover:bg-accent/40"
              >
                <CheckCircle size={12} /> Approve
              </button>
              <button
                onClick={() => updateStatus(q.id, 'cancelled')}
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
      {active.map((q) => {
        const objs = parseObjectives(q.objectives);
        const locTag = objs.find((o) => o.target)?.target;
        return (
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
            <div className="mt-1 flex items-center gap-2">
              {renderRewards(q.rewards)}
              {locTag && <span className="text-[10px] text-text-secondary">📍 {locTag}</span>}
            </div>
          </button>
        );
      })}

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
                {renderRewards(q.rewards)}
              </div>
            ))}
          </div>
        </details>
      )}
    </div>
  );
}

function renderRewards(raw: string) {
  const r = parseRewards(raw);
  if (!r) return null;
  return (
    <div className="flex items-center gap-1.5 mt-1">
      {r.xp && (
        <span className="flex items-center gap-0.5 text-[10px] text-accent">
          <Star size={10} /> {r.xp} XP
        </span>
      )}
      {r.gold && (
        <span className="flex items-center gap-0.5 text-[10px] text-yellow-500">
          <Coins size={10} /> {r.gold} G
        </span>
      )}
    </div>
  );
}
