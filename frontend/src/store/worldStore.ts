import { create } from 'zustand';
import { apiClient } from '../api/client';

const WORLD_ID_KEY = 'lwe:currentWorldId';

export interface WorldSummary {
  id: string;
  name: string;
  // Seit P25-T06 liefert das Backend kein gameSystemId mehr (Welten sind
  // systemunabhängig); optional für Abwärtskompatibilität.
  gameSystemId?: string | null;
  currentGameTime: string | null;
  createdAt: string;
}

export interface WorldEvent {
  id: number;
  event_type: string;
  source_entity_id: string;
  target_entity_id: string;
  payload: Record<string, unknown>;
  created_at: string;
}

export interface MapToken {
  entityId: string;
  name: string;
  x: number;
  y: number;
}

interface WorldState {
  currentWorld: WorldSummary | null;
  worlds: WorldSummary[];
  worldEvents: WorldEvent[];
  currentEntityId: string | null;
  tokens: MapToken[];

  setCurrentWorld: (world: WorldSummary | null) => void;
  setWorlds: (worlds: WorldSummary[]) => void;
  addEvent: (event: WorldEvent) => void;
  clearEvents: () => void;
  setCurrentEntityId: (id: string | null) => void;
  updateTokenPosition: (entityId: string, x: number, y: number) => void;
  setTokens: (tokens: MapToken[]) => void;
  rehydrateCurrentWorld: () => Promise<void>;
}

export const useWorldStore = create<WorldState>((set) => ({
  currentWorld: null,
  worlds: [],
  worldEvents: [],
  currentEntityId: null,
  tokens: [],

  setCurrentWorld: (world) => {
    if (typeof localStorage !== 'undefined') {
      if (world) localStorage.setItem(WORLD_ID_KEY, world.id);
      else localStorage.removeItem(WORLD_ID_KEY);
    }
    set({ currentWorld: world, worldEvents: [], currentEntityId: null, tokens: [] });
  },

  setWorlds: (worlds) => set({ worlds }),

  addEvent: (event) =>
    set((state) => ({
      worldEvents: [...state.worldEvents.slice(-199), event],
    })),

  clearEvents: () => set({ worldEvents: [] }),

  setCurrentEntityId: (id) => set({ currentEntityId: id }),

  updateTokenPosition: (entityId, x, y) =>
    set((state) => ({
      tokens: state.tokens.map((t) => (t.entityId === entityId ? { ...t, x, y } : t)),
    })),

  setTokens: (tokens) => set({ tokens }),

  rehydrateCurrentWorld: async () => {
    const id = typeof localStorage !== 'undefined' ? localStorage.getItem(WORLD_ID_KEY) : null;
    if (!id) return;
    try {
      const res = await apiClient.get<WorldSummary>(`/worlds/${id}`);
      set({ currentWorld: res.data, worldEvents: [], currentEntityId: null, tokens: [] });
    } catch {
      /* best effort */
    }
  },
}));
