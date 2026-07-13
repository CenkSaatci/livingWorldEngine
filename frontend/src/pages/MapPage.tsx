import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, MessageSquare } from 'lucide-react';
import { useState } from 'react';
import { MapCanvas } from '../components/map/MapCanvas';
import { ChatPanel } from '../components/chat/ChatPanel';
import { RollLog } from '../components/chat/RollLog';

export default function MapPage() {
  const { t } = useTranslation('map');
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [showChat, setShowChat] = useState(true);

  return (
    <div className="flex h-screen flex-col bg-bg-primary">
      {/* Top Bar */}
      <header className="flex items-center justify-between border-b border-bg-elevated bg-bg-surface px-4 py-2">
        <div className="flex items-center gap-3">
          <button onClick={() => navigate('/dashboard')} className="text-text-secondary hover:text-accent">
            <ArrowLeft size={20} />
          </button>
          <h1 className="text-lg font-heading text-text-primary">{t('title')}</h1>
        </div>
        <button
          onClick={() => setShowChat(!showChat)}
          className="flex items-center gap-1 text-sm text-text-secondary hover:text-accent"
        >
          <MessageSquare size={16} />
          Chat
        </button>
      </header>

      {/* Main Area */}
      <div className="flex flex-1 overflow-hidden">
        {/* Canvas */}
        <div className="flex-1 p-3">
          <div className="h-full rounded-lg border border-bg-elevated bg-bg-surface/50">
            <MapCanvas cols={20} rows={15} tileSize={48} />
          </div>
        </div>

        {/* Right Panel (Chat + Roll Log) */}
        {showChat && (
          <aside className="flex w-80 flex-col border-l border-bg-elevated bg-bg-surface">
            <div className="flex-1">
              <ChatPanel worldId={id ?? ''} />
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
