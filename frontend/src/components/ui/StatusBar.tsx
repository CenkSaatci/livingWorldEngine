import { useState } from 'react';
import { Play, Eye, SkipForward } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useWorldStore } from '../../store/worldStore';
import { useSessionStore } from '../../store/sessionStore';
import { useCombatStore } from '../../store/combatStore';
import { useAuthStore } from '../../store/authStore';
import { useFogStore } from '../../store/fogStore';
import { apiClient } from '../../api/client';
import { useToast } from '../../hooks/useToast';

const ADVANCE_OPTIONS = [
  { label: '+1h', value: '1h' },
  { label: '+6h', value: '6h' },
  { label: '+1d', value: '1d' },
  { label: 'bis Dawn', value: 'dawn' },
  { label: 'bis Dusk', value: 'dusk' },
];

function formatGameTime(iso: string | null) {
  if (!iso) return '—';
  const d = new Date(iso);
  return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

function dayPhase(iso: string | null): string {
  if (!iso) return '—';
  const hour = new Date(iso).getUTCHours();
  if (hour >= 5 && hour < 8) return '🌅 Dawn';
  if (hour >= 8 && hour < 18) return '☀️ Day';
  if (hour >= 18 && hour < 21) return '🌆 Dusk';
  return '🌙 Night';
}

export function StatusBar() {
  const { t } = useTranslation('common');
  const world = useWorldStore((s) => s.currentWorld);
  const activeSession = useSessionStore((s) => s.activeSession);
  const combatSession = useCombatStore((s) => s.session);
  const combatParticipants = useCombatStore((s) => s.participants);
  const user = useAuthStore((s) => s.user);
  const toast = useToast();
  const [showAdvance, setShowAdvance] = useState(false);

  const isDm = user?.role === 'ADMIN';

  const time = world?.currentGameTime ?? null;
  const phase = dayPhase(time);
  const formattedTime = formatGameTime(time);

  const worldId = world?.id ?? '';

  const handlePauseResume = async () => {
    if (!worldId) return;
    try {
      // Toggle: if currently paused → resume, else pause
      await apiClient.post(`/worlds/${worldId}/time/pause`);
      toast.info('Time paused');
    } catch {
      toast.error('Failed to pause');
    }
  };

  const handleAdvance = async (by: string) => {
    if (!worldId) return;
    try {
      await apiClient.post(`/worlds/${worldId}/time/advance`, { by });
      toast.success(`Time advanced ${by}`);
      setShowAdvance(false);
    } catch {
      toast.error('Failed to advance time');
    }
  };

  return (
    <div className="flex h-6 items-center justify-between border-t border-bg-elevated bg-bg-surface px-4 text-[10px] text-text-secondary">
      <div className="flex items-center gap-4">
        {/* Weltzeit */}
        <span className="flex items-center gap-1">
          {phase} {formattedTime}
        </span>

        {/* Session */}
        <span className="flex items-center gap-1" aria-live="polite">
          {activeSession ? (
            <span className="text-success">● {t('session_active')}</span>
          ) : (
            <span>○ {t('no_session')}</span>
          )}
        </span>

        {/* Combat Turn */}
        {combatSession?.status === 'ACTIVE' && (
          <span className="flex items-center gap-1" aria-live="polite">
            ⚔️ Turn{' '}
            {combatParticipants.findIndex(
              (p) => p.entityId === combatSession.currentTurnEntityId,
            ) + 1}
            /{combatParticipants.length} (Rd. {combatSession.round})
          </span>
        )}
      </div>

      {/* DM Controls */}
      {isDm && worldId && (
        <div className="flex items-center gap-1">
          <button
            onClick={() => useFogStore.getState().toggle()}
            className="rounded p-0.5 text-text-secondary hover:text-accent hover:bg-bg-elevated"
            aria-label="Toggle fog of war"
          >
            <Eye size={12} />
          </button>

          <button
            onClick={handlePauseResume}
            className="rounded p-0.5 text-text-secondary hover:text-accent hover:bg-bg-elevated"
            aria-label="Pause or resume time"
          >
            <Play size={12} />
          </button>

          <div className="relative">
            <button
              onClick={() => setShowAdvance(!showAdvance)}
              className="rounded p-0.5 text-text-secondary hover:text-accent hover:bg-bg-elevated"
              aria-label="Advance time"
            >
              <SkipForward size={12} />
            </button>

            {showAdvance && (
              <div className="absolute bottom-full right-0 mb-1 rounded border border-bg-elevated bg-bg-surface p-1 shadow-lg">
                {ADVANCE_OPTIONS.map((opt) => (
                  <button
                    key={opt.value}
                    onClick={() => handleAdvance(opt.value)}
                    className="block w-full rounded px-2 py-1 text-left text-[10px] text-text-primary hover:bg-bg-elevated whitespace-nowrap"
                  >
                    {opt.label}
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
