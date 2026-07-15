import { useEffect, useRef } from 'react';
import * as PIXI from 'pixi.js';
import { usePixiApp } from './usePixiApp';
import { drawGrid } from './Grid';
import { useTokenLayer } from './useTokenLayer';
import { useFogLayer } from './useFogLayer';
import { apiClient } from '../../api/client';

interface Props {
  cols?: number;
  rows?: number;
  tileSize?: number;
  worldId?: string;
  mapId?: string;
}

interface MapData {
  id: string;
  image_url: string | null;
  width: number;
  height: number;
}

export function MapCanvas({ cols = 20, rows = 15, tileSize = 48, worldId = '', mapId }: Props) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const { getApp } = usePixiApp(containerRef);
  const tokenLayerRef = useRef<PIXI.Container | null>(null);
  const gridDrawn = useRef(false);
  const stageRef = useRef<PIXI.Container | null>(null);
  const mapDataRef = useRef<MapData | null>(null);

  // Fetch map data when mapId changes
  useEffect(() => {
    if (!mapId) {
      mapDataRef.current = null;
      return;
    }
    let cancelled = false;
    apiClient
      .get(`/maps/${mapId}`)
      .then((r) => {
        if (!cancelled) mapDataRef.current = r.data as MapData;
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, [mapId]);

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
