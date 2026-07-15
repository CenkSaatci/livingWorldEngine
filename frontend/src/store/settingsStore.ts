import { create } from 'zustand';

export type DiceMode = 'css' | '3d';
export type ThemeMode = 'dark';

interface SettingsState {
  diceMode: DiceMode;
  theme: ThemeMode;
  setDiceMode: (mode: DiceMode) => void;
  setTheme: (theme: ThemeMode) => void;
}

export const useSettingsStore = create<SettingsState>((set) => ({
  diceMode: (localStorage.getItem('lwe:diceMode') as DiceMode) ?? 'css',
  theme: 'dark',

  setDiceMode: (mode) => {
    localStorage.setItem('lwe:diceMode', mode);
    set({ diceMode: mode });
  },

  setTheme: (theme) => set({ theme }),
}));
