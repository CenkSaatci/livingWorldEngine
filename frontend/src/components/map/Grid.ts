import * as PIXI from 'pixi.js';

const LINE_COLOR = 0x2a3640;
const LINE_WIDTH = 1;

/**
 * Zeichnet ein quadratisches Grid n×m auf das gegebene PIXI.Graphics-Objekt.
 * Wird in MapCanvas verwendet.
 */
export function drawGrid(gfx: PIXI.Graphics, cols: number, rows: number, tileSize: number) {
  gfx.clear();
  gfx.lineStyle(LINE_WIDTH, LINE_COLOR, 0.5);

  // Vertikale Linien
  for (let x = 0; x <= cols; x++) {
    gfx.moveTo(x * tileSize, 0);
    gfx.lineTo(x * tileSize, rows * tileSize);
  }

  // Horizontale Linien
  for (let y = 0; y <= rows; y++) {
    gfx.moveTo(0, y * tileSize);
    gfx.lineTo(cols * tileSize, y * tileSize);
  }
}
