import { useState, useEffect, useCallback } from 'react';
import { apiClient } from '../api/client';

export interface SheetData {
  entity: { id: string; name: string; entityType: string };
  experiencePoints: number;
  level: number;
  attributes: { name: string; value: number; modifier: number; min: number; max: number }[];
  derivedValues: { name: string; value: number }[];
  skills: { name: string; total: number; perCharacterValue?: number | null }[];
  conditionals: { name: string; active: boolean; description: string }[];
  abilities: { name: string; type: string; apCost: number; effect: string; diceExpression: string }[];
}

export function useSheet(entityId: string | undefined) {
  const [data, setData] = useState<SheetData | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchSheet = useCallback(async () => {
    if (!entityId) return;
    setLoading(true);
    setError(null);
    try {
      const res = await apiClient.get<SheetData>(`/entities/${entityId}/sheet`);
      setData(res.data);
    } catch {
      setError('Failed to load character sheet');
    } finally {
      setLoading(false);
    }
  }, [entityId]);

  useEffect(() => {
    let cancelled = false;
    const run = async () => {
      if (!entityId) return;
      setLoading(true);
      setError(null);
      try {
        const res = await apiClient.get<SheetData>(`/entities/${entityId}/sheet`);
        if (!cancelled) setData(res.data);
      } catch {
        if (!cancelled) setError('Failed to load character sheet');
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    run();
    return () => {
      cancelled = true;
    };
  }, [entityId]);

  return { data, loading, error, refetch: fetchSheet };
}
