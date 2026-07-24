import { describe, it, expect, beforeEach } from 'vitest';
import { BACKEND_ORIGIN, setTokens, getAccessToken, clearTokens } from './client';

describe('client', () => {
  beforeEach(() => {
    clearTokens();
    localStorage.clear();
  });

  it('exports BACKEND_ORIGIN', () => {
    expect(BACKEND_ORIGIN).toContain('localhost:8080');
  });

  it('getAccessToken returns null initially', () => {
    expect(getAccessToken()).toBeNull();
  });

  it('setTokens stores access token', () => {
    setTokens('access123', 'refresh123');
    expect(getAccessToken()).toBe('access123');
  });

  it('setTokens stores to localStorage', () => {
    setTokens('access456', 'refresh456');
    expect(localStorage.getItem('lwe:accessToken')).toBe('access456');
    expect(localStorage.getItem('lwe:refreshToken')).toBe('refresh456');
  });

  it('clearTokens removes tokens', () => {
    setTokens('a', 'r');
    clearTokens();
    expect(getAccessToken()).toBeNull();
    expect(localStorage.getItem('lwe:accessToken')).toBeNull();
    expect(localStorage.getItem('lwe:refreshToken')).toBeNull();
  });

  it('setTokens overwrites previous tokens', () => {
    setTokens('old', 'oldr');
    setTokens('new', 'newr');
    expect(getAccessToken()).toBe('new');
  });
});
