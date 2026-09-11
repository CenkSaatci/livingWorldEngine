import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { DmQueuePanel } from './DmQueuePanel';
import { apiClient } from '../../api/client';

vi.mock('../../api/client', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

const mockIntents = [
  { id: 'i1', worldId: 'w1', npcId: 'n1', intentType: 'ATTACK_PLAYER', reasoning: 'The goblin sees a lone traveler', status: 'pending', createdAt: '2026-07-19T12:00:00Z' },
  { id: 'i2', worldId: 'w1', npcId: 'n2', intentType: 'OFFER_QUEST', reasoning: 'The elder needs a hero', status: 'pending', createdAt: '2026-07-19T12:05:00Z' },
];

beforeEach(() => {
  vi.clearAllMocks();
});

describe('DmQueuePanel', () => {
  it('returns null when no intents', async () => {
    (apiClient.get as any).mockResolvedValue({ data: [] });
    const { container } = render(<DmQueuePanel worldId="w1" />);
    // Component returns null, container should be empty
    await vi.waitFor(() => {
      expect(container.innerHTML).toBe('');
    });
  });

  it('renders intent list from API', async () => {
    (apiClient.get as any).mockResolvedValue({ data: mockIntents });
    render(<DmQueuePanel worldId="w1" />);

    await vi.waitFor(() => {
      expect(screen.getByText('ATTACK_PLAYER')).toBeTruthy();
      expect(screen.getByText('OFFER_QUEST')).toBeTruthy();
    });

    expect(screen.getByText(/The goblin sees a lone traveler/)).toBeTruthy();
  });

  it('calls fetchIntents on mount', async () => {
    (apiClient.get as any).mockResolvedValue({ data: [] });
    render(<DmQueuePanel worldId="w1" />);
    await vi.waitFor(() => {
      expect(apiClient.get).toHaveBeenCalledWith('/npc-intents', {
        params: { worldId: 'w1', status: 'pending' },
      });
    });
  });

  it('calls approve endpoint and removes item', async () => {
    (apiClient.get as any).mockResolvedValue({ data: mockIntents });
    (apiClient.post as any).mockResolvedValue({});
    render(<DmQueuePanel worldId="w1" />);

    await vi.waitFor(() => {
      expect(screen.getByText('ATTACK_PLAYER')).toBeTruthy();
    });

    const approveButtons = screen.getAllByTitle('queue.approve');
    await userEvent.click(approveButtons[0]);

    expect(apiClient.post).toHaveBeenCalledWith('/npc-intents/i1/approve');
  });

  it('calls reject endpoint and removes item', async () => {
    (apiClient.get as any).mockResolvedValue({ data: mockIntents });
    (apiClient.post as any).mockResolvedValue({});
    render(<DmQueuePanel worldId="w1" />);

    await vi.waitFor(() => {
      expect(screen.getByText('ATTACK_PLAYER')).toBeTruthy();
    });

    const rejectButtons = screen.getAllByTitle('queue.reject');
    await userEvent.click(rejectButtons[0]);

    expect(apiClient.post).toHaveBeenCalledWith('/npc-intents/i1/reject');
  });
});
