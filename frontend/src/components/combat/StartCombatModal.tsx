import { useState, useEffect } from 'react';
import { X, Swords } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { apiClient } from '../../api/client';

interface EntitySummary {
  id: string;
  name: string;
  entity_type: string;
}

interface MapSummary {
  id: string;
  name: string;
}

interface Props {
  worldId: string;
  onClose: () => void;
}

export function StartCombatModal({ worldId, onClose }: Props) {
  const { t } = useTranslation('common');
  const navigate = useNavigate();
  const [entities, setEntities] = useState<EntitySummary[]>([]);
  const [maps, setMaps] = useState<MapSummary[]>([]);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [selectedMapId, setSelectedMapId] = useState<string>('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!worldId) return;
    apiClient
      .get(`/worlds/${worldId}/entities`)
      .then((r) => {
        const all = (r.data as EntitySummary[]).filter(
          (e) => e.entity_type === 'PC' || e.entity_type === 'NPC',
        );
        setEntities(all);
      })
      .catch(() => {});
    apiClient
      .get(`/worlds/${worldId}/map`)
      .then((r) => {
        const map = r.data as MapSummary;
        setMaps([map]);
        setSelectedMapId(map.id);
      })
      .catch(() => {});
  }, [worldId]);

  const toggle = (id: string) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const handleStart = async () => {
    if (selectedIds.size < 2) {
      setError(t('combatModal.minParticipants'));
      return;
    }
    setSaving(true);
    setError('');
    try {
      const body: Record<string, unknown> = { worldId, participantIds: [...selectedIds] };
      if (selectedMapId) body.mapId = selectedMapId;
      const res = await apiClient.post('/combat/start', body);
      if (res.status === 201) {
        navigate(`/combat/${worldId}`);
      }
    } catch {
      setError(t('combatModal.startError'));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/60 pt-12"
      onClick={onClose}
    >
      <div
        className="w-full max-w-lg rounded-xl border border-bg-elevated bg-bg-surface p-6 shadow-2xl mb-12"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between mb-5">
          <h2 className="flex items-center gap-2 text-lg font-heading text-text-primary">
            <Swords size={20} className="text-danger" /> {t('combatModal.title')}
          </h2>
          <button onClick={onClose} className="text-text-secondary hover:text-text-primary">
            <X size={20} />
          </button>
        </div>

        <div className="space-y-4">
          {error && (
            <div className="rounded border border-danger/40 bg-danger/10 px-3 py-2 text-sm text-danger">
              {error}
            </div>
          )}

          {/* Entity Selection */}
          <div>
            <label className="block text-xs text-text-secondary mb-2">
              {t('combatModal.selectParticipants')}
            </label>
            <div className="max-h-56 overflow-y-auto space-y-1">
              {entities.map((e) => (
                <label
                  key={e.id}
                  className={`flex cursor-pointer items-center gap-3 rounded px-3 py-2 text-sm hover:bg-bg-elevated ${
                    selectedIds.has(e.id) ? 'bg-accent/10 ring-1 ring-accent/30' : ''
                  }`}
                >
                  <input
                    type="checkbox"
                    checked={selectedIds.has(e.id)}
                    onChange={() => toggle(e.id)}
                    className="accent-accent"
                  />
                  <span className="flex-1 text-text-primary">{e.name || e.id.slice(0, 12)}</span>
                  <span className="text-[10px] uppercase text-text-secondary">{e.entity_type}</span>
                </label>
              ))}
              {entities.length === 0 && (
                <p className="text-xs text-text-secondary">{t('combatModal.noEntities')}</p>
              )}
            </div>
          </div>

          {/* Map Selection */}
          <div>
            <label className="block text-xs text-text-secondary mb-1">
              {t('combatModal.battleMap')}
            </label>
            <select
              value={selectedMapId}
              onChange={(e) => setSelectedMapId(e.target.value)}
              className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
            >
              {maps.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.name}
                </option>
              ))}
            </select>
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
              onClick={handleStart}
              disabled={saving || selectedIds.size < 2}
              className="flex items-center gap-1 rounded bg-danger px-4 py-2 text-sm text-white hover:bg-danger/80 disabled:opacity-40"
            >
              <Swords size={16} />
              {saving ? '…' : t('combatModal.start')}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
