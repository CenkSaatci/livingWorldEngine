/* eslint-disable @typescript-eslint/no-explicit-any */
import { useState, useEffect, useCallback, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Save, Plus } from 'lucide-react';
import {
  ReactFlow,
  Background,
  Controls,
  MiniMap,
  useNodesState,
  useEdgesState,
  addEdge,
  type Connection,
  type Node,
  type Edge,
  Handle,
  Position,
  MarkerType,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { apiClient } from '../api/client';
import { useToast } from '../hooks/useToast';

interface AdventureNodeData {
  label: string;
  text: string;
  imageUrl: string;
  isEnd: boolean;
  isStart: boolean;
  [key: string]: unknown;
}

function AdventureNode({ data, selected }: { data: AdventureNodeData; selected: boolean }) {
  return (
    <div
      className={`rounded-lg border-2 px-4 py-3 shadow-lg min-w-[180px] ${
        data.isEnd
          ? 'border-danger bg-danger/10'
          : data.isStart
            ? 'border-success bg-success/10'
            : selected
              ? 'border-accent bg-accent/10'
              : 'border-bg-elevated bg-bg-surface'
      }`}
    >
      <Handle type="target" position={Position.Top} className="!bg-accent" />
      <div className="text-xs font-heading text-text-primary mb-1">{data.label || 'Untitled'}</div>
      {data.text && <div className="text-[10px] text-text-secondary line-clamp-2">{data.text}</div>}
      {data.isEnd && <div className="text-[10px] text-danger mt-1">🏁 End</div>}
      <Handle type="source" position={Position.Bottom} className="!bg-accent" />
    </div>
  );
}

const nodeTypes = { adventure: AdventureNode };

export default function AdventureEditorPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const toast = useToast();
  const worldId = id ?? '';

  const [nodes, setNodes, onNodesChange] = useNodesState([] as any);
  const [edges, setEdges, onEdgesChange] = useEdgesState([] as any);
  const [selectedNode, setSelectedNode] = useState<any>(null);
  const [adventureName, setAdventureName] = useState('');
  const [adventureId, setAdventureId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [adventures, setAdventures] = useState<{ id: string; name: string }[]>([]);
  const reactFlowWrapper = useRef<HTMLDivElement>(null);

  // Load adventures for this world
  useEffect(() => {
    if (!worldId) return;
    apiClient
      .get(`/adventures?worldId=${worldId}`)
      .then((r) => {
        setAdventures(r.data as { id: string; name: string }[]);
        setLoading(false);
      })
      .catch(() => setLoading(false));
  }, [worldId]);

  // Load specific adventure nodes/edges
  const loadAdventure = useCallback(
    async (advId: string) => {
      try {
        const advRes = await apiClient.get(`/adventures/${advId}`);
        setAdventureName((advRes.data as { name: string }).name);
        setAdventureId(advId);

        const nodesRes = await apiClient.get(`/adventures/${advId}/nodes`);
        const rawNodes = nodesRes.data as Array<{
          id: string;
          text: string;
          imageUrl: string;
          isEnd: boolean;
        }>;

        const flowNodes: Node<AdventureNodeData>[] = rawNodes.map((n, i) => ({
          id: n.id,
          type: 'adventure',
          position: { x: 250 * (i % 3), y: 200 * Math.floor(i / 3) },
          data: {
            label: n.text?.slice(0, 30) || `Node ${i + 1}`,
            text: n.text || '',
            imageUrl: n.imageUrl || '',
            isEnd: n.isEnd,
            isStart: i === 0,
          },
        }));
        setNodes(flowNodes);

        // Load choices as edges
        const allEdges: Edge[] = [];
        for (const n of rawNodes) {
          try {
            const choicesRes = await apiClient.get(`/adventures/${advId}/nodes/${n.id}/choices`);
            const choices = choicesRes.data as Array<{
              id: string;
              label: string;
              targetNodeId: string;
            }>;
            for (const c of choices) {
              allEdges.push({
                id: c.id,
                source: n.id,
                target: c.targetNodeId,
                label: c.label,
                markerEnd: { type: MarkerType.ArrowClosed },
                style: { stroke: '#5BB8C5' },
              });
            }
          } catch {
            /* no choices for this node */
          }
        }
        setEdges(allEdges);
      } catch {
        toast.error('Failed to load adventure');
      }
    },
    [setNodes, setEdges, toast],
  );

  const onConnect = useCallback(
    (params: Connection) => {
      if (!adventureId || !params.source || !params.target) return;
      const label = prompt('Choice label:') || 'Continue';
      apiClient
        .post(`/adventures/${adventureId}/nodes/${params.source}/choices`, {
          label,
          targetNodeId: params.target,
        })
        .then((r) => {
          setEdges((eds: any[]) =>
            addEdge(
              {
                ...params,
                id: r.data.id || `e-${Date.now()}`,
                label,
                markerEnd: { type: MarkerType.ArrowClosed },
                style: { stroke: '#5BB8C5' },
              },
              eds,
            ),
          );
        })
        .catch(() => toast.error('Failed to create choice'));
    },
    [adventureId, setEdges, toast],
  );

  const handleAddNode = async () => {
    if (!adventureId) return;
    try {
      const res = await apiClient.post(`/adventures/${adventureId}/nodes`, {
        text: 'New node',
        imageUrl: null,
        isEnd: false,
      });
      const newNode = res.data as { id: string };
      setNodes((nds: any[]) => [
        ...nds,
        {
          id: newNode.id,
          type: 'adventure',
          position: { x: Math.random() * 400, y: Math.random() * 300 },
          data: { label: 'New node', text: '', imageUrl: '', isEnd: false, isStart: false },
        },
      ]);
    } catch {
      toast.error('Failed to add node');
    }
  };

  const handleCreateAdventure = async () => {
    if (!worldId || !adventureName.trim()) return;
    try {
      const res = await apiClient.post('/adventures', {
        worldId,
        name: adventureName.trim(),
      });
      const data = res.data as { id: string };
      setAdventureId(data.id);
      setAdventures((prev: any) => [...prev, { id: data.id, name: adventureName.trim() }]);
      toast.success('Adventure created');
    } catch {
      toast.error('Failed to create adventure');
    }
  };

  const handleSave = async () => {
    if (!adventureId) return;
    setSaving(true);
    // Update node texts
    for (const n of nodes) {
      try {
        await apiClient.patch(`/adventures/${adventureId}/nodes/${n.id}`, {
          text: n.data.text,
          imageUrl: n.data.imageUrl || null,
          isEnd: n.data.isEnd,
        });
      } catch {
        /* */
      }
    }
    // Mark first node as start
    if (nodes.length > 0) {
      await apiClient.post(`/adventures/${adventureId}/start-node/${nodes[0].id}`).catch(() => {});
    }
    setSaving(false);
    toast.success('Adventure saved');
  };

  const onNodeClick = (_: React.MouseEvent, node: any) => {
    setSelectedNode(node);
  };

  const updateSelectedNode = (field: string, value: string | boolean) => {
    if (!selectedNode) return;
    setNodes((nds: any[]) =>
      nds.map((n) =>
        n.id === selectedNode.id ? { ...n, data: { ...n.data, [field]: value } } : n,
      ),
    );
    setSelectedNode((prev: any) =>
      prev ? { ...prev, data: { ...prev.data, [field]: value } } : null,
    );
  };

  if (loading) {
    return (
      <div className="flex h-screen items-center justify-center bg-bg-primary">
        <div className="h-8 w-8 animate-spin rounded-full border-2 border-accent border-t-transparent" />
      </div>
    );
  }

  return (
    <div className="flex h-screen flex-col bg-bg-primary">
      {/* Header */}
      <header className="flex items-center gap-3 border-b border-bg-elevated bg-bg-surface px-4 py-2">
        <button
          onClick={() => navigate(`/worlds/${worldId}`)}
          className="text-text-secondary hover:text-accent"
        >
          <ArrowLeft size={20} />
        </button>
        <h1 className="text-lg font-heading text-text-primary">Adventure Editor</h1>
        <div className="flex-1" />

        {!adventureId ? (
          <div className="flex items-center gap-2">
            <input
              value={adventureName}
              onChange={(e) => setAdventureName(e.target.value)}
              placeholder="New adventure name"
              className="rounded border border-bg-elevated bg-bg-primary px-3 py-1.5 text-xs text-text-primary outline-none focus:border-accent"
            />
            <button
              onClick={handleCreateAdventure}
              className="flex items-center gap-1 rounded bg-accent px-3 py-1.5 text-xs text-white hover:bg-accent/80"
            >
              <Plus size={14} /> Create
            </button>
          </div>
        ) : (
          <div className="flex items-center gap-2">
            <button
              onClick={handleAddNode}
              className="flex items-center gap-1 rounded border border-bg-elevated px-3 py-1.5 text-xs text-text-secondary hover:text-accent"
            >
              <Plus size={14} /> Add Node
            </button>
            <button
              onClick={handleSave}
              disabled={saving}
              className="flex items-center gap-1 rounded bg-accent px-3 py-1.5 text-xs text-white hover:bg-accent/80 disabled:opacity-40"
            >
              <Save size={14} /> {saving ? 'Saving…' : 'Save'}
            </button>
          </div>
        )}
      </header>

      <div className="flex flex-1 overflow-hidden">
        {/* Adventure List / Canvas */}
        <div ref={reactFlowWrapper} className="flex-1">
          {adventureId ? (
            <ReactFlow
              nodes={nodes}
              edges={edges}
              onNodesChange={onNodesChange}
              onEdgesChange={onEdgesChange}
              onConnect={onConnect}
              onNodeClick={onNodeClick}
              nodeTypes={nodeTypes}
              fitView
            >
              <Background color="#2a3440" gap={20} />
              <Controls />
              <MiniMap
                nodeColor="#3b82f6"
                maskColor="rgba(15,20,25,0.8)"
                style={{ background: '#0f1419' }}
              />
            </ReactFlow>
          ) : (
            <div className="flex h-full items-center justify-center">
              <div className="text-center">
                <p className="text-text-secondary mb-4">Select or create an adventure</p>
                <div className="space-y-2">
                  {adventures.map((a) => (
                    <button
                      key={a.id}
                      onClick={() => loadAdventure(a.id)}
                      className="block w-full rounded border border-bg-elevated bg-bg-surface px-4 py-2 text-left text-sm text-text-primary hover:border-accent"
                    >
                      {a.name}
                    </button>
                  ))}
                  {adventures.length === 0 && (
                    <p className="text-xs text-text-secondary">
                      No adventures yet. Create one above.
                    </p>
                  )}
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Sidebar */}
        {selectedNode && (
          <aside className="w-72 border-l border-bg-elevated bg-bg-surface p-4 overflow-y-auto">
            <h3 className="mb-3 font-heading text-sm text-text-primary">Node Editor</h3>
            <div className="space-y-3">
              <div>
                <label className="block text-[10px] text-text-secondary mb-1">Text</label>
                <textarea
                  value={selectedNode.data.text}
                  onChange={(e) => updateSelectedNode('text', e.target.value)}
                  rows={4}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-2 py-1.5 text-xs text-text-primary outline-none focus:border-accent resize-none"
                />
              </div>
              <div>
                <label className="block text-[10px] text-text-secondary mb-1">Image URL</label>
                <input
                  value={selectedNode.data.imageUrl}
                  onChange={(e) => updateSelectedNode('imageUrl', e.target.value)}
                  className="w-full rounded border border-bg-elevated bg-bg-primary px-2 py-1.5 text-xs text-text-primary outline-none focus:border-accent"
                />
              </div>
              <label className="flex items-center gap-2">
                <input
                  type="checkbox"
                  checked={selectedNode.data.isEnd}
                  onChange={(e) => updateSelectedNode('isEnd', e.target.checked)}
                  className="accent-accent"
                />
                <span className="text-xs text-text-primary">End Node</span>
              </label>
            </div>
          </aside>
        )}
      </div>
    </div>
  );
}
