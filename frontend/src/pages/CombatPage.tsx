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
import { useMediaQuery } from '../hooks/useMediaQuery';

export default function CombatPage() {
  const { t } = useTranslation('map');
  const { t: tc } = useTranslation('common');
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [showChat, setShowChat] = useState(true);
  const [tab, setTab] = useState<'map' | 'init' | 'chat'>('map');
  const isMobile = useMediaQuery('(max-width: 767px)');
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
            <Swords size={20} className="text-danger" /> {tc('gameView.combat')}
          </h1>
        </div>
        {!isMobile && (
          <button
            onClick={() => setShowChat(!showChat)}
            className="flex items-center gap-1 text-sm text-text-secondary hover:text-accent"
          >
            <MessageSquare size={16} /> {tc('gameView.chat')}
          </button>
        )}
      </header>

      {/* R4: Mobile Tabs (Karte/Initiative/Chat) */}
      {isMobile && (
        <div className="flex border-b border-bg-elevated bg-bg-surface">
          {([['map', t('title')], ['init', tc('combat.initiative')], ['chat', tc('gameView.chat')]] as const).map(
            ([key, label]) => (
              <button
                key={key}
                onClick={() => setTab(key)}
                className={`flex-1 px-3 py-2 text-sm ${tab === key ? 'border-b-2 border-accent text-accent' : 'text-text-secondary'}`}
              >
                {label}
              </button>
            ),
          )}
        </div>
      )}

      {/* Main Area */}
      <div className="flex flex-1 overflow-hidden">
        {/* Left: Combat Panel */}
        {(!isMobile || tab === 'init') && (
        <aside className={`flex flex-col border-r border-bg-elevated bg-bg-surface p-3 ${isMobile ? 'w-full' : 'w-64'}`}>
          <InitiativeList />
          {!hasSession && (
            <div className="mt-4 space-y-2 rounded border border-dashed border-bg-elevated p-3 text-center text-xs text-text-secondary">
              <p>{tc('combat.noActiveCombat')}</p>
              <button
                onClick={() => navigate(`/worlds/${worldId}`)}
                className="rounded bg-accent px-3 py-1.5 text-xs text-white hover:bg-accent/80"
              >
                {tc('combat.startFromWorld')}
              </button>
            </div>
          )}
        </aside>
        )}

        {/* Center: Canvas with combat map */}
        {(!isMobile || tab === 'map') && (
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
        )}

        {/* Right Panel (Chat + Roll Log) */}
        {(!isMobile ? showChat : tab === 'chat') && (
          <aside className={`flex flex-col border-l border-bg-elevated bg-bg-surface ${isMobile ? 'w-full flex-1' : 'w-80'}`}>
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
