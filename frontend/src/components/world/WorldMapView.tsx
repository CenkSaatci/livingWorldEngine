import { useEffect, useState } from 'react';
import {
  MapPin,
  Castle,
  Building2,
  ScrollText,
  Sun,
  Cloud,
  CloudRain,
  CloudSnow,
  CloudFog,
  CloudLightning,
  Wind,
  Thermometer,
} from 'lucide-react';
import { apiClient } from '../../api/client';
import { MapCanvas } from '../map/MapCanvas';
import { useApiGet } from '../../hooks/useApiGet';

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

interface WeatherData {
  regionId: string;
  weatherType: string;
  temperature: number;
  wind: number;
  description: string;
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
    case 'city':
    case 'town':
      return <Building2 size={18} />;
    case 'castle':
      return <Castle size={18} />;
    case 'dungeon':
    case 'ruin':
      return <ScrollText size={18} />;
    default:
      return <MapPin size={18} />;
  }
}

function weatherIcon(type: string, size = 14) {
  switch (type) {
    case 'CLEAR':
      return <Sun size={size} className="text-yellow-400" />;
    case 'CLOUDY':
      return <Cloud size={size} className="text-text-secondary" />;
    case 'RAIN':
      return <CloudRain size={size} className="text-blue-400" />;
    case 'STORM':
      return <CloudLightning size={size} className="text-purple-400" />;
    case 'FOG':
      return <CloudFog size={size} className="text-text-secondary" />;
    case 'SNOW':
      return <CloudSnow size={size} className="text-blue-200" />;
    case 'WINDY':
      return <Wind size={size} className="text-text-secondary" />;
    case 'EXTREME_HEAT':
      return <Thermometer size={size} className="text-red-500" />;
    default:
      return <Sun size={size} className="text-yellow-400" />;
  }
}

function weatherOverlayClass(weatherType?: string) {
  switch (weatherType) {
    case 'RAIN':
      return 'bg-blue-500/5 pointer-events-none';
    case 'STORM':
      return 'bg-purple-500/8 pointer-events-none';
    case 'FOG':
      return 'bg-white/10 pointer-events-none';
    case 'SNOW':
      return 'bg-blue-200/8 pointer-events-none';
    default:
      return '';
  }
}

export function WorldMapView({
  worldId,
  cols = 24,
  rows = 18,
  tileSize = 48,
  onSelectLocation,
}: Props) {
  const [locations, setLocations] = useState<Location[]>([]);
  const [weather, setWeather] = useState<Record<string, WeatherData>>({});
  const [selected, setSelected] = useState<string | null>(null);

  const { data: regions } = useApiGet<Region[]>(`/worlds/${worldId}/regions`, [worldId]);

  useEffect(() => {
    if (!worldId || !regions || regions.length === 0) return;
    let cancelled = false;

    Promise.all([
      Promise.allSettled(
        regions.map(async (r) => {
          const wr = await apiClient.get(`/regions/${r.id}/weather`);
          return { id: r.id, data: wr.data };
        }),
      ),
      Promise.allSettled(
        regions.map(async (r) => {
          const locRes = await apiClient.get(`/regions/${r.id}/locations`);
          return { id: r.id, data: locRes.data as Location[] };
        }),
      ),
    ]).then(([weatherResults, locationResults]) => {
      if (cancelled) return;

      const weatherMap: Record<string, WeatherData> = {};
      for (const result of weatherResults) {
        if (result.status === 'fulfilled' && result.value.data) {
          weatherMap[result.value.id] = { regionId: result.value.id, ...result.value.data };
        }
      }
      setWeather(weatherMap);

      const allLocs: Location[] = [];
      for (const result of locationResults) {
        if (result.status === 'fulfilled') {
          allLocs.push(
            ...result.value.data.map((l) => ({
              ...l,
              x: Math.random() * cols * tileSize,
              y: Math.random() * rows * tileSize,
            })),
          );
        }
      }
      setLocations(allLocs);
    });

    return () => {
      cancelled = true;
    };
  }, [worldId, regions, cols, rows, tileSize]);

  // Dominantes Wetter für Fullscreen-Overlay
  const dominantWeather = Object.values(weather)[0]?.weatherType;

  return (
    <div className={`relative h-full w-full ${weatherOverlayClass(dominantWeather)}`}>
      <MapCanvas cols={cols} rows={rows} tileSize={tileSize} />

      {/* Location Markers */}
      {locations.map((loc) => (
        <button
          key={loc.id}
          onClick={() => {
            setSelected(loc.id);
            onSelectLocation?.(loc.id);
          }}
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

      {/* Region + Weather Legend */}
      {regions && regions.length > 0 && (
        <div className="absolute bottom-3 left-3 max-h-60 overflow-y-auto rounded-lg bg-bg-surface/90 p-3 text-xs text-text-secondary shadow-lg">
          <p className="font-semibold text-text-primary mb-1">Regions ({regions.length})</p>
          {regions.map((r) => {
            const w = weather[r.id];
            return (
              <div key={r.id} className="flex items-center gap-2 py-0.5">
                <span
                  className={`inline-block h-2 w-2 shrink-0 rounded-full ${r.danger_level > 5 ? 'bg-danger' : 'bg-success'}`}
                />
                <span className="truncate">{r.name}</span>
                {w && (
                  <span className="flex items-center gap-1 shrink-0" title={w.description}>
                    {weatherIcon(w.weatherType)}
                    <span className="text-[10px]">{w.temperature}°</span>
                  </span>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
