import { useTranslation } from 'react-i18next';
import { Sword, SkipForward, LogOut, Move, Shield } from 'lucide-react';
import { apiClient } from '../../api/client';
import { useCombatStore } from '../../store/combatStore';
import { playCombatHit } from '../../utils/sound';

interface Props {
  worldId: string;
}

export function ActionBar({ worldId: _worldId }: Props) {
  const { t } = useTranslation('common');
  const session = useCombatStore((s) => s.session);
  const participants = useCombatStore((s) => s.participants);
  const targetEntityId = useCombatStore((s) => s.targetEntityId);
  const setTargetEntityId = useCombatStore((s) => s.setTargetEntityId);

  if (!session || session.status !== 'ACTIVE') return null;

  const currentActor = participants.find((p) => p.entity_id === session.current_turn_entity_id);
  const aliveTargets = participants.filter(
    (p) => p.entity_id !== session.current_turn_entity_id && p.ap_current > 0,
  );

  const handleAttack = async () => {
    if (!targetEntityId) return;
    try {
      const res = await apiClient.post(`/combat/${session.id}/action`, {
        actorId: currentActor?.entity_id,
        actionType: 'ATTACK',
        targetId: targetEntityId,
      });
      if (res.data.success) {
        useCombatStore
          .getState()
          .updateParticipantAp(currentActor?.entity_id ?? '', res.data.ap_remaining);
        playCombatHit();
      }
      // Neu laden für aktualisierte HP
      const refresh = await apiClient.get(`/combat/${session.id}`);
      useCombatStore.getState().setSession(refresh.data.session, refresh.data.participants);
    } catch {
      /* */
    }
  };

  const handleDefend = async () => {
    try {
      const res = await apiClient.post(`/combat/${session.id}/action`, {
        actorId: currentActor?.entity_id,
        actionType: 'DEFEND',
      });
      if (res.data.success) {
        useCombatStore
          .getState()
          .updateParticipantAp(currentActor?.entity_id ?? '', res.data.ap_remaining);
      }
    } catch {
      /* */
    }
  };

  const handleMove = async () => {
    try {
      const res = await apiClient.post(`/combat/${session.id}/action`, {
        actorId: currentActor?.entity_id,
        actionType: 'MOVE',
      });
      if (res.data.success) {
        useCombatStore
          .getState()
          .updateParticipantAp(currentActor?.entity_id ?? '', res.data.ap_remaining);
      }
    } catch {
      /* */
    }
  };

  const handleNextTurn = async () => {
    try {
      await apiClient.post(`/combat/${session.id}/next-turn`);
    } catch {
      /* */
    }
  };

  const handleEndCombat = async () => {
    try {
      await apiClient.post(`/combat/${session.id}/end`);
    } catch {
      /* */
    }
  };

  return (
    <div className="space-y-3">
      {/* Target Selection */}
      {aliveTargets.length > 0 && (
        <div>
          <p className="text-xs text-text-secondary mb-1">{t('combat.target')}</p>
          <div className="flex flex-wrap gap-1">
            {aliveTargets.map((t) => (
              <button
                key={t.entity_id}
                onClick={() =>
                  setTargetEntityId(t.entity_id === targetEntityId ? null : t.entity_id)
                }
                className={`rounded px-2 py-1 text-xs ${
                  t.entity_id === targetEntityId
                    ? 'bg-danger text-white'
                    : 'bg-bg-elevated text-text-secondary hover:text-text-primary'
                }`}
              >
                {t.entity_id.slice(0, 8)}…
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Actions */}
      <div className="flex items-center gap-2">
        <button
          onClick={handleAttack}
          disabled={!targetEntityId}
          className="flex items-center gap-1 rounded bg-accent px-3 py-1.5 text-xs text-white hover:bg-accent/80 disabled:opacity-40"
        >
          <Sword size={14} /> {t('combat.attack')}
        </button>

        <button
          onClick={handleDefend}
          className="flex items-center gap-1 rounded bg-bg-elevated px-3 py-1.5 text-xs text-text-secondary hover:text-text-primary"
        >
          <Shield size={14} /> Defend
        </button>

        <button
          onClick={handleMove}
          className="flex items-center gap-1 rounded bg-bg-elevated px-3 py-1.5 text-xs text-text-secondary hover:text-text-primary"
        >
          <Move size={14} /> Move
        </button>

        <button
          onClick={handleNextTurn}
          className="flex items-center gap-1 rounded bg-bg-elevated px-3 py-1.5 text-xs text-text-secondary hover:text-text-primary"
        >
          <SkipForward size={14} /> {t('combat.nextTurn')}
        </button>

        <button
          onClick={handleEndCombat}
          className="flex items-center gap-1 rounded bg-danger/20 px-3 py-1.5 text-xs text-danger hover:bg-danger/30"
        >
          <LogOut size={14} /> {t('combat.end')}
        </button>
      </div>
    </div>
  );
}
