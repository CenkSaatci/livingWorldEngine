import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ActionBar } from './ActionBar';
import { apiClient } from '../../api/client';

vi.mock('../../api/client', () => ({
  apiClient: {
    get: vi.fn(() => Promise.resolve({ data: {} })),
    post: vi.fn(() => Promise.resolve({ data: { session: null, participants: [] } })),
  },
}));

const combatState: Record<string, unknown> = {
  session: null,
  participants: [],
  targetEntityId: null,
  setTargetEntityId: vi.fn(),
  setSession: vi.fn(),
  updateParticipantAp: vi.fn(),
};

vi.mock('../../store/combatStore', () => ({
  useCombatStore: Object.assign(
    vi.fn((sel: ((s: unknown) => unknown) | undefined) =>
      sel ? sel(combatState) : combatState),
    { getState: () => combatState },
  ),
}));

vi.mock('../../store/campaignStore', () => ({
  useCampaignStore: Object.assign(
    vi.fn((sel: ((s: unknown) => unknown) | undefined) =>
      sel ? sel({ activeCampaignId: 'c1' }) : { activeCampaignId: 'c1' }),
    { getState: () => ({ campaigns: [], setActiveCampaign: vi.fn() }) },
  ),
  useActiveCampaign: () => ({ gameSystemId: 'gs1', worldId: 'w1' }),
}));

describe('ActionBar', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    combatState.session = null;
    combatState.participants = [];
    combatState.targetEntityId = null;
    vi.mocked(apiClient.get).mockImplementation((url: string) => {
      if (url === '/game-systems/gs1') {
        return Promise.resolve({
          data: {
            rulesJson: JSON.stringify({
              dice_mechanics: { combat: { maneuvers: [{ name: 'Wuchtschlag', apCost: 2 }] } },
            }),
          },
        });
      }
      if (url.endsWith('/abilities')) return Promise.resolve({ data: [] });
      return Promise.resolve({ data: {} });
    });
  });

  it('renders nothing when no active combat session', () => {
    const { container } = render(<ActionBar worldId="w1" />);
    expect(container.textContent).toBe('');
  });

  it('sends the equipped weapon itemId with actions (P23-T02)', async () => {
    combatState.session = { id: 's1', status: 'ACTIVE', currentTurnEntityId: 'e1' };
    combatState.participants = [
      { entityId: 'e1', entityName: 'Aragorn', apCurrent: 2, apMax: 2 },
      { entityId: 'e2', entityName: 'Ork', apCurrent: 2, apMax: 2 },
    ];
    combatState.targetEntityId = 'e2';
    vi.mocked(apiClient.get).mockImplementation((url: string) => {
      if (url === '/game-systems/gs1') return Promise.resolve({ data: { rulesJson: '{}' } });
      if (url.endsWith('/abilities')) return Promise.resolve({ data: [] });
      if (url.endsWith('/inventory')) {
        return Promise.resolve({
          data: { items: [{ itemId: 'w1', equipped: true, slot: 'weapon', type: 'WEAPON' }] },
        });
      }
      return Promise.resolve({ data: {} });
    });

    render(<ActionBar worldId="w1" />);

    const btn = await screen.findByText('action');
    fireEvent.click(btn);

    await waitFor(() => expect(apiClient.post).toHaveBeenCalledWith('/combat/s1/action', {
      actorId: 'e1',
      actionType: 'ACTION',
      targetId: 'e2',
      itemId: 'w1',
    }));
  });

  it('shows configured maneuvers and posts the clicked one', async () => {
    combatState.session = { id: 's1', status: 'ACTIVE', currentTurnEntityId: 'e1' };
    combatState.participants = [
      { entityId: 'e1', entityName: 'Aragorn', apCurrent: 2, apMax: 2 },
      { entityId: 'e2', entityName: 'Ork', apCurrent: 2, apMax: 2 },
    ];
    combatState.targetEntityId = 'e2';

    render(<ActionBar worldId="w1" />);

    const btn = await screen.findByText('Wuchtschlag');
    fireEvent.click(btn);

    await waitFor(() => expect(apiClient.post).toHaveBeenCalledWith('/combat/s1/maneuver', {
      actorId: 'e1',
      targetId: 'e2',
      maneuver: 'Wuchtschlag',
    }));
  });
});
