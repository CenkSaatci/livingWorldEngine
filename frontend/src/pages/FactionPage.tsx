import { useEffect, useState, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Plus, Swords, Handshake, Minus, Check, X } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { apiClient } from '../api/client';
import { useFactionStore, type Faction, type FactionRelation } from '../store/factionStore';
import { useApiGet } from '../hooks/useApiGet';
import { useLazyApiGet } from '../hooks/useLazyApiGet';
import { LoadingSpinner } from '../components/ui/LoadingSpinner';

const STATUS_ICONS: Record<string, JSX.Element> = {
  ALLIANCE: <Handshake size={14} className="text-success" />,
  FRIENDLY: <Handshake size={14} className="text-accent" />,
  NEUTRAL: <Minus size={14} className="text-text-secondary" />,
  UNFRIENDLY: <Swords size={14} className="text-yellow-500" />,
  WAR: <Swords size={14} className="text-danger" />,
};

const STATUS_ORDER = ['ALLIANCE', 'FRIENDLY', 'NEUTRAL', 'UNFRIENDLY', 'WAR'] as const;

function nextStatus(current: string): string {
  const idx = STATUS_ORDER.indexOf(current as (typeof STATUS_ORDER)[number]);
  if (idx < 0 || idx >= STATUS_ORDER.length - 1) return 'ALLIANCE';
  return STATUS_ORDER[idx + 1];
}

