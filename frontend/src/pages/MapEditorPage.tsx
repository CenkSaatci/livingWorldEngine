import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Save, Upload } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import * as PIXI from 'pixi.js';
import { usePixiApp } from '../components/map/usePixiApp';
import { drawGrid } from '../components/map/Grid';
import { apiClient } from '../api/client';
import { useToast } from '../hooks/useToast';
import { LoadingSpinner } from '../components/ui/LoadingSpinner';

const COLS = 20;
const ROWS = 15;
const TILE = 48;

const POLY_COLORS = [
  0x4a90d9, 0x50b86c, 0xd9a84a, 0x9b59b6, 0xe67e22, 0x1abc9c, 0xe74c3c, 0x3498db,
];

interface Region {
  id: string;
  name: string;
  polygon_points?: string;
}

interface Location {
  id: string;
  name: string;
  position_json?: string;
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
  const [drawingPoints, setDrawingPoints] = useState<{ x: number; y: number }[]>([]);
  const [mode, setMode] = useState<'view' | 'draw'>('view');
  const [backgroundImage, setBackgroundImage] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const regionsLayerRef = useRef<PIXI.Graphics | null>(null);
  const locationsLayerRef = useRef<PIXI.Container | null>(null);
  const drawingLayerRef = useRef<PIXI.Graphics | null>(null);
  const bgLayerRef = useRef<PIXI.Sprite | null>(null);

