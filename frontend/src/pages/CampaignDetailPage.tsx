import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Play, UserPlus, Trash2, Crown, User } from 'lucide-react';
import { apiClient } from '../api/client';
import { useCampaignStore } from '../store/campaignStore';
import { useAuthStore } from '../store/authStore';
import { useWorldStore } from '../store/worldStore';
import { useToastStore } from '../store/toastStore';
import { LoadingSpinner } from '../components/ui/LoadingSpinner';

interface CampaignDetail {
  id: string;
  worldId: string;
  gameSystemId: string;
  gameSystemVersion?: number | null;
  name: string;
  settingsJson: string;
  stateJson: string;
  createdAt: string;
  updatedAt: string;
}

interface CampaignMember {
  id: string;
  campaignId: string;
  userId: string;
  role: string;
  joinedAt: string;
}

interface WorldDetail {
  id: string;
  name: string;
}

interface SystemDetail {
  id: string;
  name: string;
  version: number;
}

export default function CampaignDetailPage() {
  const { t } = useTranslation('common');
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const campaignId = id ?? '';
  const setActiveCampaign = useCampaignStore((s) => s.setActiveCampaign);
  const setCurrentWorld = useWorldStore((s) => s.setCurrentWorld);

  const [campaign, setCampaign] = useState<CampaignDetail | null>(null);
  const [members, setMembers] = useState<CampaignMember[]>([]);
  const [world, setWorld] = useState<WorldDetail | null>(null);
  const [system, setSystem] = useState<SystemDetail | null>(null);
  const [loading, setLoading] = useState(true);

  const [search, setSearch] = useState('');
  const [searchResults, setSearchResults] = useState<{ id: string; username: string; email: string }[]>([]);
  const [searchTimer, setSearchTimer] = useState<ReturnType<typeof setTimeout> | null>(null);
  const [adding, setAdding] = useState(false);
  const [removingId, setRemovingId] = useState<string | null>(null);
  const [roleChangingId, setRoleChangingId] = useState<string | null>(null);
  const addToast = useToastStore((s) => s.addToast);

  useEffect(() => {
    if (!campaignId) return;
    Promise.all([
      apiClient.get<CampaignDetail>(`/campaigns/${campaignId}`),
      apiClient.get<CampaignMember[]>(`/campaigns/${campaignId}/members`),
    ])
      .then(([cRes, mRes]) => {
        setCampaign(cRes.data);
        setMembers(mRes.data);
        // Zusammenfassung cachen, damit der GameView-Badge den Namen kennt —
        // auch ohne geladene Kampagnenliste (Deep-Link, Reload).
        setActiveCampaign(campaignId, {
          id: cRes.data.id,
          worldId: cRes.data.worldId,
          gameSystemId: cRes.data.gameSystemId,
          gameSystemVersion: cRes.data.gameSystemVersion ?? null,
          name: cRes.data.name,
          settingsJson: cRes.data.settingsJson,
          stateJson: cRes.data.stateJson,
          createdAt: cRes.data.createdAt,
          updatedAt: cRes.data.updatedAt,
        });
        return Promise.all([
          apiClient.get<WorldDetail>(`/worlds/${cRes.data.worldId}`).catch(() => null),
          apiClient.get<SystemDetail>(`/game-systems/${cRes.data.gameSystemId}`).catch(() => null),
        ]);
      })
      .then(([w, s]) => {
        setWorld(w?.data ?? null);
        setSystem(s?.data ?? null);
      })
      .catch(() => addToast(t('campaign.loadFailed'), 'error'))
      .finally(() => setLoading(false));
  }, [campaignId, setActiveCampaign, addToast, t]);

  const handleSearch = (value: string) => {
    setSearch(value);
    if (searchTimer) clearTimeout(searchTimer);
    if (value.length < 3) {
      setSearchResults([]);
      return;
    }
    setSearchTimer(setTimeout(async () => {
      try {
        const res = await apiClient.get(`/users/search?q=${encodeURIComponent(value)}`);
        setSearchResults(res.data);
      } catch {
        setSearchResults([]);
      }
    }, 300));
  };

  const handleAdd = async (userId: string) => {
    setAdding(true);
    try {
      await apiClient.post(`/campaigns/${campaignId}/members`, { userId, role: 'PLAYER' });
      const res = await apiClient.get<CampaignMember[]>(`/campaigns/${campaignId}/members`);
      setMembers(res.data);
      setSearch('');
      setSearchResults([]);
      addToast(t('campaign.memberAdded'), 'success');
    } catch {
      addToast(t('campaign.memberAddFailed'), 'error');
    } finally {
      setAdding(false);
    }
  };

  const handleRemove = async (member: CampaignMember) => {
    setRemovingId(member.id);
    try {
      await apiClient.delete(`/campaigns/${campaignId}/members/${member.userId}`);
      setMembers((prev) => prev.filter((m) => m.id !== member.id));
      addToast(t('campaign.memberRemoved'), 'success');
    } catch {
      addToast(t('campaign.memberRemoveFailed'), 'error');
    } finally {
      setRemovingId(null);
    }
  };

  const currentUser = useAuthStore((s2) => s2.user);
  const currentIsDm = members.some((m) => m.userId === currentUser?.id && m.role === 'DM');

  const handleRoleChange = async (member: CampaignMember, role: 'DM' | 'PLAYER') => {
    setRoleChangingId(member.id);
    try {
      await apiClient.patch(`/campaigns/${campaignId}/members/${member.userId}`, { role });
      setMembers((prev) => prev.map((m) => (m.id === member.id ? { ...m, role } : m)));
      addToast(t('campaign.memberRoleChanged'), 'success');
    } catch {
      addToast(t('campaign.memberRoleChangeFailed'), 'error');
    } finally {
      setRoleChangingId(null);
    }
  };

  const botMode: string = (() => {
    try {
      return JSON.parse(campaign?.settingsJson || '{}')?.bot?.mode ?? '';
    } catch {
      return '';
    }
  })();

  const handleBotMode = async (mode: string) => {
    try {
      let settings: Record<string, unknown> = {};
      try {
        settings = JSON.parse(campaign?.settingsJson || '{}');
      } catch {
        settings = {};
      }
      const bot = (settings.bot as Record<string, unknown>) ?? {};
      settings.bot = { ...bot, mode };
      const res = await apiClient.patch(`/campaigns/${campaignId}`, {
        settingsJson: JSON.stringify(settings),
      });
      setCampaign(res.data);
      addToast(t('campaign.botModeSaved'), 'success');
    } catch {
      addToast(t('campaign.botModeFailed'), 'error');
    }
  };

  const handlePullSystem = async () => {
    try {
      const res = await apiClient.post(`/campaigns/${campaignId}/pull-system`);
      useCampaignStore.getState().setActiveCampaign(res.data.id, {
        ...res.data,
        gameSystemVersion: res.data.gameSystemVersion ?? null,
      });
    } catch {
      addToast(t('campaign.pullSystemFailed'), 'error');
    }
  };

  const handleEnterWorld = () => {
    if (!campaign) return;
    setActiveCampaign(campaign.id, {
      id: campaign.id,
      worldId: campaign.worldId,
      gameSystemId: campaign.gameSystemId,
      name: campaign.name,
      settingsJson: campaign.settingsJson,
      stateJson: campaign.stateJson,
      createdAt: campaign.createdAt,
      updatedAt: campaign.updatedAt,
    });
    const summary = {
      id: campaign.worldId,
      name: world?.name ?? campaign.worldId,
      gameSystemId: campaign.gameSystemId,
      currentGameTime: null,
      createdAt: campaign.createdAt,
    };
    setCurrentWorld(summary);
    navigate(`/worlds/${campaign.worldId}`);
  };

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-bg-primary">
        <LoadingSpinner />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-bg-primary">
      <header className="flex items-center justify-between border-b border-bg-elevated bg-bg-surface px-6 py-3">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate('/dashboard')}
            className="flex items-center gap-1 text-sm text-text-secondary hover:text-accent"
          >
            <ArrowLeft size={16} /> {t('nav.backToDashboard')}
          </button>
        </div>
        <h1 className="text-lg font-heading text-accent">{campaign?.name}</h1>
        <button
          onClick={handleEnterWorld}
          className="flex items-center gap-2 rounded bg-accent px-4 py-2 text-sm font-semibold text-white hover:bg-accent/80"
        >
          <Play size={16} /> {t('campaign.enterWorld')}
        </button>
      </header>

      <main className="mx-auto max-w-3xl p-6">
        <div className="mb-6 grid gap-4 sm:grid-cols-3">
          <div className="rounded-lg border border-bg-elevated bg-bg-surface p-4">
            <p className="text-xs text-text-secondary">{t('campaign.world')}</p>
            <p className="mt-1 font-heading text-text-primary">{world?.name ?? '—'}</p>
          </div>
          <div className="rounded-lg border border-bg-elevated bg-bg-surface p-4">
            <p className="text-xs text-text-secondary">{t('campaign.system')}</p>
            <p className="mt-1 font-heading text-text-primary">
              {system ? `${system.name} v${system.version}` : '—'}
            </p>
            {campaign?.gameSystemVersion != null && system && system.version > campaign.gameSystemVersion && (
              <div className="mt-2 space-y-1">
                <span className="rounded bg-warning/10 px-1.5 py-0.5 text-[10px] text-warning">
                  {t('campaign.systemUpdateAvailable', { from: campaign.gameSystemVersion, to: system.version })}
                </span>
                <button
                  onClick={handlePullSystem}
                  className="block text-xs text-accent hover:text-accent/80"
                >
                  {t('campaign.pullSystem')}
                </button>
              </div>
            )}
          </div>
          <div className="rounded-lg border border-bg-elevated bg-bg-surface p-4">
            <p className="text-xs text-text-secondary">{t('campaign.members')}</p>
            <p className="mt-1 font-heading text-text-primary">{members.length}</p>
          </div>
        </div>

        <section className="mb-6 rounded-lg border border-bg-elevated bg-bg-surface p-4">
          <p className="text-xs text-text-secondary">{t('campaign.botMode')}</p>
          <select
            value={botMode}
            onChange={(e) => handleBotMode(e.target.value)}
            className="mt-1 rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-sm text-text-primary outline-none focus:border-accent"
          >
            <option value="">{t('campaign.botModeWorld')}</option>
            <option value="autonom">{t('campaign.botModeAutonom')}</option>
            <option value="suggest">{t('campaign.botModeSuggest')}</option>
            <option value="off">{t('campaign.botModeOff')}</option>
          </select>
          <p className="mt-1 text-[10px] text-text-secondary">{t('campaign.botModeHint')}</p>
        </section>

        <section>
          <h2 className="mb-3 flex items-center gap-2 font-heading text-text-primary">
            <UserPlus size={18} className="text-accent" />
            {t('campaign.addMember')}
          </h2>
          <div className="relative mb-4">
            <input
              value={search}
              onChange={(e) => handleSearch(e.target.value)}
              placeholder={t('campaign.addMemberPlaceholder')}
              className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
            />
            {searchResults.length > 0 && (
              <div className="absolute left-0 top-full mt-1 w-full rounded border border-bg-elevated bg-bg-surface shadow-lg z-10">
                {searchResults.map((u) => (
                  <button
                    key={u.id}
                    onClick={() => handleAdd(u.id)}
                    disabled={adding}
                    className="flex w-full items-center gap-3 px-3 py-2 text-left text-sm hover:bg-bg-elevated/50 disabled:opacity-50"
                  >
                    <span className="text-text-primary">{u.username}</span>
                    <span className="text-text-secondary text-xs">{u.email}</span>
                  </button>
                ))}
              </div>
            )}
          </div>

          {members.length === 0 ? (
            <p className="text-sm text-text-secondary">{t('campaign.noMembers')}</p>
          ) : (
            <ul className="space-y-2">
              {members.map((m) => (
                <li
                  key={m.id}
                  className="flex items-center justify-between rounded-lg border border-bg-elevated bg-bg-surface px-4 py-2"
                >
                  <div className="flex items-center gap-2">
                    {m.role === 'DM' ? (
                      <Crown size={16} className="text-warning" />
                    ) : (
                      <User size={16} className="text-text-secondary" />
                    )}
                    <span className="text-sm text-text-primary">{m.userId.slice(0, 8)}</span>
                    <span className="text-xs text-text-secondary">
                      {m.role === 'DM' ? t('campaign.dm') : t('campaign.player')}
                    </span>
                  </div>
                  {currentIsDm && (
                    <span className="flex items-center gap-2">
                      <button
                        onClick={() => handleRoleChange(m, m.role === 'DM' ? 'PLAYER' : 'DM')}
                        disabled={roleChangingId === m.id}
                        className="text-xs text-text-secondary hover:text-accent disabled:opacity-40"
                      >
                        {m.role === 'DM' ? t('campaign.makePlayer') : t('campaign.makeDm')}
                      </button>
                      <button
                        onClick={() => handleRemove(m)}
                        disabled={removingId === m.id}
                        className="text-text-secondary hover:text-danger disabled:opacity-40"
                        aria-label={t('actions.delete')}
                      >
                        <Trash2 size={16} />
                      </button>
                    </span>
                  )}
                </li>
              ))}
            </ul>
          )}
        </section>
      </main>
    </div>
  );
}
