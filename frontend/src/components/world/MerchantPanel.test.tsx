import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MerchantPanel } from './MerchantPanel';

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
const merchants = [
  {
    npcId: 'm1',
    name: 'Hugh',
    occupation: 'Händler',
    greeting: 'Was darfs sein?',
    sellRate: 0.5,
    offers: [
      { item: 'Heiltrank', itemId: 'i1', price: 30, sellPrice: 15, resolved: true },
      { item: 'Geisterklinge', itemId: null, price: 0, sellPrice: 0, resolved: false },
    ],
  },
];

beforeEach(() => {
  vi.clearAllMocks();
  (apiClient.get as ReturnType<typeof vi.fn>).mockResolvedValue({ data: { moneyText: '100 K' } });
  (useApiGet as ReturnType<typeof vi.fn>).mockImplementation((url: string) => {
    if (url.includes('/merchants')) return { data: merchants, loading: false, refetch };
    return { data: [{ id: 'a1', name: 'Mira', entityType: 'PC' }], loading: false, refetch };
  });
});

describe('MerchantPanel', () => {
  it('renders assortment prices and flags unknown items', () => {
    render(<MerchantPanel worldId="w1" locationId="l1" />);
    expect(screen.getByText('Hugh')).toBeTruthy();
    expect(screen.getByText('Heiltrank')).toBeTruthy();
    expect(screen.getByText('30')).toBeTruthy();
    expect(screen.getByText('15')).toBeTruthy();
    expect(screen.getByText('market.unresolved')).toBeTruthy();
  });

  it('buys an item via the merchant endpoint', async () => {
    (apiClient.post as ReturnType<typeof vi.fn>).mockResolvedValue({
      data: {
        item: 'Heiltrank',
        qty: 1,
        unitPrice: 30,
        total: 30,
        moneyBeforeText: '100 K',
        moneyAfterText: '70 K',
      },
    });
    render(<MerchantPanel worldId="w1" locationId="l1" />);

    fireEvent.click(screen.getAllByRole('button', { name: 'market.buy' })[0]);

    expect(apiClient.post).toHaveBeenCalledWith(
      '/merchants/m1/buy',
      expect.objectContaining({ locationId: 'l1', actorId: 'a1', item: 'Heiltrank', qty: 1 }),
    );
    expect(await screen.findByText('70 K')).toBeTruthy();
  });
});
