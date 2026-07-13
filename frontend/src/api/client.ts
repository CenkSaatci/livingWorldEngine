import axios, { AxiosInstance } from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api/v1';

/**
 * Axios-Client für das LWE-Backend (alle Endpunkte unter /api/v1 — siehe docs/API.md).
 *
 * Header:
 *  - Authorization: Bearer {jwt} aus authStore (vorerst via localStorage — P3-T02 wired den
 *    automatischen Refresh-Interceptor, siehe docs/ADR/001-frontend-react-vite.md).
 *  - Accept-Language: BCP 47-Tag aus lwe:locale (Default `de`), siehe docs/ADR/007.
 */
function createClient(): AxiosInstance {
  const instance = axios.create({
    baseURL: API_BASE_URL,
    headers: {
      'Content-Type': 'application/json',
    },
  });

  instance.interceptors.request.use((config) => {
    if (typeof localStorage !== 'undefined') {
      const token = localStorage.getItem('lwe:accessToken');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      config.headers['Accept-Language'] = localStorage.getItem('lwe:locale') ?? 'de';
    }
    return config;
  });

  // TODO (P3-T02): 401-Interceptor, der einen Refresh durchführt und den Request wiederholt.
  // TODO (P3-T02): WS-Integration via separate useWorldSocket hook (STOMP).

  return instance;
}

export const apiClient = createClient();
export { API_BASE_URL };