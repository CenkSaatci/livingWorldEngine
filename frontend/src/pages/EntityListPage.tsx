import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Search, Plus, Sparkles, Trash2 } from 'lucide-react';
import { useApiGet } from '../hooks/useApiGet';
import { apiClient } from '../api/client';
import { useToast } from '../hooks/useToast';
import { EntityCreateModal } from '../components/world/EntityCreateModal';
import { CharacterWizard } from '../components/character/CharacterWizard';
import { useActiveCampaign } from '../store/campaignStore';
import { fromRulesJson, type WizardData } from '../types/gameSystem';

interface EntitySummary {
  id: string;
  name: string;
  entityType: string;
  age: number;
  experienceLevel: string;
  socialStanding: string;
  factionId: string;
}

export default function EntityListPage() {
  const { t } = useTranslation('common');
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const toast = useToast();
  const worldId = id ?? '';

  const {
    data: entities,
    loading,
    refetch,
  } = useApiGet<EntitySummary[]>(`/worlds/${worldId}/entities`, [worldId]);

  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState<'ALL' | 'PC' | 'NPC'>('ALL');
  const [showCreate, setShowCreate] = useState(false);
  const [charRules, setCharRules] = useState<WizardData | null>(null);
  const [showWizard, setShowWizard] = useState(false);
  const activeCampaign = useActiveCampaign();
  const activeGameSystemId = activeCampaign?.gameSystemId;

  const filtered = (entities ?? []).filter((e) => {
    if (e.entityType === 'FACTION') return false;
    if (typeFilter !== 'ALL' && e.entityType !== typeFilter) return false;
    if (search && !e.name.toLowerCase().includes(search.toLowerCase())) return false;
    return true;
  });

  // Charakter-Wizard anbieten, wenn das aktive System Erstellungsdaten hat (P30).
  useEffect(() => {
    if (!activeGameSystemId) { setCharRules(null); return; }
    let cancelled = false;
    apiClient.get(`/game-systems/${activeGameSystemId}`).then((res) => {
      if (cancelled) return;
      const parsed = fromRulesJson(res.data.rulesJson ?? '');
      const usable = parsed && (
        parsed.creationBudget != null || (parsed.packages?.length ?? 0) > 0
      );
      setCharRules(usable ? parsed : null);
    }).catch(() => setCharRules(null));
    return () => { cancelled = true; };
  }, [activeGameSystemId]);

  const handleDelete = async (entityId: string, entityName: string) => {
    if (!confirm(`Delete "${entityName || entityId.slice(0, 12)}"?`)) return;
    try {
      await apiClient.delete(`/worlds/${worldId}/entities/${entityId}`);
      toast.success(`Deleted ${entityName || 'entity'}`);
      refetch();
    } catch {
      toast.error('Failed to delete');
    }
  };

  return (
    <div className="flex h-screen flex-col bg-bg-primary">
      {/* Top Bar */}
      <header className="flex items-center justify-between border-b border-bg-elevated bg-bg-surface px-4 py-2">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate(`/worlds/${worldId}`)}
            className="text-text-secondary hover:text-accent"
          >
            <ArrowLeft size={20} />
          </button>
          <h1 className="text-lg font-heading text-text-primary">{t('entityList.title')}</h1>
        </div>
        <div className="flex items-center gap-2">
          {charRules && (
            <button
              onClick={() => setShowWizard(true)}
              className="flex items-center gap-1 rounded border border-accent/50 px-3 py-1.5 text-sm text-accent hover:bg-accent/10"
            >
              <Sparkles size={16} /> {t('entity.createCharacter')}
            </button>
          )}
          <button
            onClick={() => setShowCreate(true)}
            className="flex items-center gap-1 rounded bg-accent px-3 py-1.5 text-sm text-white hover:bg-accent/80"
          >
            <Plus size={16} /> {t('entity.create')}
          </button>
        </div>
      </header>

      {/* Filters */}
      <div className="flex items-center gap-3 border-b border-bg-elevated bg-bg-surface/50 px-4 py-2">
        <div className="relative flex-1 max-w-xs">
          <Search
            size={14}
            className="absolute left-3 top-1/2 -translate-y-1/2 text-text-secondary"
          />
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder={t('entityList.search')}
            className="w-full rounded border border-bg-elevated bg-bg-primary pl-8 pr-3 py-1.5 text-sm text-text-primary outline-none focus:border-accent"
          />
        </div>
        <select
          value={typeFilter}
          onChange={(e) => setTypeFilter(e.target.value as 'ALL' | 'PC' | 'NPC')}
          className="rounded border border-bg-elevated bg-bg-primary px-3 py-1.5 text-sm text-text-primary outline-none focus:border-accent"
        >
          <option value="ALL">{t('entityList.allTypes')}</option>
          <option value="PC">PC</option>
          <option value="NPC">NPC</option>
        </select>
        <span className="text-xs text-text-secondary">
          {filtered.length} / {entities?.length ?? 0}
        </span>
      </div>

      {/* Table */}
      <div className="flex-1 overflow-auto p-4">
        {loading ? (
          <p className="text-sm text-text-secondary">{t('status.loading')}</p>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-bg-elevated text-left text-xs uppercase text-text-secondary">
                <th className="pb-2 pr-4 font-medium">{t('entityList.name')}</th>
                <th className="pb-2 pr-4 font-medium">{t('entityList.type')}</th>
                <th className="pb-2 pr-4 font-medium">{t('entityList.age')}</th>
                <th className="pb-2 pr-4 font-medium">{t('entityList.experience')}</th>
                <th className="pb-2 pr-4 font-medium">{t('entityList.standing')}</th>
                <th className="pb-2 pr-4 font-medium">{t('entityList.faction')}</th>
                <th className="pb-2 font-medium">{t('entityList.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((e) => (
                <tr
                  key={e.id}
                  className="cursor-pointer border-b border-bg-elevated/50 hover:bg-bg-elevated/20"
                  onClick={() => navigate(e.entityType === 'PC' ? `/characters/${e.id}` : `/worlds/${worldId}/npcs/${e.id}`)}
                >
                  <td className="py-2 pr-4 text-text-primary">{e.name || e.id.slice(0, 12)}</td>
                  <td className="py-2 pr-4">
                    <span
                      className={`rounded px-2 py-0.5 text-[10px] font-medium uppercase ${
                        e.entityType === 'PC'
                          ? 'bg-accent/10 text-accent'
                          : 'bg-bg-elevated text-text-secondary'
                      }`}
                    >
                      {e.entityType}
                    </span>
                  </td>
                  <td className="py-2 pr-4 text-text-secondary">{e.age}</td>
                  <td className="py-2 pr-4 text-text-secondary capitalize">{e.experienceLevel}</td>
                  <td className="py-2 pr-4 text-text-secondary capitalize">{e.socialStanding}</td>
                  <td className="py-2 pr-4 text-text-secondary">
                    {e.factionId ? e.factionId.slice(0, 12) : '—'}
                  </td>
                  <td className="py-2">
                    <button
                      onClick={(ev) => {
                        ev.stopPropagation();
                        handleDelete(e.id, e.name);
                      }}
                      className="rounded p-1 text-text-secondary hover:text-danger hover:bg-danger/10"
                      aria-label={t('actions.delete')}
                    >
                      <Trash2 size={14} />
                    </button>
                  </td>
                </tr>
              ))}
              {filtered.length === 0 && (
                <tr>
                  <td colSpan={7} className="py-8 text-center text-sm text-text-secondary">
                    {t('entityList.empty')}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        )}
      </div>

      {showWizard && charRules && (
        <CharacterWizard
          worldId={worldId}
          rules={charRules}
          onCreated={(id) => {
            setShowWizard(false);
            refetch();
            navigate(`/characters/${id}`);
          }}
          onClose={() => setShowWizard(false)}
        />
      )}

      {showCreate && (
        <EntityCreateModal
          worldId={worldId}
          onCreated={(id, entityType) => {
            setShowCreate(false);
            refetch();
            if (entityType === 'PC') {
              navigate(`/characters/${id}`);
            }
          }}
          onClose={() => setShowCreate(false)}
        />
      )}
    </div>
  );
}
