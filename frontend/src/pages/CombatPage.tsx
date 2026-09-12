import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, MessageSquare, Swords } from 'lucide-react';
import { useState, useEffect } from 'react';
import { MapCanvas } from '../components/map/MapCanvas';
import { ChatPanel } from '../components/chat/ChatPanel';
import { RollLog } from '../components/chat/RollLog';
import { InitiativeList } from '../components/combat/InitiativeList';
import { ActionBar } from '../components/combat/ActionBar';
import { ApBar } from '../components/combat/ApBar';
import { useWorldSocket } from '../hooks/useWorldSocket';
import { useCombatStore } from '../store/combatStore';

export default function CombatPage() {
  const { t } = useTranslation('map');
  const { t: tc } = useTranslation('common');
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [showChat, setShowChat] = useState(true);
  const worldId = id ?? '';
  const mapId = useCombatStore((s) => s.session?.mapId);
  const hasSession = useCombatStore((s) => s.session !== null);

  useWorldSocket(worldId);

  // Playtest-Befund #10: Reload/Deep-Link lädt die aktive Session nach.
  useEffect(() => {
    useCombatStore.getState().loadActiveSession(worldId);
  }, [worldId]);

  return (
    <div className="flex h-screen flex-col bg-bg-primary">
      {/* Top Bar */}
      <header className="flex items-center justify-between border-b border-bg-elevated bg-bg-surface px-4 py-2">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate('/dashboard')}
            className="text-text-secondary hover:text-accent"
          >
            <ArrowLeft size={20} />
          </button>
          <h1 className="flex items-center gap-2 text-lg font-heading text-text-primary">
            <Swords size={20} className="text-danger" /> {t('title')}
          </h1>
        </div>
        <button
          onClick={() => setShowChat(!showChat)}
          className="flex items-center gap-1 text-sm text-text-secondary hover:text-accent"
        >
          <MessageSquare size={16} /> {tc('gameView.chat')}
        </button>
      </header>

      {/* Main Area */}
      <div className="flex flex-1 overflow-hidden">
        {/* Left: Combat Panel */}
        <aside className="flex w-64 flex-col border-r border-bg-elevated bg-bg-surface p-3">
          <InitiativeList />
          {!hasSession && (
            <p className="mt-4 rounded border border-dashed border-bg-elevated p-3 text-center text-xs text-text-secondary">
              {tc('combat.noActiveCombat')}
            </p>
          )}
        </aside>

        {/* Center: Canvas with combat map */}
        <div className="flex flex-1 flex-col">
          <div className="flex-1 p-3">
            <div className="h-full rounded-lg border border-bg-elevated bg-bg-surface/50">
              <MapCanvas cols={20} rows={15} tileSize={48} worldId={worldId} mapId={mapId} />
            </div>
          </div>

          {/* Bottom: AP Bar + Actions */}
          <div className="border-t border-bg-elevated bg-bg-surface px-4 py-3">
            <div className="mx-auto flex max-w-2xl items-center gap-6">
              <div className="w-48">
                <ApBar />
              </div>
              <div className="flex-1">
                <ActionBar worldId={worldId} />
              </div>
            </div>
          </div>
        </div>

        {/* Right Panel (Chat + Roll Log) */}
        {showChat && (
          <aside className="flex w-80 flex-col border-l border-bg-elevated bg-bg-surface">
            <div className="flex-1">
              <ChatPanel worldId={worldId} />
            </div>
            <div className="h-48 border-t border-bg-elevated">
              <RollLog />
            </div>
          </aside>
        )}
      </div>
    </div>
  );
}
