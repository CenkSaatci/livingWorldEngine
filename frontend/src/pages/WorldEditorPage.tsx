import { useState, useCallback, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Copy, Users, Trash2, Save, RotateCcw } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useApiGet } from '../hooks/useApiGet';
import { apiClient } from '../api/client';
import { useToast } from '../hooks/useToast';
import { LoadingSpinner } from '../components/ui/LoadingSpinner';
import type { WorldSummary } from '../store/worldStore';

interface WorldDetail extends WorldSummary {
  ownerId: string;
  settingsJson: string;
  gameSystemId: string | null;
}

interface GameSystem {
  id: string;
  name: string;
  version: number;
}

interface Member {
  id: string;
  userId: string;
  role: string;
  joinedAt: string;
}

interface WorldSettings {
  ai_mode?: string;
  combat_chat_log?: boolean;
  event_archive_days?: number;
  time?: {
    mode?: string;
    tick_interval_real_seconds?: number;
    tick_advance_game_minutes?: number;
    day_starts_at_hour?: number;
  };
}

const AI_MODES = [
  { value: 'off', label: 'Off', desc: 'No AI bot activity' },
  { value: 'suggest', label: 'Suggest', desc: 'AI proposes, DM approves' },
  { value: 'autonom', label: 'Autonomous', desc: 'AI acts freely (rule-checked)' },
];

const TIME_MODES = [
  { value: 'manual', label: 'Manual', desc: 'DM advances time only' },
  { value: 'hybrid', label: 'Hybrid', desc: 'Auto-tick + DM override' },
  { value: 'automatic', label: 'Automatic', desc: 'Time runs on its own' },
];

