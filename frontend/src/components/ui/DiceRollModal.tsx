import { useEffect, useState } from 'react';
import { X } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useSettingsStore } from '../../store/settingsStore';
import { CssDice } from './CssDice';
import { ThreeDice } from './ThreeDice';
import { DICE_SKINS } from './diceSkins';

interface DieRoll {
  sides: number;
  value: number;
}

interface Props {
  label: string;
  dice: DieRoll[];
  modifier: number;
  total: number;
  onClose: () => void;
}

export function DiceRollModal({ label, dice, modifier, total, onClose }: Props) {
  const { t } = useTranslation('common');
  const diceMode = useSettingsStore((s) => s.diceMode);
  const diceSkin = useSettingsStore((s) => s.diceSkin);
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    requestAnimationFrame(() => setVisible(true));
    const handleEsc = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handleEsc);
    return () => window.removeEventListener('keydown', handleEsc);
  }, [onClose]);

  return (
    <div
      className={`fixed inset-0 z-50 flex items-center justify-center bg-black/60 transition-opacity duration-200 ${
        visible ? 'opacity-100' : 'opacity-0'
      }`}
      onClick={onClose}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-label={label}
        className="relative mx-4 w-full max-w-md rounded-xl border border-bg-elevated bg-bg-surface p-8 shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <button
          onClick={onClose}
          aria-label={t('close')}
          title={t('close')}
          className="absolute right-3 top-3 text-text-secondary hover:text-text-primary"
        >
          <X size={18} />
        </button>

        {diceMode === '3d' ? (
          <ThreeDice
            results={dice.map((d) => ({ value: d.value, sides: d.sides }))}
            modifier={modifier}
            total={total}
            skin={DICE_SKINS.find((s) => s.name === diceSkin) ?? DICE_SKINS[0]}
          />
        ) : (
          <CssDice
            results={dice.map((d) => ({ sides: d.sides, value: d.value }))}
            modifier={modifier}
            total={total}
            label={label}
          />
        )}
        <p className="sr-only" role="status" aria-live="polite">
          {t('rollResult', { total })}
        </p>
      </div>
    </div>
  );
}
