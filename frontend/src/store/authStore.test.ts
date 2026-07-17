import { describe, it, expect } from 'vitest';
import { useAuthStore } from './authStore';

describe('authStore', () => {
  it('has initial state', () => {
    const state = useAuthStore.getState();
    expect(state).toBeDefined();
  });
});
