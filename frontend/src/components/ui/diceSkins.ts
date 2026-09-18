/** Würfel-Skins für die 3D-Darstellung (aus ThreeDice ausgelagert, Fast-Refresh-Regel). */
export interface DiceSkin {
  name: string;
  faceBg: string;
  dotColor: string;
  numColor: string;
  edgeColor: number;
  meshColor: number;
}

export const DICE_SKINS: DiceSkin[] = [
  { name: 'classic', faceBg: '#f8f4f0', dotColor: '#1a1a2e', numColor: '#1a1a2e', edgeColor: 0x000000, meshColor: 0xf8f4f0 },
  { name: 'cyber', faceBg: '#0a0a1a', dotColor: '#00d2ff', numColor: '#00d2ff', edgeColor: 0x00d2ff, meshColor: 0x0a0a1a },
  { name: 'metal', faceBg: '#2a2a2a', dotColor: '#ffd700', numColor: '#ffd700', edgeColor: 0x888888, meshColor: 0x3a3a3a },
  { name: 'wood', faceBg: '#deb887', dotColor: '#4a2800', numColor: '#4a2800', edgeColor: 0x8b6914, meshColor: 0xdeb887 },
];
