import { useState, useRef, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Send } from 'lucide-react';
import { apiClient } from '../../api/client';
import { useWorldStore } from '../../store/worldStore';

interface ChatMessage {
  id: string;
  sender: string;
  text: string;
  timestamp: string;
}

export function ChatPanel({ worldId }: { worldId: string }) {
  const { t } = useTranslation('chat');
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState('');
  const bottomRef = useRef<HTMLDivElement | null>(null);
  const worldEvents = useWorldStore((s) => s.worldEvents);

  // WS-Events als Chat-Nachrichten anzeigen
  useEffect(() => {
    const last = worldEvents[worldEvents.length - 1];
    if (last && last.event_type) {
      setMessages((prev) => [
        ...prev.slice(-99),
        {
          id: String(last.id),
          sender: last.event_type,
          text: JSON.stringify(last.payload),
          timestamp: last.created_at,
        },
      ]);
    }
  }, [worldEvents]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleSend = async () => {
    const text = input.trim();
    if (!text) return;
    setInput('');

    // Inline-Roll-Befehl
    if (text.startsWith('/r ')) {
      const expr = text.slice(3);
      try {
        const res = await apiClient.post('/rolls', {
          worldId,
          entityId: useWorldStore.getState().currentEntityId,
          skillId: 'custom',
          modifier: 0,
          target: 10,
        });
        const data = res.data;
        const sysMsg = { sender: '🎲 System', text: `Rolls ${expr}: ${data.total} (${data.expression})`, timestamp: new Date().toISOString() };
        await apiClient.post(`/chat/${worldId}`, sysMsg);
        setMessages((prev) => [...prev, { ...sysMsg, id: Date.now().toString() + '-roll' }]);
      } catch {
        // silent
      }
      return;
    }

    // Normaler Chat via API (broadcastet an WS)
    try {
      await apiClient.post(`/chat/${worldId}`, { sender: 'You', text });
    } catch {
      // offline fallback: lokale Nachricht
      setMessages((prev) => [...prev, { id: Date.now().toString(), sender: 'You', text, timestamp: new Date().toISOString() }]);
    }
  };

  return (
    <div className="flex h-full flex-col">
      <div className="flex-1 space-y-1 overflow-y-auto p-2">
        {messages.length === 0 && (
          <p className="pt-4 text-center text-xs text-text-secondary">
            {t('roll_command_hint')}
          </p>
        )}
        {messages.map((msg) => (
          <div key={msg.id} className="rounded bg-bg-primary/50 px-2 py-1">
            <span className="text-xs font-semibold text-accent">{msg.sender}</span>
            <p className="text-sm text-text-primary">{msg.text}</p>
          </div>
        ))}
        <div ref={bottomRef} />
      </div>

      <div className="flex gap-2 border-t border-bg-elevated p-2">
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleSend()}
          placeholder={t('placeholder')!}
          className="flex-1 rounded border border-bg-elevated bg-bg-primary px-2 py-1.5 text-sm
                     text-text-primary placeholder:text-text-secondary/50"
        />
        <button
          onClick={handleSend}
          className="rounded bg-accent p-1.5 text-white hover:bg-accent/80"
        >
          <Send size={16} />
        </button>
      </div>
    </div>
  );
}
