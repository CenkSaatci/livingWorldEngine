import { useTranslation } from 'react-i18next';
import { useCombatStore } from '../../store/combatStore';

export function ApBar() {
  const { t } = useTranslation('common');
  const session = useCombatStore((s) => s.session);
  const participants = useCombatStore((s) => s.participants);

  if (!session || session.status !== 'ACTIVE') return null;

  const currentActor = participants.find((p) => p.entityId === session.currentTurnEntityId);
  if (!currentActor) return null;

  const pct = (currentActor.apCurrent / currentActor.apMax) * 100;

  return (
    <div className="space-y-1">
      <div className="flex items-center justify-between text-xs">
        <span className="text-text-secondary">{t('combat.ap')}</span>
        <span className="text-text-primary font-medium">
          {currentActor.apCurrent} / {currentActor.apMax}
        </span>
      </div>
      <div className="h-2 w-full overflow-hidden rounded-full bg-bg-elevated">
        <div
          className={`h-full rounded-full transition-all duration-300 ${
            pct > 50 ? 'bg-accent' : pct > 25 ? 'text-yellow-500 bg-yellow-500' : 'bg-danger'
          }`}
          style={{ width: `${pct}%` }}
        />
      </div>
    </div>
  );
}
