import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Dice1 as Dice } from 'lucide-react';
import { apiClient } from '../../api/client';

interface Props {
  skillName: string;
  skillTotal: number;
}

interface RollResult {
  expression: string;
  dice: number[];
  total: number;
  success?: boolean;
  modifier?: number;
}

export function ProbeRoller({ skillTotal }: Props) {
  const { t } = useTranslation('character');
  const [result, setResult] = useState<RollResult | null>(null);
  const [rolling, setRolling] = useState(false);

  const handleRoll = async () => {
    setRolling(true);
    try {
      // Free roll — use 1d20+skillTotal as default expression
      const res = await apiClient.post('/rolls/free', {
        expression: `1d20+${skillTotal}`,
      });
      setResult({
        expression: res.data.expression,
        dice: res.data.dice,
        total: res.data.total,
      });
    } catch {
      // Fallback: local roll
      const rolled = Math.floor(Math.random() * 20) + 1;
      const total = rolled + skillTotal;
      setResult({ expression: `1d20+${skillTotal}`, dice: [rolled], total });
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
        <span className="text-xs font-mono">
          <span className="text-text-primary">{result.total}</span>
          <span className="text-text-secondary text-[10px] ml-1">
            ({result.dice.join(', ')})
          </span>
        </span>
      )}
    </div>
  );
}
