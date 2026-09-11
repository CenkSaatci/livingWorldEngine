import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { useToastStore } from './toastStore';

beforeEach(() => {
  useToastStore.setState({ toasts: [] });
});

afterEach(() => {
  vi.useRealTimers();
});

describe('toastStore', () => {
  it('fügt Toast mit Defaults hinzu (info, 4000ms)', () => {
    useToastStore.getState().addToast('Hallo');
    const [toast] = useToastStore.getState().toasts;
    expect(toast.message).toBe('Hallo');
    expect(toast.type).toBe('info');
    expect(toast.timeout).toBe(4000);
    expect(toast.id).toMatch(/^toast-/);
  });

  it('entfernt Toast per removeToast', () => {
    useToastStore.getState().addToast('a', 'success', 0);
    useToastStore.getState().addToast('b', 'error', 0);
    const [first] = useToastStore.getState().toasts;
    useToastStore.getState().removeToast(first.id);
    expect(useToastStore.getState().toasts.map((t) => t.message)).toEqual(['b']);
  });

  it('läuft nach Timeout automatisch ab', () => {
    vi.useFakeTimers();
    useToastStore.getState().addToast('kurz', 'info', 1000);
    expect(useToastStore.getState().toasts).toHaveLength(1);
    vi.advanceTimersByTime(1000);
    expect(useToastStore.getState().toasts).toHaveLength(0);
  });
});
