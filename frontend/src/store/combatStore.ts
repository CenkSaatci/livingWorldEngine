import { create } from 'zustand';

export interface CombatParticipant {
  id: string;
  entity_id: string;
  name: string;
  initiative: number;
  ap_current: number;
  ap_max: number;
  hp_current: number;
  hp_max: number;
  side: string;
}

export interface CombatSession {
  id: string;
  world_id: string;
  status: string;
  round: number;
  current_turn_entity_id: string;
  map_id?: string;
  created_at: string;
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
}

export const useCombatStore = create<CombatState>((set) => ({
  session: null,
  participants: [],
  targetEntityId: null,

  setSession: (session, participants) => set({ session, participants, targetEntityId: null }),

  updateParticipantAp: (entityId, apCurrent) =>
    set((state) => ({
      participants: state.participants.map((p) =>
        p.entity_id === entityId ? { ...p, ap_current: apCurrent } : p,
      ),
    })),

  handleTurnChanged: (currentTurn, round) =>
    set((state) => ({
      session: state.session
        ? { ...state.session, current_turn_entity_id: currentTurn, round }
        : null,
    })),

  setCurrentTurn: (entityId) =>
    set((state) => ({
      session: state.session ? { ...state.session, current_turn_entity_id: entityId } : null,
    })),

  clearCombat: () => set({ session: null, participants: [], targetEntityId: null }),

  setTargetEntityId: (id) => set({ targetEntityId: id }),
}));
