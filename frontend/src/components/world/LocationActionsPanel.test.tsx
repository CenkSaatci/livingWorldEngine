import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { LocationActionsPanel } from './LocationActionsPanel';

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}));

vi.mock('../../api/client', () => ({
  apiClient: { post: vi.fn(), get: vi.fn() },
}));

vi.mock('../../hooks/useApiGet', () => ({
  useApiGet: vi.fn(),
}));

vi.mock('../../hooks/useToast', () => ({
  useToast: () => ({ success: vi.fn(), error: vi.fn(), info: vi.fn() }),
}));

vi.mock('../../store/campaignStore', () => ({
  useCampaignStore: (sel: (s: { activeCampaignId: string | null }) => unknown) =>
    sel({ activeCampaignId: null }),
}));

import { apiClient } from '../../api/client';
import { useApiGet } from '../../hooks/useApiGet';

const refetch = vi.fn();

const actions = [
  {
    name: 'Medicus',
    description: 'Ein Wundarzt.',
    chat: 'actor',
    trade: false,
    dmOnly: false,
    requiresTrait: null,
    available: true,
    reason: null,
    costs: [{ type: 'money', amount: -15, text: '15 K' }],
  },
  {
    name: 'Handeln',
    description: 'Kaufen und verkaufen.',
    chat: 'actor',
    trade: true,
    dmOnly: false,
    requiresTrait: null,
    available: true,
    reason: null,
    costs: [],
  },
  {
    name: 'Geheim',
    trade: false,
    dmOnly: true,
    requiresTrait: null,
    available: false,
    reason: 'DM_ONLY',
    costs: [],
  },
];

beforeEach(() => {
  vi.clearAllMocks();
  (useApiGet as ReturnType<typeof vi.fn>).mockImplementation((url: string) => {
    if (url.includes('/entities')) {
      return { data: [{ id: 'a1', name: 'Mira', entityType: 'PC' }], loading: false, refetch };
    }
    return { data: actions, loading: false, refetch };
  });
});

describe('LocationActionsPanel', () => {
  it('renders actions, costs and unavailable reason', () => {
    render(<LocationActionsPanel worldId="w1" locationId="l1" />);
    expect(screen.getByText('Medicus')).toBeTruthy();
    expect(screen.getByText(/15 K/)).toBeTruthy();
    expect(screen.getByText('poi.reasonDmOnly')).toBeTruthy();
  });

  it('executes an action and shows the result', async () => {
    (apiClient.post as ReturnType<typeof vi.fn>).mockResolvedValue({
      data: {
        action: 'Medicus',
        success: true,
        trade: false,
        text: 'Verbunden.',
        moneyBefore: 100,
        moneyAfter: 85,
        moneyBeforeText: '1 G',
        moneyAfterText: '85 K',
        effects: [
          { type: 'money', amount: -15, text: '15 K' },
          { type: 'heal', amount: 7 },
        ],
        probe: null,
      },
    });
    render(<LocationActionsPanel worldId="w1" locationId="l1" />);

    fireEvent.click(screen.getByText('Medicus'));
    expect(apiClient.post).toHaveBeenCalledWith(
      '/locations/l1/actions/Medicus',
      expect.objectContaining({ actorId: 'a1' }),
    );
    expect(await screen.findByText('Verbunden.')).toBeTruthy();
    expect(screen.getByText(/1 G/)).toBeTruthy();
  });

  it('opens the market for a trade action instead of posting', () => {
    const onOpenMarket = vi.fn();
    render(<LocationActionsPanel worldId="w1" locationId="l1" onOpenMarket={onOpenMarket} />);

    fireEvent.click(screen.getByText('Handeln'));
    expect(onOpenMarket).toHaveBeenCalled();
    expect(apiClient.post).not.toHaveBeenCalled();
  });
});
