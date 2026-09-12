/* eslint-disable @typescript-eslint/no-explicit-any */
import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Play, CheckCircle, XCircle, Zap } from 'lucide-react';
import { apiClient } from '../api/client';
import { useToast } from '../hooks/useToast';
import { useTranslation } from 'react-i18next';

export default function AdventurePlayPage() {
  const { worldId, adventureId } = useParams<{ worldId: string; adventureId: string }>();
  const navigate = useNavigate();
  const toast = useToast();
  const { t } = useTranslation('common');
  const [loading, setLoading] = useState(true);
  const [title, setTitle] = useState('');
  const [nodeText, setNodeText] = useState('');
  const [imageUrl, setImageUrl] = useState('');
  const [isEnd, setIsEnd] = useState(false);
  const [choices, setChoices] = useState<any[]>([]);
  const [advancing, setAdvancing] = useState(false);
  const [result, setResult] = useState<string | null>(null);
  const [resultOk, setResultOk] = useState(true);
  const [entityId, setEntityId] = useState<string | null>(null);

  // Start or resume the adventure
  useEffect(() => {
    if (!adventureId || !worldId) return;
    let cancelled = false;

    (async () => {
      try {
        // Find a character for this player in this world
        const entitiesRes = await apiClient.get(`/worlds/${worldId}/entities`);
        const entities = entitiesRes.data as any[];
        const pc = entities.find((e: any) => e.entityType === 'PC');
        if (!pc) {
          if (!cancelled) {
            toast.error(t('adventure.noCharacter'));
            setLoading(false);
          }
          return;
        }

        const res = await apiClient.post(`/adventures/${adventureId}/start`, { entityId: pc.id });
        const data = res.data as any;
        if (cancelled) return;

        setEntityId(pc.id);
        setNodeText(data.node?.text || '(empty)');
        setImageUrl(data.node?.imageUrl || '');
        setIsEnd(data.node?.isEnd || false);

        const advRes = await apiClient.get(`/adventures/${adventureId}`);
        setTitle((advRes.data as any).name || 'Adventure');

        // Load choices for current node
        if (data.node?.id) {
          loadChoices(data.node.id, setChoices);
        }

        setLoading(false);
      } catch (err: any) {
        if (!cancelled) {
          toast.error(err?.response?.data?.error?.message || t('adventure.startFailed'));
          setLoading(false);
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [adventureId, worldId]); // eslint-disable-line react-hooks/exhaustive-deps

  const loadChoices = async (nodeId: string, setter: any) => {
    try {
      const res = await apiClient.get(`/adventures/${adventureId}/nodes/${nodeId}/choices`);
      setter(res.data);
    } catch {
      setter([]);
    }
  };

  const handleChoice = async (choiceId: string) => {
    if (!adventureId) return;
    if (!entityId) {
      toast.error(t('adventure.noCharacterStarted'));
      return;
    }
    setAdvancing(true);
    setResult(null);
    try {
      const res = await apiClient.post(`/adventures/${adventureId}/advance`, {
        entityId,
        choiceId,
      });
      const data = res.data as any;

      if (data.skillCheckSuccess !== undefined) {
        setResultOk(!!data.skillCheckSuccess);
        setResult(data.skillCheckSuccess ? t('adventure.success') : t('adventure.failed'));
        setTimeout(() => setResult(null), 2000);
      }

      setNodeText(data.nodeText || '(empty)');
      setIsEnd(data.isEnd || false);
      setImageUrl('');
      setAdvancing(false);

      if (data.nextNodeId) {
        loadChoices(data.nextNodeId, setChoices);
      } else {
        setChoices([]);
      }
    } catch {
      toast.error('Failed to advance');
      setAdvancing(false);
    }
  };

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-bg-primary">
        <div className="h-8 w-8 animate-spin rounded-full border-2 border-accent border-t-transparent" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-bg-primary">
      <header className="flex items-center gap-3 border-b border-bg-elevated bg-bg-surface px-6 py-3">
        <button
          onClick={() => navigate(`/worlds/${worldId}`)}
          className="text-text-secondary hover:text-accent"
        >
          <ArrowLeft size={20} />
        </button>
        <h1 className="text-lg font-heading text-text-primary">{title}</h1>
        <div className="flex-1" />
        {isEnd && (
          <span className="rounded bg-success/10 px-2 py-0.5 text-xs text-success">
            {t('adventure.completed')}
          </span>
        )}
      </header>

      <main className="mx-auto max-w-2xl px-6 py-8">
        {/* Image */}
        {imageUrl && (
          <img
            src={imageUrl}
            alt=""
            className="mb-6 w-full rounded-lg border border-bg-elevated max-h-64 object-cover"
          />
        )}

        {/* Node Text */}
        <div className="mb-8 rounded-lg border border-bg-elevated bg-bg-surface p-6">
          <p className="text-text-primary leading-relaxed whitespace-pre-wrap">{nodeText}</p>
        </div>

        {/* Skill Check Result */}
        {result && (
          <div
            className={`mb-4 flex items-center gap-2 rounded-lg px-4 py-3 text-sm ${
              resultOk ? 'bg-success/10 text-success' : 'bg-danger/10 text-danger'
            }`}
          >
            {resultOk ? <CheckCircle size={18} /> : <XCircle size={18} />}
            {result}
          </div>
        )}

        {/* Choices */}
        {choices.length > 0 && (
          <div className="space-y-3">
            <h2 className="text-sm font-heading text-text-secondary">{t('adventure.whatNext')}</h2>
            {choices.map((c: any) => (
              <button
                key={c.id}
                onClick={() => handleChoice(c.id)}
                disabled={advancing}
                className="w-full rounded-lg border border-bg-elevated bg-bg-surface px-4 py-3 text-left text-sm text-text-primary hover:border-accent hover:bg-accent/5 transition disabled:opacity-40"
              >
                <div className="flex items-center justify-between">
                  <span>{c.label}</span>
                  {c.skillCheck && (
                    <span className="flex items-center gap-1 text-xs text-warning">
                      <Zap size={12} /> {c.skillCheck}
                    </span>
                  )}
                </div>
              </button>
            ))}
          </div>
        )}

        {isEnd && (
          <div className="text-center pt-8">
            <button
              onClick={() => navigate(`/worlds/${worldId}`)}
              className="rounded bg-accent px-6 py-2 text-sm text-white hover:bg-accent/80"
            >
              <Play size={14} className="inline mr-1" /> {t('adventure.backToWorld')}
            </button>
          </div>
        )}
      </main>
    </div>
  );
}
