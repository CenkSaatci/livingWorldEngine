import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Save, Upload, X } from 'lucide-react';
import { useCallback, useEffect, useRef, useState } from 'react';
import * as PIXI from 'pixi.js';
import { usePixiApp } from '../components/map/usePixiApp';
import { drawGrid } from '../components/map/Grid';
import { apiClient, BACKEND_ORIGIN } from '../api/client';
import { useToast } from '../hooks/useToast';
import { LoadingSpinner } from '../components/ui/LoadingSpinner';

const COLS = 20;
const ROWS = 15;
const TILE = 48;

const POLY_COLORS = [
  0x3b82f6, 0x22c55e, 0xf59e0b, 0xa855f7, 0xef4444, 0x06b6d4, 0x84cc16, 0xf97316,
];

interface Region {
  id: string;
  name: string;
  polygon_points?: string;
}

interface Location {
  id: string;
  regionId?: string;
  name: string;
  type?: string;
  description?: string;
  population?: number;
  wealth?: number;
  positionJson?: string;
}

export default function MapEditorPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const toast = useToast();
  const containerRef = useRef<HTMLDivElement | null>(null);
  const { getApp, getViewport } = usePixiApp(containerRef);
  const gridDrawn = useRef(false);

  const [regions, setRegions] = useState<Region[]>([]);
  const [locations, setLocations] = useState<Location[]>([]);
  const [selectedRegion, setSelectedRegion] = useState<string | null>(null);
  const [selectedLocation, setSelectedLocation] = useState<string | null>(null);
  const [drawingPoints, setDrawingPoints] = useState<{ x: number; y: number }[]>([]);
  const [mode, setMode] = useState<'view' | 'draw' | 'place'>('view');
  const [backgroundImage, setBackgroundImage] = useState<string | null>(null);
  const [renderTick, setRenderTick] = useState(0);
  const redraw = useCallback(() => {
    setRenderTick(t => t + 1);
  }, []);
  const [loading, setLoading] = useState(true);
  const [showLocModal, setShowLocModal] = useState<'create' | 'info' | null>(null);
  const [infoLocation, setInfoLocation] = useState<Location | null>(null);
  const [locForm, setLocForm] = useState({ name: '', type: 'village', regionName: '', description: '', population: 0, wealth: 5 });

  const LOCATION_TYPES = ['village', 'town', 'city', 'hamlet', 'fortress', 'ruin', 'dungeon', 'tower', 'shrine', 'camp', 'mine', 'farm', 'inn', 'harbor', 'bridge'];

  const regionsLayerRef = useRef<PIXI.Graphics | null>(null);
  const locationsLayerRef = useRef<PIXI.Container | null>(null);
  const drawingLayerRef = useRef<PIXI.Graphics | null>(null);
  const bgLayerRef = useRef<PIXI.Sprite | null>(null);

  useEffect(() => {
    if (!id) return;
    const fetchData = async () => {
      try {
        const [regionsRes, mapRes] = await Promise.all([
          apiClient.get(`/worlds/${id}/regions`),
          apiClient.get(`/worlds/${id}/map`),
        ]);
        const regions = regionsRes.data ?? [];
        setRegions(regions);
        if (mapRes.data?.imageUrl) {
          setBackgroundImage(
           BACKEND_ORIGIN + mapRes.data.imageUrl,
          );
        }
        const locPromises = regions.map((r: Region) => apiClient.get(`/regions/${r.id}/locations`));
        const locResults = await Promise.all(locPromises);
        setLocations(locResults.flatMap((r) => r.data ?? []));
      } catch {
        toast.error('Failed to load map data');
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [id]);

  useEffect(() => {
    const app = getApp();
    if (!app || gridDrawn.current) return;

    const grid = new PIXI.Graphics();
    drawGrid(grid, COLS, ROWS, TILE);
    app.stage.addChild(grid);

    gridDrawn.current = true;
  }, [getApp]);

  useEffect(() => {
    const app = getApp();
    if (!app) return;

    if (bgLayerRef.current) {
      try {
        app.stage.removeChild(bgLayerRef.current);
        bgLayerRef.current.destroy(true);
      } catch { /* already destroyed */ }
      bgLayerRef.current = null;
    }

    if (backgroundImage) {
      const texture = PIXI.Texture.from(backgroundImage);
      const sprite = new PIXI.Sprite(texture);
      app.stage.addChildAt(sprite, 0);
      bgLayerRef.current = sprite;
    }
  }, [getApp, backgroundImage]);

  useEffect(() => {
    const app = getApp();
    if (!app) return;

    if (regionsLayerRef.current) {
      try {
        app.stage.removeChild(regionsLayerRef.current);
        regionsLayerRef.current.destroy(true);
      } catch { /* already destroyed */ }
      regionsLayerRef.current = null;
    }

    const regionsGfx = new PIXI.Graphics();
    regions.forEach((region, index) => {
      if (!region.polygon_points) return;
      try {
        const pts = JSON.parse(region.polygon_points) as { x: number; y: number }[];
        if (pts.length < 3) return;
        const color = POLY_COLORS[index % POLY_COLORS.length];
        regionsGfx.beginFill(color, 0.25);
        regionsGfx.lineStyle(3, color, 0.9);
        regionsGfx.moveTo(pts[0].x, pts[0].y);
        for (let i = 1; i < pts.length; i++) {
          regionsGfx.lineTo(pts[i].x, pts[i].y);
        }
        regionsGfx.closePath();
        regionsGfx.endFill();
      } catch (e) {
        console.warn('Polygon draw error:', e);
      }
    });

    app.stage.addChild(regionsGfx);
    regionsLayerRef.current = regionsGfx;

    if (locationsLayerRef.current) {
      try {
        app.stage.removeChild(locationsLayerRef.current);
        locationsLayerRef.current.destroy(true);
      } catch { /* already destroyed */ }
      locationsLayerRef.current = null;
    }

    const locsContainer = new PIXI.Container();
    locations.forEach((loc) => {
      if (!loc.positionJson) return;
      try {
        const pos = JSON.parse(loc.positionJson) as { x: number; y: number };
        const circle = new PIXI.Graphics();
        circle.beginFill(0xe74c3c);
        circle.drawCircle(0, 0, 6);
        circle.endFill();
        circle.lineStyle(2, 0xffffff, 0.8);
        circle.drawCircle(0, 0, 6);
        circle.position.set(pos.x, pos.y);
        circle.eventMode = 'static';
        circle.cursor = 'pointer';
        circle.on('pointerdown', () => {
          setInfoLocation(loc);
          setShowLocModal('info');
        });
        locsContainer.addChild(circle);

        const text = new PIXI.Text(loc.name, {
          fontSize: 11,
          fill: 0xffffff,
          dropShadow: true,
          dropShadowColor: 0x000000,
          dropShadowBlur: 2,
        });
        text.anchor.set(0.5, 1);
        text.position.set(pos.x, pos.y - 10);
        locsContainer.addChild(text);
      } catch (e) {
        console.warn('Location draw error:', e);
      }
    });

    app.stage.addChild(locsContainer);
    locationsLayerRef.current = locsContainer;

    if (drawingLayerRef.current) {
      try {
        app.stage.removeChild(drawingLayerRef.current);
        drawingLayerRef.current.destroy(true);
      } catch { /* already destroyed */ }
      drawingLayerRef.current = null;
    }

    const drawingGfx = new PIXI.Graphics();
    if (drawingPoints.length > 0) {
      drawingGfx.lineStyle(2, 0xff4444, 0.9);
      drawingGfx.moveTo(drawingPoints[0].x, drawingPoints[0].y);
      for (let i = 1; i < drawingPoints.length; i++) {
        drawingGfx.lineTo(drawingPoints[i].x, drawingPoints[i].y);
      }
      if (drawingPoints.length >= 3) {
        drawingGfx.lineStyle(1, 0xff4444, 0.3);
        drawingGfx.lineTo(drawingPoints[0].x, drawingPoints[0].y);
      }
    }

    drawingPoints.forEach((pt) => {
      drawingGfx.beginFill(0xff4444);
      drawingGfx.drawCircle(pt.x, pt.y, 4);
      drawingGfx.endFill();
    });

    app.stage.addChild(drawingGfx);
    drawingLayerRef.current = drawingGfx;
  }, [getApp, regions, locations, drawingPoints, renderTick]);

  const handleCanvasClick = (e: React.MouseEvent) => {
    const canvas = containerRef.current?.querySelector('canvas');
    if (!canvas) return;
    const rect = canvas.getBoundingClientRect();
    const mouseX = e.clientX - rect.left;
    const mouseY = e.clientY - rect.top;
    const vp = getViewport();
    const worldX = (mouseX - vp.x) / vp.zoom;
    const worldY = (mouseY - vp.y) / vp.zoom;

    if (mode === 'draw') {
      setDrawingPoints((prev) => [...prev, { x: worldX, y: worldY }]);
    } else if (mode === 'place' && selectedLocation) {
      saveLocationPosition(selectedLocation, worldX, worldY);
    }
  };

  const saveLocationPosition = async (locationId: string, x: number, y: number) => {
    if (!id) return;
    const loc = locations.find(l => l.id === locationId);
    console.log('Place:', { loc, x, y });
    if (!loc?.regionId) { toast.error('Location has no region'); return; }
    try {
      const patchRes = await apiClient.patch(`/regions/${loc.regionId}/locations/${locationId}`, {
        positionJson: JSON.stringify({ x, y }),
      });
      console.log('PATCH response:', patchRes.status, patchRes.data);
      const locPromises = regions.map((r) => apiClient.get(`/regions/${r.id}/locations`));
      const locResults = await Promise.all(locPromises);
      setLocations(locResults.flatMap((r) => r.data ?? []));
      toast.success('Location placed');
      setMode('view');
      setSelectedLocation(null);
    } catch {
      toast.error('Failed to place location');
    }
  };

  const handleRightClick = async (e: React.MouseEvent) => {
    e.preventDefault();
    if (mode !== 'draw') return;
    if (drawingPoints.length >= 3) {
      await handleSavePolygon();
    }
    setDrawingPoints([]);
    setSelectedRegion(null);
    setMode('view');
  };

  const handleSavePolygon = async () => {
    if (!id || !selectedRegion || drawingPoints.length < 3) {
      toast.error('Select a region and draw at least 3 points');
      return;
    }
    try {
      await apiClient.patch(`/worlds/${id}/regions/${selectedRegion}`, {
        polygonPoints: JSON.stringify(drawingPoints),
      });
      toast.success('Polygon saved');
      setDrawingPoints([]);
      setSelectedRegion(null);
      setMode('view');
      const res = await apiClient.get(`/worlds/${id}/regions`);
      console.log('Regions after save:', res.data);
      setRegions(res.data ?? []);
    } catch {
      toast.error('Failed to save polygon');
    }
  };

  const handleStartDraw = (regionId: string) => {
    setSelectedRegion(regionId);
    setDrawingPoints([]);
    setMode('draw');
  };

  const handleCancelDraw = () => {
    setDrawingPoints([]);
    setSelectedRegion(null);
    setMode('view');
  };

  const handleImageUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file || !id) return;
    const formData = new FormData();
    formData.append('file', file);
    setBackgroundImage(URL.createObjectURL(file));
    try {
      const res = await apiClient.post(`/worlds/${id}/map/upload`, formData);
      setBackgroundImage(
      BACKEND_ORIGIN + res.data.imageUrl,
      );
    } catch (err) {
      console.error('Upload failed:', err);
    }
  };

  return (
    <div className="flex h-screen flex-col bg-bg-primary">
      {loading && (
        <div className="absolute inset-0 z-50 flex items-center justify-center bg-bg-primary/80">
          <LoadingSpinner />
        </div>
      )}
      <header className="flex items-center justify-between border-b border-bg-elevated bg-bg-surface px-6 py-3">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate(`/worlds/${id}/edit`)}
            className="text-text-secondary hover:text-accent"
          >
            <ArrowLeft size={20} />
          </button>
          <h1 className="text-lg font-heading text-text-primary">Map Editor</h1>
          {(mode === 'draw' || mode === 'place') && (
            <span className="rounded bg-accent/20 px-2 py-0.5 text-xs text-accent">
              {mode === 'draw' ? 'Drawing mode' : 'Place mode'}
            </span>
          )}
        </div>
        <div className="flex items-center gap-2">
          {mode === 'draw' && (
            <>
              <button
                onClick={handleSavePolygon}
                className="flex items-center gap-1 rounded bg-accent px-3 py-1.5 text-xs text-white hover:bg-accent/80"
              >
                <Save size={14} /> Save Polygon
              </button>
              <button
                onClick={handleCancelDraw}
                className="rounded border border-bg-elevated px-3 py-1.5 text-xs text-text-secondary hover:text-text-primary"
              >
                Cancel
              </button>
            </>
          )}
        </div>
      </header>

      <div className="flex flex-1 overflow-hidden">
        <aside className="flex w-64 flex-col gap-4 overflow-y-auto border-r border-bg-elevated bg-bg-surface p-4">
          {/* Upload */}
          <div>
            <label className="mb-1 block text-xs text-text-secondary">Map Image</label>
            <label className="flex cursor-pointer items-center gap-2 rounded border border-bg-elevated px-3 py-2 text-sm text-text-secondary hover:text-text-primary">
              <Upload size={14} />
              Upload
              <input type="file" accept="image/*" onChange={handleImageUpload} className="hidden" />
            </label>
            {backgroundImage && <p className="mt-1 text-xs text-text-secondary">Image loaded</p>}
          </div>

          {/* Mode toggle */}
          <div>
            <label className="mb-1 block text-xs text-text-secondary">Mode</label>
            <div className="flex gap-1">
              {(['view', 'draw', 'place'] as const).map((m) => (
                <button
                  key={m}
                  onClick={() => {
                    setMode(m);
                    if (m !== 'draw') setDrawingPoints([]);
                    if (m !== 'place') setSelectedLocation(null);
                  }}
                  className={`flex-1 rounded px-3 py-1.5 text-xs ${
                    mode === m ? 'bg-accent text-white' : 'bg-bg-elevated text-text-secondary'
                  }`}
                >
                  {m === 'view' ? 'View' : m === 'draw' ? 'Draw' : 'Place'}
                </button>
              ))}
            </div>
          </div>

          {/* Regions list */}
          <div>
            <h3 className="mb-2 text-xs font-heading uppercase tracking-wider text-text-secondary">
              Regions
            </h3>
            <div className="space-y-1">
              {regions.length === 0 && <p className="text-xs text-text-secondary">No regions</p>}
              {regions.map((region, idx) => {
                const color = POLY_COLORS[idx % POLY_COLORS.length];
                return (
                <div
                  key={region.id}
                  className={`flex items-center justify-between rounded px-2 py-1 hover:bg-bg-elevated/50 ${
                    selectedRegion === region.id ? 'bg-accent/10' : ''
                  }`}
                >
                  <div className="flex items-center gap-2">
                    <span
                      className="inline-block h-2.5 w-2.5 rounded-full shrink-0"
                      style={{ backgroundColor: `#${color.toString(16).padStart(6, '0')}` }}
                    />
                    <span className="text-sm text-text-primary">{region.name}</span>
                  </div>
                  <button
                    onClick={() => handleStartDraw(region.id)}
                    className="text-xs text-accent hover:text-accent/80"
                  >
                    {region.polygon_points ? 'Redraw' : 'Draw'}
                  </button>
                </div>
                );
              })}
              <button
                onClick={async () => {
                  const name = prompt('Region name:');
                  if (!name || !id) return;
                  try {
                    await apiClient.post(`/worlds/${id}/regions`, { name, dangerLevel: 5 });
                    const res = await apiClient.get(`/worlds/${id}/regions`);
                    setRegions(res.data ?? []);
                  } catch { toast.error('Failed to create region'); }
                }}
                className="w-full rounded border border-dashed border-bg-elevated py-1 text-xs text-text-secondary hover:text-accent hover:border-accent/50"
              >
                + Create Region
              </button>
            </div>
          </div>

          {/* Locations list */}
          <div>
            <h3 className="mb-2 text-xs font-heading uppercase tracking-wider text-text-secondary">
              Locations
            </h3>
            <div className="space-y-1">
              {locations.length === 0 && (
                <p className="text-xs text-text-secondary">No locations</p>
              )}
              {locations.map((loc) => (
                <div
                  key={loc.id}
                  onClick={() => {
                    if (mode === 'place') {
                      setSelectedLocation(loc.id);
                      toast.info(`Click on map to place "${loc.name}"`);
                    }
                  }}
                  className={`flex items-center justify-between rounded px-2 py-1 text-sm ${
                    selectedLocation === loc.id
                      ? 'bg-accent/20 text-accent'
                      : 'text-text-primary hover:bg-bg-elevated/50'
                  } ${mode === 'place' ? 'cursor-pointer' : ''}`}
                >
                  <span>{loc.name}</span>
                  {loc.positionJson && (
                    <span className="text-xs text-text-secondary">{'\u2713'}</span>
                  )}
                </div>
              ))}
              <button
                onClick={() => {
                  setLocForm({ name: '', type: 'village', regionName: regions[0]?.name ?? '', description: '', population: 0, wealth: 5 });
                  setShowLocModal('create');
                }}
                className="w-full rounded border border-dashed border-bg-elevated py-1 text-xs text-text-secondary hover:text-accent hover:border-accent/50"
              >
                + Create Location
              </button>
            </div>
          </div>

          {/* Instructions */}
          <button
            onClick={redraw}
            className="w-full rounded border border-bg-elevated py-1.5 text-xs text-text-secondary hover:text-accent hover:border-accent/50"
          >
            Refresh Map ({regions.length}r / {locations.length}l / {backgroundImage ? 'bg' : 'no bg'})
          </button>
          {mode === 'draw' && (
            <div className="mt-auto rounded bg-accent/10 p-3">
              <p className="text-xs text-text-secondary">
                Linksklick = Punkt setzen, Rechtsklick = Polygon schließen
              </p>
            </div>
          )}
          {mode === 'place' && (
            <div className="mt-auto rounded bg-accent/10 p-3">
              <p className="text-xs text-text-secondary">
                {selectedLocation
                  ? 'Auf die Karte klicken um den Ort zu platzieren'
                  : 'Ort in der Liste auswählen, dann auf Karte klicken'}
              </p>
            </div>
          )}
        </aside>

        {/* Canvas area */}
        <main className="relative flex-1 overflow-hidden">
          <div className="relative h-full w-full">
            <div ref={containerRef} className="h-full w-full" style={{ minHeight: 400 }} />
            {(mode === 'draw' || mode === 'place') && (
              <div
                className="absolute inset-0 z-10 cursor-crosshair"
                onClick={handleCanvasClick}
                onContextMenu={handleRightClick}
              />
            )}
          </div>
        </main>
      </div>

      {/* Create Location Modal */}
      {showLocModal === 'create' && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60">
          <div className="w-80 rounded-xl border border-bg-elevated bg-bg-surface p-5 shadow-2xl">
            <div className="flex items-center justify-between mb-4">
              <h3 className="font-heading text-text-primary">Create Location</h3>
              <button onClick={() => setShowLocModal(null)} className="text-text-secondary hover:text-text-primary"><X size={16} /></button>
            </div>
            <div className="space-y-3">
              <div>
                <label className="block text-xs text-text-secondary mb-1">Name</label>
                <input value={locForm.name} onChange={(e) => setLocForm(f => ({ ...f, name: e.target.value }))}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent" />
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">Type</label>
                <select value={locForm.type} onChange={(e) => setLocForm(f => ({ ...f, type: e.target.value }))}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent">
                  {LOCATION_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
                </select>
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">Region</label>
                <select value={locForm.regionName} onChange={(e) => setLocForm(f => ({ ...f, regionName: e.target.value }))}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent">
                  {regions.map(r => <option key={r.id} value={r.name}>{r.name}</option>)}
                </select>
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">Description</label>
                <textarea value={locForm.description} onChange={(e) => setLocForm(f => ({ ...f, description: e.target.value }))} rows={2}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent resize-none" />
              </div>
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block text-xs text-text-secondary mb-1">Population</label>
                  <input type="number" min={0} value={locForm.population} onChange={(e) => setLocForm(f => ({ ...f, population: Number(e.target.value) }))}
                    className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent" />
                </div>
                <div>
                  <label className="block text-xs text-text-secondary mb-1">Wealth (1-10)</label>
                  <input type="number" min={1} max={10} value={locForm.wealth} onChange={(e) => setLocForm(f => ({ ...f, wealth: Number(e.target.value) }))}
                    className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent" />
                </div>
              </div>
              <button onClick={async () => {
                if (!locForm.name || !locForm.regionName) return;
                const region = regions.find(r => r.name === locForm.regionName);
                if (!region) { toast.error('Region not found'); return; }
                try {
                  await apiClient.post(`/regions/${region.id}/locations`, {
                    name: locForm.name, type: locForm.type, description: locForm.description || undefined,
                    population: locForm.population, wealth: locForm.wealth,
                  });
                  const locPromises = regions.map((r) => apiClient.get(`/regions/${r.id}/locations`));
                  const locResults = await Promise.all(locPromises);
                  setLocations(locResults.flatMap((r) => r.data ?? []));
                  setShowLocModal(null);
                  toast.success('Location created');
                } catch { toast.error('Failed to create location'); }
              }}
                className="w-full rounded bg-accent py-2 text-sm text-white hover:bg-accent/80">
                Create
              </button>
              <button onClick={() => setShowLocModal(null)}
                className="w-full rounded border border-bg-elevated py-2 text-sm text-text-secondary hover:text-text-primary">
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Location Info Modal */}
      {showLocModal === 'info' && infoLocation && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60">
          <div className="w-72 rounded-xl border border-bg-elevated bg-bg-surface p-5 shadow-2xl">
            <div className="flex items-center justify-between mb-3">
              <h3 className="font-heading text-text-primary">{infoLocation.name}</h3>
              <button onClick={() => { setShowLocModal(null); setInfoLocation(null); }} className="text-text-secondary hover:text-text-primary"><X size={16} /></button>
            </div>
            <p className="text-xs text-text-secondary mb-2">Type: {infoLocation.type}</p>
            {infoLocation.description && <p className="text-sm text-text-primary mb-2">{infoLocation.description}</p>}
            <p className="text-xs text-text-secondary">Population: {infoLocation.population} · Wealth: {infoLocation.wealth}/10</p>
            {infoLocation.positionJson && <p className="text-xs text-text-secondary mt-1">✓ Placed on map</p>}
          </div>
        </div>
      )}
    </div>
  );
}
