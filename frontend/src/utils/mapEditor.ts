/**
 * Reine Helfer des Karten-Editors (testbar ohne Pixi/DOM):
 * stabile Regionsfarben, POI-Typen/Icons, Upload-Validierung.
 */

export const MAP_MAX_BYTES = 10 * 1024 * 1024;

export const POLY_COLORS = [
  0x3b82f6, 0x22c55e, 0xf59e0b, 0xa855f7, 0xef4444, 0x06b6d4, 0x84cc16, 0xf97316,
];

/** Stabile Farbe pro Region-ID — bleibt beim Sortieren/Neuzeichnen gleich. */
export function regionColor(id: string): number {
  let hash = 0;
  for (let i = 0; i < id.length; i++) {
    hash = (hash * 31 + id.charCodeAt(i)) >>> 0;
  }
  return POLY_COLORS[hash % POLY_COLORS.length];
}

export function regionColorHex(id: string): string {
  return `#${regionColor(id).toString(16).padStart(6, '0')}`;
}

export const POI_TYPES = [
  'village', 'town', 'city', 'hamlet', 'fortress', 'ruin', 'dungeon', 'tower',
  'shrine', 'camp', 'mine', 'farm', 'inn', 'harbor', 'bridge',
] as const;

const POI_ICONS: Record<string, string> = {
  city: '🏙️',
  town: '🏘️',
  village: '🏡',
  hamlet: '🛖',
  fortress: '🏰',
  ruin: '🏚️',
  dungeon: '🕳️',
  tower: '🗼',
  shrine: '⛩️',
  camp: '⛺',
  mine: '⛏️',
  farm: '🌾',
  inn: '🍺',
  harbor: '⚓',
  bridge: '🌉',
};

export function locationTypeIcon(type?: string | null): string {
  if (!type) return '📍';
  return POI_ICONS[type.trim().toLowerCase()] ?? '📍';
}

export type MapFileError = 'type' | 'size' | null;

/** Client-seitige Vorprüfung (Server prüft zusätzlich Magic Bytes). */
export function validateMapFile(
  file: { type?: string; size?: number } | null | undefined,
): MapFileError {
  if (!file) return 'type';
  if (!file.type || !file.type.toLowerCase().startsWith('image/')) return 'type';
  if ((file.size ?? 0) > MAP_MAX_BYTES) return 'size';
  return null;
}
