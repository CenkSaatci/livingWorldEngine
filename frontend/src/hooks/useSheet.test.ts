import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { useSheet } from './useSheet';
import { useCampaignStore } from '../store/campaignStore';

vi.mock('../api/client', () => ({
  apiClient: { get: vi.fn() },
}));

import { apiClient } from '../api/client';

const getMock = apiClient.get as ReturnType<typeof vi.fn>;

const sheet = {
  entity: { id: 'e1', name: 'Held', entityType: 'PC' },
  experiencePoints: 0,
  level: 1,
  attributes: [],
  derivedValues: [],
  skills: [],
  conditionals: [],
  abilities: [],
};

beforeEach(() => {
  vi.clearAllMocks();
  useCampaignStore.setState({ activeCampaignId: null });
});

describe('useSheet', () => {
  it('lädt das Sheet bei Erfolg', async () => {
    getMock.mockResolvedValue({ data: sheet });
    const { result } = renderHook(() => useSheet('e1'));

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.data).toEqual(sheet);
    expect(result.current.error).toBeNull();
  });

  it('setzt Fehler bei Fehlschlag', async () => {
    getMock.mockRejectedValue(new Error('boom'));
    const { result } = renderHook(() => useSheet('e1'));

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.error).toBe('Failed to load character sheet');
    expect(result.current.data).toBeNull();
  });

  it('ignoriert späte Antworten nach Entity-Wechsel (cancel)', async () => {
    let resolve!: (v: { data: typeof sheet }) => void;
    getMock.mockReturnValue(new Promise((res) => { resolve = res; }));
    const { result, rerender, unmount } = renderHook(({ id }) => useSheet(id), {
      initialProps: { id: 'e1' },
    });

    rerender({ id: 'e2' });
    unmount();
    resolve({ data: sheet });
    await Promise.resolve();
    expect(result.current.data).toBeNull();
    expect(result.current.error).toBeNull();
  });
});
