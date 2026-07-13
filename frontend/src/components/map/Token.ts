import * as PIXI from 'pixi.js';

export interface TokenData {
  id: string;
  name: string;
  x: number;
  y: number;
  color?: number;
  isSelected?: boolean;
}

const TOKEN_RADIUS = 20;
const COLORS = [0x5bb8c5, 0xe0556b, 0x7ac784, 0xf2a65a, 0xa78bfa];

/**
 * Erzeugt einen runden Token auf dem Canvas.
 * @returns Graphics-Objekt und Label
 */
export function createToken(token: TokenData): { gfx: PIXI.Graphics; label: PIXI.Text } {
  const color = token.color ?? COLORS[Math.floor(Math.random() * COLORS.length)];

  const gfx = new PIXI.Graphics();
  gfx.beginFill(color, token.isSelected ? 0.8 : 0.6);
  gfx.lineStyle(2, token.isSelected ? 0xffffff : color, 0.9);
  gfx.drawCircle(0, 0, TOKEN_RADIUS);
  gfx.endFill();
  gfx.x = token.x;
  gfx.y = token.y;
  gfx.eventMode = 'static';
  gfx.cursor = 'pointer';

  const label = new PIXI.Text(token.name.charAt(0).toUpperCase(), {
    fontSize: 14,
    fill: 0xffffff,
    fontWeight: 'bold',
  });
  label.anchor.set(0.5);
  label.x = token.x;
  label.y = token.y;

  return { gfx, label };
}

/**
 * Aktualisiert Token-Position (nach Drag).
 */
export function moveToken(gfx: PIXI.Graphics, label: PIXI.Text, x: number, y: number) {
  gfx.x = x;
  gfx.y = y;
  label.x = x;
  label.y = y;
}

/**
 * Hebt Token hervor (Selection).
 */
export function selectToken(gfx: PIXI.Graphics, selected: boolean) {
  gfx.alpha = selected ? 1.0 : 0.7;
}
