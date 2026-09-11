import { create } from 'zustand';
import { setTokens, clearTokens, getAccessToken } from '../api/client';
import i18n from '../i18n';
import { useWorldStore } from './worldStore';
import { useCampaignStore } from './campaignStore';
import { useSessionStore } from './sessionStore';
import { useCombatStore } from './combatStore';

export interface AuthUser {
  id: string;
  email: string;
  username: string;
  role: 'USER' | 'ADMIN' | 'BOT';
  locale: string;
  emailVerified?: boolean;
}

interface AuthState {
  user: AuthUser | null;
  locale: string;
  isAuthenticated: boolean;
  login: (session: { user: AuthUser; accessToken: string; refreshToken: string }) => void;
  setSession: (session: { user: AuthUser; accessToken: string; refreshToken: string }) => void;
  setLocale: (locale: string) => void;
  logout: () => void;
  restoreSession: () => boolean;
}

const DEFAULT_LOCALE: string =
  typeof localStorage !== 'undefined' ? (localStorage.getItem('lwe:locale') ?? 'de') : 'de';

function restoreUserFromStorage(): AuthUser | null {
  try {
    const raw = localStorage.getItem('lwe:user');
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export const useAuthStore = create<AuthState>((set) => ({
  user: restoreUserFromStorage(),
  locale: DEFAULT_LOCALE,
  isAuthenticated: restoreUserFromStorage() !== null && getAccessToken() !== null,

  login: ({ user, accessToken, refreshToken }) => {
    setTokens(accessToken, refreshToken);
    localStorage.setItem('lwe:user', JSON.stringify(user));
    const locale = user.locale ?? DEFAULT_LOCALE;
    i18n.changeLanguage(locale);
    set({ user, locale, isAuthenticated: true });
  },

  setSession: ({ user, accessToken, refreshToken }) => {
    setTokens(accessToken, refreshToken);
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem('lwe:user', JSON.stringify(user));
    }
    const locale = user.locale ?? DEFAULT_LOCALE;
    i18n.changeLanguage(locale);
    set({ user, locale, isAuthenticated: true });
  },

  setLocale: (locale) => {
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem('lwe:locale', locale);
    }
    i18n.changeLanguage(locale);
    set({ locale });
  },

  logout: () => {
    clearTokens();
    if (typeof localStorage !== 'undefined') {
      localStorage.removeItem('lwe:user');
      localStorage.removeItem('lwe:currentWorldId');
      localStorage.removeItem('lwe:combatId');
    }
    set({ user: null, isAuthenticated: false });
    useWorldStore.setState({
      currentWorld: null,
      worlds: [],
      worldEvents: [],
      currentEntityId: null,
      tokens: [],
    });
    useCampaignStore.setState({ campaigns: [], activeCampaignId: null, activeCampaign: null, loading: false });
    useSessionStore.setState({ sessions: [], activeSession: null });
    useCombatStore.setState({ session: null, participants: [], targetEntityId: null });
  },

  restoreSession: () => {
    const user = restoreUserFromStorage();
    const token = getAccessToken();
    if (user && token) {
      const locale = user.locale ?? DEFAULT_LOCALE;
      i18n.changeLanguage(locale);
      set({ user, locale, isAuthenticated: true });
      return true;
    }
    return false;
  },
}));

// Globales Logout-Event vom Axios-Interceptor abonnieren
if (typeof window !== 'undefined') {
  window.addEventListener('lwe:logout', () => {
    useAuthStore.getState().logout();
  });
}
