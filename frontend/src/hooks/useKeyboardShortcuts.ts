import { useEffect, useRef } from 'react';

interface ShortcutMap {
  [key: string]: () => void;
}

/**
 * Globaler Keyboard-Shortcut-Handler.
 * Verwendet useRef, um die Callbacks stabil zu halten — kein Re-Register bei Re-Render.
 */
export function useKeyboardShortcuts(shortcuts: ShortcutMap) {
  const ref = useRef(shortcuts);
  ref.current = shortcuts;

  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      const tag = (e.target as HTMLElement)?.tagName;
      const isInput = tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT';

      if (e.key === 'Escape') {
        ref.current['Escape']?.();
        return;
      }
      if (isInput) return;

      const key = e.key.toLowerCase();
      ref.current[key]?.();
    };

    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, []);
}
