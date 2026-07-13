import * as PIXI from 'pixi.js';

/**
 * Zeichnet einen Fog-of-War-Layer (dunkle Abdeckung) auf das Canvas.
 * DM kann via setFog kreisförmige Sichtbereiche freilegen.
 */
export class FogOfWar {
  private background: PIXI.Graphics;
  private revealContainer: PIXI.Container;

  constructor(stage: PIXI.Container) {
    // Halbtransparenter schwarzer Overlay
    this.background = new PIXI.Graphics();
    this.background.beginFill(0x000000, 0.75);
    this.background.drawRect(-5000, -5000, 10000, 10000);
    this.background.endFill();
    stage.addChild(this.background);

    // Container für freigelegte Bereiche
    this.revealContainer = new PIXI.Container();
    stage.addChild(this.revealContainer);
  }

  /**
   * Legt einen kreisförmigen Sichtbereich frei.
   */
  revealCircle(x: number, y: number, radius: number) {
    const hole = new PIXI.Graphics();
    hole.beginFill(0x000000, 0.0); // transparent hole
    hole.beginHole();
    hole.drawCircle(0, 0, radius);
    hole.endHole();
    hole.x = x;
    hole.y = y;
    this.revealContainer.addChild(hole);
  }

  /**
   * Entfernt alle freigelegten Bereiche (reset).
   */
  reset() {
    this.revealContainer.removeChildren();
  }
}
