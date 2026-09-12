import { useState, useRef, useEffect, memo } from 'react';
import { useTranslation } from 'react-i18next';
import { Send } from 'lucide-react';
import { apiClient } from '../../api/client';
import { useWorldStore } from '../../store/worldStore';
import { DiceRollModal } from '../ui/DiceRollModal';
import { playChatMessage } from '../../utils/sound';

interface ChatMessage {
  id: string;
  sender: string;
  text: string;
  timestamp: string;
}

const ChatMessageItem = memo(({ msg }: { msg: ChatMessage }) => (
  <div className="rounded bg-bg-primary/50 px-2 py-1">
    <span className="text-xs font-semibold text-accent">{msg.sender}</span>
    <p className="text-sm text-text-primary">{msg.text}</p>
  </div>
));

interface RollModalState {
  label: string;
  dice: { sides: number; value: number }[];
  modifier: number;
  total: number;
}

export function ChatPanel({ worldId }: { worldId: string }) {
  const { t } = useTranslation('chat');
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState('');
  const [rollModal, setRollModal] = useState<RollModalState | null>(null);
  const bottomRef = useRef<HTMLDivElement | null>(null);
  const worldEvents = useWorldStore((s) => s.worldEvents);

  // WS-Events als Chat-Nachrichten anzeigen — aber nur mit menschlichem Text.
  // Technische Events (COMBAT_ACTION_EXECUTED, PROBE_ROLLED, …) aktualisieren
  // Stores/UI direkt; als Roh-JSON im Chat wären sie nur Rauschen (Playtest #11).
  useEffect(() => {
    const last = worldEvents[worldEvents.length - 1];
    if (last && last.event_type) {
      const payload = last.payload as Record<string, unknown> ?? {};
      const text = payload.text;
      if (typeof text !== 'string' || !text.trim()) return;
      const sender =
        (payload.sender as string) ||
        (last.event_type === 'CHAT_MESSAGE' ? 'System' : last.event_type);
      setMessages((prev) => [
        ...prev.slice(-99),
        {
          id: `${Date.now()}-${last.event_type}-${Math.random().toString(36).slice(2, 6)}`,
          sender,
          text,
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
      const rawExpr = text.slice(3).trim();
      // Prüfe ob Expression gültig ist (z.B. "2d6+3", "1d20", "3d8-2")
      const exprMatch = rawExpr.match(/^(\d+)d(\d+)([+-]\d+)?$/i);
      if (!exprMatch) {
        setMessages((prev) => [
          ...prev,
          {
            id: Date.now().toString() + '-err',
            sender: '🎲 System',
            text: `Invalid expression. Use /r <count>d<sides>[+mod], e.g. /r 2d6+3`,
            timestamp: new Date().toISOString(),
          },
        ]);
        return;
      }

      try {
        const res = await apiClient.post('/rolls/free', { expression: rawExpr });
        const data = res.data;
        const diceValues = (data.dice as number[]) ?? [0];
        const total = (data.total as number) ?? 0;
        const sides = (data.sides as number) ?? 6;
        const modifier = (data.modifier as number) ?? 0;
        const dice = diceValues.map((value) => ({ sides, value }));

        setRollModal({ label: rawExpr, dice, modifier, total });

        const sysMsg = {
          sender: '🎲 System',
          text: `🎲 ${rawExpr} = ${total}`,
          timestamp: new Date().toISOString(),
        };
        await apiClient.post(`/chat/${worldId}`, sysMsg);
        setMessages((prev) => [...prev, { ...sysMsg, id: Date.now().toString() + '-roll' }]);
        playChatMessage();
      } catch {
        // silent
      }
      return;
    }

    // Normaler Chat via API (broadcastet an WS)
    try {
      await apiClient.post(`/chat/${worldId}`, { sender: 'You', text });
      playChatMessage();
    } catch {
      // offline fallback: lokale Nachricht
      setMessages((prev) => [
        ...prev,
        { id: Date.now().toString(), sender: 'You', text, timestamp: new Date().toISOString() },
      ]);
    }
  };

  return (
    <div className="flex h-full flex-col">
      <div className="flex-1 space-y-1 overflow-y-auto p-2" role="log" aria-live="polite">
        {messages.length === 0 && (
          <p className="pt-4 text-center text-xs text-text-secondary">{t('roll_command_hint')}</p>
        )}
        {messages.map((msg) => (
          <ChatMessageItem key={msg.id} msg={msg} />
        ))}
        <div ref={bottomRef} />
      </div>

      <div className="flex gap-2 border-t border-bg-elevated p-2">
        <input
          data-chat-input
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

      {rollModal && (
        <DiceRollModal
          label={rollModal.label}
          dice={rollModal.dice}
          modifier={rollModal.modifier}
          total={rollModal.total}
          onClose={() => setRollModal(null)}
        />
      )}
    </div>
  );
}
