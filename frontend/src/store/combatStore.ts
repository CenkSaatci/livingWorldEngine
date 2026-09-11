import { create } from 'zustand';
import { apiClient } from '../api/client';

const COMBAT_ID_KEY = 'lwe:combatId';

export interface CombatParticipant {
  id: string;
  entityId: string;
  name: string;
  initiative: number;
  apCurrent: number;
  apMax: number;
  hpCurrent: number;
  hpMax: number;
  side: string;
}

export interface CombatSession {
  id: string;
  worldId: string;
  status: string;
  round: number;
  currentTurnEntityId: string;
  mapId?: string;
  createdAt: string;
}

interface CombatState {
  session: CombatSession | null;
  participants: CombatParticipant[];
  targetEntityId: string | null;

  setSession: (session: CombatSession | null, participants: CombatParticipant[]) => void;
  updateParticipantAp: (entityId: string, apCurrent: number) => void;
  setCurrentTurn: (entityId: string) => void;
  handleTurnChanged: (currentTurn: string, round: number) => void;
  clearCombat: () => void;
  setTargetEntityId: (id: string | null) => void;
  rehydrateCombat: () => Promise<void>;
}

export const useCombatStore = create<CombatState>((set, get) => ({
  session: null,
  participants: [],
  targetEntityId: null,

  setSession: (session, participants) => {
    if (typeof localStorage !== 'undefined') {
      if (session) localStorage.setItem(COMBAT_ID_KEY, session.id);
      else localStorage.removeItem(COMBAT_ID_KEY);
    }
    set({ session, participants, targetEntityId: null });
  },

  updateParticipantAp: (entityId, apCurrent) =>
    set((state) => ({
      participants: state.participants.map((p) =>
        p.entityId === entityId ? { ...p, apCurrent } : p,
      ),
    })),

  handleTurnChanged: (currentTurn, round) =>
    set((state) => ({
      session: state.session
        ? { ...state.session, currentTurnEntityId: currentTurn, round }
        : null,
    })),

  setCurrentTurn: (entityId) =>
    set((state) => ({
      session: state.session ? { ...state.session, currentTurnEntityId: entityId } : null,
    })),

  clearCombat: () => {
    if (typeof localStorage !== 'undefined') localStorage.removeItem(COMBAT_ID_KEY);
    set({ session: null, participants: [], targetEntityId: null });
  },

  setTargetEntityId: (id) => set({ targetEntityId: id }),

  rehydrateCombat: async () => {
    const id = typeof localStorage !== 'undefined' ? localStorage.getItem(COMBAT_ID_KEY) : null;
    if (!id) return;
    try {
      const res = await apiClient.get(`/combat/${id}`);
      get().setSession(res.data.session, res.data.participants);
    } catch {
      if (typeof localStorage !== 'undefined') localStorage.removeItem(COMBAT_ID_KEY);
    }
  },
}));
