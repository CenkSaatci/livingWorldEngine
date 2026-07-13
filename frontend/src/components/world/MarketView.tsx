import { useEffect, useState } from 'react';
import { Store } from 'lucide-react';
import { apiClient } from '../../api/client';
import { PriceTag } from '../ui/PriceTag';

interface MarketItem {
  service: string;
  npc: string;
  base_price: number;
  final_price: number;
  wealth_factor: number;
  price_modifier: number;
}

interface Props {
  locationId: string;
}

export function MarketView({ locationId }: Props) {
  const [items, setItems] = useState<MarketItem[]>([]);
  const [wealth, setWealth] = useState(5);

  useEffect(() => {
    Promise.all([
      apiClient.get(`/locations/${locationId}/market`).catch(() => null),
      apiClient.get(`/locations/${locationId}`).catch(() => null),
    ]).then(([marketRes, locRes]) => {
      if (marketRes) setItems(marketRes.data);
      if (locRes) setWealth(locRes.data.wealth ?? 5);
    });
  }, [locationId]);

  return (
    <div className="max-w-2xl">
      <h2 className="flex items-center gap-2 text-lg font-heading text-text-primary mb-1">
        <Store size={20} className="text-accent" /> Market
      </h2>
      <p className="text-xs text-text-secondary mb-4">
        Wealth: {'💰'.repeat(Math.ceil(wealth / 2))}{'○'.repeat(5 - Math.ceil(wealth / 2))} ({wealth}/10)
      </p>

      {items.length === 0 ? (
        <p className="text-sm text-text-secondary">No market data available</p>
      ) : (
        <div className="overflow-hidden rounded-lg border border-bg-elevated">
          <table className="w-full text-sm">
            <thead className="bg-bg-surface">
              <tr className="text-xs text-text-secondary uppercase tracking-wide">
                <th className="px-3 py-2 text-left">Service</th>
                <th className="px-3 py-2 text-left">NPC</th>
                <th className="px-3 py-2 text-right">Base</th>
                <th className="px-3 py-2 text-right">Price</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-bg-elevated">
              {items.map((item, i) => (
                <tr key={i} className="bg-bg-surface/30 hover:bg-bg-surface/60">
                  <td className="px-3 py-2 text-text-primary capitalize">
                    {item.service.replace(/_/g, ' ')}
                  </td>
                  <td className="px-3 py-2 text-text-secondary">{item.npc}</td>
                  <td className="px-3 py-2 text-right text-text-secondary">{item.base_price} G</td>
                  <td className="px-3 py-2 text-right">
                    <PriceTag basePrice={item.base_price} finalPrice={item.final_price} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