export default function FactionPage() {
  const { t } = useTranslation('common');
  const { worldId } = useParams<{ worldId: string }>();
  const navigate = useNavigate();
  const [showCreate, setShowCreate] = useState(false);
  const [newName, setNewName] = useState('');
  const [newColor, setNewColor] = useState('#888888');
  const [expanded, setExpanded] = useState<Record<string, boolean>>({});

  const [editingFaction, setEditingFaction] = useState<string | null>(null);
  const [editName, setEditName] = useState('');
  const [editColor, setEditColor] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [showDeleteFaction, setShowDeleteFaction] = useState<string | null>(null);

  const factions = useFactionStore((s) => s.factions);
  const relations = useFactionStore((s) => s.relations);
  const setFactions = useFactionStore((s) => s.setFactions);
  const addFaction = useFactionStore((s) => s.addFaction);
  const removeFaction = useFactionStore((s) => s.removeFaction);
  const setRelations = useFactionStore((s) => s.setRelations);

  const { data: factionData, loading } = useApiGet<Faction[]>(`/worlds/${worldId}/factions`, [
    worldId,
  ]);

  useEffect(() => {
    if (factionData) setFactions(factionData);
  }, [factionData, setFactions]);

  const { fetch: fetchRelations } = useLazyApiGet<FactionRelation[]>();

  const handleCreate = async () => {
    if (!newName.trim()) return;
    try {
      const res = await apiClient.post(`/worlds/${worldId}/factions`, {
        name: newName.trim(),
        color: newColor,
      });
      addFaction(res.data as Faction);
      setNewName('');
      setShowCreate(false);
    } catch {
      /* */
    }
  };

  const toggleExpand = async (factionId: string) => {
    const next = { ...expanded, [factionId]: !expanded[factionId] };
    setExpanded(next);
    if (next[factionId] && !relations[factionId]) {
      const rels = await fetchRelations(`/factions/${factionId}/relations`);
      if (rels) setRelations(factionId, rels);
    }
  };

  const handleCycleStatus = async (aId: string, bId: string, current: string) => {
    const next = nextStatus(current);
    try {
      await apiClient.post(`/factions/${aId}/relations/${bId}`, { status: next });
      const r = await apiClient.get(`/factions/${aId}/relations`);
      setRelations(aId, r.data as FactionRelation[]);
    } catch {
      /* */
    }
  };

  const openEdit = (f: Faction) => {
    setEditingFaction(f.id);
    setEditName(f.name);
    setEditColor(f.color);
    setEditDescription(f.description ?? '');
  };

  const handleEditSave = async () => {
    if (!editingFaction || !editName.trim()) return;
    try {
      await apiClient.put(`/factions/${editingFaction}`, {
        name: editName.trim(),
        color: editColor,
        description: editDescription || null,
        leaderEntityId: null,
      });
      setEditingFaction(null);
      const res = await apiClient.get(`/worlds/${worldId}/factions`);
      setFactions(res.data as Faction[]);
    } catch {
      /* */
    }
  };

  const handleDeleteConfirm = async (factionId: string) => {
    try {
      await apiClient.delete(`/factions/${factionId}`);
      removeFaction(factionId);
      setShowDeleteFaction(null);
    } catch {
      /* */
    }
  };

  const relationMap = useMemo(() => {
    const map = new Map<string, string>();
    for (const [, rels] of Object.entries(relations)) {
      for (const r of rels) {
        map.set(`${r.factionAId}:${r.factionBId}`, r.relationStatus);
        map.set(`${r.factionBId}:${r.factionAId}`, r.relationStatus);
      }
    }
    return map;
  }, [relations]);

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
          <button onClick={() => navigate(-1)} className="text-text-secondary hover:text-accent">
            <ArrowLeft size={20} />
          </button>
          <h1 className="text-lg font-heading text-text-primary">{t('faction.title')}</h1>
        </div>
        <button
          onClick={() => setShowCreate(true)}
          className="flex items-center gap-1 rounded bg-accent px-3 py-1.5 text-xs text-white hover:bg-accent/80"
        >
          <Plus size={14} /> {t('faction.new')}
        </button>
      </header>

      <main className="mx-auto max-w-4xl p-6">
        {/* Create Form */}
        {showCreate && (
          <div className="mb-6 flex items-center gap-3 rounded-lg bg-bg-surface p-4">
            <input
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              placeholder={t('faction.namePlaceholder')}
              className="flex-1 rounded border border-bg-elevated bg-bg-primary px-3 py-1.5 text-sm text-text-primary outline-none focus:border-accent"
            />
            <input
              type="color"
              value={newColor}
              onChange={(e) => setNewColor(e.target.value)}
              className="h-8 w-8 cursor-pointer rounded border border-bg-elevated"
            />
            <button
              onClick={handleCreate}
              className="rounded bg-accent px-3 py-1.5 text-xs text-white"
            >
              {t('actions.create')}
            </button>
            <button
              onClick={() => setShowCreate(false)}
              className="text-xs text-text-secondary hover:text-text-primary"
            >
              {t('actions.cancel')}
            </button>
          </div>
        )}

        {/* Faction List */}
        <div className="space-y-3">
          {factions.map((f) => (
            <div key={f.id} className="rounded-lg border border-bg-elevated bg-bg-surface">
              <div className="flex items-center gap-3 px-4 py-3">
                <button
                  onClick={() => toggleExpand(f.id)}
                  className="flex items-center gap-3 flex-1 text-left hover:opacity-80"
                >
                  <span
                    className="inline-block h-4 w-4 shrink-0 rounded-full"
                    style={{ backgroundColor: f.color }}
                  />
                  <span className="font-medium text-text-primary">{f.name}</span>
                  {f.leaderEntityId && (
                    <span className="text-xs text-text-secondary">{t('faction.hasLeader')}</span>
                  )}
                </button>
                <button
                  onClick={() => openEdit(f)}
                  className="text-xs text-accent hover:text-accent/60"
                >
                  {t('actions.edit')}
                </button>
                <button
                  onClick={() => setShowDeleteFaction(f.id)}
                  className="text-xs text-danger hover:text-danger/60"
                >
                  {t('actions.delete')}
                </button>
              </div>

              {expanded[f.id] && (
                <div className="border-t border-bg-elevated px-4 py-3">
                  {editingFaction === f.id ? (
                    <div className="mb-3 space-y-2">
                      <div className="flex items-center gap-2">
                        <input
                          value={editName}
                          onChange={(e) => setEditName(e.target.value)}
                          className="flex-1 rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-sm text-text-primary outline-none focus:border-accent"
                        />
                        <input
                          type="color"
                          value={editColor}
                          onChange={(e) => setEditColor(e.target.value)}
                          className="h-7 w-7 cursor-pointer rounded border border-bg-elevated"
                        />
                        <button
                          onClick={handleEditSave}
                          className="text-success hover:text-success/60"
                        >
                          <Check size={16} />
                        </button>
                        <button
                          onClick={() => setEditingFaction(null)}
                          className="text-text-secondary hover:text-text-primary"
                        >
                          <X size={16} />
                        </button>
                      </div>
                      <textarea
                        value={editDescription}
                        onChange={(e) => setEditDescription(e.target.value)}
                        rows={2}
                        placeholder="Description…"
                        className="w-full rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-sm text-text-primary outline-none focus:border-accent resize-none"
                      />
                    </div>
                  ) : (
                    <div className="flex items-center gap-2 mb-3">
                      {f.description && (
                        <p className="flex-1 text-sm text-text-secondary">{f.description}</p>
                      )}
                      <button
                        onClick={() => openEdit(f)}
                        className="text-xs text-accent hover:text-accent/60"
                      >
                        {t('actions.edit')}
                      </button>
                      <button
                        onClick={() => setShowDeleteFaction(f.id)}
                        className="text-xs text-danger hover:text-danger/60"
                      >
                        {t('actions.delete')}
                      </button>
                    </div>
                  )}

                  <p className="mb-2 text-xs font-semibold text-text-secondary uppercase tracking-wide">
                    {t('diplomacy')}
                  </p>

                  {factions
                    .filter((other) => other.id !== f.id)
                    .map((other) => {
                      const status = relationMap.get(`${f.id}:${other.id}`) ?? 'NEUTRAL';
                      return (
                        <div key={other.id} className="flex items-center gap-3 py-1.5">
                          <span
                            className="inline-block h-2 w-2 shrink-0 rounded-full"
                            style={{ backgroundColor: other.color }}
                          />
                          <span className="flex-1 text-sm text-text-primary">{other.name}</span>
                          <div className="flex items-center gap-2">
                            {STATUS_ICONS[status] ?? null}
                            <span
                              className={`text-xs ${
                                status === 'ALLIANCE'
                                  ? 'text-success'
                                  : status === 'WAR'
                                    ? 'text-danger'
                                    : status === 'FRIENDLY'
                                      ? 'text-accent'
                                      : status === 'UNFRIENDLY'
                                        ? 'text-yellow-500'
                                        : 'text-text-secondary'
                              }`}
                            >
                              {status}
                            </span>
                            <button
                              onClick={() => handleCycleStatus(f.id, other.id, status)}
                              className="rounded bg-bg-elevated px-2 py-0.5 text-[10px] text-text-secondary hover:text-text-primary"
                            >
                              Cycle
                            </button>
                          </div>
                        </div>
                      );
                    })}
                </div>
              )}
            </div>
          ))}

          {factions.length === 0 && (
            <p className="py-12 text-center text-sm text-text-secondary">
              {t('faction.empty')}
            </p>
          )}
        </div>
      </main>

      {/* Delete Confirm */}
      {showDeleteFaction && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/60"
          onClick={() => setShowDeleteFaction(null)}
        >
          <div
            className="w-72 rounded-xl border border-bg-elevated bg-bg-surface p-5 shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <h3 className="font-heading text-text-primary mb-2">{t('faction.deleteTitle')}</h3>
            <p className="text-sm text-text-secondary mb-4">
              {t('faction.deleteConfirm')}
            </p>
            <div className="flex gap-2">
              <button
                onClick={() => setShowDeleteFaction(null)}
                className="flex-1 rounded border border-bg-elevated py-2 text-sm text-text-secondary hover:text-text-primary"
              >
                {t('actions.cancel')}
              </button>
              <button
                onClick={() => handleDeleteConfirm(showDeleteFaction)}
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
