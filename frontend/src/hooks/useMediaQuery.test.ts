import { describe, it, expect, beforeAll } from 'vitest';
import { renderHook } from '@testing-library/react';
import { useMediaQuery } from './useMediaQuery';

beforeAll(() => {
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: (query: string) => ({
      matches: query === '(max-width: 1px)',
      addEventListener: () => {},
      removeEventListener: () => {},
    }),
  });
});

describe('useMediaQuery', () => {
  it('returns true when query matches', () => {
    const { result } = renderHook(() => useMediaQuery('(max-width: 1px)'));
    expect(result.current).toBe(true);
  });

  it('returns false when query does not match', () => {
    const { result } = renderHook(() => useMediaQuery('(min-width: 9999px)'));
    expect(result.current).toBe(false);
  });
});
