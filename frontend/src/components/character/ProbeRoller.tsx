import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Dice1 as Dice } from 'lucide-react';
import { apiClient } from '../../api/client';

interface Props {
  entityId: string;
  skillName: string;
  skillTotal: number;
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

export function ProbeRoller({ entityId, skillName }: Props) {
  const { t } = useTranslation('character');
  const [result, setResult] = useState<ProbeResult | null>(null);
  const [rolling, setRolling] = useState(false);
  const [showDetails, setShowDetails] = useState(false);

  const handleRoll = async () => {
    setRolling(true);
    setShowDetails(false);
    try {
      const res = await apiClient.post<ProbeResult>('/rolls/probe', {
        entityId,
        skillName,
        target: 10,
        advantage: false,
      });
      setResult(res.data);
    } catch {
      // Fallback: local roll
      const rolled = Math.floor(Math.random() * 20) + 1;
      setResult({
        probeType: 'd20_target',
        dice: [rolled],
        modifier: 0,
        total: rolled,
        success: rolled >= 10,
        details: [],
        activeConditionals: [],
      });
    } finally {
      setRolling(false);
    }
  };

  return (
    <div className="flex items-center gap-1">
      <button
        onClick={handleRoll}
        disabled={rolling}
        className="rounded p-1 text-text-secondary hover:text-accent hover:bg-bg-elevated disabled:opacity-40"
        title={t('sheet.rollProbe')!}
      >
        <Dice size={14} className={rolling ? 'animate-spin' : ''} />
      </button>
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
