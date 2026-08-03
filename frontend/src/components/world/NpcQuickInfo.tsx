import { Briefcase, Coins } from 'lucide-react';
import { useApiGet } from '../../hooks/useApiGet';

interface NpcQuickInfoData {
  id: string;
  name: string;
  entityType: string;
  metadataJson: string;
}

interface NpcMeta {
  occupation?: string;
  price_modifier?: number;
  personality?: string;
}

interface Props {
  npcId: string;
  worldId: string;
  onClose?: () => void;
}

export function NpcQuickInfo({ npcId, worldId, onClose }: Props) {
  const { data: npc } = useApiGet<NpcQuickInfoData>(`/worlds/${worldId}/entities/${npcId}`, [
    worldId,
    npcId,
  ]);

  if (!npc) return null;

  let meta: NpcMeta = {};
  try {
    meta = JSON.parse(npc.metadataJson) as NpcMeta;
  } catch {
    /* */
  }

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
              <button
                onClick={onClose}
                className="text-xs text-text-secondary hover:text-text-primary"
              >
                ✕
              </button>
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
