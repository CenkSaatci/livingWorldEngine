import { useEffect, useRef } from 'react';
import { Stomp, type CompatClient, type StompSubscription } from '@stomp/stompjs';
import { useAuthStore } from '../store/authStore';
import { useWorldStore, type WorldEvent } from '../store/worldStore';
import { useCombatStore } from '../store/combatStore';
import { useToastStore } from '../store/toastStore';
import { getAccessToken, apiClient } from '../api/client';

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
  // Snapshot als Effect-Dep (Reconnect bei Tokenwechsel, z.B. Re-Login).
  const tokenSnapshot = isAuthenticated ? getAccessToken() : null;
  const addEvent = useWorldStore((s) => s.addEvent);
  const updateTokenPos = useWorldStore((s) => s.updateTokenPosition);

  useEffect(() => {
    if (!worldId || !isAuthenticated) return;
    // Snapshot als Effect-Dep (Reconnect bei Tokenwechsel, z.B. Re-Login);
    // beforeConnect liest den Token trotzdem frisch (scheduled Reconnects!).
    const tokenSnapshot = getAccessToken();
    if (!tokenSnapshot) return;
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
        beforeConnect: () => {
          client.connectHeaders = { Authorization: `Bearer ${getAccessToken() ?? ''}` };
        },
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

              // T33-08: Intent-Aenderungen sofort an die DM-Queue melden
              if (String(event.event_type ?? '').startsWith('NPC_INTENT')) {
                window.dispatchEvent(new CustomEvent('lwe:npc-intents-changed'));
              }

              // Combat-Events
              if (event.event_type === 'COMBAT_STARTED') {
                const p = event.payload as Record<string, unknown>;
                const combatStore = useCombatStore.getState();
                apiClient
                  .get(`/combat/${p.sessionId}`)
                  .then((r) => {
                    combatStore.setSession(r.data.session, r.data.participants);
                  })
                  .catch(() => {
                    useToastStore
                      .getState()
                      .addToast('Failed to load combat session', 'error');
                    combatStore.clearCombat();
                  });
              }

              if (event.event_type === 'COMBAT_ACTION_EXECUTED') {
                const combatStore = useCombatStore.getState();
                if (combatStore.session) {
                  apiClient
                    .get(`/combat/${combatStore.session.id}`)
                    .then((r) => {
                      combatStore.setSession(r.data.session, r.data.participants);
                    })
                    .catch(() => {
                      useToastStore
                        .getState()
                        .addToast('Failed to refresh combat session', 'error');
                    });
                }
              }

              if (event.event_type === 'TURN_CHANGED') {
                const p = event.payload as { currentTurn?: string; round?: number };
                useCombatStore.getState().handleTurnChanged(p.currentTurn ?? '', p.round ?? 1);
              }

              if (event.event_type === 'COMBAT_ENDED') {
                useCombatStore.getState().clearCombat();
              }
            } catch {
              /* ignore */
            }
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
      if (clientRef.current) {
        clientRef.current.deactivate();
        globalClient = null;
      }
    };
  }, [worldId, isAuthenticated, tokenSnapshot, addEvent, updateTokenPos]);
}
