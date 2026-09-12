import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, ShoppingCart } from 'lucide-react';
import { useApiGet } from '../hooks/useApiGet';
import { PriceTag } from '../components/ui/PriceTag';
import { LoadingSpinner } from '../components/ui/LoadingSpinner';

interface MarketItem {
  service: string;
  npc: string;
  base_price: number;
  final_price: number;
  wealth_factor: number;
  price_modifier: number;
}

const SERVICE_LABELS: Record<string, string> = {
  sell_weapons: '⚔️ Weapons',
  repair: '🔧 Repair',
  buy_ore: '⛏️ Ore',
  sell_potions: '🧪 Potions',
  training: '🏋️ Training',
  healing: '❤️‍🩹 Healing',
  inn_stay: '🍺 Inn',
  buy_food: '🍞 Food',
  sell_scrolls: '📜 Scrolls',
  identification: '🔍 Identification',
};

export default function MarketPage() {
  const { t } = useTranslation('common');
  const { locationId } = useParams<{ locationId: string }>();
  const navigate = useNavigate();

  const { data: items, loading } = useApiGet<MarketItem[]>(`/locations/${locationId}/market`, [
    locationId,
  ]);

  const grouped = (items ?? []).reduce<Record<string, MarketItem[]>>((acc, item) => {
    (acc[item.npc] ??= []).push(item);
    return acc;
  }, {});

  return (
    <div className="min-h-screen bg-bg-primary">
      <header className="flex items-center gap-3 border-b border-bg-elevated bg-bg-surface px-6 py-3">
        <button onClick={() => navigate(-1)} className="text-text-secondary hover:text-accent">
          <ArrowLeft size={20} />
        </button>
        <h1 className="flex items-center gap-2 text-lg font-heading text-text-primary">
          <ShoppingCart size={20} className="text-accent" /> {t('market.title')}
        </h1>
      </header>

      <main className="mx-auto max-w-3xl p-6">
        {loading ? (
          <LoadingSpinner size="md" text="Loading market…" />
        ) : Object.keys(grouped).length === 0 ? (
          <p className="py-12 text-center text-sm text-text-secondary">
            {t('market.noServices')}
          </p>
        ) : (
          <div className="space-y-6">
            {Object.entries(grouped).map(([npc, npcItems]) => (
              <section key={npc} className="rounded-lg border border-bg-elevated bg-bg-surface p-5">
                <h2 className="mb-3 font-heading text-text-primary">{npc}</h2>
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-bg-elevated text-left text-xs text-text-secondary">
                      <th className="pb-2 font-medium">{t('market.service')}</th>
                      <th className="pb-2 font-medium">{t('market.base')}</th>
                      <th className="pb-2 font-medium">{t('market.price')}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {npcItems.map((item, i) => (
                      <tr key={i} className="border-b border-bg-elevated/50">
                        <td className="py-2 text-text-primary">
                          {t(`market.svc_${item.service}`, { defaultValue: SERVICE_LABELS[item.service] ?? item.service })}
                        </td>
                        <td className="py-2 text-text-secondary">{item.base_price} G</td>
                        <td className="py-2">
                          <PriceTag basePrice={item.base_price} finalPrice={item.final_price} />
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </section>
            ))}
          </div>
        )}
      </main>
    </div>
  );
}
