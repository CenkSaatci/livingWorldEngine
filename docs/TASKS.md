# Tasks

> Master-Task-Liste. ID-Referenzen (z. B. `P2-T04`) können in Commits, PRs und Reviews verwendet werden.

## Legende

| Symbol | Bedeutung |
|---|---|
| 📋 | offen |
| 🔄 | in Arbeit |
| ✅ | erledigt |
| ⏸️ | blockiert / verschoben |

## Konventionen

- **Workflow:** Jeder Task durchläuft **Analyse → Plan-Vorlage (User-Approval) → TDD (Red-Green-Refactor) → Verify → Done**. Kein Task springt direkt in die Implementierung. Siehe [`WORKFLOW.md`](WORKFLOW.md).
- **Statusänderungen** werden als eigener Commit committed (Task ID im Message, z. B. `chore(tasks): mark P1-T03 done`)
- **Abhängigkeiten** sind harte Vorbedingungen — der Task kann nicht started werden, bevor alle Dependencies ✅ sind.
- **Akzeptanzkriterien** müssen **alle** erfüllt sein, bevor der Task auf ✅ geht.
- **Schaue auch** [`ROADMAP.md`](ROADMAP.md) für Phasenkontext.

---

## Phase 1: Das Skelett

### P1-T01: Spring Boot Projekt aufsetzen
- **Status:** 📋
- **Aufwand:** 1 Tag
- **Abhängigkeiten:** —
- **Beschreibung:** Maven-Projekt mit Spring Boot 3.3.x, Java 21 LTS. Dependencies: Spring Web, Spring Data JPA, Spring Security, PostgreSQL Driver, Flyway, Validation, Lombok, WebSocket, Actuator.
- **Akzeptanzkriterien:**
  - [ ] `mvn spring-boot:run` startet fehlerfrei mit Profil `dev`
  - [ ] `/actuator/health` antwortet `200 UP`
  - [ ] Package-Struktur `com.lwe.core`, `.api`, `.config`, `.domain`, `.repository`, `.security`
  - [ ] `application.yml` liest Werte aus Umgebungsvariablen (`${DB_HOST}` etc.)
  - [ ] `application-dev.yml` aktiviert Hot-Reloading
- **Dateien:** `pom.xml`, `src/main/java/com/lwe/LweApplication.java`, `src/main/resources/application.yml`, `src/main/resources/application-dev.yml`

### P1-T02: Podman Compose (PostgreSQL + pgAdmin)
- **Status:** 📋
- **Aufwand:** 0,5 Tage
- **Abhängigkeiten:** —
- **Beschreibung:** `compose.yml` für pgAdmin (PostgreSQL selbst läuft extern auf Portainer `192.168.31.151:5432`). Optional: lokaler Postgres für CI-Tests. Container-Runtime ist **Podman**, siehe [`ADR/006`](ADR/006-container-runtime-podman.md).
- **Akzeptanzkriterien:**
  - [ ] `podman compose up -d pgadmin` startet pgAdmin
  - [ ] pgAdmin kann sich mit `192.168.31.151:5432` verbinden (Credentials aus `.env`)
  - [ ] `compose.override.yml.example` für persönliche Anpassungen vorhanden (in `.gitignore`)
  - [ ] README-Hinweis: externer Postgres und Podman als Runtime vorausgesetzt
- **Dateien:** `compose.yml`, `compose.override.yml.example`

### P1-T03: Flyway Grundgerüst + erste Migration
- **Status:** 📋
- **Aufwand:** 1 Tag
- **Abhängigkeiten:** P1-T01
- **Beschreibung:** Flyway konfigurieren und Schema `V001__initial.sql` anlegen. Enthält: `users`, `game_systems`, `worlds`, `world_members`, `entities` (PC/NPC/Fraktion in einerTabelle mit Type-Discriminator), `world_events`, `npc_intents`.
- **Akzeptanzkriterien:**
  - [ ] `mvn flyway:migrate` läuft fehlerfrei gegen `192.168.31.151/lwe`
  - [ ] Alle Tabellen laut [`DATA-MODEL.md`](DATA-MODEL.md) Phase-1-Sektion vorhanden
  - [ ] Indizes für häufige Queries (`world_id`, `created_at`) sind gesetzt
  - [ ] Tabellen haben `created_at`/`updated_at` Audit-Spalten
- **Dateien:** `src/main/resources/db/migration/V001__initial.sql`, `src/main/resources/db/migration/V002__combat.sql` (Platzhalter)

### P1-T04: User-Tabelle + JWT-Authentifizierung
- **Status:** 📋
- **Aufwand:** 2 Tage
- **Abhängigkeiten:** P1-T03
- **Beschreibung:** Spring Security konfiguriert mit JWT. Endpunkte `POST /api/auth/register|login|refresh`. Passwort-Hash via BCrypt. Rollen `USER`, `ADMIN` (vorbereitet, Rollen-basiertes Test-Routen in Phase 5).
- **Akzeptanzkriterien:**
  - [ ] `POST /api/auth/register` erstellt User mit BCrypt-Hash
  - [ ] `POST /api/auth/login` liefert JWT (24 h) + Refresh-Token (7 d)
  - [ ] `POST /api/auth/refresh` rotiert Token
  - [ ] Geschützte Routen ohne Token → `401`
  - [ ] Unit-Tests für `JwtService`, `AuthService`, `JwtAuthFilter`
  - [ ] Fehlermeldungen via `MessageSource` in Abhängigkeit von `Accept-Language` lokalisiert (DE + EN). Hängt von P1-T09 ab.
- **Dateien:** `src/main/java/com/lwe/security/JwtService.java`, `AuthService.java`, `JwtAuthFilter.java`, `SecurityConfig.java`, `UserController.java`

