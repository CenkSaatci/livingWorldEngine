import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Dice1 as Dice } from 'lucide-react';
import { apiClient } from '../../api/client';
import { useToast } from '../../hooks/useToast';
import { useCampaignStore } from '../../store/campaignStore';

interface Props {
  entityId: string;
  skillName: string;
  skillTotal: number;
  fateAvailable?: boolean;
  onSpendFate?: () => Promise<void>;
  casting?: { resource: string; cost: number; requiresTrait?: string } | null;
  difficultyLevels?: { name: string; multiplier?: number | null; delta?: number | null }[];
}

interface CastResult {
  probe: ProbeResult;
  resource: string;
  cost: number;
  resourceRemaining: number;
  resourceMax: number;
}

interface ProbeResult {
  probeType: string;
  dice: number[];
  modifier: number;
  total: number;
  success: boolean;
  details: { die: number; attribute: string; attrValue: number; success: boolean }[];
  activeConditionals: { name: string; bonus: string; target: string }[];
}

export function ProbeRoller({ entityId, skillName, fateAvailable, onSpendFate, casting, difficultyLevels }: Props) {
  const { t } = useTranslation('character');
  const toast = useToast();
  const [result, setResult] = useState<ProbeResult | null>(null);
  const [rolling, setRolling] = useState(false);
  const [spending, setSpending] = useState(false);
  const [showDetails, setShowDetails] = useState(false);
  const [failed, setFailed] = useState(false);
  const [remaining, setRemaining] = useState<number | null>(null);
  const [difficultyKey, setDifficultyKey] = useState('');
  const [useFate, setUseFate] = useState(false);
  const activeCampaignId = useCampaignStore((s) => s.activeCampaignId);

  const handleRoll = async () => {
    setRolling(true);
    setShowDetails(false);
    setFailed(false);
    try {
      if (casting) {
        const res = await apiClient.post<CastResult>('/rolls/cast', {
          entityId,
          skillName,
          campaignId: activeCampaignId ?? undefined,
        });
        setResult(res.data.probe);
        setRemaining(res.data.resourceRemaining);
      } else {
        const res = await apiClient.post<ProbeResult>('/rolls/probe', {
          entityId,
          skillName,
          target: 10,
          advantage: false,
          campaignId: activeCampaignId ?? undefined,
          ...(difficultyKey ? { difficultyKey } : {}),
          ...(useFate ? { useFate: true } : {}),
        });
        setResult(res.data);
        setUseFate(false); // Punkt ist ausgegeben
      }
    } catch (e: any) {
      // Kein lokaler Fallback-Wurf: Ein fehlgeschlagener Server-Wurf darf nicht
      // wie ein echtes Ergebnis aussehen. Fehler anzeigen, nichts würfeln.
      setResult(null);
      setFailed(true);
      const code = e?.response?.data?.error?.code;
      toast.error(
        code === 'CAST_INSUFFICIENT_RESOURCE'
          ? t('sheet.castNoResource')!
          : code === 'CAST_MISSING_TRAIT'
            ? t('sheet.castNoTrait')!
            : t('sheet.probeFailed')!,
      );
    } finally {
      setRolling(false);
    }
  };

  return (
    <div className="flex items-center gap-1">
      {(difficultyLevels?.length ?? 0) > 0 && !casting && (
        <select
          value={difficultyKey}
          onChange={(e) => setDifficultyKey(e.target.value)}
          aria-label={t('sheet.difficulty')}
          title={t('sheet.difficulty')!}
          className="rounded border border-bg-elevated bg-bg-primary px-1 py-0.5 text-[10px] text-text-secondary outline-none focus:border-accent"
        >
          <option value="">{t('sheet.difficulty')}</option>
          {difficultyLevels!.map((l) => (
            <option key={l.name} value={l.name}>{l.name}</option>
          ))}
        </select>
      )}
      {casting && (
        <span
          className="rounded bg-accent/10 px-1 text-[10px] text-accent"
          title={t('sheet.castCost', { cost: casting.cost, resource: casting.resource.toUpperCase() })!}
        >
          {casting.cost} {casting.resource.toUpperCase()}
        </span>
      )}
      {fateAvailable && !casting && !result && (
        <button
          onClick={() => setUseFate((v) => !v)}
          disabled={rolling}
          aria-pressed={useFate}
          className={`rounded px-1 text-xs disabled:opacity-40 ${
            useFate ? 'text-warning' : 'text-text-secondary hover:text-warning'
          }`}
          title={t('sheet.useFate')!}
          aria-label={t('sheet.useFate')!}
        >
          +★
        </button>
      )}
      <button
        onClick={handleRoll}
        disabled={rolling}
        className="rounded p-1 text-text-secondary hover:text-accent hover:bg-bg-elevated disabled:opacity-40"
        title={t('sheet.rollProbe')!}
      >
        <Dice size={14} className={rolling ? 'animate-spin' : ''} />
      </button>
      {result && fateAvailable && onSpendFate && (
        <button
          onClick={async () => {
            if (rolling || spending) return;
            setSpending(true);
            try {
              await onSpendFate();
              await handleRoll();
            } catch {
              // Punkt weg oder Wurf fehlgeschlagen: handleRoll zeigt den Wurf-Fehler.
              toast.error(t('sheet.spendFateFailed')!);
            } finally {
              setSpending(false);
            }
          }}
          disabled={rolling || spending}
          className="text-xs text-warning hover:text-warning/70 disabled:opacity-40"
          title={t('sheet.spendFate')!}
          aria-label={t('sheet.spendFate')!}
        >
          ★
        </button>
      )}
      {failed && !result && (
        <span className="text-xs font-mono text-danger" title={t('sheet.probeFailed')!}>
          !
        </span>
      )}
      {result && (
        <>
          <button
            onClick={() => setShowDetails(!showDetails)}
            className={`text-xs font-mono ${result.success ? 'text-success' : 'text-danger'}`}
          >
            {result.total}
            {result.dice.length > 1 && (
              <span className="text-text-secondary text-[10px] ml-1">
                ({result.dice.join(', ')})
              </span>
            )}
            {remaining !== null && (
              <span className="text-text-secondary text-[10px] ml-1">
                {casting?.resource.toUpperCase()} {remaining}
              </span>
            )}
          </button>

          {showDetails && (
            <div className="absolute mt-8 right-0 z-10 w-64 rounded border border-bg-elevated bg-bg-surface p-2 shadow-lg text-[10px]">
              {result.probeType && <p className="text-text-secondary mb-1">{result.probeType}</p>}
              {result.details.map((d, i) => (
                <p key={i} className={d.success ? 'text-success' : 'text-danger'}>
                  {d.die} ≤ {d.attribute}({d.attrValue}) {d.success ? '✓' : '✗'}
                </p>
              ))}
              {result.activeConditionals.length > 0 && (
                <div className="mt-1 border-t border-bg-elevated pt-1">
                  <p className="text-text-secondary mb-0.5">Conditionals:</p>
                  {result.activeConditionals.map((c, i) => (
                    <p key={i} className="text-success">{c.name}: {c.bonus} auf {c.target}</p>
                  ))}
                </div>
              )}
            </div>
          )}
        </>
      )}
    </div>
  );
}
