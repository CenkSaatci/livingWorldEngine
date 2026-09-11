import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { useApiGet } from './useApiGet';
import { useToastStore } from '../store/toastStore';

vi.mock('../api/client', () => ({
  apiClient: { get: vi.fn() },
}));

import { apiClient } from '../api/client';

const getMock = apiClient.get as ReturnType<typeof vi.fn>;

beforeEach(() => {
  vi.clearAllMocks();
  useToastStore.setState({ toasts: [] });
});

describe('useApiGet', () => {
  it('liefert Daten bei Erfolg', async () => {
    getMock.mockResolvedValue({ data: { name: 'World' } });
    const { result } = renderHook(() => useApiGet<{ name: string }>('/worlds/1'));

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.data).toEqual({ name: 'World' });
    expect(result.current.error).toBeNull();
  });

  it('setzt Fehler + Toast bei Fehlschlag', async () => {
    getMock.mockRejectedValue({ response: { data: { message: 'kaputt' } } });
    const { result } = renderHook(() => useApiGet('/worlds/1'));

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.error).toBe('kaputt');
    expect(
      useToastStore.getState().toasts.some((t) => t.message === 'kaputt' && t.type === 'error'),
    ).toBe(true);
  });

  it('ignoriert späte Antworten nach Unmount (cancel)', async () => {
    let resolve!: (v: { data: string }) => void;
    getMock.mockReturnValue(new Promise((res) => { resolve = res; }));
    const { result, unmount } = renderHook(() => useApiGet<string>('/worlds/1'));

    expect(result.current.loading).toBe(true);
    unmount();
    resolve({ data: 'zu spät' });
    await Promise.resolve();
    // Kein State-Update nach Cancel: data bleibt null, kein Crash.
    expect(result.current.data).toBeNull();
  });
});
