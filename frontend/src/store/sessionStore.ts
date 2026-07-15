import { create } from 'zustand';

export interface GameSession {
  id: string;
  world_id: string;
  status: string;
  started_at: string;
  ended_at: string;
  created_at: string;
}

interface SessionState {
  sessions: GameSession[];
  activeSession: GameSession | null;

  setSessions: (sessions: GameSession[]) => void;
  addSession: (session: GameSession) => void;
  endSession: (sessionId: string) => void;
  clearSessions: () => void;
}

export const useSessionStore = create<SessionState>((set) => ({
  sessions: [],
  activeSession: null,

  setSessions: (sessions) =>
    set({ sessions, activeSession: sessions.find((s) => s.status === 'ACTIVE') ?? null }),

  addSession: (session) =>
    set((state) => ({
      sessions: [session, ...state.sessions],
      activeSession: session.status === 'ACTIVE' ? session : state.activeSession,
    })),

  endSession: (sessionId) =>
    set((state) => ({
      sessions: state.sessions.map((s) => (s.id === sessionId ? { ...s, status: 'ENDED' } : s)),
      activeSession: state.activeSession?.id === sessionId ? null : state.activeSession,
    })),

  clearSessions: () => set({ sessions: [], activeSession: null }),
}));
