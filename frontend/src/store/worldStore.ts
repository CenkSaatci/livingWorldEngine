import { create } from 'zustand';

export interface WorldSummary {
  id: string;
  name: string;
  game_system_id: string | null;
  current_game_time: string | null;
  created_at: string;
}

interface WorldState {
  currentWorld: WorldSummary | null;
  worlds: WorldSummary[];
  setCurrentWorld: (world: WorldSummary | null) => void;
  setWorlds: (worlds: WorldSummary[]) => void;
}

export const useWorldStore = create<WorldState>((set) => ({
  currentWorld: null,
  worlds: [],
  setCurrentWorld: (world) => set({ currentWorld: world }),
  setWorlds: (worlds) => set({ worlds }),
}));