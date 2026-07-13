import { useEffect, useState } from 'react';
import { Briefcase, Coins } from 'lucide-react';
import { apiClient } from '../../api/client';

interface Props {
  npcId: string;
  onClose?: () => void;
}

export function NpcQuickInfo({ npcId, onClose }: Props) {
  const [npc, setNpc] = useState<any>(null);

  useEffect(() => {
    apiClient.get(`/entities/${npcId}`).then((r) => setNpc(r.data)).catch(() => {});
  }, [npcId]);

  if (!npc) return null;

  let meta: Record<string, any> = {};
  try { meta = JSON.parse(npc.metadata_json); } catch { /* */ }

  return (
    <div className="rounded-lg border border-bg-elevated bg-bg-surface p-3">
      <div className="flex items-start gap-3">
        <div className="h-10 w-10 shrink-0 rounded-full bg-accent/20 flex items-center justify-center text-base text-accent">
          {npc.name.charAt(0)}
        </div>
        <div className="min-w-0 flex-1">
          <div className="flex items-center justify-between gap-2">
            <p className="text-sm font-medium text-text-primary truncate">{npc.name}</p>
            {onClose && (
              <button onClick={onClose} className="text-xs text-text-secondary hover:text-text-primary">✕</button>
            )}
          </div>
          {meta.occupation && (
            <p className="mt-0.5 flex items-center gap-1 text-xs text-text-secondary">
              <Briefcase size={12} /> {meta.occupation}
            </p>
          )}
          {meta.price_modifier && meta.price_modifier !== 1.0 && (
            <p className="mt-0.5 flex items-center gap-1 text-xs text-text-secondary">
              <Coins size={12} /> ×{meta.price_modifier}
            </p>
          )}
        </div>
      </div>
    </div>
  );
}
