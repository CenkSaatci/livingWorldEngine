import { useEffect, useState } from 'react';
import { MapPin, Castle, Building2, ScrollText } from 'lucide-react';
import { apiClient } from '../../api/client';
import { MapCanvas } from '../map/MapCanvas';

interface Region {
  id: string;
  name: string;
  climate: string;
  danger_level: number;
}

interface Location {
  id: string;
  name: string;
  type: string;
  x?: number;
  y?: number;
}

interface Props {
  worldId: string;
  cols?: number;
  rows?: number;
  tileSize?: number;
  onSelectLocation?: (id: string) => void;
}

function locationIcon(type: string) {
  switch (type) {
    case 'city': case 'town': return <Building2 size={18} />;
    case 'castle': return <Castle size={18} />;
    case 'dungeon': case 'ruin': return <ScrollText size={18} />;
    default: return <MapPin size={18} />;
  }
}

export function WorldMapView({ worldId, cols = 24, rows = 18, tileSize = 48, onSelectLocation }: Props) {
  const [regions, setRegions] = useState<Region[]>([]);
  const [locations, setLocations] = useState<Location[]>([]);
  const [selected, setSelected] = useState<string | null>(null);

  useEffect(() => {
    if (!worldId) return;
    apiClient.get(`/worlds/${worldId}/regions`).then(async (regRes) => {
      const regs = regRes.data as Region[];
      setRegions(regs);

      // Locations für alle Regionen laden
      const allLocs: Location[] = [];
      for (const r of regs) {
        try {
          const locRes = await apiClient.get(`/regions/${r.id}/locations`);
          allLocs.push(...locRes.data.map((l: any) => ({ ...l, x: Math.random() * cols * tileSize, y: Math.random() * rows * tileSize })));
        } catch { /* */ }
      }
      setLocations(allLocs);
    }).catch(() => {});
  }, [worldId, cols, rows, tileSize]);

  return (
    <div className="relative h-full w-full">
      <MapCanvas cols={cols} rows={rows} tileSize={tileSize} />

      {/* Location Markers (HTML-Overlay über Canvas) */}
      {locations.map((loc) => (
        <button
          key={loc.id}
          onClick={() => { setSelected(loc.id); onSelectLocation?.(loc.id); }}
          className={`absolute flex items-center gap-1 rounded-full px-2 py-1 text-xs transition-all hover:scale-110 ${
            selected === loc.id
              ? 'bg-accent text-white shadow-lg z-10'
              : 'bg-bg-surface/80 text-text-secondary hover:bg-accent/20 hover:text-accent'
          }`}
          style={{
            left: loc.x ?? 0,
            top: loc.y ?? 0,
            transform: 'translate(-50%, -50%)',
          }}
          title={loc.name}
        >
          {locationIcon(loc.type)}
          <span className="hidden sm:inline">{loc.name}</span>
        </button>
      ))}

      {/* Region Legend (bottom-left) */}
      {regions.length > 0 && (
        <div className="absolute bottom-3 left-3 rounded-lg bg-bg-surface/90 p-3 text-xs text-text-secondary shadow-lg">
          <p className="font-semibold text-text-primary mb-1">Regions ({regions.length})</p>
          {regions.map((r) => (
            <div key={r.id} className="flex items-center gap-2">
              <span className={`inline-block h-2 w-2 rounded-full ${r.danger_level > 5 ? 'bg-danger' : 'bg-success'}`} />
              {r.name}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
