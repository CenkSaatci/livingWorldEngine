/* eslint-disable @typescript-eslint/no-explicit-any */
import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { MapPin, Users, ScrollText, Coins, ShoppingCart, Briefcase, Swords } from 'lucide-react';
import { useApiGet } from '../../hooks/useApiGet';
import { LoadingSpinner } from '../ui/LoadingSpinner';
import { apiClient } from '../../api/client';

export interface LocationData {
  id: string;
  regionId?: string;
  name: string;
  type: string;
  description: string;
  history: string;
  population: number;
  wealth: number;
  services: string[];
  createdAt: string;
}

interface NpcSummary {
  id: string;
  name: string;
  entity_type: string;
}

interface ServiceNpc {
  npc_id: string;
  npc_name: string;
  occupation: string;
  greeting: string;
}

/** `/locations/{id}/services`: Dienstname → NPCs, die ihn anbieten. */
type Services = Record<string, ServiceNpc[]>;

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

/**
 * Ortsübersicht: Beschreibung, Geschichte, Abenteuer und NPCs mit ihren Diensten.
 * Preise stehen bewusst **nicht** hier — die kommen aus dem Händlermarkt (ADR-015),
 * damit es nur eine Preisquelle gibt.
 */
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
  const { data: services } = useApiGet<Services>(`/locations/${locationId}/services`, [locationId]);

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
      // Best-effort prefetch — leere Liste ist der gültige Fallback.
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

  // Dienste je NPC-Namen gruppieren (case-sensitiv wie geliefert).
  const servicesByNpc = Object.entries(services ?? {}).reduce<Record<string, string[]>>(
    (acc, [service, list]) => {
      for (const entry of list) (acc[entry.npc_name] ??= []).push(service);
      return acc;
    },
    {},
  );

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

      {/* NPCs + ihre Dienste (ohne Preise — Preise nur im Markt) */}
      <div className="rounded-lg bg-bg-surface p-4">
        <h3 className="mb-3 text-sm font-semibold text-text-primary">
          {t('locationDetail.servicesAndNpcs')}
        </h3>

        {!npcs || npcs.length === 0 ? (
          <p className="text-xs text-text-secondary">{t('locationDetail.noNpcs')}</p>
        ) : (
          <div className="space-y-3">
            {npcs.map((npc) => {
              const npcServices = servicesByNpc[npc.name] ?? [];
              return (
                <div key={npc.id} className="rounded border border-bg-elevated bg-bg-primary/50 p-3">
                  <div
                    onClick={() => onSelectNpc?.(npc.id)}
                    className="flex cursor-pointer items-center gap-2 hover:text-accent"
                  >
                    <div className="h-7 w-7 rounded-full bg-accent/20 flex items-center justify-center text-xs text-accent">
                      {npc.name.charAt(0)}
                    </div>
                    <span className="text-sm font-medium text-text-primary">{npc.name}</span>
                    <span className="text-[10px] text-text-secondary">{npc.entity_type}</span>
                    <Briefcase size={12} className="text-text-secondary ml-auto" />
                  </div>

                  {npcServices.length > 0 && (
                    <div className="mt-2 flex flex-wrap gap-1.5">
                      {npcServices.map((service) => (
                        <span
                          key={service}
                          className="rounded bg-bg-elevated/50 px-2 py-1 text-xs text-text-primary"
                        >
                          {SERVICE_ICONS[service] ?? '📦'} {service.replace('_', ' ')}
                        </span>
                      ))}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
