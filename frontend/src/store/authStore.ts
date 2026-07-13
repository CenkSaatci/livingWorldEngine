import { create } from 'zustand';

export interface AuthUser {
  id: string;
  email: string;
  username: string;
  role: 'USER' | 'ADMIN';
  locale: string;
}

interface AuthState {
  user: AuthUser | null;
  accessToken: string | null;
  refreshToken: string | null;
  locale: string;
  isAuthenticated: boolean;
  setSession: (session: {
    user: AuthUser;
    accessToken: string;
    refreshToken: string;
  }) => void;
  setLocale: (locale: string) => void;
  logout: () => void;
}

const DEFAULT_LOCALE: string =
  typeof localStorage !== 'undefined'
    ? localStorage.getItem('lwe:locale') ?? 'de'
    : 'de';

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  accessToken: null,
  refreshToken: null,
  locale: DEFAULT_LOCALE,
  isAuthenticated: false,
  setSession: ({ user, accessToken, refreshToken }) =>
    set({
      user,
      accessToken,
      refreshToken,
      locale: user.locale ?? DEFAULT_LOCALE,
      isAuthenticated: true,
    }),
  setLocale: (locale) => {
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem('lwe:locale', locale);
    }
    set({ locale });
  },
  logout: () => {
    if (typeof localStorage !== 'undefined') {
      localStorage.removeItem('lwe:accessToken');
      localStorage.removeItem('lwe:refreshToken');
    }
    set({
      user: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,
    });
  },
}));