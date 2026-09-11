import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Plus, Globe, ScrollText } from 'lucide-react';
import { apiClient } from '../../api/client';
import { useCampaignStore } from '../../store/campaignStore';
import { SkeletonCard } from '../ui/SkeletonCard';

interface WorldOption {
  id: string;
  name: string;
}

interface SystemOption {
  id: string;
  name: string;
  version: number;
  active?: boolean;
}

export default function CampaignSection() {
  const { t } = useTranslation('common');
  const navigate = useNavigate();
  const { campaigns, loading, loadCampaigns, createCampaign, setActiveCampaign } = useCampaignStore();

  const [showCreate, setShowCreate] = useState(false);
  const [name, setName] = useState('');
  const [worldId, setWorldId] = useState('');
  const [systemId, setSystemId] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [worlds, setWorlds] = useState<WorldOption[]>([]);
  const [systems, setSystems] = useState<SystemOption[]>([]);

  useEffect(() => {
    loadCampaigns();
    apiClient
      .get('/worlds/accessible?page=0&size=50')
      .then((r) => setWorlds((r.data as { items: WorldOption[] }).items))
      // Best-effort prefetch — leere Liste ist der gültige Fallback.
      .catch(() => {});
    apiClient
      .get('/game-systems')
      .then((r) => setSystems((r.data as SystemOption[]).filter((s) => s.active !== false)))
      // Best-effort prefetch — leere Liste ist der gültige Fallback.
      .catch(() => {});
  }, [loadCampaigns]);

  const handleCreate = async () => {
    if (!name.trim() || !worldId || !systemId) return;
    setSaving(true);
    setError(null);
    try {
      const campaign = await createCampaign(worldId, systemId, name.trim());
      if (!campaign) {
        setError(t('campaign.createFailed'));
        return;
      }
      setShowCreate(false);
      setName('');
      setWorldId('');
      setSystemId('');
    } catch {
      setError(t('campaign.createFailed'));
    } finally {
      setSaving(false);
    }
  };

  const handleOpen = (campaignId: string) => {
    setActiveCampaign(campaignId);
    navigate(`/campaigns/${campaignId}`);
  };

  return (
    <section className="mt-10">
      <div className="mb-4 flex items-center justify-between">
        <div>
          <h2 className="flex items-center gap-2 text-xl font-heading text-text-primary">
            <ScrollText size={20} className="text-accent" />
            {t('campaign.title')}
          </h2>
          <p className="text-xs text-text-secondary">{t('campaign.subtitle')}</p>
        </div>
        <button
          onClick={() => setShowCreate(true)}
          className="flex items-center gap-2 rounded border border-bg-elevated px-4 py-2 text-sm font-semibold text-text-primary hover:border-accent/50 hover:text-accent"
        >
          <Plus size={18} /> {t('campaign.create')}
        </button>
      </div>

      {showCreate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="w-full max-w-md rounded-lg bg-bg-surface p-6 shadow-xl">
            <h3 className="mb-4 text-lg font-heading text-text-primary">
              {t('campaign.createTitle')}
            </h3>
            {error && (
              <div className="mb-3 rounded bg-danger/20 p-2 text-sm text-danger">{error}</div>
            )}
            <input
              autoFocus
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder={t('campaign.name')}
              className="mb-4 w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2
                         text-text-primary placeholder:text-text-secondary/50
                         focus:border-accent focus:outline-none"
            />
            <label className="mb-1 block text-xs text-text-secondary">
              {t('campaign.world')}
            </label>
            <select
              value={worldId}
              onChange={(e) => setWorldId(e.target.value)}
              className="mb-4 w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2
                         text-text-primary focus:border-accent focus:outline-none"
            >
              <option value="">{t('campaign.worldRequired')}</option>
              {worlds.map((w) => (
                <option key={w.id} value={w.id}>
                  {w.name}
                </option>
              ))}
            </select>
            <label className="mb-1 block text-xs text-text-secondary">
              {t('campaign.system')}
            </label>
            <select
              value={systemId}
              onChange={(e) => setSystemId(e.target.value)}
              className="mb-4 w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2
                         text-text-primary focus:border-accent focus:outline-none"
            >
              <option value="">{t('campaign.systemRequired')}</option>
              {systems.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name} v{s.version}
                </option>
              ))}
            </select>
            <div className="flex justify-end gap-3">
              <button
                onClick={() => {
                  setShowCreate(false);
                  setName('');
                  setWorldId('');
                  setSystemId('');
                  setError(null);
                }}
                className="rounded px-4 py-2 text-sm text-text-secondary hover:text-text-primary"
              >
                {t('actions.cancel')}
              </button>
              <button
                onClick={handleCreate}
                disabled={saving || !name.trim() || !worldId || !systemId}
                className="rounded bg-accent px-4 py-2 text-sm font-semibold text-white
                           hover:bg-accent/80 disabled:opacity-50"
              >
                {saving ? t('status.loading') : t('actions.create')}
              </button>
            </div>
          </div>
        </div>
      )}

      {loading ? (
        <div className="grid gap-4 sm:grid-cols-2">
          {Array.from({ length: 2 }).map((_, i) => (
            <SkeletonCard key={i} lines={3} />
          ))}
        </div>
      ) : campaigns.length === 0 ? (
        <div className="rounded-lg border border-dashed border-bg-elevated p-8 text-center">
          <p className="text-text-secondary">{t('campaign.noCampaigns')}</p>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2">
          {campaigns.map((c) => (
            <div
              key={c.id}
              onClick={() => handleOpen(c.id)}
              className="cursor-pointer rounded-lg border border-bg-elevated bg-bg-surface p-4 text-left
                         transition hover:border-accent/50 hover:bg-bg-elevated/50"
            >
              <div className="flex items-center justify-between">
                <h3 className="font-heading text-text-primary">{c.name}</h3>
                <Globe size={16} className="text-accent" />
              </div>
              <p className="mt-1 text-xs text-text-secondary">
                {c.worldId.slice(0, 8)} · {c.gameSystemId.slice(0, 8)}
              </p>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
