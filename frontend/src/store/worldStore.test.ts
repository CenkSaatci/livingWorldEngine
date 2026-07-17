import { describe, it, expect } from 'vitest';
import { useWorldStore } from './worldStore';

describe('worldStore', () => {
  it('has initial state', () => {
    const state = useWorldStore.getState();
    expect(state).toBeDefined();
  });
});
