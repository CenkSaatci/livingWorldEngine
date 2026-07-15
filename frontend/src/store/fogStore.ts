import { create } from 'zustand';

interface FogState {
  visible: boolean;
  toggle: () => void;
  setVisible: (v: boolean) => void;
}

export const useFogStore = create<FogState>((set) => ({
  visible: false,
  toggle: () => set((state) => ({ visible: !state.visible })),
  setVisible: (v) => set({ visible: v }),
}));
