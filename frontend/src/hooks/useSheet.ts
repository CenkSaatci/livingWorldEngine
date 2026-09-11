import { useState, useEffect, useCallback } from 'react';
import { apiClient } from '../api/client';
import { useCampaignStore } from '../store/campaignStore';

export interface SheetData {
  entity: { id: string; name: string; entityType: string };
  experiencePoints: number;
  level: number;
  fatePoints?: number;
  fateMax?: number;
  damageArmor?: number;
  attributes: { name: string; value: number; modifier: number; min: number; max: number }[];
  derivedValues: { name: string; value: number; error?: string | null }[];
  skills: { name: string; total: number; perCharacterValue?: number | null; advanceCost?: number | null }[];
  conditionals: { name: string; active: boolean; description: string }[];
  activeConditions: { name: string; rounds?: number | null }[];
  conditionCatalog: string[];
  abilities: { name: string; type: string; apCost: number; effect: string; diceExpression: string; damageType?: string | null }[];
}

export function useSheet(entityId: string | undefined) {
  const [data, setData] = useState<SheetData | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const activeCampaignId = useCampaignStore((s) => s.activeCampaignId);

  const fetchSheet = useCallback(async () => {
    if (!entityId) return;
    setLoading(true);
    setError(null);
    try {
      const params = activeCampaignId ? `?campaignId=${activeCampaignId}` : '';
      const res = await apiClient.get<SheetData>(`/entities/${entityId}/sheet${params}`);
      setData(res.data);
    } catch {
      setError('Failed to load character sheet');
    } finally {
      setLoading(false);
    }
  }, [entityId, activeCampaignId]);

  useEffect(() => {
    let cancelled = false;
    const run = async () => {
      if (!entityId) return;
      setLoading(true);
      setError(null);
      try {
        const params = activeCampaignId ? `?campaignId=${activeCampaignId}` : '';
        const res = await apiClient.get<SheetData>(`/entities/${entityId}/sheet${params}`);
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
  }, [entityId, activeCampaignId]);

  return { data, loading, error, refetch: fetchSheet };
}
