import { create } from 'zustand';

export interface WorldSummary {
  id: string;
  name: string;
  game_system_id: string | null;
  current_game_time: string | null;
  created_at: string;
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
}

export const useWorldStore = create<WorldState>((set) => ({
  currentWorld: null,
  worlds: [],
  worldEvents: [],
  currentEntityId: null,
  tokens: [],

  setCurrentWorld: (world) =>
    set({ currentWorld: world, worldEvents: [], currentEntityId: null, tokens: [] }),

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
}));
