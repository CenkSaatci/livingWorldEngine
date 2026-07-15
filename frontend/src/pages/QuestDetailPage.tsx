import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Star, Coins, Bot, Target, Gift } from 'lucide-react';
import { useApiGet } from '../hooks/useApiGet';
import { LoadingSpinner } from '../components/ui/LoadingSpinner';

interface QuestObjective {
  type?: string;
  target?: string;
  count?: number;
}

interface QuestReward {
  xp?: number;
  gold?: number;
  item?: string;
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

export default function QuestDetailPage() {
  const { questId } = useParams<{ questId: string }>();
  const navigate = useNavigate();
  const { data: quest, loading } = useApiGet<Quest>(`/quests/${questId}`, [questId]);

  if (loading || !quest) return <LoadingSpinner size="lg" text="Loading…" />;

  let rewards: QuestReward = {};
  let objectives: QuestObjective[] = [];
  try {
    rewards = JSON.parse(quest.rewards) as QuestReward;
  } catch {
    /* */
  }
  try {
    objectives = JSON.parse(quest.objectives) as QuestObjective[];
  } catch {
    /* */
  }

  const statusColors: Record<string, string> = {
    active: 'text-accent',
    completed: 'text-success',
    cancelled: 'text-text-secondary',
  };

  return (
    <div className="min-h-screen bg-bg-primary">
      <header className="flex items-center gap-3 border-b border-bg-elevated bg-bg-surface px-6 py-3">
        <button onClick={() => navigate(-1)} className="text-text-secondary hover:text-accent">
          <ArrowLeft size={20} />
        </button>
        <h1 className="text-lg font-heading text-text-primary">{quest.title}</h1>
        <span className={`text-xs ${statusColors[quest.status] ?? ''}`}>{quest.status}</span>
        {quest.ai_generated && (
          <span className="flex items-center gap-1 rounded bg-accent/15 px-2 py-0.5 text-[10px] text-accent">
            <Bot size={10} /> AI
          </span>
        )}
      </header>

      <main className="mx-auto max-w-2xl space-y-6 p-6">
        {/* Description */}
        {quest.description && (
          <section className="rounded-lg border border-bg-elevated bg-bg-surface p-5">
            <p className="text-sm text-text-primary leading-relaxed">{quest.description}</p>
          </section>
        )}

        {/* Objectives */}
        {objectives.length > 0 && (
          <section className="rounded-lg border border-bg-elevated bg-bg-surface p-5">
            <h2 className="flex items-center gap-1.5 text-sm font-semibold text-text-primary mb-3">
              <Target size={16} /> Objectives
            </h2>
            <ul className="space-y-2">
              {objectives.map((obj, i) => (
                <li key={i} className="flex items-center gap-2 text-sm text-text-secondary">
                  <span className="h-1.5 w-1.5 rounded-full bg-accent shrink-0" />
                  {obj.type && <span className="capitalize text-text-primary">{obj.type}</span>}
                  {obj.target && <span>{obj.target}</span>}
                  {obj.count && <span>×{obj.count}</span>}
                </li>
              ))}
            </ul>
          </section>
        )}

        {/* Rewards */}
        {(rewards.xp || rewards.gold || rewards.item) && (
          <section className="rounded-lg border border-bg-elevated bg-bg-surface p-5">
            <h2 className="flex items-center gap-1.5 text-sm font-semibold text-text-primary mb-3">
              <Gift size={16} /> Rewards
            </h2>
            <div className="flex flex-wrap gap-3">
              {rewards.xp && (
                <span className="flex items-center gap-1 rounded bg-accent/15 px-3 py-1.5 text-xs text-accent">
                  <Star size={14} /> {rewards.xp} XP
                </span>
              )}
              {rewards.gold && (
                <span className="flex items-center gap-1 rounded bg-yellow-500/15 px-3 py-1.5 text-xs text-yellow-500">
                  <Coins size={14} /> {rewards.gold} G
                </span>
              )}
              {rewards.item && (
                <span className="rounded bg-bg-elevated px-3 py-1.5 text-xs text-text-primary">
                  🎁 {rewards.item}
                </span>
              )}
            </div>
          </section>
        )}

        {/* Meta */}
        <section className="rounded-lg border border-bg-elevated bg-bg-surface p-5">
          <dl className="space-y-2 text-xs">
            <div className="flex justify-between">
              <dt className="text-text-secondary">Type</dt>
              <dd className="text-text-primary capitalize">{quest.type}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-text-secondary">Status</dt>
              <dd className={`capitalize ${statusColors[quest.status] ?? ''}`}>{quest.status}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-text-secondary">Created</dt>
              <dd className="text-text-primary">
                {new Date(quest.created_at).toLocaleDateString()}
              </dd>
            </div>
          </dl>
        </section>
      </main>
    </div>
  );
}
