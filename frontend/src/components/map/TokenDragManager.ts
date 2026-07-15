import type { Graphics, Text, Container, FederatedPointerEvent } from 'pixi.js';
import { useWorldStore } from '../../store/worldStore';
import { stompSend } from '../../hooks/useWorldSocket';
import { moveToken, selectToken } from './Token';

const DRAG_THROTTLE_MS = 33; // ~30fps

/**
 * Aktiviert Drag-n-Drop auf einem Token-Graphics.
 * - pointerdown: Offset speichern, draggen starten
 * - pointermove: Position live updaten (throttled)
 * - pointerup: Finale Position → stompSend + Store
 */
export function enableDrag(
  tokenId: string,
  gfx: Graphics,
  label: Text,
  worldId: string,
  stage: Container,
) {
  let dragging = false;
  let offsetX = 0;
  let offsetY = 0;
  let lastMove = 0;

  const onDown = (e: FederatedPointerEvent) => {
    dragging = true;
    const pos = e.getLocalPosition(stage);
    offsetX = gfx.x - pos.x;
    offsetY = gfx.y - pos.y;
    gfx.alpha = 0.9;
    gfx.scale.set(1.15);
    selectToken(gfx, true);
  };

  const onMove = (e: FederatedPointerEvent) => {
    if (!dragging) return;
    const now = Date.now();
    if (now - lastMove < DRAG_THROTTLE_MS) return;
    lastMove = now;

    const pos = e.getLocalPosition(stage);
    const newX = Math.max(0, pos.x + offsetX);
    const newY = Math.max(0, pos.y + offsetY);
    moveToken(gfx, label, newX, newY);
  };

  const onUp = () => {
    if (!dragging) return;
    dragging = false;
    gfx.alpha = 0.7;
    gfx.scale.set(1);
    selectToken(gfx, false);

    // Per STOMP an andere Clients senden
    stompSend(`/token/move/${worldId}`, {
      entityId: tokenId,
      x: gfx.x,
      y: gfx.y,
    });

    // Lokalen Store aktualisieren
    useWorldStore.getState().updateTokenPosition(tokenId, gfx.x, gfx.y);
  };

  gfx.on('pointerdown', onDown);
  gfx.on('globalpointermove', onMove);
  gfx.on('pointerup', onUp);
  gfx.on('pointerupoutside', onUp);
  gfx.eventMode = 'static';
  gfx.cursor = 'grab';
}

export function disableDrag(gfx: Graphics) {
  gfx.removeAllListeners('pointerdown');
  gfx.removeAllListeners('globalpointermove');
  gfx.removeAllListeners('pointerup');
  gfx.removeAllListeners('pointerupoutside');
  gfx.eventMode = 'none';
  gfx.cursor = 'default';
}
