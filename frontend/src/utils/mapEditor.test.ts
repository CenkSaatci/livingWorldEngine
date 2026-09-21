import { describe, it, expect } from 'vitest';
import {
  MAP_MAX_BYTES,
  POLY_COLORS,
  POI_TYPES,
  locationTypeIcon,
  regionColor,
  regionColorHex,
  validateMapFile,
} from './mapEditor';

describe('regionColor', () => {
  it('ist stabil pro ID und liegt in der Palette', () => {
    const a = regionColor('region-1');
    expect(regionColor('region-1')).toBe(a);
    expect(POLY_COLORS).toContain(a);
  });

  it('liefert gültige Hex-Farben', () => {
    expect(regionColorHex('abc')).toMatch(/^#[0-9a-f]{6}$/);
  });
});

describe('locationTypeIcon', () => {
  it('kennt Städte, Dörfer und Ruinen', () => {
    expect(locationTypeIcon('city')).toBe('🏙️');
    expect(locationTypeIcon('village')).toBe('🏡');
    expect(locationTypeIcon('ruin')).toBe('🏚️');
  });

  it('ist tolerant und fällt auf den Standard-Pin zurück', () => {
    expect(locationTypeIcon(' CITY ')).toBe('🏙️');
    expect(locationTypeIcon('unbekannt')).toBe('📍');
    expect(locationTypeIcon(null)).toBe('📍');
  });

  it('deckt alle angebotenen Typen ab', () => {
    for (const type of POI_TYPES) {
      expect(locationTypeIcon(type)).not.toBe('📍');
    }
  });
});

describe('validateMapFile', () => {
  it('akzeptiert Bilder bis 10 MB', () => {
    expect(validateMapFile({ type: 'image/png', size: MAP_MAX_BYTES })).toBeNull();
  });

  it('lehnt Nicht-Bilder und zu große Dateien ab', () => {
    expect(validateMapFile({ type: 'text/plain', size: 10 })).toBe('type');
    expect(validateMapFile({ type: 'image/png', size: MAP_MAX_BYTES + 1 })).toBe('size');
    expect(validateMapFile(null)).toBe('type');
  });
});
