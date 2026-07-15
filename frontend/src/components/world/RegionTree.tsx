import { useEffect, useState } from 'react';
import {
  ChevronRight,
  ChevronDown,
  MapPin,
  Castle,
  Building2,
  Trees,
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
import { useToast } from '../../hooks/useToast';
import { useApiGet } from '../../hooks/useApiGet';
import { useLazyApiGet } from '../../hooks/useLazyApiGet';

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

interface WeatherData {
  regionId: string;
  weatherType: string;
  temperature: number;
  description: string;
}

function locationIcon(type: string) {
  switch (type) {
    case 'city':
    case 'town':
      return <Building2 size={16} />;
    case 'castle':
      return <Castle size={16} />;
    case 'dungeon':
    case 'ruin':
      return <ScrollText size={16} />;
    default:
      return <MapPin size={16} />;
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

interface Props {
  worldId: string;
  onSelectRegion?: (id: string) => void;
  onSelectLocation?: (id: string) => void;
}

export function RegionTree({ worldId, onSelectRegion, onSelectLocation }: Props) {
  const toast = useToast();
  const [expanded, setExpanded] = useState<Record<string, boolean>>({});
  const [locations, setLocations] = useState<Record<string, Location[]>>({});
  const [weather, setWeather] = useState<Record<string, WeatherData>>({});

  const { data: regions } = useApiGet<Region[]>(`/worlds/${worldId}/regions`, [worldId]);
  const { fetch: fetchWeather } = useLazyApiGet<WeatherData>();

  useEffect(() => {
    if (!regions || regions.length === 0) return;
    let cancelled = false;
    Promise.all(
      regions.map(async (reg) => {
        const data = await fetchWeather(`/regions/${reg.id}/weather`);
        return { id: reg.id, data };
      }),
    ).then((results) => {
      if (cancelled) return;
      const w: Record<string, WeatherData> = {};
      results.forEach((r) => {
        if (r.data) w[r.id] = { ...r.data, regionId: r.id };
      });
      setWeather(w);
    });
    return () => {
      cancelled = true;
    };
  }, [regions, fetchWeather]);

  const toggleRegion = async (regionId: string) => {
    const next = { ...expanded, [regionId]: !expanded[regionId] };
    setExpanded(next);
    if (next[regionId] && !locations[regionId]) {
      try {
        const r = await apiClient.get(`/regions/${regionId}/locations`);
        setLocations((prev) => ({ ...prev, [regionId]: r.data }));
      } catch {
        toast.error('Failed to load locations');
      }
    }
  };

  return (
    <div className="space-y-1">
      {(regions ?? []).map((r) => (
        <div key={r.id}>
          <div className="flex items-center gap-1">
            <button
              onClick={() => toggleRegion(r.id)}
              className="rounded p-1 text-text-secondary hover:text-accent hover:bg-bg-elevated/50"
              aria-label={expanded[r.id] ? 'Collapse region' : 'Expand region'}
            >
              {expanded[r.id] ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
            </button>
            <button
              onClick={() => onSelectRegion?.(r.id)}
              className="flex flex-1 items-center gap-2 rounded px-2 py-1.5 text-sm text-text-primary hover:bg-bg-elevated/50"
            >
              <Trees size={16} className="text-accent" />
              <span className="flex-1 text-left">{r.name}</span>
              {weather[r.id] && (
                <span className="shrink-0" title={weather[r.id].description}>
                  {weatherIcon(weather[r.id].weatherType, 12)}
                </span>
              )}
              <span
                className={`text-xs ${r.danger_level > 5 ? 'text-danger' : 'text-text-secondary'}`}
              >
                ⚔️ {r.danger_level}
              </span>
            </button>
          </div>

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

      {(!regions || regions.length === 0) && (
        <p className="px-2 py-4 text-xs text-text-secondary text-center">No regions yet</p>
      )}
    </div>
  );
}
