/* eslint-disable @typescript-eslint/no-explicit-any */
import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Briefcase, Heart, HeartOff, Swords, Handshake, Minus } from 'lucide-react';
import { apiClient } from '../api/client';
import { useApiGet } from '../hooks/useApiGet';
import { useLazyApiGet } from '../hooks/useLazyApiGet';
import { EntityTimeline } from '../components/world/EntityTimeline';
import { LoadingSpinner } from '../components/ui/LoadingSpinner';
import { useToast } from '../hooks/useToast';
import { useAuthStore } from '../store/authStore';

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

interface NpcData {
  id: string;
  name: string;
  entityType: string;
  metadataJson: string;
  factionId: string | null;
  backstory?: string;
  age?: number;
  experienceLevel?: string;
  socialStanding?: string;
  experiencePoints?: number;
}

interface FactionData {
  id: string;
  name: string;
  color: string;
}

interface FactionRelation {
  id: string;
  factionAId: string;
  factionBId: string;
  relationStatus: string;
}

const RELATION_ICONS: Record<string, JSX.Element> = {
  ALLIANCE: <Handshake size={14} className="text-success" />,
  FRIENDLY: <Handshake size={14} className="text-accent" />,
  NEUTRAL: <Minus size={14} className="text-text-secondary" />,
  UNFRIENDLY: <Swords size={14} className="text-yellow-500" />,
  WAR: <Swords size={14} className="text-danger" />,
};

const NPC_SERVICES = [
  'sell_weapons',
  'repair',
  'buy_ore',
  'sell_potions',
  'training',
  'healing',
  'inn_stay',
  'buy_food',
  'sell_scrolls',
  'identification',
];

