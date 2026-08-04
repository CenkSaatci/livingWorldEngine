import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  PanelLeftClose,
  PanelLeft,
  MessageSquare,
  Globe,
  LogOut,
  Swords,
  Plus,
} from 'lucide-react';
import { RegionTree } from '../components/world/RegionTree';
import { LocationDetail } from '../components/world/LocationDetail';
import { WorldMapView } from '../components/world/WorldMapView';
import { QuestLog } from '../components/world/QuestLog';
import { RightPanel } from '../components/world/RightPanel';
import { DmQueuePanel } from '../components/dm/DmQueuePanel';
import { LiveAdventurePanel } from '../components/dm/LiveAdventurePanel';
import { StatusBar } from '../components/ui/StatusBar';
import { SessionManager } from '../components/session/SessionManager';
import { EntityCreateModal } from '../components/world/EntityCreateModal';
import { QuickRegionModal } from '../components/world/QuickRegionModal';
import { LocationCreateModal } from '../components/world/LocationCreateModal';
import { StartCombatModal } from '../components/combat/StartCombatModal';
import { RegionPicker } from '../components/world/RegionPicker';
import { useWorldSocket } from '../hooks/useWorldSocket';
import { useKeyboardShortcuts } from '../hooks/useKeyboardShortcuts';
import { useMediaQuery } from '../hooks/useMediaQuery';
import { useAuthStore } from '../store/authStore';
import { useActiveCampaign } from '../store/campaignStore';

