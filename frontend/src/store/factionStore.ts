import { create } from 'zustand';

export interface Faction {
  id: string;
  world_id: string;
  name: string;
  description: string | null;
  color: string;
  leader_entity_id: string | null;
  founded_at: string;
}

export interface FactionRelation {
  id: string;
  faction_a_id: string;
  faction_b_id: string;
  relation_status: 'ALLIANCE' | 'FRIENDLY' | 'NEUTRAL' | 'UNFRIENDLY' | 'WAR';
  changed_at: string;
}

interface FactionState {
  factions: Faction[];
  relations: Record<string, FactionRelation[]>;

  setFactions: (factions: Faction[]) => void;
  addFaction: (faction: Faction) => void;
  removeFaction: (id: string) => void;
  setRelations: (factionId: string, relations: FactionRelation[]) => void;
}

export const useFactionStore = create<FactionState>((set) => ({
  factions: [],
  relations: {},

  setFactions: (factions) => set({ factions }),

  addFaction: (faction) => set((state) => ({ factions: [...state.factions, faction] })),

  removeFaction: (id) =>
    set((state) => ({
      factions: state.factions.filter((f) => f.id !== id),
    })),

  setRelations: (factionId, relations) =>
    set((state) => ({
      relations: { ...state.relations, [factionId]: relations },
    })),
}));
