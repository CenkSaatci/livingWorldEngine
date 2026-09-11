import { useCallback, useEffect, useRef } from 'react';
import * as PIXI from 'pixi.js';

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

    const observer = new ResizeObserver(() => {
      if (!el || !appRef.current) return;
      // clientWidth/Height = Content-Box: getBoundingClientRect (Border-Box) wuerde
      // durch den 1px-Rahmen eine endlose Resize-Schleife erzeugen (finaler Audit).
      const width = el.clientWidth;
      const height = el.clientHeight;
      if (width > 0 && height > 0) {
        appRef.current.renderer.resize(width, height);
      }
    });
    observer.observe(el);

    const view = app.view as HTMLCanvasElement;
    view.addEventListener('wheel', (e: WheelEvent) => {
      e.preventDefault();
      const rect = view.getBoundingClientRect();
      const mouseX = e.clientX - rect.left;
      const mouseY = e.clientY - rect.top;

      const oldZoom = viewportRef.current.zoom;
      const factor = e.deltaY > 0 ? 0.9 : 1.1;
      const newZoom = Math.min(4, Math.max(0.25, oldZoom * factor));

      const worldX = (mouseX - viewportRef.current.x) / oldZoom;
      const worldY = (mouseY - viewportRef.current.y) / oldZoom;

      viewportRef.current.zoom = newZoom;
      viewportRef.current.x = mouseX - worldX * newZoom;
      viewportRef.current.y = mouseY - worldY * newZoom;

      app.stage.scale.set(newZoom);
      app.stage.position.set(viewportRef.current.x, viewportRef.current.y);
    }, { passive: false });

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

    view.addEventListener('touchstart', (e: TouchEvent) => {
      if (e.touches.length === 0) return;
      panning = true;
      panStart = { x: e.touches[0].clientX, y: e.touches[0].clientY };
      viewStart = { ...viewportRef.current };
      view.style.cursor = 'grabbing';
    });

    window.addEventListener('touchmove', (e: TouchEvent) => {
      if (!panning || e.touches.length === 0) return;
      const dx = e.touches[0].clientX - panStart.x;
      const dy = e.touches[0].clientY - panStart.y;
      viewportRef.current.x = viewStart.x + dx;
      viewportRef.current.y = viewStart.y + dy;
      app.stage.position.set(viewportRef.current.x, viewportRef.current.y);
    });

    window.addEventListener('touchend', () => {
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

  const getApp = useCallback(() => appRef.current, []);
  const getViewport = useCallback(() => viewportRef.current, []);

  return { getApp, getViewport };
}
