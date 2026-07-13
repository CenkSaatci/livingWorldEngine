# ADR-008: API-Versionierung via `/api/v1/` Prefix

- **Status:** Accepted
- **Date:** 2025-07-12

## Kontext

LWE ist auf SaaS-Betrieb ausgelegt. Breaking Changes an der API sind früher oder später unvermeidbar (Felder verschwinden, Endpunkte werden restrukturiert, Auth-Flows ändern sich). Ohne Versionierung haben Clients folgende Optionen, alle schlecht:

1. Stumme Bruchs — alte Clients funktionieren nach Server-Update nicht mehr, ohne Vorwarnung
2. Negotiation via Header (`Accept: application/vnd.lwe.v2+json`) — funktioniert, aber schlecht debuggbar (curl-Befehle brauchen seltsame Header)
3. querying-string (`?api-version=2`) — ok, aber verdirbt Caching und URL-Semantik

## Entscheidung

**URI-Prefix `/api/v1/...` für alle REST-Endpunkte.** WebSocket-STOMP-Topics bleiben ungeprefixt (sie sind nicht-HTTP und werden parallel via Subprotocol versioniert, siehe unten).

## Begründung

### Gegen Header-basierte Negotiation
- Unschön für `curl`-Beispiele — jeder Request braucht Header-Block
- Async-Clients brauchen explizite Accept-Header-Set-Logik
- Browser-Tests (Playwright) deutlich komplexer
- Proxy-Logging zeigt Version nicht in URL

### Für URI-Prefix
- **Explicit in jeder URL** — `curl http://server/api/v1/worlds` ist selbsterklärend
- VersionsResolver in Spring via `@RequestMapping("/api/v{version:[0-9]+}/...")` patterns
- Alte Versionen können parallel unterstützt werden (`/api/v1/...` + `/api/v2/...`) ohne Header-Konflikte
- Logs, Monitoring, Caching alle URL-basiert — Version ist direkt sichtbar
- API-Gateway / Nginx-Routing kann Version an verschiedene Backend-Pods leiten, falls nötig

### Wiki-Beispiel
| Statt | Lieber |
|---|---|
| `POST /api/worlds` | `POST /api/v1/worlds` |
| `POST /api/rolls` mit `Accept: application/vnd.lwe.v2+json` | `POST /api/v2/rolls` |

## Kompatibilitäts-Policy

- **Backwards-compatible Änderungen** in derselben Version:
  - Neue optionale Felder in Request/Response
  - Neue Endpunkte
  - Neue Fehlercodes
- **Breaking Changes** → neue Major-Version:
  - Pflichtfelder entfernt/umbenannt
  - Verhalten eines Endpunkts ändert sich semantisch
  - Fehlercode-Semantik ändert sich
- **Gleichzeitige Unterstützung**: Mindestens eine Major-Version alte bleibt **mindestens 6 Monate** nach Release der neuen verfügbar. Clients bekommen Deprecation-Header (`Sunset: Wed, 11 Nov 2025 23:59:59 GMT`).

## WebSocket

STOMP-Topics bleiben **ungeprefixt** (`/topic/world/{id}`). Versionierung via STOMP `ACCEPT-VERSION` Header im CONNECT-Frame (STOMP 1.1/1.2 hat das eingebaut). Bei inkompatiblen Topic-Payload-Formaten → eigene STOMP-Subprotocol-ID (z. B. `lwe.stomp.v2`).

## Implementations-Hinweise

- Spring Boot: ApplicationController-Mapping landet unter `/api/v1/...` — via gemeinsames `@RequestMapping("/api/v1")` auf Klasse oder via Konfiguration in `WebMvcConfigurer`
- Frontend: One constant `API_BASE = '/api/v1'` im Client-Setup, alle Axios-Aufrufe relativ dazu
- Bot:Konstante in `ai_bot.config.Settings.backend_url` beinhaltet `…/api/v1`
- Doku (`docs/API.md`): Alle Endpunkte ab sofort unter `/api/v1/...` dokumentiert
- `docs/API.md` Conventions-Sektion: Base URL explizit als `/api/v1`

## Konsequenzen

**Positiv:**
- Kunden-Bruchs sind erklärbar und vorhersehbar
- Logs zeigen Version — Monitoring einfach
- Echte Parallel-Test-Phase neuer Versionen möglich
- Bei Kunden-Anfragen ist klar, welche Version sie nutzen

**Negativ:**
- Mehr Tipp-Aufwand in curl-Beispielen (vernachlässigbar)
- Versionsverwaltung im Code (Filter / Routing-Logik)
- Doku muss Version bei jedem Endpunkt mitführen

## Referenzen

- [`API.md`](../API.md) — alle Endpunkte unter `/api/v1/...`
- https://spring.io/blog/versioning-rest-api