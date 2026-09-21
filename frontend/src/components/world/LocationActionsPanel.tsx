import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Sparkles, ShoppingCart, ChevronRight } from 'lucide-react';
import { apiClient } from '../../api/client';
import { useApiGet } from '../../hooks/useApiGet';
import { useToast } from '../../hooks/useToast';
import { useCampaignStore } from '../../store/campaignStore';
import { LoadingSpinner } from '../ui/LoadingSpinner';
import { ActorSelect } from './ActorSelect';

interface Cost {
  type: string;
  amount?: number;
  text?: string;
  name?: string;
  qty?: number;
}

interface ActionInfo {
  name: string;
  description?: string | null;
  chat?: string | null;
  trade: boolean;
  dmOnly: boolean;
  requiresTrait?: string | null;
  available: boolean;
  reason?: string | null;
  costs: Cost[];
}

interface AppliedEffect extends Cost {
  remove?: boolean;
  rounds?: number;
  mode?: string;
  current?: number;
}

interface ActionResult {
  action: string;
  success: boolean;
  trade: boolean;
  text?: string | null;
  moneyBefore: number;
  moneyAfter: number;
  moneyBeforeText: string;
  moneyAfterText: string;
  effects: AppliedEffect[];
  probe?: { skill: string; success: boolean; total: number; dice: number[]; modifier: number } | null;
}

