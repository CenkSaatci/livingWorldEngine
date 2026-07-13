import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api/v1';
const REFRESH_URL = `${API_BASE_URL}/auth/refresh`;

/**
 * Axios-Client für LWE-Backend.
 *
 * - Sendet Authorization + Accept-Language bei jedem Request
 * - Bei 401: automatischer Refresh-Token, dann Retry
 * - Bei Refresh-Fehler: logout
 */
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
});

// Lade Tokens aus localStorage (Seite-Reload)
let inMemoryAccessToken: string | null =
  typeof localStorage !== 'undefined' ? localStorage.getItem('lwe:accessToken') : null;
let inMemoryRefreshToken: string | null =
  typeof localStorage !== 'undefined' ? localStorage.getItem('lwe:refreshToken') : null;

export function setTokens(access: string, refresh: string) {
  inMemoryAccessToken = access;
  inMemoryRefreshToken = refresh;
  if (typeof localStorage !== 'undefined') {
    localStorage.setItem('lwe:accessToken', access);
    localStorage.setItem('lwe:refreshToken', refresh);
  }
}

export function clearTokens() {
  inMemoryAccessToken = null;
  inMemoryRefreshToken = null;
  if (typeof localStorage !== 'undefined') {
    localStorage.removeItem('lwe:accessToken');
    localStorage.removeItem('lwe:refreshToken');
  }
}

export function getAccessToken(): string | null {
  return inMemoryAccessToken;
}

// --- Request-Interceptor: Auth + Locale ---
apiClient.interceptors.request.use((config) => {
  if (inMemoryAccessToken) {
    config.headers.Authorization = `Bearer ${inMemoryAccessToken}`;
  }
  if (typeof localStorage !== 'undefined') {
    config.headers['Accept-Language'] = localStorage.getItem('lwe:locale') ?? 'de';
  }
  return config;
});

// --- Response-Interceptor: 401 → Refresh → Retry ---
let refreshPromise: Promise<boolean> | null = null;

async function doRefresh(): Promise<boolean> {
  if (!inMemoryRefreshToken) return false;
  try {
    const res = await axios.post(REFRESH_URL, {
      refreshToken: inMemoryRefreshToken,
    });
    const { accessToken, refreshToken } = res.data;
    setTokens(accessToken, refreshToken);
    return true;
  } catch {
    clearTokens();
    return false;
  }
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config;
    if (error.response?.status !== 401 || original._retry) {
      return Promise.reject(error);
    }
    original._retry = true;

    // Dedupliziere parallele Refreshes
    if (!refreshPromise) {
      refreshPromise = doRefresh().finally(() => {
        refreshPromise = null;
      });
    }

    const ok = await refreshPromise;
    if (!ok) {
      // versuche authStore.logout über globales Event
      if (typeof window !== 'undefined') {
        window.dispatchEvent(new CustomEvent('lwe:logout'));
      }
      return Promise.reject(error);
    }

    original.headers.Authorization = `Bearer ${inMemoryAccessToken}`;
    return apiClient(original);
  },
);

export { apiClient, API_BASE_URL };
