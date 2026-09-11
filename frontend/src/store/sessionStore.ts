import { create } from 'zustand';
import { apiClient } from '../api/client';
import { useWorldStore } from './worldStore';

export interface GameSession {
  id: string;
  worldId: string;
  status: string;
  startedAt: string;
  endedAt: string;
  createdAt: string;
}

interface SessionState {
  sessions: GameSession[];
  activeSession: GameSession | null;

  setSessions: (sessions: GameSession[]) => void;
  addSession: (session: GameSession) => void;
  endSession: (sessionId: string) => void;
  clearSessions: () => void;
  rehydrateSessions: () => Promise<void>;
}

export const useSessionStore = create<SessionState>((set, get) => ({
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

  rehydrateSessions: async () => {
    const worldId = useWorldStore.getState().currentWorld?.id;
    if (!worldId) return;
    try {
      const res = await apiClient.get<GameSession[]>(`/worlds/${worldId}/sessions`);
      get().setSessions(res.data);
    } catch {
      /* best effort */
    }
  },
}));
