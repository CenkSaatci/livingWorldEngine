import { create } from 'zustand';

export type ToastType = 'success' | 'error' | 'info';

export interface Toast {
  id: string;
  message: string;
  type: ToastType;
  timeout: number;
}

interface ToastState {
  toasts: Toast[];
  addToast: (message: string, type?: ToastType, timeout?: number) => void;
  removeToast: (id: string) => void;
}

let nextId = 0;

export const useToastStore = create<ToastState>((set) => ({
  toasts: [],

  addToast: (message, type = 'info', timeout = 4000) => {
    const id = `toast-${++nextId}`;
    set((s) => ({ toasts: [...s.toasts, { id, message, type, timeout }] }));
    if (timeout > 0) {
      setTimeout(() => {
        set((s) => ({ toasts: s.toasts.filter((t) => t.id !== id) }));
      }, timeout);
    }
  },

  removeToast: (id) => {
    set((s) => ({ toasts: s.toasts.filter((t) => t.id !== id) }));
  },
}));
