import { describe, it, expect } from 'vitest';
import { useSettingsStore } from './settingsStore';

describe('settingsStore', () => {
  it('has initial state', () => {
    const state = useSettingsStore.getState();
    expect(state).toBeDefined();
  });
});
