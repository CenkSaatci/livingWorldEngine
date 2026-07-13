import { useCallback, useEffect, useRef } from 'react';
import * as PIXI from 'pixi.js';

/**
 * Erzeugt und steuert eine PIXI.Application (v7) innerhalb eines Containers.
 * Berücksichtigt DPR (Retina), Resize, Zoom und Pan.
 */
export function usePixiApp(containerRef: React.RefObject<HTMLDivElement | null>) {
  const appRef = useRef<PIXI.Application | null>(null);
  const viewportRef = useRef({ x: 0, y: 0, zoom: 1 });

  const initApp = useCallback(() => {
    if (!containerRef.current || appRef.current) return;

    const el = containerRef.current;
    const rect = el.getBoundingClientRect();

    const app = new PIXI.Application({
      width: rect.width,
      height: rect.height,
      backgroundColor: 0x1a2128,
      antialias: true,
      resolution: window.devicePixelRatio || 1,
      autoDensity: true,
    });

    el.appendChild(app.view as unknown as HTMLElement);
    appRef.current = app;

    // Zoom (Mausrad)
    const view = app.view as HTMLCanvasElement;
    view.addEventListener('wheel', (e: WheelEvent) => {
      e.preventDefault();
      const delta = e.deltaY > 0 ? 0.9 : 1.1;
      viewportRef.current.zoom = Math.min(4, Math.max(0.25, viewportRef.current.zoom * delta));
      app.stage.scale.set(viewportRef.current.zoom);
    }, { passive: false });

    // Pan (Drag im leeren Bereich)
    let panning = false;
    let panStart = { x: 0, y: 0 };
    let viewStart = { x: 0, y: 0 };

    view.addEventListener('mousedown', (e: MouseEvent) => {
      panning = true;
      panStart = { x: e.clientX, y: e.clientY };
      viewStart = { ...viewportRef.current };
      view.style.cursor = 'grabbing';
    });

    window.addEventListener('mousemove', (e: MouseEvent) => {
      if (!panning) return;
      const dx = e.clientX - panStart.x;
      const dy = e.clientY - panStart.y;
      viewportRef.current.x = viewStart.x + dx;
      viewportRef.current.y = viewStart.y + dy;
      app.stage.position.set(viewportRef.current.x, viewportRef.current.y);
    });

    window.addEventListener('mouseup', () => {
      panning = false;
      view.style.cursor = 'default';
    });
  }, [containerRef]);

  useEffect(() => {
    initApp();
    return () => {
      if (appRef.current) {
        appRef.current.destroy(true, { children: true });
        appRef.current = null;
      }
    };
  }, [initApp]);

  const getApp = () => appRef.current;
  const getViewport = () => viewportRef.current;

  return { getApp, getViewport };
}