### P1-T05: Game-System Repository + JSON-Schema-Validator
- **Status:** 📋
- **Aufwand:** 2 Tage
- **Abhängigkeiten:** P1-T03, P1-T04
- **Beschreibung:** Persistenz für `game_systems`. JSON-Schema-Validator, der jedes hochgeladene Regelwerk gegen das Schema in [`RULES-SCHEMA.md`](RULES-SCHEMA.md) prüft. Endpunkte `POST /api/game-systems`, `GET /api/game-systems/{id}`, `POST /api/game-systems/{id}/validate`.
- **Akzeptanzkriterien:**
  - [ ] `POST` persistiert Regelwerk nur, wenn validierbar
  - [ ] `POST /validate` returns strukturierte Fehlermeldung bei invalidem JSON
  - [ ] Mindestens 2 Beispiel-Regelwerke als Test-Fixtures (D20Lite, TwoDicePool)
  - [ ] Integrationstest lädt beide Beispielwerke erfolgreich
- **Dateien:** `GameSystem.java`, `GameSystemRepository.java`, `GameSystemService.java`, `GameSystemController.java`, `RuleSchemaValidator.java`, `src/test/resources/rules/d20lite.json`, `src/test/resources/rules/twodicepool.json`

### P1-T06: World + Entity Repository + REST-Endpoints
- **Status:** 📋
- **Aufwand:** 2 Tage
- **Abhängigkeiten:** P1-T05
- **Beschreibung:** Endpunkte für `worlds` und `entities` (PC/NPC/Fraktionen). Worldbeanutzer (=Owner) via JWT identifiziert. `world_members` für Einladungen (vorbereitet).
- **Akzeptanzkriterien:**
  - [ ] `POST /api/worlds` erstellt Welt mit `owner_id` aus JWT
  - [ ] `GET /api/worlds` listet nur Welten des Users
  - [ ] `POST /api/worlds/{id}/entities` erstellt NPC/PC/Fraktion (Type-Feld)
  - [ ] Nur `owner_id` darf Welt bearbeiten, sonst `403`
  - [ ] Integrationstests für Permission-Checks (Cross-User-Zugriff)
- **Dateien:** `World.java`, `Entity.java`, `WorldRepository.java`, `EntityRepository.java`, `WorldController.java`, `EntityController.java`

### P1-T07: WebSocket-Konfiguration (STOMP) und Test-Topic
- **Status:** 📋
- **Aufwand:** 1 Tag
- **Abhängigkeiten:** P1-T04
- **Beschreibung:** STOMP-over-WebSocket konfigurieren. Test-Topic `/topic/world/{id}` publishable via `POST /api/test/ws/{id}`. Auth via STOMP-Header `Authorization: Bearer {jwt}`.
- **Akzeptanzkriterien:**
  - [ ] Client kann sich mit `ws://localhost:8080/ws` verbinden
  - [ ] Authentifizierte Subscription auf `/topic/world/{id}` funktioniert
  - [ ] `POST /api/test/ws/{worldId}` broadcastet Test-Event an alle Subscriber desselben Welt
  - [ ] Integrationstest mit `WebSocketStompClient`
- **Dateien:** `WebSocketConfig.java`, `WebSocketSecurityConfig.java`, `TestWsController.java`, `WorldEventBroadcaster.java`

### P1-T08: Smoke-Test + Meilenstein M1
- **Status:** 📋
- **Aufwand:** 0,5 Tage
- **Abhängigkeiten:** P1-T01 … P1-T07
- **Beschreibung:** End-to-End Smoke-Test dokumentiert: Curl-Sequenz, die User registriert, login, Game-System hochlädt, Welt erstellt, Entity anlegt, WS-Event empfängt.
- **Akzeptanzkriterien:**
  - [ ] `docs/SMOKE-TEST.md` dokumentiert komplette Sequenz
  - [ ] Smoke-Test läuft von Hand durch
  - [ ] M1 Trigger: alles via `curl`/`websocat` erreichbar
- **Dateien:** `docs/SMOKE-TEST.md`

### P1-T09: Backend i18n Grundgerüst (MessageSource)
- **Status:** 📋
- **Aufwand:** 1 Tag
- **Abhängigkeiten:** P1-T01
- **Beschreibung:** Spring `MessageSource` mit UTF-8-Resource-Bundles konfigurieren. `LocaleResolver` liest `Accept-Language`-Header (BCP 47), Fallback `en`, Default `de`. Validierungsfehler via `@Valid` + `MessageSource` übersetzt. Siehe [`ADR/007`](ADR/007-internationalization-strategy.md).
- **Akzeptanzkriterien:**
  - [ ] `src/main/resources/i18n/messages_de.properties` und `_en.properties` existieren
  - [ ] `src/main/resources/i18n/validation_de.properties` und `_en.properties` existieren
  - [ ] `LocaleResolver` extrahiert BCP 47-Tag aus Header, fallback `en`
  - [ ] Parametrisierter Test: gleicher Validierungsfehler liefert DE-Text bei `Accept-Language: de` und EN-Text bei `Accept-Language: en`
  - [ ] Bei unbekannter Locale (z. B. `fr` in Phase 1) → Fallback auf `en`
- **Dateien:** `src/main/java/com/lwe/i18n/I18nConfig.java`, `LocaleResolver.java`, `src/main/resources/i18n/messages_de.properties`, `messages_en.properties`, `validation_de.properties`, `validation_en.properties`

---

## Phase 2: Die Logik