export default function NpcViewPage() {
  const { t } = useTranslation('common');
  const { id, npcId } = useParams<{ id: string; npcId: string }>();
  const navigate = useNavigate();
  const worldId = id ?? '';
  const toast = useToast();
  const toastError = toast.error;
  const user = useAuthStore((s) => s.user);
  const isDm = user?.role === 'ADMIN';

  const { data: npc, loading, refetch } = useApiGet<NpcData>(`/worlds/${worldId}/entities/${npcId}`, [worldId, npcId]);
  const { data: faction } = useApiGet<FactionData>(
    npc?.factionId ? `/factions/${npc.factionId}` : '',
    [npc?.factionId],
  );
  const { data: relations, fetch: fetchRelations } = useLazyApiGet<FactionRelation[]>();

  const [editing, setEditing] = useState(false);
  const [showDelete, setShowDelete] = useState(false);
  const [xpAmount, setXpAmount] = useState(50);
  const [granting, setGranting] = useState(false);
  const [editName, setEditName] = useState('');
  const [editAge, setEditAge] = useState<number | undefined>(undefined);
  const [editExperienceLevel, setEditExperienceLevel] = useState('');
  const [editSocialStanding, setEditSocialStanding] = useState('');
  const [editBackstory, setEditBackstory] = useState('');
  const [editFactionId, setEditFactionId] = useState('');
  const [editLocationId, setEditLocationId] = useState('');
  const [editPersonality, setEditPersonality] = useState('');
  const [editKnowledge, setEditKnowledge] = useState('');
  const [editGoals, setEditGoals] = useState('');
  const [editServices, setEditServices] = useState<string[]>([]);
  const [editPriceMod, setEditPriceMod] = useState('');
  const [saving, setSaving] = useState(false);

  const [factions, setFactions] = useState<FactionSummary[]>([]);
  const [factionLocations, setFactionLocations] = useState<LocationSummary[]>([]);
  const [npcAdventures, setNpcAdventures] = useState<{ id: string; name: string }[]>([]);

  useEffect(() => {
    if (faction?.id) fetchRelations(`/factions/${faction.id}/relations`);
  }, [faction?.id, fetchRelations]);

  useEffect(() => {
    if (npc) {
      apiClient
        .get(`/adventures/by-giver/${npc.id}`)
        .then((r) => setNpcAdventures(r.data as any))
        .catch(() => toastError('Failed to load adventures'));
    }
  }, [npc, toastError]);

  useEffect(() => {
    if (!worldId || !editing) return;
    apiClient
      .get(`/worlds/${worldId}/factions`)
      .then((r) => setFactions(r.data))
      .catch(() => toastError('Failed to load factions'));
    apiClient
      .get(`/worlds/${worldId}/regions`)
      .then(async (regRes) => {
        const regs = regRes.data as { id: string; name: string }[];
        const allLocs: LocationSummary[] = [];
        let locationsFailed = false;
        for (const r of regs) {
          try {
            const locRes = await apiClient.get(`/regions/${r.id}/locations`);
            const locs = (locRes.data as { id: string; name: string }[]).map((l) => ({
              ...l,
              regionName: r.name,
            }));
            allLocs.push(...locs);
          } catch {
            locationsFailed = true;
          }
        }
        if (locationsFailed) toastError('Failed to load some locations');
        setFactionLocations(allLocs);
      })
      .catch(() => toastError('Failed to load regions'));
  }, [worldId, editing, toastError]);

  if (loading || !npc) return <LoadingSpinner size="lg" text="Loading NPC…" />;

  let meta: Record<string, string | string[] | number | undefined> = {};
  try {
    meta = JSON.parse(npc.metadataJson) as Record<string, string | string[] | number | undefined>;
  } catch {
    /* */
  }

  const occupation = meta.occupation as string | undefined;
  const greeting = meta.greeting as string | undefined;
  const priceMod = meta.price_modifier as number | undefined;
  const relationships = (meta.relationships ?? {}) as Record<string, string>;
  const services = (meta.services_offered ?? []) as string[];

  const openEdit = () => {
    setEditName(npc.name);
    setEditAge(npc.age);
    setEditExperienceLevel(npc.experienceLevel ?? '');
    setEditSocialStanding(npc.socialStanding ?? '');

    setEditFactionId(npc.factionId ?? '');
    setEditLocationId((meta.location_id as string) ?? '');
    setEditPersonality((meta.personality as string) ?? '');
    setEditKnowledge(((meta.knowledge as string[]) ?? []).join(', '));
    setEditGoals(((meta.goals as string[]) ?? []).join(', '));
    setEditServices(((meta.services_offered as string[]) ?? []).filter((s) => typeof s === 'string'));
    setEditPriceMod(meta.price_modifier !== undefined ? String(meta.price_modifier) : '');
    setEditing(true);
  };

  const handleEdit = async () => {
    if (!editName.trim()) return;
    setSaving(true);
    try {
      // Playtest #15: bestehende Metadata mergen statt ersetzen — sonst gehen
      // services_offered/price_modifier/occupation/greeting/relationships verloren.
      let metadata: Record<string, unknown> = {};
      try {
        metadata = JSON.parse(npc.metadataJson) as Record<string, unknown>;
      } catch {
        /* */
      }
      const setOrDelete = (key: string, value: unknown) => {
        if (value === undefined || value === null || value === '') delete metadata[key];
        else metadata[key] = value;
      };
      setOrDelete('personality', editPersonality || undefined);
      setOrDelete(
        'knowledge',
        editKnowledge ? editKnowledge.split(',').map((s) => s.trim()).filter(Boolean) : undefined,
      );
      setOrDelete(
        'goals',
        editGoals ? editGoals.split(',').map((s) => s.trim()).filter(Boolean) : undefined,
      );
      setOrDelete('location_id', editLocationId || undefined);
      setOrDelete('services_offered', editServices.length > 0 ? editServices : undefined);
      setOrDelete(
        'price_modifier',
        editPriceMod.trim() !== '' && !Number.isNaN(Number(editPriceMod)) ? Number(editPriceMod) : undefined,
      );

      await apiClient.patch(`/worlds/${worldId}/entities/${npc.id}`, {
        name: editName.trim(),
        age: editAge,
        experienceLevel: editExperienceLevel || null,
        socialStanding: editSocialStanding || null,
        backstory: editBackstory || null,
        factionId: editFactionId || null,
        metadataJson: JSON.stringify(metadata),
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
    try {
      await apiClient.delete(`/worlds/${worldId}/entities/${npc.id}`);
      navigate(`/worlds/${worldId}`);
    } catch {
      /* */
    }
  };

  const handleGrantXp = async () => {
    if (!npc) return;
    setGranting(true);
    try {
      // Neuen XP-Gesamtwert holen (additiv), dann per PATCH setzen
      const current = npc.experiencePoints ?? 0;
      await apiClient.patch(`/entities/${npc.id}/progression`, {
        experience_points: current + xpAmount,
      });
      toast.success(`${xpAmount} XP granted`);
      refetch();
    } catch {
      toast.error('Failed to grant XP');
    } finally {
      setGranting(false);
    }
  };

  return (
    <div className="min-h-screen bg-bg-primary">
      <header className="flex items-center gap-3 border-b border-bg-elevated bg-bg-surface px-6 py-3">
        <button onClick={() => navigate(-1)} className="text-text-secondary hover:text-accent">
          <ArrowLeft size={20} />
        </button>
        <h1 className="text-lg font-heading text-text-primary">{npc.name}</h1>
        {occupation && (
          <span className="flex items-center gap-1 rounded bg-accent/15 px-2 py-0.5 text-xs text-accent">
            <Briefcase size={12} /> {occupation}
          </span>
        )}
        <button onClick={openEdit} className="text-xs text-accent hover:text-accent/60 ml-auto">
          Edit
        </button>
        {isDm && (
          <div className="flex items-center gap-1">
            <input
              type="number"
              min={1}
              value={xpAmount}
              onChange={(e) => setXpAmount(Number(e.target.value))}
              className="w-14 rounded border border-bg-elevated bg-bg-primary px-1.5 py-1 text-xs text-text-primary outline-none focus:border-accent"
            />
            <button
              onClick={handleGrantXp}
              disabled={granting}
              className="text-xs text-warning hover:text-warning/60 disabled:opacity-40"
            >
              +XP
            </button>
          </div>
        )}
        <button
          onClick={() => setShowDelete(true)}
          className="text-xs text-danger hover:text-danger/60"
        >
          Delete
        </button>
      </header>

      <main className="mx-auto max-w-4xl grid grid-cols-1 gap-6 p-6 lg:grid-cols-3">
        {/* Left: Bio */}
        <div className="lg:col-span-2 space-y-6">
          {greeting && (
            <div className="rounded-lg border border-accent/20 bg-accent/5 p-4">
              <p className="text-sm italic text-accent">"{greeting}"</p>
            </div>
          )}

          <section className="rounded-lg border border-bg-elevated bg-bg-surface p-5">
            <h2 className="mb-3 font-heading text-text-primary">Info</h2>
            <dl className="space-y-2 text-sm">
              <div className="flex justify-between">
                <dt className="text-text-secondary">Type</dt>
                <dd className="text-text-primary">{npc.entityType}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-text-secondary">Age</dt>
                <dd className="text-text-primary">{npc.age ?? '—'}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-text-secondary">Experience</dt>
                <dd className="text-text-primary capitalize">{npc.experienceLevel ?? '—'}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-text-secondary">Standing</dt>
                <dd className="text-text-primary capitalize">{npc.socialStanding ?? '—'}</dd>
              </div>
              {faction && (
                <div className="flex justify-between">
                  <dt className="text-text-secondary">Faction</dt>
                  <dd className="flex items-center gap-1 text-text-primary">
                    <span
                      className="inline-block h-2 w-2 rounded-full"
                      style={{ backgroundColor: faction.color }}
                    />
                    {faction.name}
                  </dd>
                </div>
              )}

              {/* Faction Relations */}
              {faction && relations && relations.length > 0 && (
                <div className="border-t border-bg-elevated pt-2 mt-2">
                  <p className="text-xs font-semibold text-text-secondary mb-1">Diplomacy</p>
                  {relations.map((rel) => {
                    const otherId =
                      rel.factionAId === faction.id ? rel.factionBId : rel.factionAId;
                    return (
                      <div
                        key={rel.id}
                        className="flex items-center gap-2 text-xs text-text-secondary py-0.5"
                      >
                        {RELATION_ICONS[rel.relationStatus] ?? <Minus size={14} />}
                        <span>{factions.find((f) => f.id === otherId)?.name ?? `${otherId.slice(0, 8)}…`}</span>
                        <span
                          className={
                            rel.relationStatus === 'WAR'
                              ? 'text-danger'
                              : rel.relationStatus === 'ALLIANCE'
                                ? 'text-success'
                                : rel.relationStatus === 'FRIENDLY'
                                  ? 'text-accent'
                                  : rel.relationStatus === 'UNFRIENDLY'
                                    ? 'text-yellow-500'
                                    : ''
                          }
                        >
                          {rel.relationStatus}
                        </span>
                      </div>
                    );
                  })}
                </div>
              )}
              {priceMod !== undefined && priceMod !== 1.0 && (
                <div className="flex justify-between">
                  <dt className="text-text-secondary">Price modifier</dt>
                  <dd className="text-text-primary">×{priceMod}</dd>
                </div>
              )}
            </dl>
          </section>

          {/* Backstory */}
          {npc.backstory && (
            <section className="rounded-lg border border-bg-elevated bg-bg-surface p-5">
              <h2 className="mb-3 font-heading text-text-primary">Backstory</h2>
              <p className="text-sm text-text-secondary leading-relaxed">{npc.backstory}</p>
            </section>
          )}

          {/* Services */}
          {services.length > 0 && (
            <section className="rounded-lg border border-bg-elevated bg-bg-surface p-5">
              <h2 className="mb-3 font-heading text-text-primary">Services</h2>
              <div className="flex flex-wrap gap-2">
                {services.map((s) => (
                  <span
                    key={s}
                    className="rounded bg-bg-elevated px-3 py-1 text-xs text-text-primary"
                  >
                    {s}
                  </span>
                ))}
              </div>
            </section>
          )}
        </div>

        {/* Right: Relationships + Timeline */}
        <div className="space-y-6">
          {/* Relationships */}
          {Object.keys(relationships).length > 0 && (
            <section className="rounded-lg border border-bg-elevated bg-bg-surface p-5">
              <h2 className="mb-3 font-heading text-text-primary">Relationships</h2>
              <div className="space-y-2">
                {Object.entries(relationships).map(([id, rel]) => (
                  <div key={id} className="flex items-center gap-2 text-sm">
                    {rel === 'likes' ? (
                      <Heart size={14} className="text-danger" />
                    ) : (
                      <HeartOff size={14} className="text-text-secondary" />
                    )}
                    <span className="text-text-primary">{id.slice(0, 12)}…</span>
                    <span className="text-text-secondary">{rel}</span>
                  </div>
                ))}
              </div>
            </section>
          )}

          {/* NPC Adventures */}
          {npcAdventures.length > 0 && (
            <section className="rounded-lg border border-accent/20 bg-accent/5 p-5">
              <h3 className="mb-3 flex items-center gap-2 font-heading text-text-primary">
                <span>🗺️</span> Adventures
              </h3>
              <div className="space-y-2">
                {npcAdventures.map((adv) => (
                  <button
                    key={adv.id}
                    onClick={() => navigate(`/worlds/${worldId}/adventures/${adv.id}`)}
                    className="w-full rounded border border-accent/20 bg-bg-surface px-4 py-2 text-left text-sm text-text-primary hover:border-accent"
                  >
                    {adv.name}
                  </button>
                ))}
              </div>
            </section>
          )}

          {/* Timeline */}
          <section className="rounded-lg border border-bg-elevated bg-bg-surface p-5">
            <EntityTimeline entityType="npc" entityId={npc.id} />
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
            <h3 className="font-heading text-text-primary mb-4">{t('entity.editNpc')}</h3>
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
                <label className="block text-xs text-text-secondary mb-1">{t('entity.age')}</label>
                <input
                  type="number"
                  min={0}
                  value={editAge ?? ''}
                  onChange={(e) => setEditAge(e.target.value ? Number(e.target.value) : undefined)}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                />
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">{t('entity.experience')}</label>
                <input
                  value={editExperienceLevel}
                  onChange={(e) => setEditExperienceLevel(e.target.value)}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                />
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">{t('entity.standing')}</label>
                <input
                  value={editSocialStanding}
                  onChange={(e) => setEditSocialStanding(e.target.value)}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                />
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">{t('entity.faction')}</label>
                <select
                  value={editFactionId}
                  onChange={(e) => setEditFactionId(e.target.value)}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                >
                  <option value="">{t('entity.none')}</option>
                  {factions.map((f) => (
                    <option key={f.id} value={f.id}>
                      {f.name}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">{t('entity.location')}</label>
                <select
                  value={editLocationId}
                  onChange={(e) => setEditLocationId(e.target.value)}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                >
                  <option value="">{t('entity.none')}</option>
                  {factionLocations.map((l) => (
                    <option key={l.id} value={l.id}>
                      {l.name}
                      {l.regionName ? ` (${l.regionName})` : ''}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">{t('entity.personality')}</label>
                <input
                  value={editPersonality}
                  onChange={(e) => setEditPersonality(e.target.value)}
                  placeholder={t('entity.personality_placeholder')}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                />
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">
                  {t('entity.knowledge')}
                </label>
                <input
                  value={editKnowledge}
                  onChange={(e) => setEditKnowledge(e.target.value)}
                  placeholder={t('entity.knowledge_placeholder')}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                />
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">
                  {t('entity.goals')}
                </label>
                <input
                  value={editGoals}
                  onChange={(e) => setEditGoals(e.target.value)}
                  placeholder={t('entity.goals_placeholder')}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                />
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">{t('entity.backstory')}</label>
                <textarea
                  value={editBackstory}
                  onChange={(e) => setEditBackstory(e.target.value)}
                  rows={3}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent resize-none"
                />
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">{t('entity.services')}</label>
                <div className="flex flex-wrap gap-1">
                  {NPC_SERVICES.map((s) => {
                    const on = editServices.includes(s);
                    return (
                      <button
                        key={s}
                        type="button"
                        onClick={() =>
                          setEditServices((prev) =>
                            prev.includes(s) ? prev.filter((x) => x !== s) : [...prev, s],
                          )
                        }
                        className={`rounded px-2 py-0.5 text-xs ${
                          on
                            ? 'bg-accent text-white'
                            : 'bg-bg-elevated text-text-secondary hover:text-text-primary'
                        }`}
                      >
                        {s}
                      </button>
                    );
                  })}
                </div>
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">{t('entity.priceModifier')}</label>
                <input
                  value={editPriceMod}
                  onChange={(e) => setEditPriceMod(e.target.value)}
                  placeholder="1.0"
                  inputMode="decimal"
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
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
                  {saving ? t('entity.saving') : t('actions.save')}
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
            <h3 className="font-heading text-text-primary mb-2">{t('entity.deleteTitle')}</h3>
            <p className="text-sm text-text-secondary mb-4">
              {t('entity.deleteConfirm', { name: npc.name })}
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
                {t('actions.delete')}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