export default function GameView() {
  const { i18n } = useTranslation('map');
  const { t: tc } = useTranslation('common');
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);
  const activeCampaign = useActiveCampaign();
  const worldId = id ?? '';

  useWorldSocket(worldId);

  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [chatOpen, setChatOpen] = useState(true);
  const [selectedLocation, setSelectedLocation] = useState<string | null>(null);
  const [selectedNpc, setSelectedNpc] = useState<string | null>(null);
  const [showCreateMenu, setShowCreateMenu] = useState(false);
  const [createMode, setCreateMode] = useState<'npc' | 'region' | 'location' | null>(null);
  const [showStartCombat, setShowStartCombat] = useState(false);
  const [createLocationRegion, setCreateLocationRegion] = useState<{
    id: string;
    name: string;
  } | null>(null);
  const currentLocale = i18n.language;
  const isMobile = useMediaQuery('(max-width: 767px)');
  const effectiveSidebarOpen = sidebarOpen && !isMobile;
  const effectiveChatOpen = chatOpen && !isMobile;

  useKeyboardShortcuts({
    Escape: () => {
      setSelectedLocation(null);
      setSelectedNpc(null);
    },
    b: () => {
      // Chat-Sende-Input fokussieren
      const input = document.querySelector<HTMLInputElement>('[data-chat-input]');
      input?.focus();
    },
    m: () => {
      // Canvas fokussieren
      document.querySelector<HTMLElement>('[data-canvas]')?.focus();
    },
  });

  return (
    <div className="flex h-screen flex-col bg-bg-primary">
      {/* Top Bar */}
      <header className="flex items-center justify-between border-b border-bg-elevated bg-bg-surface px-4 py-2">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate('/dashboard')}
            className="text-sm text-accent hover:text-accent/60"
          >
            {tc('nav.backToDashboard')}
          </button>
          <span className="text-text-secondary">|</span>
          <h1 className="text-lg font-heading text-text-primary">{tc('gameView.title')}</h1>
          {activeCampaign && (
            <span className="rounded-full border border-accent/40 bg-accent/10 px-3 py-0.5 text-xs text-accent">
              {activeCampaign.name}
            </span>
          )}
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={() => {
              i18n.changeLanguage(currentLocale === 'de' ? 'en' : 'de');
              useAuthStore.getState().setLocale(currentLocale === 'de' ? 'en' : 'de');
            }}
            className="flex items-center gap-1 text-sm text-text-secondary hover:text-accent"
            aria-label={tc('language.switch')}
          >
            <Globe size={16} /> {currentLocale.toUpperCase()}
          </button>
          <button
            onClick={() => setShowStartCombat(true)}
            className="flex items-center gap-1 text-sm text-danger hover:text-danger/60"
            aria-label={tc('gameView.combat')}
          >
            <Swords size={16} /> {tc('gameView.combat')}
          </button>
          <span className="text-sm text-text-secondary">{user?.username}</span>
          <button
            onClick={() => {
              logout();
              navigate('/login');
            }}
            className="flex items-center gap-1 text-sm text-danger hover:text-danger/60"
            aria-label={tc('actions.logout')}
          >
            <LogOut size={16} /> {tc('actions.logout')}
          </button>
        </div>
      </header>

      <div className="flex flex-1 overflow-hidden">
        {/* Mobile Overlay */}
        {isMobile && sidebarOpen && (
          <div className="fixed inset-0 z-30 bg-black/50" onClick={() => setSidebarOpen(false)} />
        )}

        {/* Sidebar */}
        {effectiveSidebarOpen && (
          <aside
            className={`${isMobile ? 'fixed left-0 top-14 z-40 h-[calc(100vh-3.5rem)]' : ''} flex w-64 shrink-0 flex-col overflow-y-auto border-r border-bg-elevated bg-bg-surface`}
          >
            {/* Region/Orte-Baum */}
            <div className="flex-1 overflow-y-auto p-2">
              <RegionTree
                worldId={worldId}
                onSelectLocation={setSelectedLocation}
                onSelectRegion={(regionId) => navigate(`/worlds/${worldId}/regions/${regionId}`)}
              />
            </div>

            {/* Session */}
            <div className="border-t border-bg-elevated p-2">
              <SessionManager worldId={worldId} />
            </div>

            {/* DM-Queue */}
            <div className="border-t border-bg-elevated p-2">
              <DmQueuePanel worldId={worldId} />
            </div>

            {/* Live Adventures (DM Override) */}
            <div className="border-t border-bg-elevated p-2">
              <LiveAdventurePanel worldId={worldId} />
            </div>

            {/* Entity-Übersicht Link */}
            <div className="border-t border-bg-elevated p-2">
              <button
                onClick={() => navigate(`/worlds/${worldId}/entities`)}
                className="w-full rounded px-2 py-1.5 text-left text-xs text-text-secondary hover:text-accent hover:bg-bg-elevated"
              >
                {tc('entityList.title')}
              </button>
            </div>

            {/* Quest-Log unten */}
            <div className="border-t border-bg-elevated p-2">
              <QuestLog
                worldId={worldId}
                onSelectQuest={(id) => navigate(`/worlds/${worldId}/quests/${id}`)}
              />
            </div>
          </aside>
        )}

        {/* Main */}
        <div className="flex flex-1 flex-col">
          {/* Toolbar */}
          <div className="flex items-center gap-2 border-b border-bg-elevated bg-bg-surface/50 px-3 py-1.5">
            <button
              onClick={() => setSidebarOpen(!sidebarOpen)}
              className="text-text-secondary hover:text-accent"
              aria-label={sidebarOpen ? tc('gameView.closeSidebar') : tc('gameView.openSidebar')}
            >
              {sidebarOpen ? <PanelLeftClose size={16} /> : <PanelLeft size={16} />}
            </button>

            <div className="relative">
              <button
                onClick={() => setShowCreateMenu(!showCreateMenu)}
                className="rounded p-1 text-text-secondary hover:text-accent hover:bg-bg-elevated"
                aria-label={tc('gameView.create')}
              >
                <Plus size={16} />
              </button>
              {showCreateMenu && (
                <div className="absolute left-0 top-full mt-1 w-44 rounded border border-bg-elevated bg-bg-surface py-1 shadow-lg z-20">
                  <button
                    onClick={() => {
                      setShowCreateMenu(false);
                      setCreateMode('npc');
                    }}
                    className="w-full px-3 py-1.5 text-left text-sm text-text-primary hover:bg-bg-elevated/50"
                  >
                    {tc('gameView.createNpc')}
                  </button>
                  <button
                    onClick={() => {
                      setShowCreateMenu(false);
                      setCreateMode('region');
                    }}
                    className="w-full px-3 py-1.5 text-left text-sm text-text-primary hover:bg-bg-elevated/50"
                  >
                    {tc('gameView.createRegion')}
                  </button>
                  <button
                    onClick={() => {
                      setShowCreateMenu(false);
                      setCreateMode('location');
                    }}
                    className="w-full px-3 py-1.5 text-left text-sm text-text-primary hover:bg-bg-elevated/50"
                  >
                    {tc('gameView.createLocation')}
                  </button>
                </div>
              )}
            </div>

            {selectedLocation ? (
              <div className="flex items-center gap-2 text-xs">
                <span className="text-accent">{tc('gameView.location')}</span>
                <button
                  onClick={() => {
                    setSelectedLocation(null);
                    setSelectedNpc(null);
                  }}
                  className="text-text-secondary hover:text-accent"
                  aria-label={tc('gameView.closeLocation')}
                >
                  ✕
                </button>
              </div>
            ) : (
              <span className="text-xs text-text-secondary">{tc('gameView.explore')}</span>
            )}

            <div className="flex-1" />
            <button
              onClick={() => setChatOpen(!chatOpen)}
              className="flex items-center gap-1 text-xs text-text-secondary hover:text-accent"
              aria-label={tc('gameView.chat')}
            >
              <MessageSquare size={14} /> {tc('gameView.chat')}
            </button>
          </div>

          {/* Content */}
          <div className="flex flex-1 overflow-hidden">
            <div className="flex-1 overflow-hidden">
              {selectedLocation ? (
                <LocationDetail
                  locationId={selectedLocation}
                  worldId={worldId}
                  onSelectNpc={setSelectedNpc}
                />
              ) : (
                <div className="h-full">
                  <WorldMapView worldId={worldId} onSelectLocation={setSelectedLocation} />
                </div>
              )}
            </div>

            {effectiveChatOpen && (
              <RightPanel
                worldId={worldId}
                selectedNpc={selectedNpc}
                selectedLocation={selectedLocation}
                onCloseNpc={() => setSelectedNpc(null)}
              />
            )}
          </div>
        </div>
      </div>
      <StatusBar />
      {createMode === 'npc' && (
        <EntityCreateModal
          worldId={worldId}
          onCreated={() => {
            setCreateMode(null);
          }}
          onClose={() => setCreateMode(null)}
        />
      )}
      {createMode === 'region' && (
        <QuickRegionModal
          worldId={worldId}
          onCreated={() => {
            setCreateMode(null);
            window.location.reload();
          }}
          onClose={() => setCreateMode(null)}
        />
      )}
      {createMode === 'location' && (
        <RegionPicker
          worldId={worldId}
          onSelect={(r) => {
            setCreateLocationRegion(r);
            setCreateMode(null);
          }}
          onClose={() => setCreateMode(null)}
        />
      )}
      {createLocationRegion && (
        <LocationCreateModal
          regionId={createLocationRegion.id}
          regionName={createLocationRegion.name}
          onCreated={() => {
            setCreateLocationRegion(null);
            setCreateMode(null);
            window.location.reload();
          }}
          onClose={() => setCreateLocationRegion(null)}
        />
      )}
      {showStartCombat && (
        <StartCombatModal worldId={worldId} onClose={() => setShowStartCombat(false)} />
      )}
    </div>
  );
}
