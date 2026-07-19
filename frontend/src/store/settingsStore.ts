import { create } from 'zustand';

export type DiceMode = 'css' | '3d';
export type ThemeMode = 'dark' | 'light';

interface SettingsState {
  diceMode: DiceMode;
  theme: ThemeMode;
  setDiceMode: (mode: DiceMode) => void;
  setTheme: (theme: ThemeMode) => void;
}

const storedTheme = (() => {
  try {
    const v = localStorage.getItem('lwe:theme');
    if (v === 'light' || v === 'dark') return v;
  } catch {}
  return 'dark';
})();

export const useSettingsStore = create<SettingsState>((set) => ({
  diceMode: (localStorage.getItem('lwe:diceMode') as DiceMode) ?? 'css',
  theme: storedTheme,

  setDiceMode: (mode) => {
    localStorage.setItem('lwe:diceMode', mode);
    set({ diceMode: mode });
  },

  setTheme: (theme) => {
    localStorage.setItem('lwe:theme', theme);
    document.documentElement.classList.toggle('dark', theme === 'dark');
    set({ theme });
  },
}));
