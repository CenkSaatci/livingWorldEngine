import { useEffect, useRef } from 'react';
import * as PIXI from 'pixi.js';
import { useFogStore } from '../../store/fogStore';

/**
 * Fog-of-War-Layer, der auf fogStore.visible reagiert.
 * Rendert ein dunkles Overlay über dem Grid.
 */
export function useFogLayer(stage: PIXI.Container | null) {
  const fogRef = useRef<PIXI.Graphics | null>(null);
  const visible = useFogStore((s) => s.visible);

  useEffect(() => {
    if (!stage) return;

    if (visible && !fogRef.current) {
      const overlay = new PIXI.Graphics();
      overlay.beginFill(0x000000, 0.7);
      overlay.drawRect(-5000, -5000, 10000, 10000);
      overlay.endFill();
      overlay.name = 'fog-overlay';
      // Insert at layer 1 (after grid at 0, before tokens)
      if (stage.children.length > 1) {
        stage.addChildAt(overlay, 1);
      } else {
        stage.addChild(overlay);
      }
      fogRef.current = overlay;
    }

    if (!visible && fogRef.current) {
      stage.removeChild(fogRef.current);
      fogRef.current.destroy();
      fogRef.current = null;
    }

    return () => {
      if (fogRef.current && stage) {
        stage.removeChild(fogRef.current);
        fogRef.current.destroy();
        fogRef.current = null;
      }
    };
  }, [visible, stage]);
}
