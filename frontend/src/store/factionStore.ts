import { create } from 'zustand';

export interface Faction {
  id: string;
  worldId: string;
  name: string;
  description: string | null;
  color: string;
  leaderEntityId: string | null;
  foundedAt: string;
}

export interface FactionRelation {
  id: string;
  factionAId: string;
  factionBId: string;
  relationStatus: 'ALLIANCE' | 'FRIENDLY' | 'NEUTRAL' | 'UNFRIENDLY' | 'WAR';
  changedAt: string;
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
