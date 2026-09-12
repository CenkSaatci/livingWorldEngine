import { useState, useRef, useEffect, memo } from 'react';
import { useTranslation } from 'react-i18next';
import { Send } from 'lucide-react';
import { apiClient } from '../../api/client';
import { useWorldStore } from '../../store/worldStore';
import { useToast } from '../../hooks/useToast';
import { DiceRollModal } from '../ui/DiceRollModal';
import { playChatMessage } from '../../utils/sound';

interface ChatMessage {
  id: string;
  sender: string;
  text: string;
  timestamp: string;
}

const ChatMessageItem = memo(({ msg }: { msg: ChatMessage }) => {
  const time = (() => {
    try {
      return new Date(msg.timestamp).toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });
    } catch {
      return '';
    }
  })();
  return (
    <div className="rounded bg-bg-primary/50 px-2 py-1">
      <span className="text-xs font-semibold text-accent">{msg.sender}</span>
      {time && <span className="ml-2 text-[10px] text-text-secondary">{time}</span>}
      <p className="text-sm text-text-primary">{msg.text}</p>
    </div>
  );
});

interface RollModalState {
  label: string;
  dice: { sides: number; value: number }[];
  modifier: number;
  total: number;
}

export function ChatPanel({ worldId }: { worldId: string }) {
  const { t } = useTranslation('chat');
  const toast = useToast();
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState('');
  const [rollModal, setRollModal] = useState<RollModalState | null>(null);
  const bottomRef = useRef<HTMLDivElement | null>(null);
  const worldEvents = useWorldStore((s) => s.worldEvents);

  // B5: Verlauf beim Betreten laden (neueste 50, chronologisch).
  useEffect(() => {
    if (!worldId) return;
    let cancelled = false;
    apiClient
      .get(`/chat/${worldId}`)
      .then((res) => {
        if (cancelled || !Array.isArray(res.data)) return;
        setMessages(
          res.data.map((m: { sender: string; text: string; timestamp: string }, i: number) => ({
            id: `history-${i}-${m.timestamp}`,
            sender: m.sender,
            text: m.text,
            timestamp: m.timestamp,
          })),
        );
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, [worldId]);

  // WS-Events als Chat-Nachrichten anzeigen — nur CHAT_MESSAGE mit Text.
  // Audit R4: Timestamp kommt aus payload.timestamp (Broadcast sendet kein
  // created_at); Dedupe gegen das lokale Echo verhindert Doppelzeilen.
  useEffect(() => {
    const last = worldEvents[worldEvents.length - 1];
    if (!last || last.event_type !== 'CHAT_MESSAGE') return;
    const payload = (last.payload ?? {}) as Record<string, unknown>;
    const text = payload.text;
    if (typeof text !== 'string' || !text.trim()) return;
    const sender = typeof payload.sender === 'string' && payload.sender ? payload.sender : 'System';
    const ts = typeof payload.timestamp === 'string' ? payload.timestamp : last.created_at;
    setMessages((prev) => {
      const now = Date.now();
      const isEcho = prev
        .slice(-5)
        .some((m) => m.sender === sender && m.text === text
          && Math.abs(now - new Date(m.timestamp).getTime()) < 10_000);
      if (isEcho) return prev;
      return [
        ...prev.slice(-99),
        {
          id: `${Date.now()}-chat-${Math.random().toString(36).slice(2, 6)}`,
          sender,
          text,
          timestamp: ts,
        },
      ];
    });
  }, [worldEvents]);

  const appendLocal = (sender: string, text: string) => {
    setMessages((prev) => [
      ...prev.slice(-99),
      { id: `${Date.now()}-local-${Math.random().toString(36).slice(2, 6)}`, sender, text, timestamp: new Date().toISOString() },
    ]);
  };

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
            text: t('invalidExpression'),
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

        const rollText = `🎲 ${rawExpr} = ${total}`;
        try {
          await apiClient.post(`/chat/${worldId}`, { sender: '🎲 System', text: rollText });
        } catch {
          // Audit R4: POST fehlgeschlagen → Zeile trotzdem lokal anzeigen.
        }
        appendLocal('🎲 System', rollText);
        playChatMessage();
      } catch {
        toast.error(t('invalidExpression'));
      }
      return;
    }

    // Normaler Chat: POST + lokales Echo (WS-Echo wird dedupliziert; funktioniert
    // damit auch bei totem WS — Audit R4).
    try {
      await apiClient.post(`/chat/${worldId}`, { sender: 'You', text });
      playChatMessage();
    } catch {
      /* offline: lokales Echo unten reicht */
    }
    appendLocal('You', text);
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
