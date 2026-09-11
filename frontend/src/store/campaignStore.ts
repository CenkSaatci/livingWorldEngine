import { create } from 'zustand';
import { apiClient } from '../api/client';
import { useToastStore } from './toastStore';

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
  // Zwischengespeicherte Zusammenfassung der aktiven Kampagne, damit Header/Badges
  // auch ohne (neu-)geladene Kampagnenliste den Namen anzeigen können
  // (z.B. Deep-Link auf /campaigns/:id oder Enter-World ohne Dashboard-Umweg).
  activeCampaign: CampaignSummary | null;
  loading: boolean;

  loadCampaigns: () => Promise<void>;
  createCampaign: (worldId: string, gameSystemId: string, name: string) => Promise<CampaignSummary | null>;
  setActiveCampaign: (id: string | null, summary?: CampaignSummary | null) => void;
}

export const useCampaignStore = create<CampaignState>((set) => ({
  campaigns: [],
  activeCampaignId: null,
  activeCampaign: null,
  loading: false,

  loadCampaigns: async () => {
    set({ loading: true });
    try {
      const res = await apiClient.get<CampaignSummary[]>('/campaigns');
      set({ campaigns: res.data });
    } catch {
      useToastStore.getState().addToast('Failed to load campaigns', 'error');
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
      useCampaignStore.getState().setActiveCampaign(res.data.id, res.data);
      return res.data;
    } catch {
      return null;
    }
  },

  setActiveCampaign: (id, summary) =>
    set((state) => ({
      activeCampaignId: id,
      activeCampaign:
        summary !== undefined
          ? summary
          : (state.campaigns.find((c) => c.id === id) ?? state.activeCampaign),
    })),
}));

export function useActiveCampaign(): CampaignSummary | null {
  const { campaigns, activeCampaignId, activeCampaign } = useCampaignStore();
  return activeCampaign ?? campaigns.find((c) => c.id === activeCampaignId) ?? null;
}
