import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook } from '@testing-library/react';
import { useWorldSocket } from './useWorldSocket';
import { useAuthStore } from '../store/authStore';
import { useWorldStore } from '../store/worldStore';
import { useCombatStore } from '../store/combatStore';
import { useToastStore } from '../store/toastStore';

vi.mock('@stomp/stompjs', () => ({
  Stomp: { client: vi.fn() },
}));

vi.mock('../api/client', () => ({
  getAccessToken: vi.fn(),
  apiClient: { get: vi.fn() },
}));

import { Stomp } from '@stomp/stompjs';
import { getAccessToken, apiClient } from '../api/client';

type MockClient = ReturnType<typeof makeClient>;
function makeClient() {
  return {
    connected: false,
    connectHeaders: {} as Record<string, string>,
    configure: vi.fn(),
    activate: vi.fn(),
    deactivate: vi.fn(),
    subscribe: vi.fn((_dest: string, cb: (msg: { body: string }) => void) => ({
      unsubscribe: vi.fn(),
      cb,
    })),
    send: vi.fn(),
  };
}

function getClient(): MockClient {
  const results = (Stomp.client as ReturnType<typeof vi.fn>).mock.results;
  return results[results.length - 1].value as MockClient;
}

beforeEach(() => {
  vi.clearAllMocks();
  (Stomp.client as ReturnType<typeof vi.fn>).mockImplementation(makeClient);
  useAuthStore.setState({ user: null, isAuthenticated: true });
  useWorldStore.setState({
    currentWorld: null,
    worlds: [],
    worldEvents: [],
    currentEntityId: null,
    tokens: [],
  });
  useCombatStore.setState({ session: null, participants: [], targetEntityId: null });
  useToastStore.setState({ toasts: [] });
});

describe('useWorldSocket', () => {
  it('reads token fresh in beforeConnect and subscribes to world topic', async () => {
    (getAccessToken as ReturnType<typeof vi.fn>).mockReturnValue('token-A');
    (apiClient.get as ReturnType<typeof vi.fn>).mockResolvedValue({
      data: { session: { id: 's9', status: 'ACTIVE' }, participants: [] },
    });

    const { unmount } = renderHook(() => useWorldSocket('w1'));

    expect(Stomp.client).toHaveBeenCalledTimes(1);
    const client = getClient();

    const config = client.configure.mock.calls[0][0];
    // Token changes AFTER connection setup — beforeConnect must read fresh
    (getAccessToken as ReturnType<typeof vi.fn>).mockReturnValue('token-B');
    config.beforeConnect();
    expect(client.connectHeaders.Authorization).toBe('Bearer token-B');

    config.onConnect();
    expect(client.subscribe).toHaveBeenCalledWith('/topic/world/w1', expect.any(Function));

    unmount();
  });

  it('reconnects with a new client when the token changes', () => {
    (getAccessToken as ReturnType<typeof vi.fn>).mockReturnValue('token-A');

    const { rerender, unmount } = renderHook(() => useWorldSocket('w1'));
    const firstClient = getClient();

    (getAccessToken as ReturnType<typeof vi.fn>).mockReturnValue('token-B');
    rerender();

    expect(Stomp.client).toHaveBeenCalledTimes(2);
    expect(firstClient.deactivate).toHaveBeenCalled();
    const secondClient = getClient();
    expect(secondClient).not.toBe(firstClient);

    unmount();
  });

  it('handles COMBAT_STARTED by loading combat session', async () => {
    (getAccessToken as ReturnType<typeof vi.fn>).mockReturnValue('token-A');
    (apiClient.get as ReturnType<typeof vi.fn>).mockResolvedValue({
      data: { session: { id: 's9', worldId: 'w1', status: 'ACTIVE', round: 1 }, participants: [] },
    });

    const { unmount } = renderHook(() => useWorldSocket('w1'));
    const client = getClient();
    const config = client.configure.mock.calls[0][0];
    config.beforeConnect();
    config.onConnect();

    const onMessage = client.subscribe.mock.calls[0][1];
    onMessage({
      body: JSON.stringify({
        event_type: 'COMBAT_STARTED',
        payload: { sessionId: 's9' },
      }),
    });

    await Promise.resolve();
    expect(apiClient.get).toHaveBeenCalledWith('/combat/s9');
    expect(useCombatStore.getState().session?.id).toBe('s9');

    unmount();
  });

  it('clears combat and toasts when combat load fails', async () => {
    (getAccessToken as ReturnType<typeof vi.fn>).mockReturnValue('token-A');
    (apiClient.get as ReturnType<typeof vi.fn>).mockRejectedValue(new Error('boom'));
    useCombatStore.setState({
      session: {
        id: 'old',
        worldId: 'w1',
        status: 'ACTIVE',
        round: 1,
        currentTurnEntityId: '',
        createdAt: '',
      },
      participants: [],
    });

    const { unmount } = renderHook(() => useWorldSocket('w1'));
    const client = getClient();
    const config = client.configure.mock.calls[0][0];
    config.beforeConnect();
    config.onConnect();

    const onMessage = client.subscribe.mock.calls[0][1];
    onMessage({
      body: JSON.stringify({
        event_type: 'COMBAT_STARTED',
        payload: { sessionId: 's9' },
      }),
    });

    await Promise.resolve();
    await Promise.resolve();

    expect(useCombatStore.getState().session).toBeNull();
    expect(
      useToastStore.getState().toasts.some((t) => t.message === 'Failed to load combat session'),
    ).toBe(true);

    unmount();
  });
});
