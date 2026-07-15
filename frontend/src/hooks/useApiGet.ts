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
            err?.response?.data?.message ??
            err?.response?.data?.error ??
            err?.message ??
            'Anfrage fehlgeschlagen';
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
