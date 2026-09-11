import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { SkipForward, LogOut, Shield, Swords, Zap } from 'lucide-react';
import { apiClient } from '../../api/client';
import { useCombatStore } from '../../store/combatStore';
import { useCampaignStore, useActiveCampaign } from '../../store/campaignStore';
import { useToast } from '../../hooks/useToast';
import { playCombatHit } from '../../utils/sound';

const ACTION_ICONS: Record<string, React.ReactNode> = {
  action: <Zap size={14} />,
  bonus_action: <Zap size={12} />,
  reaction: <Shield size={14} />,
};

interface Props {
  worldId: string;
}

interface AbilityEntry {
  abilityId: string;
  abilityName: string;
  apCost: number;
}

export function ActionBar({ worldId }: Props) {
  const { t } = useTranslation('common');
  const session = useCombatStore((s) => s.session);
  const participants = useCombatStore((s) => s.participants);
  const targetEntityId = useCombatStore((s) => s.targetEntityId);
  const setTargetEntityId = useCombatStore((s) => s.setTargetEntityId);

  const [abilities, setAbilities] = useState<AbilityEntry[]>([]);
  const [actionTypes, setActionTypes] = useState<string[]>(['action']);
  const [actionsPerTurn, setActionsPerTurn] = useState<Record<string, number>>({ action: 1 });
  const [maneuvers, setManeuvers] = useState<{ name: string; apCost?: number }[]>([]);
  const [weaponItemId, setWeaponItemId] = useState<string | null>(null);
  const [usedActions, setUsedActions] = useState<Record<string, number>>({});
  const toast = useToast();
  const activeCampaignId = useCampaignStore((s) => s.activeCampaignId);
  // GameView-Badge-Pattern: gecachte activeCampaign zuerst (Deep-Link/Reload),
  // campaigns[]-Liste ist dann ggf. noch leer.
  const activeCampaign = useActiveCampaign();
  const activeGameSystemId = activeCampaign?.gameSystemId;

  const currentActor = participants.find((p) => p.entityId === session?.currentTurnEntityId);

  // Load action config from game system (campaign context preferred, world fallback)
  useEffect(() => {
    if (!worldId) return;
    const loadActions = (gsId: string | null | undefined) => {
      if (!gsId) return;
      apiClient.get(`/game-systems/${gsId}`).then((gr) => {
        try {
          const rules = JSON.parse(gr.data.rulesJson);
          const combat = rules.dice_mechanics?.combat;
          if (combat?.action_types) setActionTypes(combat.action_types);
          if (combat?.actions_per_turn) setActionsPerTurn(combat.actions_per_turn);
          if (combat?.maneuvers) setManeuvers(combat.maneuvers);
        } catch { /* Ungültiges rulesJson → Standard-Action-Typen bleiben. */ }
        // Best-effort Config-Ladung — Defaults aus useState gelten weiter.
        // Best-effort Config-Ladung — Defaults aus useState gelten weiter.
      }).catch(() => {});
    };
    const campaign = useCampaignStore.getState().campaigns.find(
      (c) => c.id === activeCampaignId,
    );
    // 1) Gecachte aktive Kampagne zuerst (Deep-Link/Refresh: CampaignDetail
    //    cacht die Summary, campaigns[] ist nie geladen).
    if (activeGameSystemId) {
      loadActions(activeGameSystemId);
      return;
    }
    if (campaign?.gameSystemId) {
      loadActions(campaign.gameSystemId);
      return;
    }
    // 2) ID bekannt, aber weder Cache noch Liste haben sie → genau einmal
    //    GET /campaigns/{id} und cachen, dann Action-Typen laden.
    if (activeCampaignId) {
      let cancelled = false;
      apiClient.get(`/campaigns/${activeCampaignId}`).then((res) => {
        if (cancelled) return;
        const c = res.data;
        if (c?.id) useCampaignStore.getState().setActiveCampaign(c.id, c);
        if (c?.gameSystemId) loadActions(c.gameSystemId);
      }).catch(() => {
        if (!cancelled) {
          setActionTypes([]);
          setActionsPerTurn({});
        }
      });
      return () => { cancelled = true; };
    }
    // Kein Welt-Fallback mehr (P25-T06): Welten tragen kein System;
    // ohne Kampagne gibt es keine Action-Typen.
    setActionTypes([]);
    setActionsPerTurn({});
  }, [worldId, activeCampaignId, activeGameSystemId]);

  // Reset used actions on turn change
  useEffect(() => {
    setUsedActions({});
  }, [session?.currentTurnEntityId]);

  // Fetch abilities for current actor
  useEffect(() => {
    if (!currentActor?.entityId) { setAbilities([]); return; }
    let c = false;
    apiClient.get(`/entities/${currentActor.entityId}/abilities`)
      .then((r) => { if (!c) setAbilities(r.data as AbilityEntry[]); })
      .catch(() => setAbilities([]));
    return () => { c = true; };
  }, [currentActor?.entityId]);

  // Equipped weapon (P23-T02): liefert die Schadensart der Waffe mit
  useEffect(() => {
    if (!currentActor?.entityId) { setWeaponItemId(null); return; }
    let c = false;
    apiClient.get(`/entities/${currentActor.entityId}/inventory`)
      .then((r) => {
        if (c) return;
        const items = (r.data as { items?: { itemId: string; equipped: boolean; slot?: string | null; type?: string }[] }).items ?? [];
        const weapon = items.find((it) => it.equipped && (it.slot === 'weapon' || it.type === 'WEAPON'));
        setWeaponItemId(weapon?.itemId ?? null);
      })
      .catch(() => setWeaponItemId(null));
    return () => { c = true; };
  }, [currentActor?.entityId]);

  if (!session || session.status !== 'ACTIVE') return null;

  const aliveTargets = participants.filter(
    (p) => p.entityId !== session.currentTurnEntityId && p.apCurrent > 0,
  );

  const handleAction = async (type: string, abilityId?: string) => {
    try {
      const url = abilityId
        ? `/combat/${session.id}/ability`
        : `/combat/${session.id}/action`;
      const body: Record<string, unknown> = abilityId
        ? { actorId: currentActor?.entityId, abilityId, targetId: targetEntityId }
        : { actorId: currentActor?.entityId, actionType: type.toUpperCase(), targetId: targetEntityId, itemId: weaponItemId ?? undefined };

      const res = await apiClient.post(url, body);
      // POST returns full session state → AP-Werte sind bereits korrekt
      useCombatStore.getState().setSession(res.data.session, res.data.participants);
      setUsedActions((prev) => ({ ...prev, [type]: (prev[type] ?? 0) + 1 }));
      if (!abilityId) playCombatHit();
    } catch { toast.error('Action failed'); }
  };

  const handleManeuver = async (maneuver: string) => {
    try {
      const res = await apiClient.post(`/combat/${session.id}/maneuver`, {
        actorId: currentActor?.entityId,
        targetId: targetEntityId,
        maneuver,
      });
      useCombatStore.getState().setSession(res.data.session, res.data.participants);
      playCombatHit();
    } catch { toast.error(t('combat.maneuverFailed', { defaultValue: 'Maneuver failed' })); }
  };

  const isAvailable = (type: string) => (usedActions[type] ?? 0) < (actionsPerTurn[type] ?? 1);

  return (
    <div className="space-y-3">
      {/* Target Selection */}
      {aliveTargets.length > 0 && (
        <div>
          <p className="text-xs text-text-secondary mb-1">{t('combat.target')}</p>
          <div className="flex flex-wrap gap-1">
            {aliveTargets.map((t) => (
              <button key={t.entityId}
                onClick={() => setTargetEntityId(t.entityId === targetEntityId ? null : t.entityId)}
                className={`rounded px-2 py-1 text-xs ${t.entityId === targetEntityId ? 'bg-danger text-white' : 'bg-bg-elevated text-text-secondary hover:text-text-primary'}`}
              >
                {t.name ?? t.entityId.slice(0, 8)}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Action Buttons */}
      <div className="flex flex-wrap items-center gap-2">
        {actionTypes.map((type) => {
          const available = isAvailable(type);
          const remaining = (actionsPerTurn[type] ?? 1) - (usedActions[type] ?? 0);
          return (
            <button key={type}
              onClick={() => handleAction(type)}
              disabled={!available || !targetEntityId}
              className={`flex items-center gap-1 rounded px-3 py-1.5 text-xs
                ${type === 'action' ? 'bg-accent text-white hover:bg-accent/80' : 'bg-bg-elevated text-text-secondary hover:text-text-primary'}
                disabled:opacity-40`}
              title={t('combat.remaining', { remaining, total: actionsPerTurn[type] ?? 1 })}
            >
              {ACTION_ICONS[type] ?? <Zap size={14} />}
              {t(`combat.action_${type}`, { defaultValue: type })}
              {!available && t('combat.used')}
            </button>
          );
        })}

        {/* Maneuvers (P29-T03) */}
        {maneuvers.map((m) => (
          <button key={m.name}
            onClick={() => handleManeuver(m.name)}
            disabled={!currentActor || !targetEntityId || (currentActor.apCurrent ?? 0) < (m.apCost ?? 1)}
            className="flex items-center gap-1 rounded bg-warning/10 px-3 py-1.5 text-xs text-warning hover:bg-warning/25 disabled:opacity-40"
            title={t('combat.apCost', { cost: m.apCost ?? 1 })}
          >
            <Swords size={14} /> {m.name}
          </button>
        ))}

        {/* Abilities */}
        {abilities.map((a) => (
          <button key={a.abilityId}
            onClick={() => handleAction('ability', a.abilityId)}
            disabled={!targetEntityId}
            className="flex items-center gap-1 rounded bg-warning/20 px-3 py-1.5 text-xs text-warning hover:bg-warning/30 disabled:opacity-40"
            title={t('combat.apCost', { cost: a.apCost })}
          >
            <Zap size={14} /> {a.abilityName}
          </button>
        ))}

        {/* Turn Controls */}
          <button onClick={async () => { try { await apiClient.post(`/combat/${session.id}/next-turn`); } catch { toast.error('Next turn failed'); } }}
            className="flex items-center gap-1 rounded bg-bg-elevated px-3 py-1.5 text-xs text-text-secondary hover:text-text-primary ml-auto">
          <SkipForward size={14} /> {t('combat.nextTurn')}
        </button>
        <button onClick={async () => { try { await apiClient.post(`/combat/${session.id}/end`); } catch { toast.error('Failed to end combat'); } }}
          className="flex items-center gap-1 rounded bg-danger/20 px-3 py-1.5 text-xs text-danger hover:bg-danger/30">
          <LogOut size={14} /> {t('combat.end')}
        </button>
      </div>
    </div>
  );
}