### P2-T01: Rule-Engine Interface und Implementierung
- **Status:** 📋
- **Aufwand:** 3 Tage
- **Abhängigkeiten:** P1-T05
- **Beschreibung:** `RuleEngine`-Interface gruntleg. Implementierung `d20` (1W20 + Mod vs. Target) und `pool` (2W6 mit Erfolgsstufen). Liest Konfiguration aus `game_systems.rules_json`. Dice-Expression-Parser (z. B. `1d20+stärke`, `2d6+intelligenz`).
- **Akzeptanzkriterien:**
  - [ ] `RuleEngine.executeProbe(ProbeRequest)` returns `ProbeResult` mit Würfel/Erfolg
  - [ ] Ausdrücke mit Attributreferenz funktionieren
  - [ ] Beide Beispielwerke durchtesten
  - [ ] Unit-Tests für Expression-Parser, kritisch für Edge-Cases (negativer Mod, leere Attributreferenz)
- **Dateien:** `RuleEngine.java`, `D20RuleEngine.java`, `PoolRuleEngine.java`, `DiceExpressionParser.java`, `ProbeRequest.java`, `ProbeResult.java`

### P2-T02: Probe-Service und REST `/api/rolls`
- **Status:** 📋
- **Aufwand:** 1 Tag
- **Abhängigkeiten:** P2-T01, P1-T06
- **Beschreibung:** `POST /api/rolls` nimmt `ProbeRequest` (Char-Id, Skill-Name, Modifier) entgegen, ruft Rule-Engine, loggt in `world_events` (`PROBE_ROLLED`), broadcastet Ergebnis via WS `/topic/world/{id}`.
- **Akzeptanzkriterien:**
  - [ ] Endpunkt liefert `ProbeResult` mit allen Einzelwürfeln
  - [ ] Event wird in `world_events` mit `payload_json` gespeichert
  - [ ] WS-Subscriber empfängt Event
  - [ ] Ungültige Char-Id/Skill → `400`
- **Dateien:** `RollService.java`, `RollController.java`

### P2-T03: Kampf-Modul (Turn-basiert)
- **Status:** 📋
- **Aufwand:** 4 Tage
- **Abhängigkeiten:** P2-T02
- **Beschreibung:** Initiative-Reihenfolge, Turn-Verwaltung, Aktionen (Angriff, Verteidigung, Zauber vorbereitet). Validierung: Reichweite, AP-Kosten, Sichtlinie (vereinfacht: nur Grid-Distanz). Endpunkte `POST /api/combat/{sessionId}/{start|nextTurn|action|end}`.
- **Akzeptenzkriterien:**
  - [ ] Initiative basierend auf Regelwerk (`combat.initiative`-Expression)
  - [ ] Aktionen werden per Rule-Engine validiert
  - [ ] Schaden via `combat.damage`-Expression berechnet
  - [ ] WS-Updates zu `/topic/combat/{id}` (Teilnehmerliste, aktueller Turn)
  - [ ] Integrationstest: 3 Charaktere, komplette Kampfrunde durchgespielt
- **Dateien:** `CombatSession.java`, `CombatParticipant.java`, `CombatService.java`, `CombatController.java`, `ActionRequest.java`, `db/migration/V003__combat.sql`

### P2-T04: Inventar-System + Equip-Berechnung
- **Status:** 📋
- **Aufwand:** 2 Tage
- **Abhängigkeiten:** P1-T06
- **Beschreibung:** Items konfigurieren (Waffen, Rüstungen, Verbrauchsgüter). Inventar eines Charakters in `entities.inventory_json`. Endpunkte `/api/inventory/{charId}/{add|remove|equip|unequip}`. Rüstungsklasse / Bonus automatisch aus Equip berechnet.
- **Akzeptanzkriterien:**
  - [ ] `GET /api/entities/{id}/inventory` returns vollständiges Inventar
  - [ ] Equip toggelt `equipped`-Flag und aktualisiert ` armor_class` in `attributes_json`
  - [ ] Stack-Artikel aggregieren `quantity`
  - [ ] Gewicht / Gewichtslimit (optional) validiert
- **Dateien:** `Item.java`, `InventoryService.java`, `InventoryController.java`, `db/migration/V004__items.sql`

### P2-T05: Abenteuer-Struktur (Node-basiert)
- **Status:** 📋
- **Aufwand:** 3 Tage
- **Abhängigkeiten:** P1-T06
- **Beschreibung:** Datenmodell für Abenteuer. `adventures`, `adventure_nodes`, `node_choices`. Jeder Node hat Text/Bilder/Multiple-Choice/Skill-Check. Skill-Check integriert über Rule-Engine. Endpunkte `POST /api/adventures`, `POST /api/adventures/{id}/start`, `POST /api/adventures/{id}/advance`.
- **Akzeptanzkriterien:**
  - [ ] Lineare Adventure-Skizze (3 Nodes, 2 Choices) über API spielbar
  - [ ] Skill-Check in einem Node triggert Rule-Engine
  - [ ] Pfad im Adventure wird in `world_events` protokolliert
  - [ ] Anfangs- und Endnodes sind explizit markiert
- **Dateien:** `Adventure.java`, `AdventureNode.java`, `NodeChoice.java`, `AdventureService.java`, `AdventureController.java`, `db/migration/V005__adventures.sql`

### P2-T06: Choice-Auswertung + Skill-Check in Adventures
- **Status:** 📋
- **Aufwand:** 2 Tage
- **Abhängigkeiten:** P2-T05, P2-T02
- **Beschreibung:** Choice wird ausgewertet: bedingte nächste Node (via Bedingung wie Skill-Check-Erfolg) oder direkte Skill-Check-Integration. Adventure-State pro Charakter in `adventure_progress` (Pseudonym: neue Tabelle).
- **Akzeptanzkriterien:**
  - [ ] Skill-Check-Choice triggert `/api/rolls` intern
  - [ ] Erfolg/Misserfolg verzweigen zu unterschiedlichen Nodes
  - [ ] Adventures können pausiert und wieder aufgenommen werden
  - [ ] State enthält `current_node_id`, `visited_nodes[]`
- **Dateien:** `AdventureProgress.java`, `AdventureProgressRepository.java`

