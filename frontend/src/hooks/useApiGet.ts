import { useState, useEffect, useCallback } from 'react';
import { apiClient } from '../api/client';
import { useToast } from './useToast';

export interface UseApiGetResult<T> {
  data: T | null;
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

export function useApiGet<T>(url: string, deps: unknown[] = []): UseApiGetResult<T> {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [trigger, setTrigger] = useState(0);
  const toast = useToast();

  const refetch = useCallback(() => setTrigger((n) => n + 1), []);

  useEffect(() => {
    if (!url) { setLoading(false); return; }
    let cancelled = false;
    setLoading(true);
    setError(null);

    apiClient
      .get<T>(url)
      .then((res) => {
        if (!cancelled) {
          setData(res.data);
          setLoading(false);
        }
      })
      .catch((err) => {
        if (!cancelled) {
          const msg =
            typeof err?.response?.data?.message === 'string'
              ? err.response.data.message
              : typeof err?.response?.data?.error === 'string'
                ? err.response.data.error
                : err?.message ?? 'Anfrage fehlgeschlagen';
          setError(msg);
          setLoading(false);
          toast.error(msg);
        }
      });

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [url, trigger, ...deps]);

  return { data, loading, error, refetch };
}
