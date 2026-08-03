import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render } from '@testing-library/react';
import { ActionBar } from './ActionBar';

vi.mock('../../api/client', () => ({
  apiClient: {
    get: vi.fn(() => Promise.resolve({ data: {} })),
    post: vi.fn(() => Promise.resolve({ data: { session: null, participants: [] } })),
  },
}));

vi.mock('../../store/combatStore', () => ({
  useCombatStore: vi.fn((sel) => {
    const state = {
      session: null,
      participants: [],
      targetEntityId: null,
      setTargetEntityId: vi.fn(),
      setSession: vi.fn(),
      updateParticipantAp: vi.fn(),
    };
    return sel ? sel(state) : state;
  }),
}));

describe('ActionBar', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders nothing when no active combat session', () => {
    const { container } = render(<ActionBar worldId="w1" />);
    expect(container.textContent).toBe('');
  });
});
