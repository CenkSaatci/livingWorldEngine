import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ShoppingCart, Coins } from 'lucide-react';
import { apiClient } from '../../api/client';
import { useApiGet } from '../../hooks/useApiGet';
import { useToast } from '../../hooks/useToast';
import { useCampaignStore } from '../../store/campaignStore';
import { LoadingSpinner } from '../ui/LoadingSpinner';
import { ActorSelect } from './ActorSelect';

interface Offer {
  item: string;
  itemId?: string | null;
  price: number;
  sellPrice: number;
  resolved: boolean;
}

interface MerchantInfo {
  npcId: string;
  name: string;
  occupation?: string | null;
  greeting?: string | null;
  sellRate: number;
  offers: Offer[];
}

interface TradeResult {
  item: string;
  qty: number;
  unitPrice: number;
  total: number;
  moneyBeforeText: string;
  moneyAfterText: string;
}

/** Markt-/Händler-Ansicht (ADR-015): Sortiment, Kauf und Verkauf. */
export function MerchantPanel({ worldId, locationId }: { worldId: string; locationId: string }) {
  const { t } = useTranslation('common');
  const toast = useToast();
  const campaignId = useCampaignStore((s) => s.activeCampaignId);
  const [actorId, setActorId] = useState('');
  const [qty, setQty] = useState<Record<string, number>>({});
  const [busy, setBusy] = useState<string | null>(null);
  const [moneyText, setMoneyText] = useState<string | null>(null);

  const query = campaignId ? `?campaignId=${campaignId}` : '';
  const { data: merchants, loading, refetch } = useApiGet<MerchantInfo[]>(
    `/locations/${locationId}/merchants${query}`,
    [locationId, campaignId],
  );

  const onActorChange = (id: string) => {
    setActorId(id);
    void (async () => {
      try {
        const res = await apiClient.get<{ moneyText?: string }>(
          `/entities/${id}/sheet${campaignId ? `?campaignId=${campaignId}` : ''}`,
        );
        setMoneyText(res.data.moneyText ?? null);
      } catch {
        /* Anzeige ist optional */
      }
    })();
  };

  const trade = async (merchant: MerchantInfo, offer: Offer, mode: 'buy' | 'sell') => {
    if (!actorId) return;
    const quantity = qty[`${merchant.npcId}:${offer.item}`] ?? 1;
    const key = `${mode}:${merchant.npcId}:${offer.item}`;
    setBusy(key);
    try {
      const res = await apiClient.post<TradeResult>(
        `/merchants/${merchant.npcId}/${mode}`,
        { locationId, actorId, item: offer.item, qty: quantity, campaignId },
      );
      const label = mode === 'buy' ? t('market.bought') : t('market.sold');
      toast.success(
        t('market.tradeDone', {
          label,
          qty: res.data.qty,
          item: res.data.item,
          total: res.data.total,
          money: res.data.moneyAfterText,
        }),
      );
      setMoneyText(res.data.moneyAfterText);
      refetch();
    } catch (e: unknown) {
      const code = (e as { response?: { data?: { error?: { code?: string } } } })?.response?.data?.error
        ?.code;
      toast.error(t(`errors:${code}`, { defaultValue: t('market.failed') }));
    } finally {
      setBusy(null);
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between gap-2">
        <ActorSelect worldId={worldId} value={actorId} onChange={onActorChange} />
        {moneyText !== null && (
          <span className="flex items-center gap-1 text-xs text-text-secondary">
            <Coins size={13} className="text-warning" /> {moneyText}
          </span>
        )}
      </div>

      {loading ? (
        <LoadingSpinner size="md" text={t('status.loading')} />
      ) : !merchants || merchants.length === 0 ? (
        <p className="py-8 text-center text-sm text-text-secondary">{t('market.noMerchants')}</p>
      ) : (
        merchants.map((merchant) => (
          <section key={merchant.npcId} className="rounded-lg border border-bg-elevated bg-bg-surface p-5">
            <h2 className="mb-1 flex items-center gap-2 font-heading text-text-primary">
              <ShoppingCart size={16} className="text-accent" /> {merchant.name}
              {merchant.occupation && (
                <span className="text-xs text-text-secondary">{merchant.occupation}</span>
              )}
            </h2>
            {merchant.greeting && (
              <p className="mb-3 text-xs italic text-text-secondary">„{merchant.greeting}"</p>
            )}
            {merchant.offers.length === 0 ? (
              <p className="text-xs text-text-secondary">{t('market.noAssortment')}</p>
            ) : (
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-bg-elevated text-left text-xs text-text-secondary">
                    <th className="pb-2 font-medium">{t('market.item')}</th>
                    <th className="pb-2 font-medium">{t('market.buy')}</th>
                    <th className="pb-2 font-medium">{t('market.sell')}</th>
                    <th className="pb-2 font-medium">{t('market.quantity')}</th>
                    <th className="pb-2" />
                  </tr>
                </thead>
                <tbody>
                  {merchant.offers.map((offer) => {
                    const key = `${merchant.npcId}:${offer.item}`;
                    return (
                      <tr key={offer.item} className="border-b border-bg-elevated/50">
                        <td className="py-2 text-text-primary">
                          {offer.item}
                          {!offer.resolved && (
                            <span className="ml-2 text-[10px] text-warning">
                              {t('market.unresolved')}
                            </span>
                          )}
                        </td>
                        <td className="py-2 text-text-secondary">{offer.price}</td>
                        <td className="py-2 text-text-secondary">{offer.sellPrice}</td>
                        <td className="py-2">
                          <input
                            type="number"
                            min={1}
                            value={qty[key] ?? 1}
                            onChange={(e) =>
                              setQty((prev) => ({ ...prev, [key]: Math.max(1, Number(e.target.value)) }))
                            }
                            className="w-16 rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
                          />
                        </td>
                        <td className="py-2">
                          <div className="flex justify-end gap-1">
                            <button
                              onClick={() => trade(merchant, offer, 'buy')}
                              disabled={!actorId || !offer.resolved || busy !== null}
                              className="rounded bg-accent px-2 py-1 text-xs text-white hover:bg-accent/80 disabled:opacity-40"
                            >
                              {t('market.buy')}
                            </button>
                            <button
                              onClick={() => trade(merchant, offer, 'sell')}
                              disabled={!actorId || !offer.resolved || busy !== null}
                              className="rounded border border-bg-elevated px-2 py-1 text-xs text-text-secondary hover:text-text-primary disabled:opacity-40"
                            >
                              {t('market.sell')}
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            )}
          </section>
        ))
      )}
    </div>
  );
}
