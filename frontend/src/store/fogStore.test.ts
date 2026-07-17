import { describe, it, expect } from 'vitest';
import { useFogStore } from './fogStore';

describe('fogStore', () => {
  it('has initial state', () => {
    const state = useFogStore.getState();
    expect(state).toBeDefined();
  });
});
