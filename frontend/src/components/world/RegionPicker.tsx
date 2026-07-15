import { useEffect, useState } from 'react';
import { X } from 'lucide-react';
import { apiClient } from '../../api/client';

interface RegionItem {
  id: string;
  name: string;
}

interface Props {
  worldId: string;
  onSelect: (region: RegionItem) => void;
  onClose: () => void;
}

export function RegionPicker({ worldId, onSelect, onClose }: Props) {
  const [regions, setRegions] = useState<RegionItem[]>([]);

  useEffect(() => {
    if (!worldId) return;
    apiClient
      .get(`/worlds/${worldId}/regions`)
      .then((r) => setRegions(r.data))
      .catch(() => {});
  }, [worldId]);

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60"
      onClick={onClose}
    >
      <div
        className="w-72 rounded-xl border border-bg-elevated bg-bg-surface p-5 shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-heading text-text-primary">Select Region</h3>
          <button onClick={onClose} className="text-text-secondary hover:text-text-primary">
            <X size={18} />
          </button>
        </div>
        <div className="space-y-1 max-h-60 overflow-y-auto">
          {regions.map((r) => (
            <button
              key={r.id}
              onClick={() => onSelect(r)}
              className="w-full rounded px-3 py-2 text-left text-sm text-text-primary hover:bg-bg-elevated/50"
            >
              {r.name}
            </button>
          ))}
          {regions.length === 0 && (
            <p className="text-xs text-text-secondary text-center py-4">
              No regions yet. Create one first.
            </p>
          )}
        </div>
      </div>
    </div>
  );
}
