import { useEffect, useState } from 'react';
import { ChevronRight, ChevronDown, MapPin, Castle, Building2, Trees, ScrollText } from 'lucide-react';
import { apiClient } from '../../api/client';

interface Region {
  id: string;
  name: string;
  danger_level: number;
  climate: string;
}

interface Location {
  id: string;
  name: string;
  type: string;
  wealth: number;
}

function locationIcon(type: string) {
  switch (type) {
    case 'city': case 'town': return <Building2 size={16} />;
    case 'castle': return <Castle size={16} />;
    case 'dungeon': case 'ruin': return <ScrollText size={16} />;
    default: return <MapPin size={16} />;
  }
}

interface Props {
  worldId: string;
  onSelectRegion?: (id: string) => void;
  onSelectLocation?: (id: string) => void;
}

export function RegionTree({ worldId, onSelectLocation }: Props) {
  const [regions, setRegions] = useState<Region[]>([]);
  const [expanded, setExpanded] = useState<Record<string, boolean>>({});
  const [locations, setLocations] = useState<Record<string, Location[]>>({});

  useEffect(() => {
    apiClient.get(`/worlds/${worldId}/regions`).then((r) => setRegions(r.data)).catch(() => {});
  }, [worldId]);

  const toggleRegion = async (regionId: string) => {
    const next = { ...expanded, [regionId]: !expanded[regionId] };
    setExpanded(next);
    if (next[regionId] && !locations[regionId]) {
      try {
        const r = await apiClient.get(`/regions/${regionId}/locations`);
        setLocations((prev) => ({ ...prev, [regionId]: r.data }));
      } catch { /* */ }
    }
  };

  return (
    <div className="space-y-1">
      {regions.map((r) => (
        <div key={r.id}>
          <button
            onClick={() => toggleRegion(r.id)}
            className="flex w-full items-center gap-2 rounded px-2 py-1.5 text-sm text-text-primary hover:bg-bg-elevated/50"
          >
            {expanded[r.id] ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
            <Trees size={16} className="text-accent" />
            <span className="flex-1 text-left">{r.name}</span>
            <span className={`text-xs ${r.danger_level > 5 ? 'text-danger' : 'text-text-secondary'}`}>
              ⚔️ {r.danger_level}
            </span>
          </button>

          {expanded[r.id] && (
            <div className="ml-4 space-y-0.5 border-l border-bg-elevated pl-2">
              {(locations[r.id] ?? []).length === 0 && (
                <p className="py-1 text-xs text-text-secondary">Loading…</p>
              )}
              {(locations[r.id] ?? []).map((loc) => (
                <button
                  key={loc.id}
                  onClick={() => onSelectLocation?.(loc.id)}
                  className="flex w-full items-center gap-2 rounded px-2 py-1 text-xs text-text-secondary hover:bg-bg-elevated/50 hover:text-text-primary"
                >
                  {locationIcon(loc.type)}
                  <span className="flex-1 text-left">{loc.name}</span>
                  <span className="text-yellow-500">{'💰'.repeat(Math.ceil(loc.wealth / 3))}</span>
                </button>
              ))}
            </div>
          )}
        </div>
      ))}

      {regions.length === 0 && (
        <p className="px-2 py-4 text-xs text-text-secondary text-center">No regions yet</p>
      )}
    </div>
  );
}
