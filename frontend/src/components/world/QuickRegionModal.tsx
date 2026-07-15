import { useState } from 'react';
import { X } from 'lucide-react';
import { apiClient } from '../../api/client';

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

interface Props {
  worldId: string;
  onCreated: () => void;
  onClose: () => void;
}

export function QuickRegionModal({ worldId, onCreated, onClose }: Props) {
  const [name, setName] = useState('');
  const [climate, setClimate] = useState('temperate');
  const [dangerLevel, setDangerLevel] = useState(3);
  const [saving, setSaving] = useState(false);

  const handleSave = async () => {
    if (!name.trim()) return;
    setSaving(true);
    try {
      await apiClient.post(`/worlds/${worldId}/regions`, {
        name: name.trim(),
        climate,
        dangerLevel,
      });
      onCreated();
      onClose();
    } catch {
      /* */
    } finally {
      setSaving(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60"
      onClick={onClose}
    >
      <div
        className="w-80 rounded-xl border border-bg-elevated bg-bg-surface p-5 shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-heading text-text-primary">New Region</h3>
          <button onClick={onClose} className="text-text-secondary hover:text-text-primary">
            <X size={18} />
          </button>
        </div>
        <div className="space-y-3">
          <div>
            <label className="block text-xs text-text-secondary mb-1">Name</label>
            <input
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
              autoFocus
            />
          </div>
          <div>
            <label className="block text-xs text-text-secondary mb-1">Climate</label>
            <select
              value={climate}
              onChange={(e) => setClimate(e.target.value)}
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
            <label className="block text-xs text-text-secondary mb-1">Danger Level (1-10)</label>
            <input
              type="range"
              min={1}
              max={10}
              value={dangerLevel}
              onChange={(e) => setDangerLevel(Number(e.target.value))}
              className="w-full accent-accent"
            />
            <span className="text-xs text-text-secondary">{dangerLevel}/10</span>
          </div>
          <button
            onClick={handleSave}
            disabled={saving || !name.trim()}
            className="w-full rounded bg-accent py-2 text-sm text-white hover:bg-accent/80 disabled:opacity-40"
          >
            {saving ? 'Creating…' : 'Create Region'}
          </button>
        </div>
      </div>
    </div>
  );
}
