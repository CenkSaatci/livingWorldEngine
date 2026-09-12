import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ScrollText, CheckCircle, XCircle, Clock, Bot, Star, Coins, Plus } from 'lucide-react';
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
  aiGenerated: boolean;
  createdAt: string;
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
  const { t } = useTranslation('common');
  const { data: _quests, refetch } = useApiGet<Quest[]>(`/quests?worldId=${worldId}`, [worldId]);
  const quests = _quests ?? [];
  const [showCreate, setShowCreate] = useState(false);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [qtype, setQtype] = useState('fetch');
  const [saving, setSaving] = useState(false);

  const createQuest = async () => {
    if (!title.trim() || saving) return;
    setSaving(true);
    try {
      await apiClient.post('/quests', {
        worldId, title: title.trim(), description: description.trim() || null,
        type: qtype, aiGenerated: false,
      });
      setTitle('');
      setDescription('');
      setShowCreate(false);
      refetch();
    } finally {
      setSaving(false);
    }
  };

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
        <ScrollText size={14} /> {t('quest.title')} ({quests.length})
        <button
          onClick={() => setShowCreate((v) => !v)}
          className="ml-auto flex items-center gap-1 rounded px-1.5 py-0.5 text-accent hover:bg-accent/10"
          aria-label={t('quest.create')}
        >
          <Plus size={14} />
        </button>
      </h3>

      {showCreate && (
        <div className="space-y-2 rounded border border-bg-elevated bg-bg-surface/50 p-3">
          <input
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder={t('quest.titlePlaceholder')}
            className="w-full rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-sm text-text-primary outline-none focus:border-accent"
          />
          <div className="flex gap-2">
            <select
              value={qtype}
              onChange={(e) => setQtype(e.target.value)}
              className="rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
            >
              {['kill', 'fetch', 'escort', 'deliver', 'explore', 'talk'].map((t) => (
                <option key={t} value={t}>{t}</option>
              ))}
            </select>
            <button
              onClick={createQuest}
              disabled={!title.trim() || saving}
              className="rounded bg-accent px-3 py-1 text-xs text-white hover:bg-accent/80 disabled:opacity-40"
            >
              {t('quest.create')}
            </button>
          </div>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder={t('quest.descriptionPlaceholder')}
            rows={2}
            className="w-full rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
          />
        </div>
      )}

      {/* AI-Generated Pending */}
      {pending
        .filter((q) => q.aiGenerated)
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
                <CheckCircle size={12} /> {t('quest.approve')}
              </button>
              <button
                onClick={() => updateStatus(q.id, 'cancelled')}
                className="flex items-center gap-1 rounded bg-danger/20 px-2 py-1 text-xs text-danger hover:bg-danger/40"
              >
                <XCircle size={12} /> {t('quest.reject')}
              </button>
            </div>
          </div>
        ))}

      {/* Active Quests */}
      {active.length === 0 && pending.length === 0 && (
        <p className="text-xs text-text-secondary">{t('quest.noQuests')}</p>
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
              {q.aiGenerated && <span className="shrink-0 text-[10px] text-accent">AI</span>}
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
