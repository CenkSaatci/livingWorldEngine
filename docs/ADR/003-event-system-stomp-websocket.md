# ADR-003: Event-Kommunikation via STOMP over WebSocket

- **Status:** Accepted
- **Date:** 2025-07-12

## Kontext

LWE benötigt Echtzeit-Kommunikation zwischen Backend und Frontend für:
- Live-Updates (Würfelwürfe, NPC-Aktionen, Chat)
- Token-Bewegungen auf Karte
- Fog-of-War-Updates
- DM-Queue-Notifications

In Frage kommende Optionen:
1. **Server-Sent Events (SSE)** — HTTP-basiert,单向
2. **Plain WebSockets** — Vollduplex, manuell
3. **STOMP over WebSocket** — Vollduplex + Subprotocol mit Topic-Subscription
4. **gRPC-Web** — Binary, type-safe

## Entscheidung

**STOMP over WebSocket** via Spring Boot's `spring-boot-starter-websocket`.

## Begründung

### Gegen SSE
- SSE ist nur Server→Client. KI-Modi (z. B. Spielerwagon Würfel) brauchen Browser→Server.
- SSE hat Connection-Limits (6 in HTTP/1.1) und ist nicht überaching-replay-tauglich
- Client-Topics (z. B. nur `/topic/world/{id}` des nicht lobby) müssen im Frontend selbst gefiltert werden

### Gegen Plain WebSockets
- Plain WebSockets liefern nur raw Byte-Stream; Aufbau von Topic/Subscription/Messaging muss kodiert werden
- Bei Backend-Last (mehrere Clients, eine Welt) fehlt Broker-Funktionalität
- STOMP als Standard etabliert und via Spring direkt unterstützt

### Gegen gRPC-Web
- gRPC-Web erfordert Proxy-Setup (Envoy), hohe Komplexität
- Browser-Tooling für Debugging deutlich schlechter
- LWE hat nicht genug Request-Last, um die Binary-Stream-Effizienz auszuspielen

### Für STOMP
- Spring Boot hat erstklassige STOMP-Unterstützung (Socket-Broker, `/topic`-Routing, `@MessageMapping`-Annotationen)
- Browser-Clients via `@stomp/stompjs` alleine — kein SockJS nötig (STOMP über native WebSockets)
- Subscription-Konzeptът passt natürlich zu LWE: `/topic/world/{id}`, `/topic/combat/{id}`, `/topic/dm/intents/{worldId}`
- STOMP-Header carry JWT (`Authorization`)

## Konsequenzen

**Positiv:**
- Saubere Topic-Hierarchie matcht Domäne (Welt, Kampf, Chat, DM)
- Server-seitiges Routing ohne Boilerplate
- Authentifizierung im CONNECT-Frame standardisiert

**Negativ:**
- STOMP-Frame-Parsing hat etwas mehr Overhead als plain JSON (in LWE vernachlässigbar)
- Wenn in Zukunft massiv skaliert muss, eventuell auf Message-Broker (RabbitMQ/Redis Pub-Sub als STOMP-Broker) upgraden

## Implementations-Hinweise

- `WebSocketConfig` als `@EnableWebSocketMessageBroker`
- `MessageBrokerRegistry.enableSimpleBroker("/topic/")` (Phase 1–4), `enableStompBrokerRelay` (Phase 5)
- `@MessageMapping("/app/...")` für Server-empfangene Channels
- `SimpMessagingTemplate.convertAndSend("/topic/...")` für Server-Broadcasts
- Auth via `WebSocketSecurityConfig` (ChannelInterceptor, liest JWT aus Header)

## Referenzen

- https://spring.io/guides/gs/messaging-stomp-websocket
- [`API.md`](../API.md) — Themenliste