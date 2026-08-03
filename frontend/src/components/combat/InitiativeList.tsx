import { useTranslation } from 'react-i18next';
import { useCombatStore } from '../../store/combatStore';

export function InitiativeList() {
  const { t } = useTranslation('common');
  const session = useCombatStore((s) => s.session);
  const participants = useCombatStore((s) => s.participants);

  if (!session) return null;

  return (
    <div className="space-y-1">
      <div className="flex items-center justify-between text-xs text-text-secondary mb-2">
        <span className="font-semibold uppercase tracking-wide">{t('combat.initiative')}</span>
        <span>
          {t('combat.round')} {session.round}
        </span>
      </div>

      {participants.map((p, idx) => {
        const isActive = p.entityId === session.currentTurnEntityId;
        return (
          <div
            key={p.entityId}
            className={`flex items-center gap-2 rounded px-3 py-1.5 text-xs ${
              isActive
                ? 'bg-accent/15 text-accent font-medium ring-1 ring-accent/40'
                : 'bg-bg-elevated/40 text-text-secondary'
            }`}
          >
            <span className="w-5 text-center text-[10px] text-text-secondary">{idx + 1}.</span>
            <span className="flex-1 truncate">{p.name ?? p.entityId.slice(0, 12)}</span>
            <span className="text-text-secondary">{p.initiative}</span>
            <span
              className={`text-[10px] ${(p.hpCurrent ?? 10) > 0 ? 'text-success' : 'text-danger'}`}
            >
              {(p.hpCurrent ?? 10) > 0 ? `❤️ ${p.hpCurrent}/${p.hpMax ?? '?'}` : '💀 Defeated'}
            </span>
            <span
              className={`text-[10px] ${p.apCurrent > 0 ? 'text-accent' : 'text-text-secondary'}`}
            >
              AP {p.apCurrent}/{p.apMax}
            </span>
          </div>
        );
      })}

      {participants.length === 0 && (
        <p className="text-xs text-text-secondary">{t('combat.noParticipants')}</p>
      )}
    </div>
  );
}
