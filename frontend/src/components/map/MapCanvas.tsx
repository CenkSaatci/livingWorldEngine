import { useEffect, useRef, useState } from 'react';
import * as PIXI from 'pixi.js';
import { usePixiApp } from './usePixiApp';
import { drawGrid } from './Grid';
import { useTokenLayer } from './useTokenLayer';
import { useFogLayer } from './useFogLayer';
import { apiClient, BACKEND_ORIGIN } from '../../api/client';

interface Props {
  cols?: number;
  rows?: number;
  tileSize?: number;
  worldId?: string;
  mapId?: string;
}

interface MapData {
  id: string;
  imageUrl: string | null;
  width: number;
  height: number;
}

export function MapCanvas({ cols = 20, rows = 15, tileSize = 48, worldId = '', mapId }: Props) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const { getApp } = usePixiApp(containerRef);
  const tokenLayerRef = useRef<PIXI.Container | null>(null);
  const gridDrawn = useRef(false);
  const stageRef = useRef<PIXI.Container | null>(null);
  const bgSpriteRef = useRef<PIXI.Sprite | null>(null);
  const [bgUrl, setBgUrl] = useState<string | null>(null);

  // Fetch map data
  useEffect(() => {
    if (!worldId) return;
    let cancelled = false;
    const id = mapId;
    if (id) {
      apiClient.get(`/maps/${id}`)
        .then((r) => { if (!cancelled) setBgUrl((r.data as MapData).imageUrl); })
        .catch(() => {});
    } else {
      apiClient.get(`/worlds/${worldId}/map`)
        .then((r) => { if (!cancelled) setBgUrl((r.data as MapData).imageUrl); })
        .catch(() => {});
    }
    return () => { cancelled = true; };
  }, [worldId, mapId]);

  // Draw background image
  useEffect(() => {
    const app = getApp();
    if (!app || !bgUrl) return;
    const texture = PIXI.Texture.from(BACKEND_ORIGIN + bgUrl);
    const sprite = new PIXI.Sprite(texture);
    app.stage.addChildAt(sprite, 0);
    bgSpriteRef.current = sprite;
    return () => {
      if (bgSpriteRef.current) {
        try { app.stage.removeChild(bgSpriteRef.current); bgSpriteRef.current.destroy(true); } catch {}
        bgSpriteRef.current = null;
      }
    };
  }, [getApp, bgUrl]);

  useEffect(() => {
    const app = getApp();
    if (!app || gridDrawn.current) return;

    stageRef.current = app.stage;

    const grid = new PIXI.Graphics();
    drawGrid(grid, cols, rows, tileSize);
    app.stage.addChild(grid);

    const tokens = new PIXI.Container();
    app.stage.addChild(tokens);
    tokenLayerRef.current = tokens;

    gridDrawn.current = true;
  }, [getApp, cols, rows, tileSize]);

  useTokenLayer(tokenLayerRef.current, worldId);
  useFogLayer(stageRef.current);

  return (
    <div
      ref={containerRef}
      data-canvas
      role="img"
      aria-label="Game map canvas"
      className="h-full w-full overflow-hidden rounded-lg border border-bg-elevated"
      style={{ minHeight: 400, position: 'relative' }}
    />
  );
}
