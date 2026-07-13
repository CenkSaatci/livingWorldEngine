import { useEffect, useRef } from 'react';
import * as PIXI from 'pixi.js';
import { usePixiApp } from './usePixiApp';
import { drawGrid } from './Grid';

interface Props {
  cols?: number;
  rows?: number;
  tileSize?: number;
}

/**
 * PixiJS-Karten-Canvas mit Grid, Zoom und Pan (v7 API via usePixiApp).
 */
export function MapCanvas({ cols = 20, rows = 15, tileSize = 48 }: Props) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const { getApp } = usePixiApp(containerRef);
  const gridDrawn = useRef(false);

  useEffect(() => {
    const app = getApp();
    if (!app || gridDrawn.current) return;

    const gfx = new PIXI.Graphics();
    drawGrid(gfx, cols, rows, tileSize);
    app.stage.addChild(gfx);
    gridDrawn.current = true;
  }, [getApp, cols, rows, tileSize]);

  return (
    <div
      ref={containerRef}
      className="h-full w-full overflow-hidden rounded-lg border border-bg-elevated"
      style={{ minHeight: 400, position: 'relative' }}
    />
  );
}
