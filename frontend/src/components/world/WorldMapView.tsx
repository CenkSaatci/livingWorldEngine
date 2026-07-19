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
import { apiClient, BACKEND_ORIGIN } from '../../api/client';
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
  positionJson?: string;
}

interface MapData {
  imageUrl: string | null;
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
  onSelectLocation,
}: Props) {
  const [locations, setLocations] = useState<Location[]>([]);
  const [weather, setWeather] = useState<Record<string, WeatherData>>({});
  const [selected, setSelected] = useState<string | null>(null);
  const [mapUrl, setMapUrl] = useState<string | null>(null);
  const [naturalSize, setNaturalSize] = useState({ w: 1754, h: 2481 });
  const [zoom, setZoom] = useState(1);
  const [pan, setPan] = useState({ x: 0, y: 0 });
  const [dragging, setDragging] = useState(false);
  const [dragStart, setDragStart] = useState({ x: 0, y: 0 });
  const [panStart, setPanStart] = useState({ x: 0, y: 0 });

  const { data: regions } = useApiGet<Region[]>(`/worlds/${worldId}/regions`, [worldId]);

  const handleWheel = (e: React.WheelEvent) => {
    e.preventDefault();
    const factor = e.deltaY > 0 ? 0.9 : 1.1;
    setZoom((z) => Math.min(4, Math.max(0.25, z * factor)));
  };

  const handleMouseDown = (e: React.MouseEvent) => {
    if (e.button !== 0) return;
    setDragging(true);
    setDragStart({ x: e.clientX, y: e.clientY });
    setPanStart({ ...pan });
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    if (!dragging) return;
    const dx = e.clientX - dragStart.x;
    const dy = e.clientY - dragStart.y;
    setPan({ x: panStart.x + dx, y: panStart.y + dy });
  };

  const handleMouseUp = () => setDragging(false);

  // Fetch map image + locations + weather
  useEffect(() => {
    if (!worldId || !regions) return;
    let cancelled = false;
    const load = async () => {
      try {
        const [mapRes] = await Promise.all([
          apiClient.get<MapData>(`/worlds/${worldId}/map`),
        ]);
        if (cancelled) return;
        if (mapRes.data?.imageUrl) {
          setMapUrl(BACKEND_ORIGIN + mapRes.data.imageUrl);
        }
      } catch {}
    };
    load();

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
          allLocs.push(...result.value.data);
        }
      }
      setLocations(allLocs);
    });

    return () => { cancelled = true; };
  }, [worldId, regions]);

  const dominantWeather = Object.values(weather)[0]?.weatherType;

  return (
    <div className="relative h-full w-full overflow-hidden bg-bg-primary">
      {/* Zoom controls */}
      <div className="absolute top-3 right-3 z-20 flex flex-col gap-1">
        <button onClick={() => setZoom((z) => Math.min(4, z * 1.3))}
          className="rounded bg-bg-surface/80 px-2 py-1 text-xs text-text-primary hover:bg-bg-surface border border-bg-elevated">+</button>
        <button onClick={() => setZoom((z) => Math.max(0.25, z / 1.3))}
          className="rounded bg-bg-surface/80 px-2 py-1 text-xs text-text-primary hover:bg-bg-surface border border-bg-elevated">−</button>
        <button onClick={() => { setZoom(1); setPan({ x: 0, y: 0 }); }}
          className="rounded bg-bg-surface/80 px-2 py-1 text-xs text-text-primary hover:bg-bg-surface border border-bg-elevated">⟲</button>
      </div>

      {/* Map + markers layer */}
      <div
        className="absolute inset-0"
        style={{ transform: `translate(${pan.x}px, ${pan.y}px) scale(${zoom})`, transformOrigin: '0 0' }}
        onWheel={handleWheel}
        onMouseDown={handleMouseDown}
        onMouseMove={handleMouseMove}
        onMouseUp={handleMouseUp}
        onMouseLeave={handleMouseUp}
      >
        {/* Map image */}
        {mapUrl ? (
          <img
            src={mapUrl}
            alt="Map"
            className="block max-w-none"
            draggable={false}
            onLoad={(e) => {
              const img = e.currentTarget;
              setNaturalSize({ w: img.naturalWidth, h: img.naturalHeight });
            }}
            style={{ width: naturalSize.w + 'px' }}
          />
        ) : (
          <div className="w-[960px] h-[720px]" />
        )}

        {/* Grid overlay */}
        <div
          className="absolute inset-0 pointer-events-none"
          style={{
            backgroundImage: `linear-gradient(rgba(42,54,64,0.4) 1px, transparent 1px),
              linear-gradient(90deg, rgba(42,54,64,0.4) 1px, transparent 1px)`,
            backgroundSize: `${48}px ${48}px`,
          }}
        />

        {/* Location Markers */}
        {locations.map((loc) => {
          const pos = loc.positionJson ? (() => { try { return JSON.parse(loc.positionJson) as { x: number; y: number }; } catch { return null; } })() : null;
          if (!pos) return null;
          return (
          <button key={loc.id}
            onClick={() => { setSelected(loc.id); onSelectLocation?.(loc.id); }}
            className={`absolute flex items-center gap-1 rounded-full px-2 py-1 text-xs transition-all hover:scale-110 ${
              selected === loc.id ? 'bg-accent text-white shadow-lg z-10' : 'bg-bg-surface/80 text-text-secondary hover:bg-accent/20 hover:text-accent'
            }`}
            style={{ left: pos.x, top: pos.y, transform: 'translate(-50%, -50%)' }}
            title={loc.name}
          >
            {locationIcon(loc.type)}
            <span className="hidden sm:inline">{loc.name}</span>
          </button>
          );
        })}
      </div>

      {/* Weather overlay */}
      {dominantWeather && (
        <div className={`absolute inset-0 pointer-events-none ${weatherOverlayClass(dominantWeather)}`} />
      )}

      {/* Region Legend */}
      {regions && regions.length > 0 && (
        <div className="absolute bottom-3 left-3 max-h-60 overflow-y-auto rounded-lg bg-bg-surface/90 p-3 text-xs text-text-secondary shadow-lg z-10">
          <p className="font-semibold text-text-primary mb-1">Regions ({regions.length})</p>
          {regions.map((r) => {
            const w = weather[r.id];
            return (
              <div key={r.id} className="flex items-center gap-2 py-0.5">
                <span className={`inline-block h-2 w-2 shrink-0 rounded-full ${r.danger_level > 5 ? 'bg-danger' : 'bg-success'}`} />
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