### P2-T07: Event-Log-Architektur (`world_events`)
- **Status:** 📋
- **Aufwand:** 1 Tag
- **Abhängigkeiten:** P1-T03
- **Beschreibung:** Zentraler Service `WorldEventService`, der über allen anderen Services liegt. Jede spiel-relevante Aktion erzeugt ein `WorldEvent`. Idempotenz über `event_hash` (verhinderung von Duplikaten).
- **Akzeptanzkriterien:**
  - [ ] `WorldEventService.publish(WorldEvent)` speichert + broadcastet
  - [ ] Andere Services nutzen ausschließlich diesen Service
  - [ ] Typisierung der Events via Enum (`EventType`)
  - [ ] Retention-Policy dokumentiert (Vorbereitung für Phase 5)
- **Dateien:** `WorldEventService.java`, `EventType.java`, `WorldEvent.java`

### P2-T08: Integrationstests für Regel-Engine
- **Status:** 📋
- **Aufwand:** 1 Tag
- **Abhängigkeiten:** P2-T01 … P2-T07
- **Beschreibung:** End-to-End Integrationstests kombiniert: `Game-System hochladen → Welt erstellen → Charakter anlegen → Probe würfeln → Kampf → event-log prüfen`.
- **Akzeptanzkriterien:**
  - [ ] Test suite läuft via `mvn test`
  - [ ] Test deckt beide Beispielwerke ab
  - [ ] M2 Trigger: Test suite grün
- **Dateien:** `src/test/java/com/lwe/integration/RuleEngineFlowIT.java`

