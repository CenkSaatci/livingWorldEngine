import { useEffect, useState } from 'react';
import { Heart, HeartOff, Coins, Briefcase, Star } from 'lucide-react';
import { apiClient } from '../../api/client';
import { EntityTimeline } from './EntityTimeline';

interface NpcData {
  id: string;
  name: string;
  entity_type: string;
  attributes_json: string;
  metadata_json: string;
}

interface Props {
  npcId: string;
}

export function NpcProfile({ npcId }: Props) {
  const [npc, setNpc] = useState<NpcData | null>(null);

  useEffect(() => {
    apiClient.get(`/entities/${npcId}`).then((r) => setNpc(r.data)).catch(() => {});
  }, [npcId]);

  if (!npc) return <div className="text-text-secondary text-sm p-4">Loading NPC…</div>;

  let meta: Record<string, any> = {};
  try { meta = JSON.parse(npc.metadata_json); } catch { /* */ }

  const occupation = meta.occupation ?? '—';
  const priceMod = meta.price_modifier ?? 1.0;
  const greeting = meta.greeting ?? '';
  const relationships: Record<string, string> = meta.relationships ?? {};
  const services: string[] = meta.services_offered ?? [];

  return (
    <div className="space-y-4 p-4">
      {/* Header */}
      <div className="flex items-start gap-4">
        <div className="h-14 w-14 rounded-full bg-accent/20 flex items-center justify-center text-2xl text-accent shrink-0">
          {npc.name.charAt(0)}
        </div>
        <div className="min-w-0">
          <h2 className="text-lg font-heading text-text-primary">{npc.name}</h2>
          <div className="mt-1 flex flex-wrap gap-2 text-xs">
            <span className="flex items-center gap-1 rounded bg-bg-elevated px-2 py-0.5 text-text-secondary">
              <Briefcase size={12} /> {occupation}
            </span>
            <span className="flex items-center gap-1 rounded bg-bg-elevated px-2 py-0.5 text-text-secondary">
              <Star size={12} /> {npc.entity_type}
            </span>
          </div>
        </div>
      </div>

      {/* Greeting */}
      {greeting && (
        <p className="italic text-sm text-text-secondary border-l-2 border-accent pl-3">"{greeting}"</p>
      )}

      {/* Services */}
      {services.length > 0 && (
        <div>
          <h3 className="mb-2 text-xs font-semibold text-text-secondary uppercase tracking-wide">Services</h3>
          <div className="flex flex-wrap gap-2">
            {services.map((s) => (
              <span key={s} className="rounded bg-accent/10 px-2 py-1 text-xs text-accent">{s}</span>
            ))}
          </div>
        </div>
      )}

      {/* Price Modifier */}
      {priceMod !== 1.0 && (
        <div className="flex items-center gap-2 text-xs text-text-secondary">
          <Coins size={14} />
          Price modifier: <span className={priceMod > 1 ? 'text-danger' : 'text-success'}>×{priceMod}</span>
        </div>
      )}

      {/* Relationships */}
      {Object.keys(relationships).length > 0 && (
        <div>
          <h3 className="mb-2 text-xs font-semibold text-text-secondary uppercase tracking-wide">Relationships</h3>
          <div className="space-y-1">
            {Object.entries(relationships).map(([target, rel]) => (
              <div key={target} className="flex items-center gap-2 text-xs">
                {rel === 'likes' || rel === 'loyal' ? (
                  <Heart size={12} className="text-success" />
                ) : (
                  <HeartOff size={12} className="text-danger" />
                )}
                <span className="text-text-primary">{target.slice(0, 8)}…</span>
                <span className="text-text-secondary">({rel})</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Timeline */}
      <div className="border-t border-bg-elevated pt-3">
        <EntityTimeline entityType="npc" entityId={npcId} />
      </div>
    </div>
  );
}
