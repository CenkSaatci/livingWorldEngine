import { describe, it, expect } from 'vitest';
import { useSessionStore } from './sessionStore';

describe('sessionStore', () => {
  it('has initial state', () => {
    const state = useSessionStore.getState();
    expect(state).toBeDefined();
  });
});
