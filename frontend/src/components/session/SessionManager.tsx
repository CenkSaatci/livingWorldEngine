import { useEffect } from 'react';
import { Play, Square, Clock } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { apiClient } from '../../api/client';
import { useSessionStore } from '../../store/sessionStore';
import { useToast } from '../../hooks/useToast';

interface Props {
  worldId: string;
}

export function SessionManager({ worldId }: Props) {
  const { t } = useTranslation('common');
  const sessions = useSessionStore((s) => s.sessions);
  const activeSession = useSessionStore((s) => s.activeSession);
  const setSessions = useSessionStore((s) => s.setSessions);
  const addSession = useSessionStore((s) => s.addSession);
  const endSession = useSessionStore((s) => s.endSession);
  const toast = useToast();

  useEffect(() => {
    if (!worldId) return;
    apiClient
      .get(`/worlds/${worldId}/sessions`)
      .then((r) => setSessions(r.data))
      .catch(() => {});
  }, [worldId]);

  const handleStart = async () => {
    try {
      const res = await apiClient.post('/sessions/start', { worldId });
      addSession(res.data);
      toast.success(t('session.started'));
    } catch {
      toast.error(t('session.start_error'));
    }
  };

  const handleEnd = async () => {
    if (!activeSession) return;
    try {
      await apiClient.post(`/sessions/${activeSession.id}/end`);
      endSession(activeSession.id);
      toast.success(t('session.ended'));
    } catch {
      toast.error(t('session.end_error'));
    }
  };

  return (
    <div className="rounded-lg bg-bg-surface p-3">
      <div className="flex items-center justify-between mb-2">
        <h3 className="flex items-center gap-1.5 text-xs font-semibold text-text-secondary uppercase tracking-wide">
          <Clock size={14} /> {t('session.title')}
        </h3>

        {activeSession ? (
          <button
            onClick={handleEnd}
            className="flex items-center gap-1 rounded bg-danger/20 px-2 py-1 text-xs text-danger hover:bg-danger/30"
          >
            <Square size={12} /> {t('session.end')}
          </button>
        ) : (
          <button
            onClick={handleStart}
            className="flex items-center gap-1 rounded bg-success/20 px-2 py-1 text-xs text-success hover:bg-success/30"
          >
            <Play size={12} /> {t('session.start')}
          </button>
        )}
      </div>

      {activeSession && (
        <p className="text-xs text-success mb-2">
          {t('session.active_since')} {new Date(activeSession.startedAt).toLocaleTimeString()}
        </p>
      )}

      {/* Recent sessions */}
      {sessions.length > 0 && (
        <div className="space-y-1 mt-2 border-t border-bg-elevated pt-2">
          <p className="text-[10px] text-text-secondary">{t('session.recent')}</p>
          {sessions.slice(0, 5).map((s) => (
            <div
              key={s.id}
              className="flex items-center justify-between text-[10px] text-text-secondary"
            >
              <span>{new Date(s.startedAt).toLocaleDateString()}</span>
              <span className={s.status === 'ACTIVE' ? 'text-success' : ''}>
                {s.status === 'ACTIVE' ? t('session.active') : t('session.ended')}
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
