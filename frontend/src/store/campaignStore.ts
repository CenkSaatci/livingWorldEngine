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

const ACTIVE_KEY = 'lwe:activeCampaign';

interface PersistedActive {
  id: string;
  summary: CampaignSummary | null;
}

/** Aktive Kampagne über Reloads halten (Deep-Link/Wizard/ActionBar). */
function loadPersistedActive(): PersistedActive | null {
  if (typeof localStorage === 'undefined') return null;
  try {
    const raw = localStorage.getItem(ACTIVE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as PersistedActive;
    return parsed.id ? { id: parsed.id, summary: parsed.summary ?? null } : null;
  } catch {
    return null;
  }
}

const persistedActive = loadPersistedActive();

export const useCampaignStore = create<CampaignState>((set, get) => ({
  campaigns: [],
  activeCampaignId: persistedActive?.id ?? null,
  activeCampaign: persistedActive?.summary ?? null,
  loading: false,

  loadCampaigns: async () => {
    set({ loading: true });
    try {
      const res = await apiClient.get<CampaignSummary[]>('/campaigns');
      set({ campaigns: res.data });
      // Geloeschte/nicht mehr zugaengliche aktive Kampagne aufraeumen (Audit P30).
      const { activeCampaignId } = get();
      if (activeCampaignId && !res.data.some((c) => c.id === activeCampaignId)) {
        get().setActiveCampaign(null);
      }
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
    set((state) => {
      const nextSummary =
        summary !== undefined
          ? summary
          : (state.campaigns.find((c) => c.id === id) ?? state.activeCampaign);
      if (typeof localStorage !== 'undefined') {
        try {
          if (!id) {
            localStorage.removeItem(ACTIVE_KEY);
          } else if (nextSummary) {
            localStorage.setItem(ACTIVE_KEY, JSON.stringify({ id, summary: nextSummary }));
          }
        } catch {
          // Storage voll/blockiert: Persistenz ist Best-Effort.
        }
      }
      return {
        activeCampaignId: id,
        activeCampaign: id ? nextSummary : null,
      };
    }),
}));

export function useActiveCampaign(): CampaignSummary | null {
  const { campaigns, activeCampaignId, activeCampaign } = useCampaignStore();
  return activeCampaign ?? campaigns.find((c) => c.id === activeCampaignId) ?? null;
}
