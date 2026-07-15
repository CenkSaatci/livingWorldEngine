import { useEffect, useRef } from 'react';
import type { Graphics, Text, Container } from 'pixi.js';
import { useWorldStore } from '../../store/worldStore';
import { createToken, moveToken } from './Token';
import { enableDrag, disableDrag } from './TokenDragManager';

interface TokenEntry {
  gfx: Graphics;
  label: Text;
}

export function useTokenLayer(container: Container | null, worldId: string) {
  const tokensRef = useRef<Map<string, TokenEntry>>(new Map());
  const containerRef = useRef(container);
  containerRef.current = container;

  useEffect(() => {
    const tokens = useWorldStore.getState().tokens;
    const stage = containerRef.current;
    if (!stage) return;

    const current = tokensRef.current;
    for (const token of tokens) {
      const existing = current.get(token.entityId);
      if (existing) {
        if (existing.gfx.x !== token.x || existing.gfx.y !== token.y) {
          moveToken(existing.gfx, existing.label, token.x, token.y);
        }
      } else {
        const { gfx, label } = createToken({
          id: token.entityId,
          name: token.name,
          x: token.x,
          y: token.y,
        });
        stage.addChild(gfx, label);
        enableDrag(token.entityId, gfx, label, worldId, stage);
        current.set(token.entityId, { gfx, label });
      }
    }
  }, [worldId]);

  useEffect(() => {
    const stage = containerRef.current;
    if (!stage) return;

    const unsub = useWorldStore.subscribe(() => {
      const tokens = useWorldStore.getState().tokens;
      const current = tokensRef.current;
      const newIds = new Set(tokens.map((t) => t.entityId));
      const container = containerRef.current;
      if (!container) return;

      for (const [id, entry] of current) {
        if (!newIds.has(id)) {
          disableDrag(entry.gfx);
          container.removeChild(entry.gfx, entry.label);
          entry.gfx.destroy();
          entry.label.destroy();
          current.delete(id);
        }
      }

      for (const token of tokens) {
        const existing = current.get(token.entityId);
        if (existing) {
          if (existing.gfx.x !== token.x || existing.gfx.y !== token.y) {
            moveToken(existing.gfx, existing.label, token.x, token.y);
          }
        } else {
          const { gfx, label } = createToken({
            id: token.entityId,
            name: token.name,
            x: token.x,
            y: token.y,
          });
          container.addChild(gfx, label);
          enableDrag(token.entityId, gfx, label, worldId, container);
          current.set(token.entityId, { gfx, label });
        }
      }
    });

    return () => {
      unsub();
      const c = containerRef.current;
      if (!c) return;
      const currentTokens = tokensRef.current; // eslint-disable-line react-hooks/exhaustive-deps
      for (const [, entry] of currentTokens) {
        disableDrag(entry.gfx);
        c.removeChild(entry.gfx, entry.label);
        entry.gfx.destroy();
        entry.label.destroy();
      }
      currentTokens.clear();
    };
  }, [worldId]);
}
