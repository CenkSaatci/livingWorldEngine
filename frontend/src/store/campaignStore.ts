import { create } from 'zustand';
import { apiClient } from '../api/client';

export interface CampaignSummary {
  id: string;
  worldId: string;
  gameSystemId: string;
  name: string;
  settingsJson: string;
  stateJson: string;
  createdAt: string;
  updatedAt: string;
}

interface CampaignState {
  campaigns: CampaignSummary[];
  activeCampaignId: string | null;
  loading: boolean;

  loadCampaigns: () => Promise<void>;
  createCampaign: (worldId: string, gameSystemId: string, name: string) => Promise<CampaignSummary | null>;
  setActiveCampaign: (id: string | null) => void;
}

export const useCampaignStore = create<CampaignState>((set) => ({
  campaigns: [],
  activeCampaignId: null,
  loading: false,

  loadCampaigns: async () => {
    set({ loading: true });
    try {
      const res = await apiClient.get<CampaignSummary[]>('/campaigns');
      set({ campaigns: res.data });
    } catch {
      /* ignore */
    } finally {
      set({ loading: false });
    }
  },

  createCampaign: async (worldId, gameSystemId, name) => {
    try {
      const res = await apiClient.post<CampaignSummary>('/campaigns', {
        worldId,
        gameSystemId,
        name,
      });
      set((state) => ({ campaigns: [res.data, ...state.campaigns] }));
      return res.data;
    } catch {
      return null;
    }
  },

  setActiveCampaign: (id) => set({ activeCampaignId: id }),
}));

export function useActiveCampaign(): CampaignSummary | null {
  const { campaigns, activeCampaignId } = useCampaignStore();
  return campaigns.find((c) => c.id === activeCampaignId) ?? null;
}