export function LocationActionsPanel({
  worldId,
  locationId,
  onOpenMarket,
  onExecuted,
}: {
  worldId: string;
  locationId: string;
  onOpenMarket?: () => void;
  onExecuted?: () => void;
}) {
  const { t } = useTranslation('common');
  const toast = useToast();
  const campaignId = useCampaignStore((s) => s.activeCampaignId);
  const [actorId, setActorId] = useState('');
  const [busy, setBusy] = useState<string | null>(null);
  const [result, setResult] = useState<ActionResult | null>(null);

  const query = new URLSearchParams();
  if (actorId) query.set('actorId', actorId);
  if (campaignId) query.set('campaignId', campaignId);
  const { data: actions, loading, refetch } = useApiGet<ActionInfo[]>(
    `/locations/${locationId}/actions?${query.toString()}`,
    [locationId, actorId, campaignId],
  );

  const execute = async (action: ActionInfo) => {
    if (action.trade) {
      onOpenMarket?.();
      return;
    }
    if (!actorId) return;
    setBusy(action.name);
    setResult(null);
    try {
      const res = await apiClient.post<ActionResult>(
        `/locations/${locationId}/actions/${encodeURIComponent(action.name)}`,
        { actorId, campaignId },
      );
      setResult(res.data);
      refetch();
      onExecuted?.();
    } catch (e: unknown) {
      const code = (e as { response?: { data?: { error?: { code?: string } } } })?.response?.data?.error
        ?.code;
      toast.error(t(`errors:${code}`, { defaultValue: t('poi.failed') }));
    } finally {
      setBusy(null);
    }
  };

  const reasonText = (reason?: string | null) =>
    reason === 'DM_ONLY'
      ? t('poi.reasonDmOnly')
      : reason === 'TRAIT_REQUIRED'
        ? t('poi.reasonTrait')
        : t('poi.reasonNotHere');

  return (
    <section className="rounded-lg border border-bg-elevated bg-bg-surface p-4">
      <div className="mb-3 flex items-center justify-between gap-2">
        <h3 className="flex items-center gap-1.5 text-sm font-semibold text-text-primary">
          <Sparkles size={16} className="text-accent" /> {t('poi.title')}
        </h3>
        <ActorSelect worldId={worldId} value={actorId} onChange={setActorId} />
      </div>

      {loading ? (
        <LoadingSpinner size="sm" text={t('status.loading')} />
      ) : !actions || actions.length === 0 ? (
        <p className="text-xs text-text-secondary">{t('poi.empty')}</p>
      ) : (
        <ul className="space-y-2">
          {actions.map((action) => (
            <li key={action.name}>
              <button
                onClick={() => execute(action)}
                disabled={!action.available || busy === action.name || !actorId}
                title={!action.available ? reasonText(action.reason) : undefined}
                className="flex w-full items-start justify-between gap-2 rounded border border-bg-elevated bg-bg-primary/50 px-3 py-2 text-left transition hover:border-accent hover:bg-accent/5 disabled:cursor-not-allowed disabled:opacity-50"
              >
                <span className="min-w-0">
                  <span className="flex items-center gap-1.5 text-sm text-text-primary">
                    {action.trade && <ShoppingCart size={13} className="text-accent" />}
                    {action.name}
                  </span>
                  {action.description && (
                    <span className="mt-0.5 block text-xs text-text-secondary">
                      {action.description}
                    </span>
                  )}
                  {!action.available && (
                    <span className="mt-0.5 block text-[11px] text-warning">
                      {reasonText(action.reason)}
                    </span>
                  )}
                  {action.costs.length > 0 && action.available && (
                    <span className="mt-0.5 block text-[11px] text-text-secondary">
                      {t('poi.cost')}:{' '}
                      {action.costs
                        .map((c) =>
                          c.type === 'money'
                            ? c.text
                            : `${Math.abs(c.qty ?? 0)}× ${c.name ?? ''}`,
                        )
                        .join(', ')}
                    </span>
                  )}
                </span>
                <ChevronRight size={16} className="mt-0.5 shrink-0 text-text-secondary" />
              </button>
            </li>
          ))}
        </ul>
      )}

      {result && (
        <div className="mt-3 rounded border border-accent/30 bg-accent/5 p-3 text-xs">
          <p className="font-medium text-text-primary">{result.action}</p>
          {result.probe && (
            <p className="mt-1 text-text-secondary">
              {t('poi.probe', {
                skill: result.probe.skill,
                dice: result.probe.dice.join(' + '),
                total: result.probe.total,
              })}{' '}
              <span className={result.probe.success ? 'text-success' : 'text-danger'}>
                {result.probe.success ? t('poi.probeSuccess') : t('poi.probeFailure')}
              </span>
            </p>
          )}
          {result.text && <p className="mt-1 text-text-primary">{result.text}</p>}
          {result.moneyBefore !== result.moneyAfter && (
            <p className="mt-1 text-text-secondary">
              {t('poi.money')}: {result.moneyBeforeText} → {result.moneyAfterText}
            </p>
          )}
          {result.effects.filter((e) => e.type !== 'text').length > 0 && (
            <ul className="mt-1 space-y-0.5 text-text-secondary">
              {result.effects
                .filter((e) => e.type !== 'text')
                .map((e, i) => (
                  <li key={i}>{effectLabel(t, e)}</li>
                ))}
            </ul>
          )}
        </div>
      )}
    </section>
  );
}

function effectLabel(
  t: (key: string, opts?: Record<string, unknown>) => string,
  e: AppliedEffect,
): string {
  switch (e.type) {
    case 'money':
      return t('poi.effect.money', { text: e.text, amount: e.amount });
    case 'item':
      return (e.qty ?? 0) >= 0
        ? t('poi.effect.itemAdd', { qty: e.qty, name: e.name })
        : t('poi.effect.itemRemove', { qty: -(e.qty ?? 0), name: e.name });
    case 'heal':
      return t('poi.effect.heal', { amount: e.amount });
    case 'condition':
      return e.remove
        ? t('poi.effect.conditionRemove', { name: e.name })
        : t('poi.effect.condition', { name: e.name });
    case 'fate':
      return t('poi.effect.fate', { amount: e.amount });
    case 'rest':
      return e.mode === 'short' ? t('poi.effect.restShort') : t('poi.effect.restLong');
    default:
      return e.type;
  }
}
