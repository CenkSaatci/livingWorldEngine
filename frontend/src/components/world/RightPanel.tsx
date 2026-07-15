import { ChatPanel } from '../chat/ChatPanel';
import { RollLog } from '../chat/RollLog';
import { EntityTimeline } from './EntityTimeline';
import { NpcQuickInfo } from './NpcQuickInfo';

interface Props {
  worldId: string;
  selectedNpc: string | null;
  selectedLocation: string | null;
  onCloseNpc: () => void;
}

export function RightPanel({ worldId, selectedNpc, selectedLocation, onCloseNpc }: Props) {
  return (
    <aside className="flex w-80 flex-col border-l border-bg-elevated bg-bg-surface">
      {selectedNpc && (
        <div className="border-b border-bg-elevated p-2">
          <NpcQuickInfo npcId={selectedNpc} worldId={worldId} onClose={onCloseNpc} />
        </div>
      )}

      {selectedLocation && (
        <div className="max-h-48 overflow-y-auto border-b border-bg-elevated p-3">
          <EntityTimeline entityType="location" entityId={selectedLocation} />
        </div>
      )}

      <div className="flex-1 min-h-0">
        <ChatPanel worldId={worldId} />
      </div>

      <div className="h-40 shrink-0 border-t border-bg-elevated">
        <RollLog />
      </div>
    </aside>
  );
}
