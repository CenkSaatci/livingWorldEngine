import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, MapPin, Globe, ScrollText } from 'lucide-react';
import { apiClient } from '../api/client';
import { useApiGet } from '../hooks/useApiGet';
import { LoadingSpinner } from '../components/ui/LoadingSpinner';
import { EntityTimeline } from '../components/world/EntityTimeline';

const CLIMATES = [
  'temperate',
  'forest',
  'desert',
  'mountains',
  'plains',
  'swamp',
  'coast',
  'tundra',
  'jungle',
  'arctic',
];

interface RegionData {
  id: string;
  worldId: string;
  name: string;
  description: string;
  history: string;
  dangerLevel: number;
  climate: string;
  population: number;
  resources: string | null;
  factions: string | null;
}

interface LocationSummary {
  id: string;
  name: string;
  type: string;
  wealth: number;
}

export default function RegionViewPage() {
  const { t } = useTranslation('common');
  const { id, regionId } = useParams<{ id: string; regionId: string }>();
  const navigate = useNavigate();
  const worldId = id ?? '';

  const {
    data: region,
    loading,
    refetch,
  } = useApiGet<RegionData>(`/worlds/${worldId}/regions/${regionId}`, [worldId, regionId]);
  const { data: locations } = useApiGet<LocationSummary[]>(`/regions/${regionId}/locations`, [
    regionId,
  ]);

  const [editing, setEditing] = useState(false);
  const [showDelete, setShowDelete] = useState(false);
  const [editName, setEditName] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [editClimate, setEditClimate] = useState('');
  const [editDangerLevel, setEditDangerLevel] = useState(1);
  const [editPopulation, setEditPopulation] = useState(0);
  const [editResources, setEditResources] = useState('');
  const [editFactionsJson, setEditFactionsJson] = useState('');
  const [saving, setSaving] = useState(false);

  const openEdit = () => {
    if (!region) return;
    setEditName(region.name);
    setEditDescription(region.description);
    setEditClimate(region.climate);
    setEditDangerLevel(region.dangerLevel);
    setEditPopulation(region.population);
    setEditResources(region.resources ?? '');
    setEditFactionsJson(region.factions ?? '');
    setEditing(true);
  };

  const handleEdit = async () => {
    if (!region || !editName.trim()) return;
    setSaving(true);
    try {
      await apiClient.patch(`/worlds/${region.worldId}/regions/${region.id}`, {
        name: editName.trim(),
        description: editDescription || null,
        climate: editClimate,
        dangerLevel: editDangerLevel,
        population: editPopulation,
        resources: editResources || null,
        factions: editFactionsJson || null,
      });
      setEditing(false);
      refetch();
    } catch {
      /* */
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!region) return;
    try {
      await apiClient.delete(`/worlds/${region.worldId}/regions/${region.id}`);
      navigate(-1);
    } catch {
      /* */
    }
  };

  if (loading || !region) return <LoadingSpinner size="lg" text="Loading region…" />;

  return (
    <div className="min-h-screen bg-bg-primary">
      <header className="flex items-center gap-3 border-b border-bg-elevated bg-bg-surface px-6 py-3">
        <button onClick={() => navigate(-1)} className="text-text-secondary hover:text-accent">
          <ArrowLeft size={20} />
        </button>
        <h1 className="flex items-center gap-2 text-lg font-heading text-text-primary">
          <Globe size={20} className="text-accent" /> {region.name}
        </h1>
        <span
          className={`text-xs px-2 py-0.5 rounded ${region.dangerLevel > 5 ? 'bg-danger/20 text-danger' : 'bg-bg-elevated text-text-secondary'}`}
        >
          ⚔️ {region.dangerLevel}/10
        </span>
        <span className="text-xs text-text-secondary capitalize">{region.climate}</span>
        <button onClick={openEdit} className="text-xs text-accent hover:text-accent/60 ml-auto">
          {t('actions.edit')}
        </button>
        <button
          onClick={() => setShowDelete(true)}
          className="text-xs text-danger hover:text-danger/60"
        >
          {t('actions.delete')}
        </button>
      </header>

      <main className="mx-auto max-w-4xl grid grid-cols-1 gap-6 p-6 lg:grid-cols-3">
        <div className="lg:col-span-2 space-y-6">
          {region.description && (
            <section className="rounded-lg border border-bg-elevated bg-bg-surface p-5">
              <p className="text-sm text-text-primary leading-relaxed">{region.description}</p>
            </section>
          )}

          {region.history && (
            <section className="rounded-lg border border-bg-elevated bg-bg-surface p-5">
              <h2 className="flex items-center gap-1.5 text-sm font-semibold text-text-primary mb-2">
                <ScrollText size={16} /> {t('region.history')}
              </h2>
              <p className="text-sm text-text-secondary">{region.history}</p>
            </section>
          )}

          <section className="rounded-lg border border-bg-elevated bg-bg-surface p-5">
            <h2 className="flex items-center gap-1.5 text-sm font-semibold text-text-primary mb-3">
              <MapPin size={16} /> Locations ({(locations ?? []).length})
            </h2>
            {!locations || locations.length === 0 ? (
              <p className="text-xs text-text-secondary">{t('region.noLocations')}</p>
            ) : (
              <div className="grid gap-3 sm:grid-cols-2">
                {locations.map((loc) => (
                  <button
                    key={loc.id}
                    onClick={() => navigate(`/worlds/${region.worldId}/locations/${loc.id}`)}
                    className="rounded border border-bg-elevated bg-bg-primary/50 px-4 py-3 text-left hover:border-accent/50"
                  >
                    <p className="text-sm font-medium text-text-primary">{loc.name}</p>
                    <div className="flex items-center gap-2 mt-1 text-xs text-text-secondary">
                      <span className="capitalize">{loc.type}</span>
                      <span>💰 {loc.wealth}/10</span>
                    </div>
                  </button>
                ))}
              </div>
            )}
          </section>
        </div>

        <div className="space-y-6">
          <section className="rounded-lg border border-bg-elevated bg-bg-surface p-5">
            <EntityTimeline entityType="region" entityId={region.id} />
          </section>
        </div>
      </main>

      {/* Edit Modal */}
      {editing && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/60"
          onClick={() => setEditing(false)}
        >
          <div
            className="w-80 rounded-xl border border-bg-elevated bg-bg-surface p-5 shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <h3 className="font-heading text-text-primary mb-4">{t('region.editTitle')}</h3>
            <div className="space-y-3">
              <div>
                <label className="block text-xs text-text-secondary mb-1">{t('entity.name')}</label>
                <input
                  value={editName}
                  onChange={(e) => setEditName(e.target.value)}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                />
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">{t('region.description')}</label>
                <textarea
                  value={editDescription}
                  onChange={(e) => setEditDescription(e.target.value)}
                  rows={3}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent resize-none"
                />
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">{t('region.climate')}</label>
                <select
                  value={editClimate}
                  onChange={(e) => setEditClimate(e.target.value)}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                >
                  {CLIMATES.map((c) => (
                    <option key={c} value={c} className="capitalize">
                      {c}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">
                  {t('region.dangerLevel')}
                </label>
                <input
                  type="range"
                  min={1}
                  max={10}
                  value={editDangerLevel}
                  onChange={(e) => setEditDangerLevel(Number(e.target.value))}
                  className="w-full accent-accent"
                />
                <span className="text-xs text-text-secondary">{editDangerLevel}/10</span>
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">{t('region.population')}</label>
                <input
                  type="number"
                  min={0}
                  value={editPopulation}
                  onChange={(e) => setEditPopulation(Number(e.target.value))}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                />
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">{t('region.resourcesJson')}</label>
                <textarea
                  value={editResources}
                  onChange={(e) => setEditResources(e.target.value)}
                  rows={3}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent resize-none"
                />
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">{t('region.factionsJson')}</label>
                <textarea
                  value={editFactionsJson}
                  onChange={(e) => setEditFactionsJson(e.target.value)}
                  rows={3}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent resize-none"
                />
              </div>
              <div className="flex gap-2 pt-2">
                <button
                  onClick={() => setEditing(false)}
                  className="flex-1 rounded border border-bg-elevated py-2 text-sm text-text-secondary hover:text-text-primary"
                >
                  {t('actions.cancel')}
                </button>
                <button
                  onClick={handleEdit}
                  disabled={saving || !editName.trim()}
                  className="flex-1 rounded bg-accent py-2 text-sm text-white hover:bg-accent/80 disabled:opacity-40"
                >
                  {saving ? t('region.saving') : t('actions.save')}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Delete Confirm */}
      {showDelete && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/60"
          onClick={() => setShowDelete(false)}
        >
          <div
            className="w-72 rounded-xl border border-bg-elevated bg-bg-surface p-5 shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <h3 className="font-heading text-text-primary mb-2">{t('region.deleteTitle')}</h3>
            <p className="text-sm text-text-secondary mb-4">
              {t('region.deleteConfirm', { name: region.name })}
            </p>
            <div className="flex gap-2">
              <button
                onClick={() => setShowDelete(false)}
                className="flex-1 rounded border border-bg-elevated py-2 text-sm text-text-secondary hover:text-text-primary"
              >
                {t('actions.cancel')}
              </button>
              <button
                onClick={handleDelete}
                className="flex-1 rounded bg-danger py-2 text-sm text-white hover:bg-danger/80"
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
