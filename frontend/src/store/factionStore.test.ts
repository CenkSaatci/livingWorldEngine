import { describe, it, expect } from 'vitest';
import { useFactionStore } from './factionStore';

describe('factionStore', () => {
  it('has initial state', () => {
    const state = useFactionStore.getState();
    expect(state).toBeDefined();
  });
});
