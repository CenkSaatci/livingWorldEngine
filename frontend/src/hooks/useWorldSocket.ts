import { useEffect, useRef } from 'react';
import { CompatClient, Stomp, StompSubscription } from '@stomp/stompjs';
import { useAuthStore } from '../store/authStore';
import { useWorldStore, type WorldEvent } from '../store/worldStore';
import { getAccessToken } from '../api/client';

const WS_URL = import.meta.env.VITE_WS_URL ?? 'ws://localhost:8080/ws';
const RECONNECT_BASE_MS = 3000;
const RECONNECT_MAX_MS = 30000;

let globalClient: CompatClient | null = null;

/**
 * Sendet eine Nachricht über die aktive STOMP-Verbindung.
 * (muss nach useWorldSocket() aufgerufen werden)
 */
export function stompSend(destination: string, body: Record<string, unknown>) {
  if (globalClient?.connected) {
    globalClient.send(destination, {}, JSON.stringify(body));
  }
}

/**
 * Baut eine STOMP-WebSocket-Verbindung auf.
 *
 * - Abonniert /topic/world/{worldId}
 * - Verarbeitet TOKEN_MOVED → worldStore.updateTokenPosition
 * - Reconnect bei Verbindungsabbruch (exponential backoff)
 */
export function useWorldSocket(worldId: string | undefined) {
  const clientRef = useRef<CompatClient | null>(null);
  const subRef = useRef<StompSubscription | null>(null);
  const retryRef = useRef(0);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const addEvent = useWorldStore((s) => s.addEvent);
  const updateTokenPos = useWorldStore((s) => s.updateTokenPosition);

  useEffect(() => {
    if (!worldId || !isAuthenticated) return;
    const token = getAccessToken();
    if (!token) return;
    retryRef.current = 0;

    const scheduleReconnect = () => {
      const delay = Math.min(RECONNECT_BASE_MS * Math.pow(2, retryRef.current), RECONNECT_MAX_MS);
      retryRef.current++;
      timerRef.current = setTimeout(start, delay);
    };

    function start() {
      const client = Stomp.client(WS_URL);
      clientRef.current = client;
      globalClient = client;

      client.configure({
        beforeConnect: () => { client.connectHeaders = { Authorization: `Bearer ${token}` }; },
        onConnect: () => {
          retryRef.current = 0;
          subRef.current = client.subscribe(`/topic/world/${worldId}`, (msg) => {
            try {
              const event = JSON.parse(msg.body) as WorldEvent;
              addEvent(event);

              // Spezifische Event-Typen direkt verarbeiten
              if (event.event_type === 'TOKEN_MOVED') {
                const p = event.payload as Record<string, unknown>;
                updateTokenPos(
                  (p.entityId ?? p.target_entity_id ?? '') as string,
                  Number(p.x ?? 0),
                  Number(p.y ?? 0),
                );
              }
            } catch { /* ignore */ }
          });
        },
        onWebSocketClose: () => scheduleReconnect(),
        onStompError: () => scheduleReconnect(),
      });
      client.activate();
    }

    start();
    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
      if (subRef.current) subRef.current.unsubscribe();
      if (clientRef.current) { clientRef.current.deactivate(); globalClient = null; }
    };
  }, [worldId, isAuthenticated, addEvent, updateTokenPos]);
}
