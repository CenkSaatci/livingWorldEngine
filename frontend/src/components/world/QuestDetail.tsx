import { useEffect, useState } from 'react';
import { Calendar, Target, Gift, Bot, ArrowLeft } from 'lucide-react';
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
  questId: string;
  onBack?: () => void;
}

export function QuestDetail({ questId, onBack }: Props) {
  const [quest, setQuest] = useState<Quest | null>(null);

  useEffect(() => {
    apiClient.get(`/quests/${questId}`).then((r) => setQuest(r.data)).catch(() => {});
  }, [questId]);

  if (!quest) return <p className="text-sm text-text-secondary p-4">Loading…</p>;

  let rewards: Record<string, any> = {};
  let objectives: any[] = [];
  try { rewards = JSON.parse(quest.rewards); } catch { /* */ }
  try { objectives = JSON.parse(quest.objectives); } catch { /* */ }

  const statusColor = {
    active: 'text-accent',
    completed: 'text-success',
    failed: 'text-danger',
    cancelled: 'text-text-secondary',
  }[quest.status] ?? 'text-text-secondary';

  return (
    <div className="max-w-2xl space-y-4">
      {/* Back + Header */}
      <div className="flex items-center gap-3">
        {onBack && (
          <button onClick={onBack} className="text-text-secondary hover:text-accent">
            <ArrowLeft size={18} />
          </button>
        )}
        <h2 className="text-lg font-heading text-text-primary">{quest.title}</h2>
        {quest.ai_generated && <Bot size={16} className="text-accent" />}
      </div>

      {/* Meta */}
      <div className="flex gap-3 text-xs">
        <span className={`rounded bg-bg-elevated px-2 py-0.5 capitalize ${statusColor}`}>{quest.status}</span>
        <span className="rounded bg-bg-elevated px-2 py-0.5 text-text-secondary capitalize">{quest.type}</span>
        <span className="flex items-center gap-1 text-text-secondary">
          <Calendar size={12} /> {new Date(quest.created_at).toLocaleDateString()}
        </span>
      </div>

      {/* Description */}
      {quest.description && (
        <p className="text-sm leading-relaxed text-text-primary">{quest.description}</p>
      )}

      {/* Objectives */}
      {objectives.length > 0 && (
        <div>
          <h3 className="flex items-center gap-1.5 text-xs font-semibold text-text-secondary uppercase tracking-wide mb-2">
            <Target size={14} /> Objectives
          </h3>
          <ul className="space-y-1">
            {objectives.map((obj, i) => (
              <li key={i} className="flex items-center gap-2 text-sm text-text-primary">
                <span className="h-1.5 w-1.5 rounded-full bg-accent shrink-0" />
                {obj.type}: {obj.target} {obj.count ? `×${obj.count}` : ''}
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Rewards */}
      {Object.keys(rewards).length > 0 && (
        <div>
          <h3 className="flex items-center gap-1.5 text-xs font-semibold text-text-secondary uppercase tracking-wide mb-2">
            <Gift size={14} /> Rewards
          </h3>
          <div className="flex flex-wrap gap-2">
            {Object.entries(rewards).map(([key, val]) => (
              <span key={key} className="rounded bg-success/15 px-2 py-1 text-xs text-success">
                +{val} {key}
              </span>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
