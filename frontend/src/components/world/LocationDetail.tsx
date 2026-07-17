/* eslint-disable @typescript-eslint/no-explicit-any */
import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { MapPin, Users, ScrollText, Coins, ShoppingCart, Briefcase, Swords } from 'lucide-react';
import { useApiGet } from '../../hooks/useApiGet';
import { LoadingSpinner } from '../ui/LoadingSpinner';
import { PriceTag } from '../ui/PriceTag';
import { apiClient } from '../../api/client';

export interface LocationData {
  id: string;
  region_id?: string;
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

interface MarketItem {
  service: string;
  npc: string;
  base_price: number;
  final_price: number;
}

interface Props {
  locationId: string;
  worldId?: string;
  onSelectNpc?: (id: string) => void;
  onLocationLoad?: (loc: LocationData) => void;
}

const SERVICE_ICONS: Record<string, string> = {
  sell_weapons: '⚔️',
  repair: '🔧',
  buy_ore: '⛏️',
  sell_potions: '🧪',
  training: '🏋️',
  healing: '❤️‍🩹',
  inn_stay: '🍺',
  buy_food: '🍞',
  sell_scrolls: '📜',
  identification: '🔍',
};

export function LocationDetail({ locationId, worldId, onSelectNpc, onLocationLoad }: Props) {
  const { t } = useTranslation('common');
  const navigate = useNavigate();
  const { data: loc, loading: locLoading } = useApiGet<LocationData>(`/locations/${locationId}`, [
    locationId,
  ]);
  const { data: npcs, loading: npcsLoading } = useApiGet<NpcSummary[]>(
    `/locations/${locationId}/npcs`,
    [locationId],
  );
  const { data: market } = useApiGet<MarketItem[]>(`/locations/${locationId}/market`, [locationId]);

  const [adventures, setAdventures] = useState<{ id: string; name: string; description: string }[]>(
    [],
  );

  const onLocationLoadRef = useRef(onLocationLoad);
  onLocationLoadRef.current = onLocationLoad;
  useEffect(() => {
    if (loc && onLocationLoadRef.current) onLocationLoadRef.current(loc);
  }, [loc]);

  useEffect(() => {
    if (!loc) return;
    apiClient
      .get(`/adventures/by-location/${loc.id}`)
      .then((r: any) => setAdventures(r.data as any[]))
      .catch(() => {});
  }, [loc]);

  if (locLoading || npcsLoading) {
    return (
      <div className="flex items-center justify-center h-full text-text-secondary">
        <LoadingSpinner />
      </div>
    );
  }
  if (!loc) {
    return (
      <div className="flex items-center justify-center h-full text-text-secondary">
        {t('status.loading')}
      </div>
    );
  }

  // Group market items by NPC
  const npcServices = (market ?? []).reduce<Record<string, MarketItem[]>>((acc, item) => {
    (acc[item.npc] ??= []).push(item);
    return acc;
  }, {});

  // NPC detail lookup
  const npcMap = new Map(npcs?.map((n) => [n.name, n]) ?? []);

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
          <span className="flex items-center gap-1">
            <Coins size={14} /> {t('locationDetail.wealth')}: {loc.wealth}/10
          </span>
          <span className="flex items-center gap-1">
            <Users size={14} /> {loc.population}
          </span>
          {worldId && (
            <button
              onClick={() => navigate(`/worlds/${worldId}/locations/${locationId}/market`)}
              className="flex items-center gap-1 rounded bg-accent/15 px-2 py-0.5 text-accent hover:bg-accent/25"
            >
              <ShoppingCart size={14} /> Market
            </button>
          )}
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
            <ScrollText size={16} /> {t('locationDetail.history')}
          </h3>
          <p className="text-sm text-text-secondary">{loc.history}</p>
        </div>
      )}

      {/* Adventures at this location */}
      {adventures.length > 0 && (
        <div className="rounded-lg border border-accent/20 bg-accent/5 p-4">
          <h3 className="mb-3 flex items-center gap-1.5 text-sm font-semibold text-text-primary">
            <Swords size={16} className="text-warning" /> Available Adventures
          </h3>
          <div className="space-y-2">
            {adventures.map((adv) => (
              <button
                key={adv.id}
                onClick={() => navigate(`/worlds/${worldId}/adventures/${adv.id}`)}
                className="w-full rounded border border-accent/20 bg-bg-surface px-4 py-3 text-left transition hover:border-accent hover:bg-accent/5"
              >
                <div className="text-sm font-medium text-text-primary">{adv.name}</div>
                {adv.description && (
                  <div className="mt-0.5 text-xs text-text-secondary line-clamp-2">
                    {adv.description}
                  </div>
                )}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Service Cards */}
      <div className="rounded-lg bg-bg-surface p-4">
        <h3 className="mb-3 text-sm font-semibold text-text-primary">
          {t('locationDetail.servicesAndNpcs')}
        </h3>

        {Object.keys(npcServices).length === 0 && (!npcs || npcs.length === 0) ? (
          <p className="text-xs text-text-secondary">{t('locationDetail.noNpcs')}</p>
        ) : (
          <div className="space-y-3">
            {Object.entries(npcServices).map(([npcName, items]) => {
              const npc = npcMap.get(npcName);
              return (
                <div
                  key={npcName}
                  className="rounded border border-bg-elevated bg-bg-primary/50 p-3"
                >
                  {/* NPC Header */}
                  <div
                    onClick={() => npc && onSelectNpc?.(npc.id)}
                    className="flex cursor-pointer items-center gap-2 mb-2 hover:text-accent"
                  >
                    <div className="h-7 w-7 rounded-full bg-accent/20 flex items-center justify-center text-xs text-accent">
                      {npcName.charAt(0)}
                    </div>
                    <span className="text-sm font-medium text-text-primary">{npcName}</span>
                    {npc && (
                      <span className="text-[10px] text-text-secondary">{npc.entity_type}</span>
                    )}
                    <Briefcase size={12} className="text-text-secondary ml-auto" />
                  </div>

                  {/* Service Items */}
                  <div className="grid grid-cols-2 gap-1.5">
                    {items.map((item, i) => (
                      <div
                        key={i}
                        className="flex items-center justify-between rounded bg-bg-elevated/50 px-2 py-1.5 text-xs"
                      >
                        <span className="text-text-primary">
                          {SERVICE_ICONS[item.service] ?? '📦'} {item.service.replace('_', ' ')}
                        </span>
                        <PriceTag basePrice={item.base_price} finalPrice={item.final_price} />
                      </div>
                    ))}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
