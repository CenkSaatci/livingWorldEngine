import { useState, useEffect } from 'react';
import { X, Plus } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { apiClient } from '../../api/client';
import { useToast } from '../../hooks/useToast';
import { QuickFactionModal } from './QuickFactionModal';

interface FactionSummary {
  id: string;
  name: string;
  color: string;
}

interface LocationSummary {
  id: string;
  name: string;
  regionName?: string;
}

interface Props {
  worldId: string;
  onCreated: (entityId: string, entityType: string) => void;
  onClose: () => void;
}

const EXPERIENCE_LEVELS = ['green', 'veteran', 'elite'];
const SOCIAL_STANDINGS = ['peasant', 'merchant', 'guard', 'noble', 'clergy', 'criminal'];

export function EntityCreateModal({ worldId, onCreated, onClose }: Props) {
  const { t } = useTranslation('common');
  const toast = useToast();
  const [name, setName] = useState('');
  const [entityType, setEntityType] = useState<'NPC' | 'PC'>('NPC');
  const [age, setAge] = useState(30);
  const [experienceLevel, setExperienceLevel] = useState('green');
  const [socialStanding, setSocialStanding] = useState('peasant');
  const [factionId, setFactionId] = useState('');
  const [locationId, setLocationId] = useState('');
  const [backstory, setBackstory] = useState('');
  const [personality, setPersonality] = useState('');
  const [knowledge, setKnowledge] = useState('');
  const [goals, setGoals] = useState('');
  const [saving, setSaving] = useState(false);
  const [showFactionQuick, setShowFactionQuick] = useState(false);

  const [factions, setFactions] = useState<FactionSummary[]>([]);
  const [locations, setLocations] = useState<LocationSummary[]>([]);

  // Lade Fraktionen + Orte
  useEffect(() => {
    if (!worldId) return;
    apiClient
      .get(`/worlds/${worldId}/factions`)
      .then((r) => setFactions(r.data))
      .catch(() => toast.error('Failed to load factions'));
    // Lade alle Regionen → parallel deren Locations
    apiClient
      .get(`/worlds/${worldId}/regions`)
      .then(async (regRes) => {
        const regs = regRes.data as { id: string; name: string }[];
        const results = await Promise.all(
          regs.map(async (r) => {
            try {
              const locRes = await apiClient.get(`/regions/${r.id}/locations`);
              return (locRes.data as { id: string; name: string }[]).map((l) => ({
                ...l,
                regionName: r.name,
              }));
            } catch {
              return [] as LocationSummary[];
            }
          })
        );
        setLocations(results.flat());
      })
      .catch(() => toast.error('Failed to load locations'));
  }, [worldId]);

  const handleSave = async () => {
    if (!name.trim()) return;
    setSaving(true);
    try {
      const metadata: Record<string, unknown> = {};
      if (personality) metadata.personality = personality;
      if (knowledge)
        metadata.knowledge = knowledge
          .split(',')
          .map((s) => s.trim())
          .filter(Boolean);
      if (goals)
        metadata.goals = goals
          .split(',')
          .map((s) => s.trim())
          .filter(Boolean);
      if (locationId) metadata.location_id = locationId;

      const res = await apiClient.post(`/worlds/${worldId}/entities`, {
        entityType,
        name: name.trim(),
        age: age || 30,
        experienceLevel,
        socialStanding,
        factionId: factionId || null,
        backstory: backstory || null,
        metadataJson: JSON.stringify(metadata),
      });
      onCreated(res.data.id, entityType);
      onClose();
    } catch {
      toast.error('Failed to create entity');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div
      className="modal-overlay fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/60 pt-12"
      onClick={onClose}
    >
      <div
        className="modal-content w-full max-w-lg rounded-xl border border-bg-elevated bg-bg-surface p-6 shadow-2xl mb-12"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between mb-5">
          <h2 className="text-lg font-heading text-text-primary">{t('entity.create')}</h2>
          <button onClick={onClose} className="text-text-secondary hover:text-text-primary">
            <X size={20} />
          </button>
        </div>

        <div className="space-y-4">
          {/* Name + Type */}
          <div className="grid grid-cols-3 gap-3">
            <div className="col-span-2">
              <label className="block text-xs text-text-secondary mb-1">{t('entity.name')}</label>
              <input
                value={name}
                onChange={(e) => setName(e.target.value)}
                autoFocus
                className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
              />
            </div>
            <div>
              <label className="block text-xs text-text-secondary mb-1">{t('entity.type')}</label>
              <select
                value={entityType}
                onChange={(e) => setEntityType(e.target.value as 'NPC' | 'PC')}
                className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
              >
                <option value="NPC">NPC</option>
                <option value="PC">PC</option>
              </select>
            </div>
          </div>

          {/* Age + Experience + Standing */}
          <div className="grid grid-cols-3 gap-3">
            <div>
              <label className="block text-xs text-text-secondary mb-1">{t('entity.age')}</label>
              <input
                type="number"
                min={1}
                value={age}
                onChange={(e) => setAge(Number(e.target.value))}
                className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
              />
            </div>
            <div>
              <label className="block text-xs text-text-secondary mb-1">
                {t('entity.experience')}
              </label>
              <select
                value={experienceLevel}
                onChange={(e) => setExperienceLevel(e.target.value)}
                className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
              >
                {EXPERIENCE_LEVELS.map((l) => (
                  <option key={l} value={l}>
                    {l}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs text-text-secondary mb-1">
                {t('entity.standing')}
              </label>
              <select
                value={socialStanding}
                onChange={(e) => setSocialStanding(e.target.value)}
                className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
              >
                {SOCIAL_STANDINGS.map((s) => (
                  <option key={s} value={s} className="capitalize">
                    {s}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Faction */}
          <div>
            <label className="block text-xs text-text-secondary mb-1">{t('entity.faction')}</label>
            <div className="flex gap-2">
              <select
                value={factionId}
                onChange={(e) => setFactionId(e.target.value)}
                className="flex-1 rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
              >
                <option value="">— None —</option>
                {factions.map((f) => (
                  <option key={f.id} value={f.id} style={{ backgroundColor: f.color }}>
                    {f.name}
                  </option>
                ))}
              </select>
              <button
                onClick={() => setShowFactionQuick(true)}
                className="rounded bg-bg-elevated px-3 py-2 text-text-secondary hover:text-accent"
                title="New faction"
              >
                <Plus size={18} />
              </button>
            </div>
          </div>

          {/* Location */}
          <div>
            <label className="block text-xs text-text-secondary mb-1">{t('entity.location')}</label>
            <select
              value={locationId}
              onChange={(e) => setLocationId(e.target.value)}
              className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
            >
              <option value="">— None —</option>
              {locations.map((l) => (
                <option key={l.id} value={l.id}>
                  {l.name} {l.regionName ? `(${l.regionName})` : ''}
                </option>
              ))}
            </select>
          </div>

          {/* Personality */}
          <div>
            <label className="block text-xs text-text-secondary mb-1">
              {t('entity.personality')}
            </label>
            <input
              value={personality}
              onChange={(e) => setPersonality(e.target.value)}
              className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
              placeholder="arrogant, curious, fearful…"
            />
          </div>

          {/* Knowledge + Goals */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs text-text-secondary mb-1">
                {t('entity.knowledge')}
              </label>
              <input
                value={knowledge}
                onChange={(e) => setKnowledge(e.target.value)}
                className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                placeholder="goblins, trade, history…"
              />
            </div>
            <div>
              <label className="block text-xs text-text-secondary mb-1">{t('entity.goals')}</label>
              <input
                value={goals}
                onChange={(e) => setGoals(e.target.value)}
                className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                placeholder="survive, get rich…"
              />
            </div>
          </div>

          {/* Backstory */}
          <div>
            <label className="block text-xs text-text-secondary mb-1">
              {t('entity.backstory')}
            </label>
            <textarea
              value={backstory}
              onChange={(e) => setBackstory(e.target.value)}
              rows={3}
              className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent resize-none"
              placeholder={t('entity.backstory_placeholder')}
            />
          </div>

          {/* Actions */}
          <div className="flex justify-end gap-3 pt-2">
            <button
              onClick={onClose}
              className="rounded border border-bg-elevated px-4 py-2 text-sm text-text-secondary hover:text-text-primary"
            >
              {t('actions.cancel')}
            </button>
            <button
              onClick={handleSave}
              disabled={saving || !name.trim()}
              className="rounded bg-accent px-4 py-2 text-sm text-white hover:bg-accent/80 disabled:opacity-40"
            >
              {saving ? 'Creating…' : t('entity.create')}
            </button>
          </div>
        </div>
      </div>

      {showFactionQuick && (
        <QuickFactionModal
          worldId={worldId}
          onCreated={() => {
            apiClient
              .get(`/worlds/${worldId}/factions`)
              .then((r) => setFactions(r.data))
              .catch(() => toast.error('Failed to refresh factions'));
          }}
          onClose={() => setShowFactionQuick(false)}
        />
      )}
    </div>
  );
}
