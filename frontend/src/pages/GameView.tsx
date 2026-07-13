import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { PanelLeftClose, PanelLeft, MessageSquare, Globe, LogOut } from 'lucide-react';
import { RegionTree } from '../components/world/RegionTree';
import { LocationDetail } from '../components/world/LocationDetail';
import { EntityTimeline } from '../components/world/EntityTimeline';
import { MapCanvas } from '../components/map/MapCanvas';
import { ChatPanel } from '../components/chat/ChatPanel';
import { RollLog } from '../components/chat/RollLog';
import { useWorldSocket } from '../hooks/useWorldSocket';
import { useAuthStore } from '../store/authStore';

export default function GameView() {
  const { t, i18n } = useTranslation('map');
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);
  const worldId = id ?? '';

  useWorldSocket(worldId);

  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [chatOpen, setChatOpen] = useState(true);
  const [selectedLocation, setSelectedLocation] = useState<string | null>(null);
  const currentLocale = i18n.language;

  return (
    <div className="flex h-screen flex-col bg-bg-primary">
      {/* Top Bar */}
      <header className="flex items-center justify-between border-b border-bg-elevated bg-bg-surface px-4 py-2">
        <div className="flex items-center gap-3">
          <button onClick={() => navigate('/dashboard')} className="text-sm text-accent hover:text-accent/60">
            ← Dashboard
          </button>
          <span className="text-text-secondary">|</span>
          <h1 className="text-lg font-heading text-text-primary">Game View</h1>
        </div>
        <div className="flex items-center gap-3">
          <button onClick={() => { i18n.changeLanguage(currentLocale === 'de' ? 'en' : 'de'); useAuthStore.getState().setLocale(currentLocale === 'de' ? 'en' : 'de'); }}
            className="flex items-center gap-1 text-sm text-text-secondary hover:text-accent">
            <Globe size={16} /> {currentLocale.toUpperCase()}
          </button>
          <span className="text-sm text-text-secondary">{user?.username}</span>
          <button onClick={() => { logout(); navigate('/login'); }}
            className="flex items-center gap-1 text-sm text-danger hover:text-danger/60">
            <LogOut size={16} /> {t('actions.logout', { ns: 'common' })}
          </button>
        </div>
      </header>

      <div className="flex flex-1 overflow-hidden">
        {/* Sidebar */}
        {sidebarOpen && (
          <aside className="w-64 shrink-0 overflow-y-auto border-r border-bg-elevated bg-bg-surface p-2">
            <RegionTree worldId={worldId} onSelectLocation={setSelectedLocation} />
          </aside>
        )}

        {/* Main */}
        <div className="flex flex-1 flex-col">
          {/* Toolbar */}
          <div className="flex items-center gap-2 border-b border-bg-elevated bg-bg-surface/50 px-3 py-1.5">
            <button onClick={() => setSidebarOpen(!sidebarOpen)} className="text-text-secondary hover:text-accent">
              {sidebarOpen ? <PanelLeftClose size={16} /> : <PanelLeft size={16} />}
            </button>
            {selectedLocation && (
              <span className="text-xs text-text-secondary">📍 {selectedLocation.slice(0, 8)}…</span>
            )}
            <div className="flex-1" />
            <button onClick={() => setChatOpen(!chatOpen)} className="flex items-center gap-1 text-xs text-text-secondary hover:text-accent">
              <MessageSquare size={14} /> Chat
            </button>
          </div>

          {/* Content Area */}
          <div className="flex flex-1 overflow-hidden">
            <div className="flex-1 p-3 overflow-auto">
              {selectedLocation ? (
                <LocationDetail locationId={selectedLocation} />
              ) : (
                <div className="h-full rounded-lg border border-bg-elevated bg-bg-surface/30">
                  <MapCanvas cols={20} rows={15} tileSize={48} />
                </div>
              )}
            </div>

            {/* Right Panel */}
            {chatOpen && (
              <aside className="flex w-80 flex-col border-l border-bg-elevated bg-bg-surface">
                {/* Entity Timeline */}
                {selectedLocation && (
                  <div className="border-b border-bg-elevated p-3 max-h-48 overflow-y-auto">
                    <EntityTimeline entityType="location" entityId={selectedLocation} />
                  </div>
                )}
                {/* Chat */}
                <div className="flex-1 min-h-0">
                  <ChatPanel worldId={worldId} />
                </div>
                {/* Roll Log */}
                <div className="h-40 border-t border-bg-elevated">
                  <RollLog />
                </div>
              </aside>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