export default function WorldEditorPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const toast = useToast();
  const { t } = useTranslation('common');
  const [newMemberId, setNewMemberId] = useState('');
  const [searchResults, setSearchResults] = useState<
    { id: string; username: string; email: string }[]
  >([]);
  const [searchTimer, setSearchTimer] = useState<ReturnType<typeof setTimeout> | null>(null);
  const [copied, setCopied] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [inviteLink, setInviteLink] = useState('');
  const [generating, setGenerating] = useState(false);

  const handleSearchInput = (value: string) => {
    setNewMemberId(value);
    if (searchTimer) clearTimeout(searchTimer);
    if (value.length < 3) {
      setSearchResults([]);
      return;
    }
    const timer = setTimeout(async () => {
      try {
        const res = await apiClient.get(`/users/search?q=${encodeURIComponent(value)}`);
        setSearchResults(res.data);
      } catch {
        setSearchResults([]);
      }
    }, 300);
    setSearchTimer(timer);
  };

  const handleSelectUser = (user: { id: string; username: string }) => {
    setNewMemberId(user.id);
    setSearchResults([]);
  };

  const { data: world, loading, refetch } = useApiGet<WorldDetail>(`/worlds/${id}`, [id]);
  const { data: members, refetch: refetchMembers } = useApiGet<Member[]>(`/worlds/${id}/members`, [
    id,
  ]);
  const { data: gameSystems } = useApiGet<GameSystem[]>('/game-systems', []);

  // Editable state
  const [name, setName] = useState('');
  const [gameSystemId, setGameSystemId] = useState<string>('');
  const [description, setDescription] = useState('');
  const [settings, setSettings] = useState<WorldSettings>({});
  const [dirty, setDirty] = useState(false);

  // Sync API data → local state when loaded
  const initFromWorld = useCallback((w: WorldDetail) => {
    setName(w.name);
    setGameSystemId(w.gameSystemId ?? '');
    try {
      const parsed = JSON.parse(w.settingsJson) as Record<string, unknown>;
      setDescription((parsed.description as string) ?? '');
      delete parsed.description;
      setSettings(parsed as WorldSettings);
    } catch {
      setSettings({});
    }
  }, []);

  useEffect(() => {
    if (world) initFromWorld(world);
  }, [world]);

  const updateSetting = (path: string[], value: unknown) => {
    setSettings((prev) => {
      const next = { ...prev };
      let obj: Record<string, unknown> = next;
      for (let i = 0; i < path.length - 1; i++) {
        const segment = path[i];
        if (!obj[segment] || typeof obj[segment] !== 'object') {
          obj[segment] = {};
        }
        obj = obj[segment] as Record<string, unknown>;
      }
      obj[path[path.length - 1]] = value;
      return next;
    });
    setDirty(true);
  };

  const handleSave = async () => {
    if (!id) return;
    try {
      const mergedSettings = { ...settings, description };
      await apiClient.patch(`/worlds/${id}`, {
        name,
        gameSystemId: gameSystemId || null,
        settingsJson: JSON.stringify(mergedSettings),
      });
      toast.success(t('worldEditor.saved'));
      setDirty(false);
      refetch();
    } catch {
      toast.error(t('worldEditor.saveFailed'));
    }
  };

  const handleDelete = async () => {
    if (!id) return;
    try {
      await apiClient.delete(`/worlds/${id}`);
      toast.success(t('worldEditor.deleted'));
      navigate('/dashboard');
    } catch {
      toast.error(t('worldEditor.deleteFailed'));
    }
  };

  const handleClone = async () => {
    if (!id) return;
    try {
      const res = await apiClient.post(`/worlds/${id}/clone`);
      toast.success(`Cloned as "${res.data.name}"`);
      navigate(`/worlds/${res.data.id}/edit`);
    } catch {
      toast.error(t('worldEditor.cloneFailed'));
    }
  };

  const handleGenerateInvite = async () => {
    if (!id) return;
    setGenerating(true);
    try {
      const res = await apiClient.post(`/worlds/${id}/invites`, { maxUses: 1 });
      setInviteLink(res.data.url);
    } catch {
      toast.error(t('worldEditor.inviteFailed'));
    } finally {
      setGenerating(false);
    }
  };

  const handleAddMember = async () => {
    if (!id || !newMemberId.trim()) return;
    try {
      await apiClient.post(`/worlds/${id}/members`, {
        userId: newMemberId.trim(),
        role: 'PLAYER',
      });
      setNewMemberId('');
      refetchMembers();
      toast.success(t('worldEditor.memberAdded'));
    } catch {
      toast.error(t('worldEditor.memberAddFailed'));
    }
  };

  const handleRemoveMember = async (memberId: string) => {
    if (!id) return;
    try {
      await apiClient.delete(`/worlds/${id}/members/${memberId}`);
      refetchMembers();
      toast.success(t('worldEditor.memberRemoved'));
    } catch {
      toast.error(t('worldEditor.memberRemoveFailed'));
    }
  };

  if (loading || !world)
    return (
      <div className="flex min-h-screen items-center justify-center bg-bg-primary">
        <LoadingSpinner />
      </div>
    );

  return (
    <div className="min-h-screen bg-bg-primary">
      <header className="flex items-center justify-between border-b border-bg-elevated bg-bg-surface px-6 py-3">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate('/dashboard')}
            className="text-text-secondary hover:text-accent"
          >
            <ArrowLeft size={20} />
          </button>
          <h1 className="text-lg font-heading text-text-primary">{t('worldEditor.title')}</h1>
        </div>
        <div className="flex items-center gap-2">
          {dirty && (
            <button
              onClick={() => refetch()}
              className="flex items-center gap-1 rounded border border-bg-elevated px-3 py-1.5 text-xs text-text-secondary hover:text-text-primary"
            >
              <RotateCcw size={14} /> Reset
            </button>
          )}
          <button
            onClick={handleSave}
            className={`flex items-center gap-1 rounded px-3 py-1.5 text-xs ${
              dirty
                ? 'bg-accent text-white hover:bg-accent/80'
                : 'border border-bg-elevated text-text-secondary hover:text-text-primary'
            }`}
          >
            <Save size={14} /> {dirty ? 'Save *' : 'Save'}
          </button>
        </div>
      </header>

      <main className="mx-auto max-w-3xl space-y-6 p-6">
        {/* General */}
        <section className="rounded-lg border border-bg-elevated bg-bg-surface p-5">
          <h2 className="mb-4 font-heading text-text-primary">{t('worldEditor.general')}</h2>
          <div className="space-y-4">
            <div>
              <label className="block text-xs text-text-secondary mb-1">World Name</label>
              <input
                value={name}
                onChange={(e) => {
                  setName(e.target.value);
                  setDirty(true);
                }}
                className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
              />
            </div>
            <div>
              <label className="block text-xs text-text-secondary mb-1">Game System</label>
              <select
                value={gameSystemId}
                onChange={(e) => {
                  setGameSystemId(e.target.value);
                  setDirty(true);
                }}
                className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
              >
                <option value="">— None —</option>
                {(gameSystems ?? []).map((gs) => (
                  <option key={gs.id} value={gs.id}>
                    {gs.name} v{gs.version}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs text-text-secondary mb-1">Description</label>
              <textarea
                value={description}
                onChange={(e) => {
                  setDescription(e.target.value);
                  setDirty(true);
                }}
                rows={3}
                className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent resize-none"
              />
            </div>
          </div>
        </section>

        {/* AI Mode */}
        <section className="rounded-lg border border-bg-elevated bg-bg-surface p-5">
          <h2 className="mb-4 font-heading text-text-primary">{t('worldEditor.aiMode')}</h2>
          <div className="grid gap-3 sm:grid-cols-3">
            {AI_MODES.map((mode) => (
              <button
                key={mode.value}
                onClick={() => updateSetting(['ai_mode'], mode.value)}
                className={`rounded border p-3 text-left transition ${
                  (settings.ai_mode ?? 'suggest') === mode.value
                    ? 'border-accent bg-accent/10'
                    : 'border-bg-elevated hover:border-accent/50'
                }`}
              >
                <p className="text-sm font-medium text-text-primary">{mode.label}</p>
                <p className="mt-1 text-xs text-text-secondary">{mode.desc}</p>
              </button>
            ))}
          </div>
        </section>

        {/* Time Settings */}
        <section className="rounded-lg border border-bg-elevated bg-bg-surface p-5">
          <h2 className="mb-4 font-heading text-text-primary">{t('worldEditor.time')}</h2>
          <div className="space-y-4">
            <div>
              <label className="block text-xs text-text-secondary mb-1">Mode</label>
              <div className="flex gap-2">
                {TIME_MODES.map((mode) => (
                  <button
                    key={mode.value}
                    onClick={() => updateSetting(['time', 'mode'], mode.value)}
                    className={`rounded px-4 py-2 text-sm ${
                      (settings.time?.mode ?? 'hybrid') === mode.value
                        ? 'bg-accent text-white'
                        : 'bg-bg-elevated text-text-secondary hover:text-text-primary'
                    }`}
                  >
                    {mode.label}
                  </button>
                ))}
              </div>
            </div>
            <div className="grid grid-cols-3 gap-4">
              <div>
                <label className="block text-xs text-text-secondary mb-1">Tick Interval (s)</label>
                <input
                  type="number"
                  min={1}
                  value={settings.time?.tick_interval_real_seconds ?? 60}
                  onChange={(e) =>
                    updateSetting(['time', 'tick_interval_real_seconds'], Number(e.target.value))
                  }
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                />
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">Advance (min)</label>
                <input
                  type="number"
                  min={1}
                  value={settings.time?.tick_advance_game_minutes ?? 60}
                  onChange={(e) =>
                    updateSetting(['time', 'tick_advance_game_minutes'], Number(e.target.value))
                  }
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                />
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">Day Starts At (h)</label>
                <input
                  type="number"
                  min={0}
                  max={23}
                  value={settings.time?.day_starts_at_hour ?? 6}
                  onChange={(e) =>
                    updateSetting(['time', 'day_starts_at_hour'], Number(e.target.value))
                  }
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                />
              </div>
            </div>
          </div>
        </section>

        {/* Event Archiving */}
        <section className="rounded-lg border border-bg-elevated bg-bg-surface p-5">
          <h2 className="mb-3 font-heading text-text-primary">{t('worldEditor.combatChatLog')}</h2>
          <p className="mb-3 text-xs text-text-secondary">
            When enabled, all combat actions are posted as chat messages so all players can see what
            happens.
          </p>
          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="checkbox"
              checked={settings.combat_chat_log ?? true}
              onChange={(e) => updateSetting(['combat_chat_log'], e.target.checked)}
              className="accent-accent h-4 w-4"
            />
            <span className="text-sm text-text-primary">{t('worldEditor.combatChatLog')}</span>
          </label>
        </section>

        {/* Event Archiving */}
        <section className="rounded-lg border border-bg-elevated bg-bg-surface p-5">
          <h2 className="mb-3 font-heading text-text-primary">{t('worldEditor.eventArchiving')}</h2>
          <p className="mb-3 text-xs text-text-secondary">
            Events older than this many real-world days are archived (moved to archive table).
            Worlds with AI bot enabled may want a shorter interval.
          </p>
          <div>
            <label className="block text-xs text-text-secondary mb-1">Archive after (days)</label>
            <input
              type="number"
              min={1}
              max={365}
              value={settings.event_archive_days ?? 30}
              onChange={(e) => updateSetting(['event_archive_days'], Number(e.target.value))}
              className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
            />
          </div>
        </section>

        {/* Invite + Members */}
        <section className="rounded-lg border border-bg-elevated bg-bg-surface p-5">
          <h2 className="mb-3 flex items-center gap-2 font-heading text-text-primary">
            <Users size={18} /> Members ({(members ?? []).length})
          </h2>
          <div className="flex gap-2 mb-4">
            <input
              readOnly
              value={`${window.location.origin}/worlds/${id}`}
              className="flex-1 rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-secondary"
            />
            <button
              onClick={() => {
                navigator.clipboard.writeText(`${window.location.origin}/worlds/${id}`);
                setCopied(true);
                setTimeout(() => setCopied(false), 2000);
              }}
              className="flex items-center gap-1 rounded bg-accent px-3 py-2 text-sm text-white hover:bg-accent/80"
            >
              <Copy size={16} /> {copied ? 'Copied!' : 'Copy Link'}
            </button>
          </div>

          {/* Add Member */}
          <div className="flex gap-2 mb-4">
            <div className="relative flex-1">
              <input
                value={newMemberId}
                onChange={(e) => handleSearchInput(e.target.value)}
                placeholder="Search by username or email…"
                className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
              />
              {searchResults.length > 0 && (
                <div className="absolute left-0 top-full mt-1 w-full rounded border border-bg-elevated bg-bg-surface shadow-lg z-10">
                  {searchResults.map((u) => (
                    <button
                      key={u.id}
                      onClick={() => handleSelectUser(u)}
                      className="flex w-full items-center gap-3 px-3 py-2 text-left text-sm hover:bg-bg-elevated/50"
                    >
                      <span className="text-text-primary">{u.username}</span>
                      <span className="text-text-secondary text-xs">{u.email}</span>
                    </button>
                  ))}
                </div>
              )}
            </div>
            <button
              onClick={handleAddMember}
              className="rounded bg-accent px-3 py-2 text-sm text-white hover:bg-accent/80"
            >
              Add
            </button>
          </div>

          {/* Member List */}
          {(members ?? []).length === 0 ? (
            <p className="text-sm text-text-secondary">No members yet</p>
          ) : (
            <ul className="space-y-2">
              {(members ?? []).map((m) => (
                <li
                  key={m.id}
                  className="flex items-center justify-between rounded bg-bg-primary/50 px-3 py-2 text-sm"
                >
                  <div className="flex items-center gap-2">
                    <span className="text-text-primary">{m.userId.slice(0, 8)}…</span>
                    <span className="rounded bg-bg-elevated px-2 py-0.5 text-xs text-text-secondary">
                      {m.role}
                    </span>
                  </div>
                  <button
                    onClick={() => handleRemoveMember(m.id)}
                    className="text-xs text-danger hover:text-danger/80"
                  >
                    Remove
                  </button>
                </li>
              ))}
            </ul>
          )}
        </section>

        {/* Map Editor Link */}
        <section className="rounded-lg border border-bg-elevated bg-bg-surface p-5">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="font-heading text-text-primary">{t('worldEditor.map')}</h2>
              <p className="mt-1 text-xs text-text-secondary">
                Edit world map regions and locations
              </p>
            </div>
            <button
              onClick={() => navigate(`/worlds/${id}/map`)}
              className="rounded bg-accent px-4 py-2 text-sm text-white hover:bg-accent/80"
            >
              Edit Map
            </button>
          </div>
        </section>

        {/* Clone */}
        <section className="rounded-lg border border-bg-elevated bg-bg-surface p-5">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="font-heading text-text-primary">{t('worldEditor.clone')}</h2>
              <p className="mt-1 text-xs text-text-secondary">
                Create a copy of this world including regions, locations, NPCs and factions
              </p>
            </div>
            <button
              onClick={handleClone}
              className="rounded bg-accent px-4 py-2 text-sm text-white hover:bg-accent/80"
            >
              <Copy size={16} className="inline mr-1" /> Clone World
            </button>
          </div>
        </section>

        {/* Invites */}
        <section className="rounded-lg border border-bg-elevated bg-bg-surface p-5">
          <h2 className="mb-3 font-heading text-text-primary">{t('worldEditor.invites')}</h2>
          <p className="text-xs text-text-secondary mb-3">
            Generate single-use invite links for your players. Each link works once.
          </p>
          <div className="flex gap-2 mb-3">
            <input
              readOnly
              value={inviteLink}
              placeholder="Click 'Generate' to create an invite link"
              className="flex-1 rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-xs text-text-secondary"
            />
            {inviteLink && (
              <button
                onClick={() => {
                  navigator.clipboard.writeText(inviteLink);
                  setCopied(true);
                  setTimeout(() => setCopied(false), 2000);
                }}
                className="rounded bg-accent px-3 py-2 text-xs text-white hover:bg-accent/80"
              >
                {copied ? 'Copied!' : 'Copy'}
              </button>
            )}
          </div>
          <button
            onClick={handleGenerateInvite}
            disabled={generating}
            className="rounded bg-accent px-4 py-2 text-sm text-white hover:bg-accent/80 disabled:opacity-40"
          >
            {generating ? '…' : 'Generate Invite Link'}
          </button>
        </section>

        {/* Danger Zone */}
        <section className="rounded-lg border border-danger/20 bg-danger/5 p-5">
          <h2 className="mb-2 font-heading text-danger">{t('worldEditor.dangerZone')}</h2>
          <p className="text-xs text-text-secondary mb-3">
            Permanently deletes this world and all its data. This cannot be undone.
          </p>
          {showDeleteConfirm ? (
            <div className="flex items-center gap-3">
              <p className="text-sm text-danger">Are you sure?</p>
              <button
                onClick={handleDelete}
                className="rounded bg-danger px-4 py-2 text-sm text-white hover:bg-danger/80"
              >
                Confirm Delete
              </button>
              <button
                onClick={() => setShowDeleteConfirm(false)}
                className="text-sm text-text-secondary hover:text-text-primary"
              >
                Cancel
              </button>
            </div>
          ) : (
            <button
              onClick={() => setShowDeleteConfirm(true)}
              className="flex items-center gap-1 rounded border border-danger/40 px-3 py-2 text-sm text-danger hover:bg-danger/10"
            >
              <Trash2 size={16} /> Delete World
            </button>
          )}
        </section>
      </main>
    </div>
  );
}
