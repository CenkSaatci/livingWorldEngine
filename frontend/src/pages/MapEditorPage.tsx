import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Save, Upload, X, Pencil, Trash2, Undo2, Move } from 'lucide-react';
import { useCallback, useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import * as PIXI from 'pixi.js';
import { usePixiApp } from '../components/map/usePixiApp';
import { drawGrid } from '../components/map/Grid';
import { apiClient, BACKEND_ORIGIN } from '../api/client';
import { useToast } from '../hooks/useToast';
import { LoadingSpinner } from '../components/ui/LoadingSpinner';
import { POI_TYPES, locationTypeIcon, regionColor, validateMapFile } from '../utils/mapEditor';

const COLS = 20;
const ROWS = 15;
const TILE = 48;

interface Region {
  id: string;
  name: string;
  polygonPoints?: string;
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

type LocModal = { mode: 'create' | 'edit' | 'info'; location?: Location } | null;

export default function MapEditorPage() {
  const { t } = useTranslation('map');
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
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [locModal, setLocModal] = useState<LocModal>(null);
  const [locForm, setLocForm] = useState({
    name: '', type: 'village', regionName: '', description: '', population: 0, wealth: 5,
  });

  const redraw = useCallback(() => setRenderTick((tick) => tick + 1), []);

  const regionsLayerRef = useRef<PIXI.Graphics | null>(null);
  const locationsLayerRef = useRef<PIXI.Container | null>(null);
  const drawingLayerRef = useRef<PIXI.Graphics | null>(null);
  const bgLayerRef = useRef<PIXI.Sprite | null>(null);

  const refreshRegions = useCallback(async (): Promise<Region[]> => {
    if (!id) return [];
    const res = await apiClient.get(`/worlds/${id}/regions`);
    const next: Region[] = res.data ?? [];
    setRegions(next);
    return next;
  }, [id]);

  const refreshLocations = useCallback(async (regionsArg?: Region[]) => {
    const list = regionsArg ?? regions;
    if (list.length === 0) {
      setLocations([]);
      return;
    }
    const results = await Promise.all(list.map((r) => apiClient.get(`/regions/${r.id}/locations`)));
    setLocations(results.flatMap((r) => r.data ?? []));
  }, [regions]);

  useEffect(() => {
    if (!id) return;
    const fetchData = async () => {
      try {
        const regionsRes = await apiClient.get(`/worlds/${id}/regions`);
        const nextRegions: Region[] = regionsRes.data ?? [];
        setRegions(nextRegions);
        const mapRes = await apiClient.get(`/worlds/${id}/map`);
        if (mapRes.data?.imageUrl) {
          setBackgroundImage(BACKEND_ORIGIN + mapRes.data.imageUrl);
        }
        await refreshLocations(nextRegions);
      } catch {
        toast.error(t('editor.failed_load_map'));
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
    regions.forEach((region) => {
      if (!region.polygonPoints) return;
      try {
        const pts = JSON.parse(region.polygonPoints) as { x: number; y: number }[];
        if (pts.length < 3) return;
        const color = regionColor(region.id);
        regionsGfx.beginFill(color, 0.25);
        regionsGfx.lineStyle(3, color, 0.9);
        regionsGfx.moveTo(pts[0].x, pts[0].y);
        for (let i = 1; i < pts.length; i++) {
          regionsGfx.lineTo(pts[i].x, pts[i].y);
        }
        regionsGfx.closePath();
        regionsGfx.endFill();
      } catch {
        // korrupte Polygondaten ignorieren (Region bleibt ohne Form sichtbar)
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
        circle.beginFill(0x111827, 0.75);
        circle.drawCircle(0, 0, 10);
        circle.endFill();
        circle.lineStyle(2, 0xffffff, 0.9);
        circle.drawCircle(0, 0, 10);
        circle.position.set(pos.x, pos.y);
        circle.eventMode = 'static';
        circle.cursor = 'pointer';
        circle.on('pointerdown', () => setLocModal({ mode: 'info', location: loc }));
        locsContainer.addChild(circle);

        const icon = new PIXI.Text(locationTypeIcon(loc.type), { fontSize: 14 });
        icon.anchor.set(0.5);
        icon.position.set(pos.x, pos.y);
        locsContainer.addChild(icon);

        const text = new PIXI.Text(loc.name, {
          fontSize: 11,
          fill: 0xffffff,
          dropShadow: true,
          dropShadowColor: 0x000000,
          dropShadowBlur: 2,
        });
        text.anchor.set(0.5, 1);
        text.position.set(pos.x, pos.y - 12);
        locsContainer.addChild(text);
      } catch {
        // korrupte Positionsdaten ignorieren
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

  // Escape schließt Modals bzw. bricht Zeichnen/Platzieren ab.
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key !== 'Escape') return;
      if (locModal) {
        setLocModal(null);
      } else if (mode === 'draw') {
        setDrawingPoints([]);
        setSelectedRegion(null);
        setMode('view');
      } else if (mode === 'place') {
        setSelectedLocation(null);
        setMode('view');
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [locModal, mode]);

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
    const loc = locations.find((l) => l.id === locationId);
    if (!loc?.regionId) { toast.error(t('editor.location_no_region')); return; }
    try {
      await apiClient.patch(`/regions/${loc.regionId}/locations/${locationId}`, {
        positionJson: JSON.stringify({ x, y }),
      });
      await refreshLocations();
      toast.success(t('editor.location_placed'));
      setMode('view');
      setSelectedLocation(null);
    } catch {
      toast.error(t('editor.failed_place_location'));
    }
  };

  const handleSavePolygon = async () => {
    if (!id || !selectedRegion || drawingPoints.length < 3) {
      toast.error(t('editor.select_region_draw'));
      return;
    }
    try {
      await apiClient.patch(`/worlds/${id}/regions/${selectedRegion}`, {
        polygonPoints: JSON.stringify(drawingPoints),
      });
      toast.success(t('editor.polygon_saved'));
      setDrawingPoints([]);
      setSelectedRegion(null);
      setMode('view');
      await refreshRegions();
    } catch {
      toast.error(t('editor.failed_save_polygon'));
    }
  };

  const handleRightClick = async (e: React.MouseEvent) => {
    e.preventDefault();
    if (mode !== 'draw') return;
    if (drawingPoints.length >= 3) {
      await handleSavePolygon();
    } else {
      setDrawingPoints([]);
      setSelectedRegion(null);
      setMode('view');
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

  const handleUndoPoint = () => setDrawingPoints((prev) => prev.slice(0, -1));

  const handleCreateRegion = async () => {
    if (!id) return;
    const name = window.prompt(t('editor.region_name_prompt'));
    if (!name) return;
    try {
      await apiClient.post(`/worlds/${id}/regions`, { name, dangerLevel: 5 });
      await refreshRegions();
    } catch {
      toast.error(t('editor.failed_create_region'));
    }
  };

  const handleRenameRegion = async (region: Region) => {
    if (!id) return;
    const name = window.prompt(t('editor.region_rename_prompt'), region.name);
    if (!name || name === region.name) return;
    try {
      await apiClient.patch(`/worlds/${id}/regions/${region.id}`, { name });
      await refreshRegions();
      toast.success(t('editor.region_renamed'));
    } catch {
      toast.error(t('editor.failed_rename_region'));
    }
  };

  const handleClearPolygon = async (region: Region) => {
    if (!id) return;
    try {
      await apiClient.patch(`/worlds/${id}/regions/${region.id}`, { polygonPoints: '[]' });
      await refreshRegions();
      toast.success(t('editor.polygon_cleared'));
    } catch {
      toast.error(t('editor.failed_save_polygon'));
    }
  };

  const handleDeleteRegion = async (region: Region) => {
    if (!id) return;
    if (!window.confirm(t('editor.confirm_delete_region', { name: region.name }))) return;
    try {
      await apiClient.delete(`/worlds/${id}/regions/${region.id}`);
      const next = await refreshRegions();
      await refreshLocations(next);
    } catch {
      toast.error(t('editor.failed_delete_region'));
    }
  };

  const openCreateLocation = () => {
    setLocForm({
      name: '', type: 'village', regionName: regions[0]?.name ?? '',
      description: '', population: 0, wealth: 5,
    });
    setLocModal({ mode: 'create' });
  };

  const openEditLocation = (loc: Location) => {
    setLocForm({
      name: loc.name,
      type: loc.type ?? 'village',
      regionName: regions.find((r) => r.id === loc.regionId)?.name ?? '',
      description: loc.description ?? '',
      population: loc.population ?? 0,
      wealth: loc.wealth ?? 5,
    });
    setLocModal({ mode: 'edit', location: loc });
  };

  const handleSubmitLocation = async () => {
    if (!locForm.name || !locForm.regionName) return;
    const editing = locModal?.mode === 'edit' ? locModal.location : null;
    const region = regions.find((r) => r.name === locForm.regionName);
    if (!editing && !region) { toast.error(t('editor.region_not_found')); return; }
    const regionId = editing?.regionId ?? region?.id;
    if (!regionId) { toast.error(t('editor.region_not_found')); return; }
    const body = {
      name: locForm.name,
      type: locForm.type,
      description: locForm.description,
      population: locForm.population,
      wealth: locForm.wealth,
    };
    try {
      if (editing) {
        await apiClient.patch(`/regions/${regionId}/locations/${editing.id}`, body);
        toast.success(t('editor.location_updated'));
      } else {
        await apiClient.post(`/regions/${regionId}/locations`, body);
        toast.success(t('editor.location_created'));
      }
      setLocModal(null);
      await refreshLocations();
    } catch {
      toast.error(editing ? t('editor.failed_update_location') : t('editor.failed_create_location'));
    }
  };

  const handleDeleteLocation = async (loc: Location) => {
    if (!loc.regionId) { toast.error(t('editor.location_no_region')); return; }
    if (!window.confirm(t('editor.confirm_delete_location', { name: loc.name }))) return;
    try {
      await apiClient.delete(`/regions/${loc.regionId}/locations/${loc.id}`);
      setLocModal(null);
      await refreshLocations();
    } catch {
      toast.error(t('editor.failed_delete_location'));
    }
  };

  const handleImageUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file || !id) return;
    const invalid = validateMapFile(file);
    if (invalid) {
      toast.error(invalid === 'size' ? t('editor.upload_too_large') : t('editor.upload_wrong_type'));
      return;
    }
    const previous = backgroundImage;
    setUploading(true);
    try {
      const formData = new FormData();
      formData.append('file', file);
      const res = await apiClient.post(`/worlds/${id}/map/upload`, formData);
      setBackgroundImage(BACKEND_ORIGIN + res.data.imageUrl);
      toast.success(t('editor.image_loaded'));
    } catch {
      // Kein stiller Fehlschlag: Vorschau zurücksetzen und melden.
      setBackgroundImage(previous);
      toast.error(t('editor.failed_upload'));
    } finally {
      setUploading(false);
    }
  };

  const handleRefresh = async () => {
    const next = await refreshRegions();
    await refreshLocations(next);
    redraw();
  };

  const modalTitle = locModal?.mode === 'edit'
    ? t('editor.edit_location_title')
    : t('editor.create_location_title');

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
            aria-label={t('editor.back')}
            title={t('editor.back')}
            className="text-text-secondary hover:text-accent"
          >
            <ArrowLeft size={20} />
          </button>
          <h1 className="text-lg font-heading text-text-primary">{t('editor.heading')}</h1>
          {(mode === 'draw' || mode === 'place') && (
            <span className="rounded bg-accent/20 px-2 py-0.5 text-xs text-accent">
              {mode === 'draw' ? t('editor.mode_drawing') : t('editor.mode_placing')}
            </span>
          )}
        </div>
        <div className="flex items-center gap-2">
          {mode === 'draw' && (
            <>
              <span className="text-xs text-text-secondary">
                {t('editor.points', { count: drawingPoints.length })}
              </span>
              <button
                onClick={handleUndoPoint}
                disabled={drawingPoints.length === 0}
                className="flex items-center gap-1 rounded border border-bg-elevated px-3 py-1.5 text-xs text-text-secondary hover:text-text-primary disabled:opacity-40"
              >
                <Undo2 size={14} /> {t('editor.undo_point')}
              </button>
              <button
                onClick={handleSavePolygon}
                disabled={drawingPoints.length < 3}
                className="flex items-center gap-1 rounded bg-accent px-3 py-1.5 text-xs text-white hover:bg-accent/80 disabled:opacity-40"
              >
                <Save size={14} /> {t('editor.save_polygon')}
              </button>
              <button
                onClick={handleCancelDraw}
                className="rounded border border-bg-elevated px-3 py-1.5 text-xs text-text-secondary hover:text-text-primary"
              >
                {t('editor.cancel')}
              </button>
            </>
          )}
        </div>
      </header>

      <div className="flex flex-1 overflow-hidden">
        <aside className="flex w-64 flex-col gap-4 overflow-y-auto border-r border-bg-elevated bg-bg-surface p-4">
          {/* Upload */}
          <div>
            <label className="mb-1 block text-xs text-text-secondary">{t('editor.map_image')}</label>
            <label className="flex cursor-pointer items-center gap-2 rounded border border-bg-elevated px-3 py-2 text-sm text-text-secondary hover:text-text-primary">
              <Upload size={14} />
              {uploading ? t('editor.uploading') : t('editor.upload')}
              <input
                type="file"
                accept="image/png,image/jpeg,image/webp"
                onChange={handleImageUpload}
                disabled={uploading}
                className="hidden"
              />
            </label>
            <p className="mt-1 text-[10px] text-text-secondary">{t('editor.upload_hint')}</p>
          </div>

          {/* Mode toggle */}
          <div>
            <label className="mb-1 block text-xs text-text-secondary">{t('editor.mode')}</label>
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
                  {m === 'view' ? t('editor.mode_view') : m === 'draw' ? t('editor.mode_draw') : t('editor.mode_place')}
                </button>
              ))}
            </div>
          </div>

          {/* Regions list */}
          <div>
            <h3 className="mb-2 text-xs font-heading uppercase tracking-wider text-text-secondary">
              {t('editor.regions')}
            </h3>
            <div className="space-y-1">
              {regions.length === 0 && <p className="text-xs text-text-secondary">{t('editor.no_regions')}</p>}
              {regions.map((region) => (
                <div
                  key={region.id}
                  className={`rounded px-2 py-1 hover:bg-bg-elevated/50 ${
                    selectedRegion === region.id ? 'bg-accent/10' : ''
                  }`}
                >
                  <div className="flex items-center justify-between gap-2">
                    <div className="flex min-w-0 items-center gap-2">
                      <span
                        className="inline-block h-2.5 w-2.5 rounded-full shrink-0"
                        style={{ backgroundColor: `#${regionColor(region.id).toString(16).padStart(6, '0')}` }}
                      />
                      <span className="truncate text-sm text-text-primary">{region.name}</span>
                    </div>
                    <div className="flex shrink-0 items-center gap-1">
                      <button
                        onClick={() => handleRenameRegion(region)}
                        aria-label={t('editor.rename')}
                        title={t('editor.rename')}
                        className="text-text-secondary hover:text-accent"
                      >
                        <Pencil size={13} />
                      </button>
                      <button
                        onClick={() => handleDeleteRegion(region)}
                        aria-label={t('editor.delete')}
                        title={t('editor.delete')}
                        className="text-text-secondary hover:text-danger"
                      >
                        <Trash2 size={13} />
                      </button>
                    </div>
                  </div>
                  <div className="mt-0.5 flex items-center gap-2 text-xs">
                    <button
                      onClick={() => handleStartDraw(region.id)}
                      className="text-accent hover:text-accent/80"
                    >
                      {region.polygonPoints ? t('editor.redraw_polygon') : t('editor.draw_polygon')}
                    </button>
                    {region.polygonPoints && (
                      <button
                        onClick={() => handleClearPolygon(region)}
                        className="text-text-secondary hover:text-danger"
                      >
                        {t('editor.clear_polygon')}
                      </button>
                    )}
                  </div>
                </div>
              ))}
              <button
                onClick={handleCreateRegion}
                className="w-full rounded border border-dashed border-bg-elevated py-1 text-xs text-text-secondary hover:text-accent hover:border-accent/50"
              >
                {t('editor.create_region')}
              </button>
            </div>
          </div>

          {/* Locations list */}
          <div>
            <h3 className="mb-2 text-xs font-heading uppercase tracking-wider text-text-secondary">
              {t('editor.locations')}
            </h3>
            <div className="space-y-1">
              {locations.length === 0 && (
                <p className="text-xs text-text-secondary">{t('editor.no_locations')}</p>
              )}
              {locations.map((loc) => (
                <div
                  key={loc.id}
                  className={`flex items-center justify-between gap-2 rounded px-2 py-1 ${
                    selectedLocation === loc.id ? 'bg-accent/20 text-accent' : 'text-text-primary hover:bg-bg-elevated/50'
                  } ${mode === 'place' ? 'cursor-pointer' : ''}`}
                  onClick={() => {
                    if (mode === 'place') {
                      setSelectedLocation(loc.id);
                      toast.info(t('editor.place_location', { name: loc.name }));
                    }
                  }}
                >
                  <span className="flex min-w-0 items-center gap-1.5">
                    <span aria-hidden="true">{locationTypeIcon(loc.type)}</span>
                    <span className="truncate">{loc.name}</span>
                    {loc.positionJson && <span className="text-xs text-success" aria-hidden="true">✓</span>}
                  </span>
                  <span className="flex shrink-0 items-center gap-1">
                    <button
                      onClick={(e) => { e.stopPropagation(); openEditLocation(loc); }}
                      aria-label={t('editor.edit')}
                      title={t('editor.edit')}
                      className="text-text-secondary hover:text-accent"
                    >
                      <Pencil size={12} />
                    </button>
                    <button
                      onClick={(e) => { e.stopPropagation(); handleDeleteLocation(loc); }}
                      aria-label={t('editor.delete')}
                      title={t('editor.delete')}
                      className="text-text-secondary hover:text-danger"
                    >
                      <Trash2 size={12} />
                    </button>
                  </span>
                </div>
              ))}
              <button
                onClick={openCreateLocation}
                className="w-full rounded border border-dashed border-bg-elevated py-1 text-xs text-text-secondary hover:text-accent hover:border-accent/50"
              >
                {t('editor.create_location')}
              </button>
            </div>
          </div>

          <button
            onClick={handleRefresh}
            className="w-full rounded border border-bg-elevated py-1.5 text-xs text-text-secondary hover:text-accent hover:border-accent/50"
          >
            {t('editor.refresh_map', {
              r: regions.length, l: locations.length,
              bg: backgroundImage ? t('editor.bg_yes') : t('editor.bg_no'),
            })}
          </button>
          {mode === 'draw' && (
            <div className="mt-auto rounded bg-accent/10 p-3">
              <p className="text-xs text-text-secondary">{t('editor.draw_help')}</p>
            </div>
          )}
          {mode === 'place' && (
            <div className="mt-auto rounded bg-accent/10 p-3">
              <p className="text-xs text-text-secondary">
                {selectedLocation
                  ? t('editor.place_help_selected')
                  : t('editor.place_help_empty')}
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

      {/* Create/Edit Location Modal */}
      {(locModal?.mode === 'create' || locModal?.mode === 'edit') && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/60"
          onClick={() => setLocModal(null)}
        >
          <div
            role="dialog"
            aria-modal="true"
            aria-label={modalTitle}
            className="w-80 rounded-xl border border-bg-elevated bg-bg-surface p-5 shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between mb-4">
              <h3 className="font-heading text-text-primary">{modalTitle}</h3>
              <button
                onClick={() => setLocModal(null)}
                aria-label={t('editor.cancel')}
                title={t('editor.cancel')}
                className="text-text-secondary hover:text-text-primary"
              >
                <X size={16} />
              </button>
            </div>
            <div className="space-y-3">
              <div>
                <label className="block text-xs text-text-secondary mb-1">{t('editor.name')}</label>
                <input
                  value={locForm.name}
                  onChange={(e) => setLocForm((f) => ({ ...f, name: e.target.value }))}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                />
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">{t('editor.type')}</label>
                <select
                  value={locForm.type}
                  onChange={(e) => setLocForm((f) => ({ ...f, type: e.target.value }))}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                >
                  {POI_TYPES.map((type) => (
                    <option key={type} value={type}>{locationTypeIcon(type)} {type}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">{t('editor.region')}</label>
                {locModal?.mode === 'edit' ? (
                  <p className="text-sm text-text-primary">{locForm.regionName || '—'}</p>
                ) : (
                  <select
                    value={locForm.regionName}
                    onChange={(e) => setLocForm((f) => ({ ...f, regionName: e.target.value }))}
                    className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                  >
                    {regions.map((r) => <option key={r.id} value={r.name}>{r.name}</option>)}
                  </select>
                )}
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">{t('editor.description')}</label>
                <textarea
                  value={locForm.description}
                  onChange={(e) => setLocForm((f) => ({ ...f, description: e.target.value }))}
                  rows={2}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent resize-none"
                />
              </div>
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block text-xs text-text-secondary mb-1">{t('editor.population')}</label>
                  <input
                    type="number"
                    min={0}
                    value={locForm.population}
                    onChange={(e) => setLocForm((f) => ({ ...f, population: Number(e.target.value) }))}
                    className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                  />
                </div>
                <div>
                  <label className="block text-xs text-text-secondary mb-1">{t('editor.wealth')}</label>
                  <input
                    type="number"
                    min={1}
                    max={10}
                    value={locForm.wealth}
                    onChange={(e) => setLocForm((f) => ({ ...f, wealth: Number(e.target.value) }))}
                    className="w-full rounded border border-bg-elevated bg-bg-primary px-3 py-2 text-sm text-text-primary outline-none focus:border-accent"
                  />
                </div>
              </div>
              <button
                onClick={handleSubmitLocation}
                disabled={!locForm.name || !locForm.regionName}
                className="w-full rounded bg-accent py-2 text-sm text-white hover:bg-accent/80 disabled:opacity-40"
              >
                {locModal?.mode === 'edit' ? t('editor.apply') : t('editor.create')}
              </button>
              <button
                onClick={() => setLocModal(null)}
                className="w-full rounded border border-bg-elevated py-2 text-sm text-text-secondary hover:text-text-primary"
              >
                {t('editor.cancel')}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Location Info Modal */}
      {locModal?.mode === 'info' && locModal.location && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/60"
          onClick={() => setLocModal(null)}
        >
          <div
            role="dialog"
            aria-modal="true"
            aria-label={locModal.location.name}
            className="w-72 rounded-xl border border-bg-elevated bg-bg-surface p-5 shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between mb-3">
              <h3 className="font-heading text-text-primary">
                {locationTypeIcon(locModal.location.type)} {locModal.location.name}
              </h3>
              <button
                onClick={() => setLocModal(null)}
                aria-label={t('editor.cancel')}
                title={t('editor.cancel')}
                className="text-text-secondary hover:text-text-primary"
              >
                <X size={16} />
              </button>
            </div>
            <p className="text-xs text-text-secondary mb-2">
              {t('editor.type_label', { type: locModal.location.type })}
            </p>
            {locModal.location.description && (
              <p className="text-sm text-text-primary mb-2">{locModal.location.description}</p>
            )}
            <p className="text-xs text-text-secondary">
              {t('editor.population_wealth', {
                pop: locModal.location.population ?? 0,
                w: locModal.location.wealth ?? 5,
              })}
            </p>
            {locModal.location.positionJson && (
              <p className="text-xs text-text-secondary mt-1">{t('editor.placed_on_map')}</p>
            )}
            <div className="mt-4 flex flex-wrap gap-2">
              <button
                onClick={() => openEditLocation(locModal.location!)}
                className="flex items-center gap-1 rounded bg-accent px-3 py-1.5 text-xs text-white hover:bg-accent/80"
              >
                <Pencil size={12} /> {t('editor.edit')}
              </button>
              <button
                onClick={() => {
                  const loc = locModal.location!;
                  setLocModal(null);
                  setSelectedLocation(loc.id);
                  setMode('place');
                  toast.info(t('editor.place_location', { name: loc.name }));
                }}
                className="flex items-center gap-1 rounded border border-bg-elevated px-3 py-1.5 text-xs text-text-secondary hover:text-text-primary"
              >
                <Move size={12} /> {t('editor.move')}
              </button>
              <button
                onClick={() => handleDeleteLocation(locModal.location!)}
                className="flex items-center gap-1 rounded border border-danger/40 px-3 py-1.5 text-xs text-danger hover:bg-danger/10"
              >
                <Trash2 size={12} /> {t('editor.delete')}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
