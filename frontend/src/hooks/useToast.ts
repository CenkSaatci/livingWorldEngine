import { useCallback } from 'react';
import { useToastStore } from '../store/toastStore';

export function useToast() {
  const addToast = useToastStore((s) => s.addToast);

  const success = useCallback(
    (message: string, timeout?: number) => addToast(message, 'success', timeout),
    [addToast],
  );

  const error = useCallback(
    (message: string, timeout?: number) => addToast(message, 'error', timeout),
    [addToast],
  );

  const info = useCallback(
    (message: string, timeout?: number) => addToast(message, 'info', timeout),
    [addToast],
  );

  return { success, error, info };
}
