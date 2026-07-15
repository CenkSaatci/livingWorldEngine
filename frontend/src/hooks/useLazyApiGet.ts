import { useCallback, useRef, useState } from 'react';
import { apiClient } from '../api/client';
import { useToast } from './useToast';

export function useLazyApiGet<T>() {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const mountedRef = useRef(true);
  const toast = useToast();

  const fetch = useCallback(
    async (url: string) => {
      setLoading(true);
      setError(null);
      try {
        const res = await apiClient.get<T>(url);
        if (mountedRef.current) {
          setData(res.data);
          setLoading(false);
        }
        return res.data;
      } catch (err: unknown) {
        if (!mountedRef.current) return null;
        const msg =
          (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
          (err as { message?: string })?.message ??
          'Anfrage fehlgeschlagen';
        setError(msg);
        setLoading(false);
        toast.error(msg);
        return null;
      }
    },
    [toast],
  );

  return { data, loading, error, fetch };
}
