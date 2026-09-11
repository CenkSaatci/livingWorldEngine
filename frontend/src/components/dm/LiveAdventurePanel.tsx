/* eslint-disable @typescript-eslint/no-explicit-any */
import { useState, useEffect } from 'react';
import { apiClient } from '../../api/client';
import { useToast } from '../../hooks/useToast';

interface Props {
  worldId: string;
}

export function LiveAdventurePanel({ worldId }: Props) {
  const toast = useToast();
  const [adventures, setAdventures] = useState<any[]>([]);
  const [overrideText, setOverrideText] = useState('');
  const [selectedAdv, setSelectedAdv] = useState<string | null>(null);
  const [nodes, setNodes] = useState<any[]>([]);
  const [choiceLabel, setChoiceLabel] = useState('');
  const [choiceSource, setChoiceSource] = useState('');
  const [choiceTarget, setChoiceTarget] = useState('');

  useEffect(() => {
    if (!worldId) return;
    apiClient
      .get(`/adventures?worldId=${worldId}`)
      .then((r) => {
        setAdventures(r.data as any[]);
      })
      // Best-effort prefetch — leere Liste ist der gültige Fallback.
      .catch(() => {});
  }, [worldId]);

  const loadNodes = async (advId: string) => {
    try {
      const res = await apiClient.get(`/adventures/${advId}/nodes`);
      setNodes(res.data as any[]);
      setSelectedAdv(advId);
    } catch {
      toast.error('Failed to load adventure nodes');
    }
  };

  const handleOverrideText = async () => {
    if (!selectedAdv || !overrideText.trim()) return;
    try {
      await apiClient.post(`/adventures/${selectedAdv}/override-text`, { text: overrideText });
      toast.success('Text updated — players see changes immediately');
    } catch {
      toast.error('Failed to override');
    }
  };

  const handleInjectChoice = async () => {
    if (!selectedAdv || !choiceSource || !choiceTarget || !choiceLabel.trim()) return;
    try {
      await apiClient.post(`/adventures/${selectedAdv}/inject-choice/${choiceSource}`, {
        label: choiceLabel.trim(),
        targetNodeId: choiceTarget,
      });
      toast.success('Choice injected — players see it immediately');
      setChoiceLabel('');
    } catch {
      toast.error('Failed to inject choice');
    }
  };

  const handleForceNode = async (nodeId: string) => {
    if (!selectedAdv) return;
    try {
      await apiClient.post(`/adventures/${selectedAdv}/force-node/${nodeId}`);
      toast.success('Players forced to node');
    } catch {
      toast.error('Failed to force node');
    }
  };

  return (
    <div className="space-y-2">
      <p className="text-xs font-medium text-text-secondary uppercase tracking-wide">
        Live Adventures
      </p>
      {adventures.length === 0 && (
        <p className="text-xs text-text-secondary">No adventures in this world</p>
      )}
      {adventures.map((adv: any) => (
        <div key={adv.id} className="rounded border border-bg-elevated bg-bg-surface p-2">
          <button
            onClick={() => loadNodes(adv.id)}
            className="w-full text-left text-xs text-text-primary hover:text-accent"
          >
            🗺️ {adv.name || adv.id.slice(0, 12)}
          </button>
          {selectedAdv === adv.id && (
            <div className="mt-2 space-y-2 border-t border-bg-elevated pt-2">
              <textarea
                value={overrideText}
                onChange={(e) => setOverrideText(e.target.value)}
                rows={2}
                placeholder="Override text..."
                className="w-full rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent resize-none"
              />
              <button
                onClick={handleOverrideText}
                className="w-full rounded bg-accent px-2 py-1 text-xs text-white hover:bg-accent/80"
              >
                Override Text
              </button>
              {nodes.length > 0 && (
                <div className="space-y-1">
                  <p className="text-[10px] text-text-secondary mb-1">Inject Choice:</p>
                  <input
                    value={choiceLabel}
                    onChange={(e) => setChoiceLabel(e.target.value)}
                    placeholder="Choice label..."
                    className="w-full rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
                  />
                  <div className="flex gap-1">
                    <select
                      value={choiceSource}
                      onChange={(e) => setChoiceSource(e.target.value)}
                      aria-label="Source node"
                      className="w-1/2 rounded border border-bg-elevated bg-bg-primary px-1 py-1 text-[10px] text-text-primary"
                    >
                      <option value="">Source…</option>
                      {nodes.map((n: any) => (
                        <option key={n.id} value={n.id}>{(n.text || '').slice(0, 20)}</option>
                      ))}
                    </select>
                    <select
                      value={choiceTarget}
                      onChange={(e) => setChoiceTarget(e.target.value)}
                      aria-label="Target node"
                      className="w-1/2 rounded border border-bg-elevated bg-bg-primary px-1 py-1 text-[10px] text-text-primary"
                    >
                      <option value="">Target…</option>
                      {nodes.map((n: any) => (
                        <option key={n.id} value={n.id}>{(n.text || '').slice(0, 20)}</option>
                      ))}
                    </select>
                  </div>
                  <button
                    onClick={handleInjectChoice}
                    disabled={!choiceLabel.trim() || !choiceSource || !choiceTarget}
                    className="w-full rounded bg-accent px-2 py-1 text-xs text-white hover:bg-accent/80 disabled:opacity-40"
                  >
                    Inject Choice
                  </button>
                </div>
              )}
              {nodes.length > 0 && (
                <div>
                  <p className="text-[10px] text-text-secondary mb-1">Force Node:</p>
                  {nodes.map((n: any) => (
                    <button
                      key={n.id}
                      onClick={() => handleForceNode(n.id)}
                      className="block w-full rounded px-2 py-1 text-left text-[10px] text-text-secondary hover:text-accent hover:bg-bg-elevated"
                    >
                      ➡️ {(n.text || '').slice(0, 40)}
                    </button>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      ))}
    </div>
  );
}
