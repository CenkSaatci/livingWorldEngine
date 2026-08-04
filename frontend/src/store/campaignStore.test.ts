import { describe, it, expect, beforeEach, vi } from 'vitest';
import { useCampaignStore, type CampaignSummary } from './campaignStore';

vi.mock('../api/client', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

import { apiClient } from '../api/client';

const campaign = (over: Partial<CampaignSummary> = {}): CampaignSummary => ({
  id: 'c1',
  worldId: 'w1',
  gameSystemId: 'gs1',
  name: 'Runde 1',
  settingsJson: '{}',
  stateJson: '{}',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
  ...over,
});

describe('campaignStore', () => {
  beforeEach(() => {
    useCampaignStore.setState({ campaigns: [], activeCampaignId: null, loading: false });
    vi.clearAllMocks();
  });

  it('loads campaigns into state', async () => {
    const list = [campaign(), campaign({ id: 'c2', name: 'Runde 2' })];
    (apiClient.get as ReturnType<typeof vi.fn>).mockResolvedValue({ data: list });

    await useCampaignStore.getState().loadCampaigns();

    expect(useCampaignStore.getState().campaigns).toHaveLength(2);
    expect(apiClient.get).toHaveBeenCalledWith('/campaigns');
  });

  it('creates campaign and prepends it', async () => {
    const created = campaign({ id: 'c3', name: 'Neue Runde' });
    (apiClient.post as ReturnType<typeof vi.fn>).mockResolvedValue({ data: created });
    useCampaignStore.setState({ campaigns: [campaign()] });

    const result = await useCampaignStore.getState().createCampaign('w1', 'gs1', 'Neue Runde');

    expect(result?.name).toBe('Neue Runde');
    expect(apiClient.post).toHaveBeenCalledWith('/campaigns', {
      worldId: 'w1',
      gameSystemId: 'gs1',
      name: 'Neue Runde',
    });
    expect(useCampaignStore.getState().campaigns[0].name).toBe('Neue Runde');
  });

  it('sets and clears active campaign', () => {
    useCampaignStore.getState().setActiveCampaign('c1');
    expect(useCampaignStore.getState().activeCampaignId).toBe('c1');

    useCampaignStore.getState().setActiveCampaign(null);
    expect(useCampaignStore.getState().activeCampaignId).toBeNull();
  });

  it('keeps active campaign id on load', async () => {
    const list = [campaign()];
    (apiClient.get as ReturnType<typeof vi.fn>).mockResolvedValue({ data: list });
    useCampaignStore.setState({ activeCampaignId: 'c1' });

    await useCampaignStore.getState().loadCampaigns();

    expect(useCampaignStore.getState().activeCampaignId).toBe('c1');
  });
});