### P2-T09: Time Engine (Weltzeit & Kalender)
- **Status:** 📋
- **Aufwand:** 3 Tage
- **Abhängigkeiten:** P1-T06, P2-T07
- **Beschreibung:** Implementiert `WorldTimeService` laut [`ADR/009`](ADR/009-world-time-calendar-system.md). Spalten `current_game_time` und `last_tick_at` in `worlds` via Flyway `V007__world_time.sql`. Drei Modi (automatic/manual/hybrid). Scheduled Task advanced automatisch tickende Welten. Events `TIME_ADVANCED`, `TIME_PAUSED`, `TIME_RESUMED`, `TIME_MODE_CHANGED`. Endpunkte `GET/POST /api/v1/worlds/{id}/time/*`.
- **Akzeptanzkriterien:**
  - [ ] `current_game_time` und `last_tick_at` in `worlds`-Tabelle (V007 Migration)
  - [ ] `WorldTimeService` advanced automatisch alle Welten mit `mode != manual` und `!paused`
  - [ ] DM kann `/time/advance`, `/time/set`, `/time/pause`, `/time/resume` aufrufen („Tag ist vorbei" via `advance` mit `by: "1 day"`)
  - [ ] Owner kann `/time/mode` wechseln
  - [ ] `day_phase` (`dawn`/`day`/`dusk`/`night`) korrekt aus `current_game_time` + `day_starts_at_hour` abgeleitet
  - [ ] `TIME_ADVANCED` Event in `world_events` gepublished + via WS `/topic/world/{id}/time` broadcastet
  - [ ] Bot-Filter für schnelle Ticks (< 30 min) verhindert Event-Flooding (Konfiguration in Bot-Settings)
  - [ ] Unit-Tests für `WorldTimeService`, `DayPhaseCalculator`; Integration-Test für Endpunkte (alle 3 Modi)
- **Dateien:** `WorldTimeService.java`, `TimeController.java`, `DayPhaseCalculator.java`, `TimeSettings.java`, `db/migration/V007__world_time.sql`

---

## Phase 3: Das Interface

### P3-T01: Vite + React + TypeScript + pnpm Setup
- **Status:** 📋
- **Aufwand:** 0,5 Tage
- **Abhängigkeiten:** —
- **Beschreibung:** Frontend-Projekt in `frontend/`. Vite + React + TS. Dependencies: zustand, @stomp/stompjs, react-router-dom, axios, **i18next + react-i18next**, tailwindcss, framer-motion, lucide-react, @dnd-kit/core, @pixi/react, pixi.js.
- **Akzeptanzkriterien:**
  - [ ] `pnpm dev` startet unter `http://localhost:5173`
  - [ ] Proxy zur Backend-WS konfiguriert
  - [ ] ESLint + Prettier + Type-Check via `pnpm check` kombiniert
  - [ ] i18next + react-i18next installiert + initialisiert (Default `de`, Fallback `en`)
  - [ ] Ordnerstruktur laut [`UI-UX.md`](UI-UX.md)
- **Dateien:** `frontend/package.json`, `frontend/vite.config.ts`, `frontend/tsconfig.json`, `frontend/src/App.tsx`, `frontend/src/i18n/index.ts`

### P3-T02: Zustand-Store + STOMP-Client Setup
- **Status:** 📋
- **Aufwand:** 1,5 Tage
- **Abhängigkeiten:** P3-T01
- **Beschreibung:** Zustand-Store für Auth, Weltdaten und i18n-Locale. STOMP-Client als Hook `useWorldSocket(worldId)`. Axios-Interceptor für JWT-Refresh-Token und `Accept-Language`-Header (aus authStore.locale). Locale-Lösung gemäß [`ADR/007`](ADR/007-internationalization-strategy.md).
- **Akzeptanzkriterien:**
  - [ ] `useAuthStore` verwaltet Token, User und Locale
  - [ ] `useWorldStore` cached aktuelle Welt und Token/Events
  - [ ] 401 → automatischer Refresh, dann Retry
  - [ ] Axios schickt `Accept-Language`-Header aus aktuellem Locale
  - [ ] Wiederaufbau der WS-Verbindung bei Drop
  - [ ] Locale-Auflösung: User-Setting → localStorage → navigator.language → Fallback `de`
- **Dateien:** `frontend/src/store/authStore.ts`, `frontend/src/store/worldStore.ts`, `frontend/src/api/client.ts`, `frontend/src/hooks/useWorldSocket.ts`, `frontend/src/i18n/locales/{de,en}/common.json`

### P3-T03: Auth-UI (Login/Register)
- **Status:** 📋
- **Aufwand:** 1 Tag
- **Abhängigkeiten:** P3-T02
- **Beschreibung:** Login- und Register-Seite. Form-Validation. Redirect zu `/worlds` bei Erfolg. Alle UI-Strings via `useTranslation('auth')` — keine hartkodierten Texte.
- **Akzeptanzkriterien:**
  - [ ] Login mit Email/Passwort
  - [ ] Fehlermeldungen angezeigt und via `errors.json` lokalisiert
  - [ ] Token in memory (nicht localStorage — siehe [`ADR/001`](ADR/001-frontend-react-vite.md))
  - [ ] Keyboard-navigierbar (Tab-Reihenfolge, Enter zum Submit)
  - [ ] Kontrast ≥ AA
  - [ ] Sprachumschalter (🌐) in TopBar funktioniert im Login-Screen
  - [ ] Keine hartkodierten Strings (alle via `t('key')`)
- **Dateien:** `frontend/src/pages/Login.tsx`, `frontend/src/pages/Register.tsx`, `frontend/src/components/auth/AuthForm.tsx`, `frontend/src/i18n/locales/{de,en}/auth.json`, `frontend/src/i18n/locales/{de,en}/errors.json`

### P3-T04: Dashboard + Welt-Management
- **Status:** 📋
- **Aufwand:** 2 Tage
- **Abhängigkeiten:** P3-T03
- **Beschreibung:** Dashboard „Meine Welten" mit CRUD. Welt erstellen/bearbeiten, Regelwerk auswählen, Einladungslink kopieren.
- **Akzeptanzkriterien:**
  - [ ] Liste aller Welten des Users
  - [ ] Erstellen-Modal mit Weltname + Regelwerk-Dropdown
  - [ ] Bearbeiten beschränkt auf `owner`
  - [ ] Snackbar-Feedback
- **Dateien:** `frontend/src/pages/Dashboard.tsx`, `frontend/src/pages/WorldEditor.tsx`

### P3-T05: Charakterbogen (dynamisch aus Regelwerk)
- **Status:** 📋
- **Aufwand:** 3 Tage
- **Abhängigkeiten:** P3-T04, P2-T01
- **Beschreibung:** Rendering des Bogens basiert auf `game_system.attributes_json` — dynamische Felder pro Welt. Würfel-Button pro Skill. WS-Antwort wird angezeigt.
- **Akzeptanzkriterien:**
  - [ ] Attribute aus `attributes_json` gerendert
  - [ ] Skill-Liste mit Referenz-Attribut
  - [ ] Würfel-Button pro Skill → `POST /api/rolls`
  - [ ] Wurf-Ergebnis Animation + persistentes Log
- **Dateien:** `frontend/src/pages/CharacterSheet.tsx`, `frontend/src/components/character/AttributeField.tsx`, `frontend/src/components/character/SkillList.tsx`

### P3-T06: Inventar-UI
- **Status:** 📋
- **Aufwand:** 2 Tage
- **Abhängigkeiten:** P3-T05, P2-T04
- **Beschreibung:** Drag-and-drop-Items zwischen Inventar und Equip-Slots. Live-Aktualisierung von `armor_class`.
- **Akzeptanzkriterien:**
  - [ ] Items können equip / unequip
  - [ ] Stack-Items aggregiert
  - [ ] Token pro Item (Icon-Support)
  - [ ] Tooltip mit Item-Details
- **Dateien:** `frontend/src/pages/Inventory.tsx`, `frontend/src/components/inventory/ItemCard.tsx`

### P3-T07: PixiJS-Canvas-Grundgerüst + Grid
- **Status:** 📋
- **Aufwand:** 2 Tage
- **Abhängigkeiten:** P3-T02
- **Beschreibung:** PixiJS-Application in `<MapCanvas>`. Konfigurierbares Grid (quadratisch/hex), Hintergrund-Textur, Zoom + Pan. Token werden via @pixi/react gerendert.
- **Akzeptanzkriterien:**
  - [ ] Grid in Größe n×m configurable per Welt
  - [ ] Mausrad zoomt (0, 2–4, 0)
  - [ ] Drag in leeren Bereich panned
  - [ ] PixiJS respektiert DPR (Retina)
  - [ ] 60 fps mit ≥ 100 Token
- **Dateien:** `frontend/src/components/map/MapCanvas.tsx`, `frontend/src/components/map/Grid.ts`, `frontend/src/components/map/usePixiApp.ts`

### P3-T08: Token-Management (Drag, Selection)
- **Status:** 📋
- **Aufwand:** 2 Tage
- **Abhängigkeiten:** P3-T07
- **Beschreibung:** Token können vom Sidebar-Favorite auf die Karte gezogen werden. Auf Karte dragbar. Selektion (single + rectangular multi). Position via WS zu allen Teilnehmern.
- **Akzeptanzkriterien:**
  - [ ] Drag von Karte-Token synchronisiert via WS in < 200 ms
  - [ ] Selection-Highlight sichtbar
  - [ ] Double-Click auf Token rotiert durch Icon-States
  - [ ] DM-only Tools (conditions, custom path) sichtbar für DM, versteckt für Spieler
- **Dateien:** `frontend/src/components/map/Token.tsx`, `frontend/src/components/map/TokenDragManager.ts`

### P3-T09: Fog of War (Canvas Compositing)
- **Status:** 📋
- **Aufwand:** 2 Tage
- **Abhängigkeiten:** P3-T08
- **Beschreibung:** Fog of War als separate PixiJS-Layer. DM hat Tools (freehand, polygon, revert). Spieler sehen nur visible mask. Mask-Updates asynchron via WS.
- **Akzeptanzkriterien:**
  - [ ] DM shielded/unshields Bereiche
  - [ ] Spieler sehen verwendungs Filter (Vue der Maske sichtbar)
  - [ ] Mask-Versionierung (undo/redo)
  - [ ] Persistenz in `maps.fog_state_json`
- **Dateien:** `frontend/src/components/map/FogOfWarLayer.ts`, `frontend/src/store/mapStore.ts`

### P3-T10: Chat + Wurf-Logs UI
- **Status:** 📋
- **Aufwand:** 2 Tage
- **Abhängigkeiten:** P3-T05
- **Beschreibung:** Chat mit Inline-Befehlen (`/r 1d20+5` für Wurf, `/me lacht` für Aktion). Wurf-Log als chronologische Liste. WS `~all` events gehen ein.
- **Akzeptanzkriterien:**
  - [ ] Chat-Nachrichten werden per WS gesendet und empfangen
  - [ ] `/r <expr>` triggert Wurf und zeigt animiertes Ergebnis
  - [ ] Wurf-Log mit Filter (Charakter, Zeitraum, Ergebnisspektrum) oben rechts mm
  - [ ] Markdown-Render (fett, italic) für Chat
- **Dateien:** `frontend/src/components/chat/ChatPanel.tsx`, `frontend/src/components/chat/RollLog.tsx`, `frontend/src/components/chat/DiceParser.ts`

### P3-T11: Integrationstest + M3 Meilenstein
- **Status:** 📋
- **Aufwand:** 1 Tag
- **Abhängigkeiten:** P3-T01 … P3-T10
- **Beschreibung:** End-to-End-Demo dokumentiert: Login → Welt erstellen → Charakter anlegen → Karte anzeigen → Token bewegen → Wurf → Chat. M3-Auslöser.
- **Akzeptanzkriterien:**
  - [ ] `docs/DEMO.md` dokumentiert den kompletten Flow
  - [ ] Demo läuft durch
- **Dateien:** `docs/DEMO.md`

### P3-T12: Frontend i18n-Durchgang + Sprachumschalter
- **Status:** 📋
- **Aufwand:** 2 Tage
- **Abhängigkeiten:** P3-T03 … P3-T10
- **Beschreibung:** Stell sicher, dass **alle** UI-Strings via `t('key')` referenziert werden, in allen Komponenten. Sprachumschalter (Globe-Icon in TopBar) verdrahtet: Persistenz in `users.locale` via `POST /api/users/me/preferences`, aktive Locale im authStore. Siehe [`ADR/007`](ADR/007-internationalization-strategy.md) und [`UI-UX.md`](UI-UX.md) Abschnitt 14.
- **Akzeptanzkriterien:**
  - [ ] grep-Check auf hartkodierte dt./engl. Strings in Komponenten ergibt 0 Funde
  - [ ] Umschalten zwischen DE und EN via 🌐 funktioniert live ohne Neuladen
  - [ ] Datum/Uhrzeit im Wurf-Log korrekt formatiert je Locale (`Intl.DateTimeFormat`)
  - [ ] Vitest prüft Konsistenz der Keys zwischen `de` und `en` (CI fails bei Inkonsistenz)
  - [ ] Sprachumschalter persistiert in `users.locale` (Reload behält Locale)
- **Dateien:** `frontend/src/i18n/locales/**`, `frontend/src/components/ui/LanguageSwitcher.tsx`, `frontend/tests/i18n/keys.test.ts`

---

## Phase 4: KI & Lebendigkeit

### P4-T01: Python/FastAPI Bot-Service Setup
- **Status:** 📋
- **Aufwand:** 1 Tag
- **Abhängigkeiten:** —
- **Beschreibung:** Python-Service in `ai-bot/`. FastAPI + Pydantic. Health-Endpunkt. Liest Config aus `.env` (Ollama-URL, Poll-Intervall). `Containerfile` + Eintrag in `compose.yml`. Build via `podman build`.
- **Akzeptanzkriterien:**
  - [ ] `uvicorn ai_bot.main:app --reload` startet
  - [ ] `GET /health` returns `200`
  - [ ] `podman build -t lwe-ai-bot ai-bot/` produziert OCI-Image
  - [ ] `podman compose up ai-bot` startet den Service
  - [ ] Konfiguration via Pydantic-Settings (=strikte .env-Validierung)
- **Dateien:** `ai-bot/pyproject.toml`, `ai-bot/src/ai_bot/main.py`, `ai-bot/src/ai_bot/config.py`, `ai-bot/Containerfile`

### P4-T02: Event-Polling (Bot → Server)
- **Status:** 📋
- **Aufwand:** 1 Tag
- **Abhängigkeiten:** P4-T01, P2-T07
- **Beschreibung:** Asyncio-Loop pollt `GET /api/worlds/{id}/events?since=<timestamp>` für jede aktive Welt. \`since\` ist `last_event_id`-basiert (nicht Zeit, um Race-Conditions zu vermeiden).
- **Akzeptanzkriterien:**
  - [ ] Polling passiert periodisch konfiguriert
  - [ ] Bottlenecks werden in Queue gepuffert
  - [ ] Reconnect bei Server-Fehler (Exponential Backoff)
  - [ ] Logs über welchen Event verarbeitet wird
- **Dateien:** `ai-bot/src/ai_bot/poller.py`, `ai-bot/src/ai_bot/state.py`

### P4-T03: NPC-Kontext-Loader
- **Status:** 📋
- **Aufwand:** 2 Tage
- **Abhängigkeiten:** P4-T02
- **Beschreibung:** Für relevantes Event, lade NPC + sein Umfeld (lokation, benachbarte Entities, letzte Ereignisse). Stellt `NPCContext` als Pydantic-Modell zur Verfügung.
- **Akzeptanzkriterien:**
  - [ ] Nur NPC berücksichtigt, die vom Event betroffen sind (z. B. in Radius um Event)
  - [ ] Kontext enthält NPCs Attribute, Persönlichkeit, Knowledge-Exzerpt
  - [ ] Token-Limitierung: Kontext ≤ 4k Token
- **Dateien:** `ai-bot/src/ai_bot/context_loader.py`, `ai-bot/src/ai_bot/models.py`

### P4-T04: Prompt-Templates pro NPC-Typ
- **Status:** 📋
- **Aufwand:** 2 Tage
- **Abhängigkeiten:** P4-T03
- **Beschreibung:** Jinja2-Templates für `aggressiv`, `neutral`, `vorsichtig` und `magier` (Beispiel). Output-Format zwingend JSON, validiert gegen Pydantic-Model.
- **Akzeptanzkriterien:**
  - [ ] Templates beachten Persönlichkeit + Ziele
  - [ ] JSON-Struktur: `{ action: "ATTACK|MOVE|SPEAK|IDLE", target_id: "..." | null, reasoning: "..." }`
  - [ ] Fallback bei invalidem LLM-Output (retry mit Hinweis-PR)
  - [ ] Tests mit Mock-LLM
- **Dateien:** `ai-bot/src/ai_bot/prompts/templates.py`, `ai-bot/src/ai_bot/models.py`

### P4-T05: Ollama-Integration
- **Status:** 📋
- **Aufwand:** 1 Tag
- **Abhängigkeiten:** P4-T04
- **Beschreibung:** Client für Ollama-REST-API (`/api/generate`). Model-Streaming deaktiviert (rein JSON-Response gewüäännscht). Abfangen von Fehlern (Modell nicht geladen, Ollama down).
- **Akzeptanzkriterien:**
  - [ ] Aufruf asynchron
  - [ ] Timeout 30 s und reduziert nicht erzwingbar
  - [ ] Erfolgslog pro Aufruf
  - [ ] Bei Ollama-down: Bot pausiert NPCs dieser Welt
- **Dateien:** `ai-bot/src/ai_bot/llm_client.py`

### P4-T06: NPC-Intent REST-Endpoint + Validator
- **Status:** 📋
- **Aufwand:** 3 Tage
- **Abhängigkeiten:** P4-T05, P2-T03
- **Beschreibung:** `POST /api/npc-intents` nimmt Intent-Anfragen vom Bot. Validator prüft gegen Spielregeln: Reichweite, Sicht, AP, Inventar-Verfügbarkeit. `npc_intents`-Eintrag wird persistiert.
- **Akzeptanzkriterien:**
  - [ ] Validation kaskadiert durch mehrere Filter (rule-engine, visibility, range)
  - [ ] Pendente Intents sichtbar unter `GET /api/npc-intents?worldId={id}&status=pending`
  - [ ] Bot darf nicht direkt in die Welt schreiben
  - [ ] Abgelehnter Intent enthält `rejection_reason`
- **Dateien:** `NpcIntent.java`, `NpcIntentService.java`, `IntentValidator.java`, `NpcIntentController.java`

### P4-T07: Human-Fallback im DM-Interface
- **Status:** 📋
- **Aufwand:** 2 Tage
- **Abhängigkeiten:** P4-T06, P3-T10
- **Beschreibung:** DM sieht pending intents in einem Panel in der UI. Approve/Reject. `suggest`-Modus: intents werden nur ausgeführt, wenn DM approved.
- **Akzeptanzkriterien:**
  - [ ] Panel für DM sichtbar, Spieler sehen es nicht
  - [ ] Approve triggert Ausführung
  - [ ] Reject abgelehnter Intent geloggt mit Rejector
  - [ ] Auto-Approve-Modus (`autonom`) zeigt intents nur retrospektiv
- **Dateien:** `frontend/src/components/dm/NpcIntentQueue.tsx`, `frontend/src/store/intentStore.ts`

### P4-T08: M4 Meilenstein-Demo
- **Status:** 📋
- **Aufwand:** 1 Tag
- **Abhängigkeiten:** P4-T01 … P4-T07
- **Beschreibung:** Demo-Skript: Spieler entzündet Lagerfeuer → NPC in NPC-Properties `aggressiv` ist in Radius → KI generiert `ATTACK`-Intent → Validator approved → NPC greift an. DM sieht es im Log.
- **Akzeptanzkriterien:**
  - [ ] Szenario läuft durch (lokalcaster)
  - [ ] Demo-Video dokumentiert
  - [ ] M4 trigger dokumentiert
- **Dateien:** `docs/LIVING-WORLD-DEMO.md`

---

## Phase 5: SaaS & Polishing

### P5-T01: Multi-Tenancy Isolation (Row-Level Security)
- **Status:** 📋
- **Aufwand:** 3 Tage
- **Abhängigkeiten:** P4-T01 … P4-T07
- **Beschreibung:** RLS-Policies in Postgres. Jeder Request setzt `SET LOCAL app.tenant_id = {owner_id}`. Repository-Methoden prüfen implizit.
- **Akzeptanzkriterien:**
  - [ ] RLS für `worlds`, `entities`, `world_events`, `npc_intents` aktiv
  - [ ] Cross-Tenant-Query → `0 rows`
  - [ ] Tests mit 2 Usern, Welt des einen ist für anderen unsichtbar
  - [ ] Performance-Index auf `owner_id` / `tenant_id` gesetzt
- **Dateien:** `db/migration/V010__tenant_rls.sql`, `TenantInterceptor.java`

### P5-T02: Redis-Cache für Regelwerke + Welt-Status
- **Status:** 📋
- **Aufwand:** 2 Tage
- **Abhängigkeiten:** P5-T01
- **Beschreibung:** Spring Cache mit Redis. Game-System und warm-world-status cachen. Invalidation bei Update.
- **Akzeptanzkriterien:**
  - [ ] Cache-Hit-Rate≥ 90 % bei Regelwerk-Lookups
  - [ ] Schreib-Clear invalidiert korrekte Keys
  - [ ] Redis optional via `SPRING_CACHE_TYPE=none` deaktivierbar
- **Dateien:** `CacheConfig.java`, `compose.yml` (Redis-Service)

### P5-T03: Event-Archivierung
- **Status:** 📋
- **Aufwand:** 2 Tage
- **Abhängigkeiten:** P5-T01
- **Beschreibung:** Scheduled Job (Spring `@Scheduled`) archiviert Events älter als 30 Tage. Tabelle `world_events_archive_v{YYYY_MM}`. Lesend/rückholbar.
- **Akzeptanzkriterien:**
  - [ ] Job läuft Tag 1× und ist idempotent
  - [ ] Archiv-Queries via Service-API
  - [ ] `world_events` Haupttabelle bleibt performant
- **Dateien:** `EventArchiveJob.java`, `EventArchiveService.java`, `db/migration/V011__events_archive.sql`

### P5-T04: Admin-Dashboard Backend
- **Status:** 📋
- **Aufwand:** 3 Tage
- **Abhängigkeiten:** P5-T01
- **Beschreibung:** Endpunkte für Admin: User-Liste, Bot-Status (pro Welt), System-Metriken, Regelwerk-Uploads verwalten.
- **Akzeptanzkriterien:**
  - [ ] Nur Rolle `ADMIN` hat Zugriff
  - [ ] Endpunkte dokumentiert in [`API.md`](API.md)
  - [ ] Audit-Log aller Admin-Aktionen
- **Dateien:** `AdminController.java`, `AdminUserService.java`

### P5-T05: Admin-Dashboard Frontend
- **Status:** 📋
- **Aufwand:** 3 Tage
- **Abhängigkeiten:** P5-T04
- **Beschreibung:** React-Seiten für Admin-Views. Routing ` /admin/*` mit Role-Guard.
- **Akzeptanzkriterien:**
  - [ ] User-Übersicht mit Statistik pro Welt
  - [ ] Bot-Status pro Welt als Heatmap
  - [ ] Admin-Aktionen (Deaktivieren, Reset Bot) vorhanden
- **Dateien:** `frontend/src/pages/admin/*`

### P5-T06: Production Containerfile + Nginx-Setup
- **Status:** 📋
- **Aufwand:** 2 Tage
- **Abhängigkeiten:** P3-T01, P4-T01
- **Beschreibung:** Multi-Stage `Containerfile` für Backend, Frontend und Bot (gelesen von `podman build`). Nginx Reverse-Proxy liefert Frontend statisch aus und proxyt API/WS ans Backend.
- **Akzeptanzkriterien:**
  - [ ] `podman compose -f compose.prod.yml up` läuft komplett
  - [ ] Health-Checks für alle Services
  - [ ] Letsencrypt-Integration dokumentiert
  - [ ] Prod-Image ≤ 300 MB
- **Dateien:** `Containerfile` (Backend), `frontend/Containerfile`, `ai-bot/Containerfile`, `nginx/nginx.conf`, `compose.prod.yml`

### P5-T07: CI/CD (GitHub Actions)
- **Status:** 📋
- **Aufwand:** 2 Tage
- **Abhängigkeiten:** P5-T06
- **Beschreibung:** Workflows: auf PR → Build + Test (Backend, Frontend, Bot). Auf `main` → Podman-Build + Push zu OCI-Registry (z. B. quay.io). Manuelle Deploy-Stufen.
- **Akzeptanzkriterien:**
  - [ ] PRs failen bei roten Tests
  - [ ] Main-Build pusht Images via `podman push`
  - [ ] Manuelle Deploy-Workflows pro Umgebung (dev, staging, prod)
  - [ ] Secrets via GitHub Actions Secrets
  - [ ] i18n-Key-Konsistenz-Check (DE ↔ EN) als Gate
- **Dateien:** `.github/workflows/{ci,deploy}.yml`

### P5-T08: M5 Meilenstein-Demo (Invited Third Party)
- **Status:** 📋
- **Aufwand:** 1 Tag
- **Abhängigkeiten:** P5-T01 … P5-T07
- **Beschreibung:** Dritter testet vollständigen Onboarding-Ablauf: Register → Game-System hochladen → Welt erstellen → Freunde einladen → Session spielen.
- **Akzeptanzkriterien:**
  - [ ] Dokumentation für Endbenutzer (`docs/USER-GUIDE.md`)
  - [ ] Demo läuft durch
  - [ ] M5 trigger dokumentiert
- **Dateien:** `docs/USER-GUIDE.md`

---

## Statistik

| Phase | Tasks | Sum Aufwand |
|---|---|---|
| 1 | 9 | 10,0 Tage |
| 2 | 9 | 20,0 Tage |
| 3 | 12 | 21,0 Tage |
| 4 | 8 | 13 Tage |
| 5 | 8 | 18 Tage |
| **Summe** | **46** | **82,0 Tage** |

Mit Personalaufwand gerechnet. Bei ~20 effektiven Arbeitstagen/Monat entspricht das ~4,1 Monaten (vollzeit). Bei Nebenher-Betrieb ist dies entsprechend zu multiplizieren.

> i18n-spezifischer Mehraufwand ist in den obigen Zahlen bereits enthalten (P1-T09 i18n-Basis, P3-T12 Frontend-i18n-Durchgang, plus Akzeptanzkriterien in diversen Auth/UI-Tasks). Die Time Engine (P2-T09, 3 Tage) deckt [`ADR/009`](ADR/009-world-time-calendar-system.md) ab.