  useEffect(() => {
    if (!id) return;
    const fetchData = async () => {
      try {
        const regionsRes = await apiClient.get(`/worlds/${id}/regions`);
        const regions = regionsRes.data ?? [];
        setRegions(regions);
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
  }, [id, toast]);

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
      app.stage.removeChild(bgLayerRef.current);
      bgLayerRef.current.destroy(true);
      bgLayerRef.current = null;
    }

    if (backgroundImage) {
      const texture = PIXI.Texture.from(backgroundImage);
      const sprite = new PIXI.Sprite(texture);
      sprite.width = COLS * TILE;
      sprite.height = ROWS * TILE;
      app.stage.addChildAt(sprite, 0);
      bgLayerRef.current = sprite;
    }
  }, [getApp, backgroundImage]);

  useEffect(() => {
    const app = getApp();
    if (!app) return;

    if (regionsLayerRef.current) {
      app.stage.removeChild(regionsLayerRef.current);
      regionsLayerRef.current.destroy(true);
      regionsLayerRef.current = null;
    }

    const regionsGfx = new PIXI.Graphics();
    regions.forEach((region, index) => {
      if (!region.polygon_points) return;
      try {
        const pts = JSON.parse(region.polygon_points) as { x: number; y: number }[];
        if (pts.length < 3) return;
        const color = POLY_COLORS[index % POLY_COLORS.length];
        regionsGfx.beginFill(color, 0.2);
        regionsGfx.lineStyle(2, color, 0.8);
        regionsGfx.moveTo(pts[0].x, pts[0].y);
        for (let i = 1; i < pts.length; i++) {
          regionsGfx.lineTo(pts[i].x, pts[i].y);
        }
        regionsGfx.closePath();
        regionsGfx.endFill();
      } catch {
        /* skip invalid polygons */
      }
    });

    app.stage.addChild(regionsGfx);
    regionsLayerRef.current = regionsGfx;

    if (locationsLayerRef.current) {
      app.stage.removeChild(locationsLayerRef.current);
      locationsLayerRef.current.destroy(true);
      locationsLayerRef.current = null;
    }

    const locsContainer = new PIXI.Container();
    locations.forEach((loc) => {
      if (!loc.position_json) return;
      try {
        const pos = JSON.parse(loc.position_json) as { x: number; y: number };
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
          toast.info(`Location: ${loc.name}`);
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
      } catch {
        /* skip */
      }
    });

    app.stage.addChild(locsContainer);
    locationsLayerRef.current = locsContainer;

    if (drawingLayerRef.current) {
      app.stage.removeChild(drawingLayerRef.current);
      drawingLayerRef.current.destroy(true);
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
  }, [getApp, regions, locations, drawingPoints, toast]);

  const handleCanvasClick = (e: React.MouseEvent) => {
    if (mode !== 'draw') return;
    const canvas = containerRef.current?.querySelector('canvas');
    if (!canvas) return;
    const rect = canvas.getBoundingClientRect();
    const mouseX = e.clientX - rect.left;
    const mouseY = e.clientY - rect.top;
    const vp = getViewport();
    const worldX = (mouseX - vp.x) / vp.zoom;
    const worldY = (mouseY - vp.y) / vp.zoom;
    setDrawingPoints((prev) => [...prev, { x: worldX, y: worldY }]);
  };

  const handleRightClick = (e: React.MouseEvent) => {
    e.preventDefault();
    if (mode !== 'draw' || drawingPoints.length < 3) return;
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
    try {
      const res = await apiClient.post(`/worlds/${id}/map/upload`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      setBackgroundImage(
        (import.meta.env.VITE_API_URL ?? 'http://localhost:8080') + res.data.image_url,
      );
    } catch {
      // keep local preview as fallback
      const reader = new FileReader();
      reader.onload = (ev) => setBackgroundImage(ev.target?.result as string);
      reader.readAsDataURL(file);
    }
  };

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-bg-primary">
        <LoadingSpinner />
      </div>
    );
  }

  return (
    <div className="flex min-h-screen flex-col bg-bg-primary">
      <header className="flex items-center justify-between border-b border-bg-elevated bg-bg-surface px-6 py-3">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate(`/worlds/${id}/edit`)}
            className="text-text-secondary hover:text-accent"
          >
            <ArrowLeft size={20} />
          </button>
          <h1 className="text-lg font-heading text-text-primary">Map Editor</h1>
          {mode === 'draw' && (
            <span className="rounded bg-accent/20 px-2 py-0.5 text-xs text-accent">
              Drawing mode
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

      <div className="flex flex-1">
        <aside className="flex w-64 flex-col gap-4 border-r border-bg-elevated bg-bg-surface p-4">
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
            <div className="flex gap-2">
              <button
                onClick={() => {
                  setMode('view');
                  setDrawingPoints([]);
                  setSelectedRegion(null);
                }}
                className={`flex-1 rounded px-3 py-1.5 text-xs ${
                  mode === 'view' ? 'bg-accent text-white' : 'bg-bg-elevated text-text-secondary'
                }`}
              >
                View
              </button>
              <button
                onClick={() => setMode('draw')}
                className={`flex-1 rounded px-3 py-1.5 text-xs ${
                  mode === 'draw' ? 'bg-accent text-white' : 'bg-bg-elevated text-text-secondary'
                }`}
              >
                Draw
              </button>
            </div>
          </div>

          {/* Regions list */}
          <div>
            <h3 className="mb-2 text-xs font-heading uppercase tracking-wider text-text-secondary">
              Regions
            </h3>
            <div className="space-y-1">
              {regions.length === 0 && <p className="text-xs text-text-secondary">No regions</p>}
              {regions.map((region) => (
                <div
                  key={region.id}
                  className={`flex items-center justify-between rounded px-2 py-1 hover:bg-bg-elevated/50 ${
                    selectedRegion === region.id ? 'bg-accent/10' : ''
                  }`}
                >
                  <span className="text-sm text-text-primary">{region.name}</span>
                  <button
                    onClick={() => handleStartDraw(region.id)}
                    className="text-xs text-accent hover:text-accent/80"
                  >
                    {region.polygon_points ? 'Redraw' : 'Draw'}
                  </button>
                </div>
              ))}
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
                <div key={loc.id} className="rounded px-2 py-1 text-sm text-text-primary">
                  {loc.name}
                  {loc.position_json && (
                    <span className="ml-1 text-xs text-text-secondary">{'\u2713'}</span>
                  )}
                </div>
              ))}
            </div>
          </div>

          {/* Instructions */}
          {mode === 'draw' && (
            <div className="mt-auto rounded bg-accent/10 p-3">
              <p className="text-xs text-text-secondary">
                Click on the map to add vertices. Right-click to close the polygon.
              </p>
            </div>
          )}
        </aside>

        {/* Canvas area */}
        <main className="relative flex-1 overflow-hidden">
          <div className="relative h-full w-full">
            <div ref={containerRef} className="h-full w-full" style={{ minHeight: 400 }} />
            {mode === 'draw' && (
              <div
                className="absolute inset-0 z-10 cursor-crosshair"
                onClick={handleCanvasClick}
                onContextMenu={handleRightClick}
              />
            )}
          </div>
        </main>
      </div>
    </div>
  );
}
