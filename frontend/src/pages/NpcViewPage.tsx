import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Briefcase, Heart, HeartOff, Swords, Handshake, Minus } from 'lucide-react';
import { apiClient } from '../api/client';
import { useApiGet } from '../hooks/useApiGet';
import { useLazyApiGet } from '../hooks/useLazyApiGet';
import { EntityTimeline } from '../components/world/EntityTimeline';
import { LoadingSpinner } from '../components/ui/LoadingSpinner';

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
  entity_type: string;
  metadata_json: string;
  faction_id: string | null;
  backstory?: string;
  age?: number;
  experience_level?: string;
  social_standing?: string;
}

interface FactionData {
  id: string;
  name: string;
  color: string;
}

interface FactionRelation {
  id: string;
  faction_a_id: string;
  faction_b_id: string;
  relation_status: string;
}

const RELATION_ICONS: Record<string, JSX.Element> = {
  ALLIANCE: <Handshake size={14} className="text-success" />,
  FRIENDLY: <Handshake size={14} className="text-accent" />,
  NEUTRAL: <Minus size={14} className="text-text-secondary" />,
  UNFRIENDLY: <Swords size={14} className="text-yellow-500" />,
  WAR: <Swords size={14} className="text-danger" />,
};

export default function NpcViewPage() {
  const { id, npcId } = useParams<{ id: string; npcId: string }>();
  const navigate = useNavigate();
  const worldId = id ?? '';

  const { data: npc, loading, refetch } = useApiGet<NpcData>(`/entities/${npcId}`, [npcId]);
  const { data: faction } = useApiGet<FactionData>(
    npc?.faction_id ? `/factions/${npc.faction_id}` : '',
    [npc?.faction_id],
  );
  const { data: relations, fetch: fetchRelations } = useLazyApiGet<FactionRelation[]>();

  const [editing, setEditing] = useState(false);
  const [showDelete, setShowDelete] = useState(false);
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
  const [saving, setSaving] = useState(false);

  const [factions, setFactions] = useState<FactionSummary[]>([]);
  const [factionLocations, setFactionLocations] = useState<LocationSummary[]>([]);

  useEffect(() => {
    if (faction?.id) fetchRelations(`/factions/${faction.id}/relations`);
  }, [faction?.id, fetchRelations]);

  useEffect(() => {
    if (!worldId || !editing) return;
    apiClient
      .get(`/worlds/${worldId}/factions`)
      .then((r) => setFactions(r.data))
      .catch(() => {});
    apiClient
      .get(`/worlds/${worldId}/regions`)
      .then(async (regRes) => {
        const regs = regRes.data as { id: string; name: string }[];
        const allLocs: LocationSummary[] = [];
        for (const r of regs) {
          try {
            const locRes = await apiClient.get(`/regions/${r.id}/locations`);
            const locs = (locRes.data as { id: string; name: string }[]).map((l) => ({
              ...l,
              regionName: r.name,
            }));
            allLocs.push(...locs);
          } catch {
            /* */
          }
        }
        setFactionLocations(allLocs);
      })
      .catch(() => {});
  }, [worldId, editing]);

  if (loading || !npc) return <LoadingSpinner size="lg" text="Loading NPC…" />;

  let meta: Record<string, string | string[] | number | undefined> = {};
  try {
    meta = JSON.parse(npc.metadata_json) as Record<string, string | string[] | number | undefined>;
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
    setEditExperienceLevel(npc.experience_level ?? '');
    setEditSocialStanding(npc.social_standing ?? '');
    setEditBackstory(npc.backstory ?? '');
    setEditFactionId(npc.faction_id ?? '');
    setEditLocationId((meta.location_id as string) ?? '');
    setEditPersonality((meta.personality as string) ?? '');
    setEditKnowledge(((meta.knowledge as string[]) ?? []).join(', '));
    setEditGoals(((meta.goals as string[]) ?? []).join(', '));
    setEditing(true);
  };

  const handleEdit = async () => {
    if (!editName.trim()) return;
    setSaving(true);
    try {
      const metadata: Record<string, unknown> = {};
      if (editPersonality) metadata.personality = editPersonality;
      if (editKnowledge)
        metadata.knowledge = editKnowledge
          .split(',')
          .map((s) => s.trim())
          .filter(Boolean);
      if (editGoals)
        metadata.goals = editGoals
          .split(',')
          .map((s) => s.trim())
          .filter(Boolean);
      if (editLocationId) metadata.location_id = editLocationId;

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
                <dd className="text-text-primary">{npc.entity_type}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-text-secondary">Age</dt>
                <dd className="text-text-primary">{npc.age ?? '—'}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-text-secondary">Experience</dt>
                <dd className="text-text-primary capitalize">{npc.experience_level ?? '—'}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-text-secondary">Standing</dt>
                <dd className="text-text-primary capitalize">{npc.social_standing ?? '—'}</dd>
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
                      rel.faction_a_id === faction.id ? rel.faction_b_id : rel.faction_a_id;
                    return (
                      <div
                        key={rel.id}
                        className="flex items-center gap-2 text-xs text-text-secondary py-0.5"
                      >
                        {RELATION_ICONS[rel.relation_status] ?? <Minus size={14} />}
                        <span>{otherId.slice(0, 8)}…</span>
                        <span
                          className={
                            rel.relation_status === 'WAR'
                              ? 'text-danger'
                              : rel.relation_status === 'ALLIANCE'
                                ? 'text-success'
                                : rel.relation_status === 'FRIENDLY'
                                  ? 'text-accent'
                                  : rel.relation_status === 'UNFRIENDLY'
                                    ? 'text-yellow-500'
                                    : ''
                          }
                        >
                          {rel.relation_status}
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
            <h3 className="font-heading text-text-primary mb-4">Edit NPC</h3>
            <div className="space-y-3">
              <div>
                <label className="block text-xs text-text-secondary mb-1">Name</label>
                <input
                  value={editName}
                  onChange={(e) => setEditName(e.target.value)}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                />
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">Age</label>
                <input
                  type="number"
                  min={0}
                  value={editAge ?? ''}
                  onChange={(e) => setEditAge(e.target.value ? Number(e.target.value) : undefined)}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                />
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">Experience Level</label>
                <input
                  value={editExperienceLevel}
                  onChange={(e) => setEditExperienceLevel(e.target.value)}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                />
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">Social Standing</label>
                <input
                  value={editSocialStanding}
                  onChange={(e) => setEditSocialStanding(e.target.value)}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                />
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">Faction</label>
                <select
                  value={editFactionId}
                  onChange={(e) => setEditFactionId(e.target.value)}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                >
                  <option value="">— None —</option>
                  {factions.map((f) => (
                    <option key={f.id} value={f.id}>
                      {f.name}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">Location</label>
                <select
                  value={editLocationId}
                  onChange={(e) => setEditLocationId(e.target.value)}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                >
                  <option value="">— None —</option>
                  {factionLocations.map((l) => (
                    <option key={l.id} value={l.id}>
                      {l.name}
                      {l.regionName ? ` (${l.regionName})` : ''}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">Personality</label>
                <input
                  value={editPersonality}
                  onChange={(e) => setEditPersonality(e.target.value)}
                  placeholder="arrogant, curious, fearful…"
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                />
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">
                  Knowledge (comma-separated)
                </label>
                <input
                  value={editKnowledge}
                  onChange={(e) => setEditKnowledge(e.target.value)}
                  placeholder="goblins, trade, history…"
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                />
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">
                  Goals (comma-separated)
                </label>
                <input
                  value={editGoals}
                  onChange={(e) => setEditGoals(e.target.value)}
                  placeholder="survive, get rich…"
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                />
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">Backstory</label>
                <textarea
                  value={editBackstory}
                  onChange={(e) => setEditBackstory(e.target.value)}
                  rows={3}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent resize-none"
                />
              </div>
              <div className="flex gap-2 pt-2">
                <button
                  onClick={() => setEditing(false)}
                  className="flex-1 rounded border border-bg-elevated py-2 text-sm text-text-secondary hover:text-text-primary"
                >
                  Cancel
                </button>
                <button
                  onClick={handleEdit}
                  disabled={saving || !editName.trim()}
                  className="flex-1 rounded bg-accent py-2 text-sm text-white hover:bg-accent/80 disabled:opacity-40"
                >
                  {saving ? 'Saving…' : 'Save'}
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
            <h3 className="font-heading text-text-primary mb-2">Delete NPC?</h3>
            <p className="text-sm text-text-secondary mb-4">
              This will permanently delete &quot;{npc.name}&quot; and all associated data.
            </p>
            <div className="flex gap-2">
              <button
                onClick={() => setShowDelete(false)}
                className="flex-1 rounded border border-bg-elevated py-2 text-sm text-text-secondary hover:text-text-primary"
              >
                Cancel
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
