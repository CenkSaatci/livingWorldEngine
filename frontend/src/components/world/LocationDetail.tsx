import { useEffect, useState } from 'react';
import { MapPin, Users, ScrollText, Coins } from 'lucide-react';
import { apiClient } from '../../api/client';

interface LocationData {
  id: string;
  name: string;
  type: string;
  description: string;
  history: string;
  population: number;
  wealth: number;
  services: string[];
  created_at: string;
}

interface NpcSummary {
  id: string;
  name: string;
  entity_type: string;
}

interface Props {
  locationId: string;
}

export function LocationDetail({ locationId }: Props) {
  const [loc, setLoc] = useState<LocationData | null>(null);
  const [npcs, setNpcs] = useState<NpcSummary[]>([]);

  useEffect(() => {
    Promise.all([
      apiClient.get(`/locations/${locationId}`).catch(() => null),
      apiClient.get(`/locations/${locationId}/npcs`).catch(() => null),
    ]).then(([locRes, npcRes]) => {
      if (locRes) setLoc(locRes.data);
      if (npcRes) setNpcs(npcRes.data);
    });
  }, [locationId]);

  if (!loc) {
    return <div className="flex items-center justify-center h-full text-text-secondary">Loading…</div>;
  }

  return (
    <div className="max-w-2xl space-y-5">
      {/* Header */}
      <div>
        <h2 className="flex items-center gap-2 text-xl font-heading text-text-primary">
          <MapPin size={22} className="text-accent" />
          {loc.name}
        </h2>
        <div className="mt-1 flex flex-wrap gap-3 text-xs text-text-secondary">
          <span className="rounded bg-bg-elevated px-2 py-0.5 capitalize">{loc.type}</span>
          <span className="flex items-center gap-1"><Coins size={14} /> Wealth: {loc.wealth}/10</span>
          <span className="flex items-center gap-1"><Users size={14} /> {loc.population}</span>
        </div>
      </div>

      {/* Description */}
      {loc.description && (
        <p className="text-sm leading-relaxed text-text-primary">{loc.description}</p>
      )}

      {/* History */}
      {loc.history && (
        <div className="rounded-lg bg-bg-surface p-4">
          <h3 className="mb-2 flex items-center gap-1.5 text-sm font-semibold text-text-primary">
            <ScrollText size={16} /> History
          </h3>
          <p className="text-sm text-text-secondary">{loc.history}</p>
        </div>
      )}

      {/* Services */}
      <div className="rounded-lg bg-bg-surface p-4">
        <h3 className="mb-3 text-sm font-semibold text-text-primary">Services & NPCs</h3>
        {npcs.length === 0 ? (
          <p className="text-xs text-text-secondary">No NPCs found at this location</p>
        ) : (
          <div className="space-y-2">
            {npcs.map((npc) => (
              <div key={npc.id} className="flex items-center gap-3 rounded bg-bg-primary/50 px-3 py-2">
                <div className="h-8 w-8 rounded-full bg-accent/20 flex items-center justify-center text-sm text-accent">
                  {npc.name.charAt(0)}
                </div>
                <div>
                  <p className="text-sm text-text-primary">{npc.name}</p>
                  <p className="text-xs text-text-secondary">{npc.entity_type}</p>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
