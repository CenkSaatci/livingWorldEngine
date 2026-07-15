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
- **Status:** ✅
- **Aufwand:** 1 Tag
- **Abhängigkeiten:** —
- **Erledigt:** 2026-07-13
- **Beschreibung:** Maven-Projekt mit Spring Boot 3.3.5, Java 21 LTS. Enthält alle Deps (Spring Web, JPA, Security, Validation, WebSocket, Flyway, PostgreSQL, Lombok, Actuator, json-schema-validator, jjwt, spring-dotenv).
- **Akzeptanzkriterien:**
  - [x] `mvn spring-boot:run` startet fehlerfrei mit Profil `dev`
  - [x] `/actuator/health` antwortet `200 UP`
  - [x] Package-Struktur `com.lwe.core`, `.api`, `.config`, `.security`, `.rules`, `.events`, `.ai`, `.combat`, `.time`, `.i18n` (Unterpakete `core/domain` + `core/repository` für Entities/Repositories)
  - [x] `application.yml` liest Werte aus Umgebungsvariablen (`${DB_HOST}` etc.)
  - [x] `application-dev.yml` aktiviert Hot-Reloading (LiveReload)
  - [x] XML-Encoding-Fix (`&amp;` statt `&` in pom.xml)
- **Bemerkungen:**
  - JDK 21 `javac` war nicht systemweit installiert → manuell in `~/.local/share/jdk21` abgelegt, `source scripts/setup.sh` vor `mvn` ausführen
  - Datenbank `lwe` auf `192.168.31.151:5432` musste via `psql` erstmalig angelegt werden
  - `com.lwe.domain` und `com.lwe.repository` sind Unterpakete von `com.lwe.core`
- **Dateien:** `pom.xml`, `src/main/java/com/lwe/LweApplication.java`, `src/main/resources/application.yml`, `src/main/resources/application-dev.yml`, `scripts/setup.sh`

### P1-T02: Podman Compose (PostgreSQL + pgAdmin)
- **Status:** ⏸️ **cancelled**
- **Aufwand:** —
- **Abhängigkeiten:** —
- **Beschreibung:** Entfällt. PostgreSQL läuft lokal per Docker (siehe `docker ps`), Portainer verwaltet es. PgAdmin ist bereits unter `http://localhost:5050` verfügbar. Kein eigenes `compose.yml` nötig.

### P1-T03: Flyway Grundgerüst + erste Migration
- **Status:** ✅
- **Aufwand:** 1 Tag
- **Abhängigkeiten:** P1-T01
- **Erledigt:** 2026-07-13
- **Beschreibung:** Flyway 10.3 konfiguriert und Schema `V001__initial.sql` angelegt. Enthält 7 Tabellen mit allen Constraints, Indizes und Audit-Spalten. `baseline-version: 0` gesetzt, um Konflikte zwischen Baseline und V001 zu vermeiden.
- **Akzeptanzkriterien:**
  - [x] `mvn test` läuft integrationstest gegen `192.168.31.151/lwe` — 4 Tests grün
  - [x] Alle 7 Tabellen laut [`DATA-MODEL.md`](DATA-MODEL.md) Phase-1: `users`, `game_systems`, `worlds`, `world_members`, `entities`, `world_events`, `npc_intents`
  - [x] Indizes: `idx_events_world_time`, `idx_intents_world_status`, GIN-Indizes auf `entities.attributes_json`/`metadata_json`, etc.
  - [x] Audit-Spalten `created_at`/`updated_at` auf allen Tabellen
  - [x] `baseline-version: 0` eingestellt (Flyway-Version-1-Konfliktvermeidung)
- **Bemerkungen:**
  - Flyway 10.x in Spring Boot 3.3.5 unterstützt PostgreSQL 18.3, zeigt lediglich Warnung (nicht kritisch)
  - Der Flyway-Integrationstest von V001 initial erzeugt global `flyway_schema_history`, was bei parallelen Tests kollidiert — spätere P2-T08 führt Testcontainers ein
- **Dateien:** `src/main/resources/db/migration/V001__initial.sql`, `src/main/resources/application.yml` (flyway.baseline-version)

### P1-T04: User-Tabelle + JWT-Authentifizierung
- **Status:** ✅
- **Aufwand:** 2 Tage
- **Abhängigkeiten:** P1-T03
- **Erledigt:** 2026-07-13
- **Beschreibung:** Spring Security + JWT komplett. Endpunkte `POST /api/v1/auth/{register|login|refresh|service-login}`. Passwort-Hash via BCrypt (12 Rounds). Rollen `USER`, `ADMIN`, `BOT`. Refresh-Token-Rotation mit DB-Persistenz. Rate-Limiting (5 Fehlversuche/Minute IP-basiert) für Login.
- **Akzeptanzkriterien:**
  - [x] `POST /api/v1/auth/register` erstellt User mit BCrypt-Hash und `Accept-Language`→`users.locale`
  - [x] `POST /api/v1/auth/login` liefert JWT (24 h) + Refresh-Token (7 d)
  - [x] `POST /api/v1/auth/refresh` rotiert Token (altes widerrufen, neues persistiert)
  - [x] Geschützte Routen ohne Token → `401 {"error":{"code":"AUTH_TOKEN_INVALID",...}}`
  - [x] Unit-Tests: `JwtServiceTest` (6 Tests), `AuthServiceTest` (7 Tests) — beide Mockito-isoliert
  - [x] Integration-Tests: `DatabaseMigrationTest` prüft V002-Autoplay
  - [x] E2E-Verifikation: Register 201 + Login 200 + Health UP
- **Bemerkungen:**
  - `JwtService` erzeugt und validiert JWT via jjwt 0.12.x mit HMAC-SHA-256 (32+ Zeichen Secret)
  - `jti` (JWT ID) = UUID.randomUUID() → jedes Token ist garantiert einzigartig
  - Refresh-Token wird als SHA-256 Hash in `refresh_tokens` persistiert (Token selbst nie im Klartext)
  - `LoginRateLimiter` blockiert IPs nach 5 Fehlversuchen für 60 Sekunden
  - `GlobalExceptionHandler` einheitliches Fehlerformat über alle Endpunkte
  - Service-Login für AI-Bot via `/api/v1/auth/service-login` vorbereitet (Role `BOT`)
- **Dateien:** `src/main/java/com/lwe/security/*`, `src/main/java/com/lwe/core/domain/User.java`, `src/main/java/com/lwe/core/domain/RefreshToken.java`, `src/main/java/com/lwe/core/repository/*`, `src/main/java/com/lwe/core/service/AuthService.java`, `src/main/java/com/lwe/api/UserController.java`, `src/main/java/com/lwe/api/GlobalExceptionHandler.java`, `src/main/resources/db/migration/V002__auth.sql`

### P1-T05: Game-System Repository + JSON-Schema-Validator
- **Status:** ✅
- **Aufwand:** 2 Tage
- **Abhängigkeiten:** P1-T03, P1-T04
- **Erledigt:** 2026-07-13
- **Beschreibung:** JPA Entity `GameSystem` + `GameSystemRepository` + `GameSystemService` + `GameSystemController` mit CRUD und JSON-Schema-Validierung via `com.networknt:json-schema-validator`. Endpunkte `POST /api/v1/game-systems`, `GET /api/v1/game-systems/{id}`, `POST /api/v1/game-systems/{id}/validate`.
- **Akzeptanzkriterien:**
  - [x] `POST /api/v1/game-systems` persistiert nur nach erfolgreicher Validierung; invalide JSON/fehlende Felder → 400 + strukturierte Fehler
  - [x] `POST /api/v1/game-systems/{id}/validate` returns `{valid:true/false, errors:[...]}`
  - [x] 2 Test-Fixtures: `src/test/resources/rules/d20lite.json` + `twodicepool.json` (beide schema-konform)
  - [x] `RuleSchemaValidatorTest` lädt beide Fixtures und prüft Validität, plus invalide JSON-Cases
  - [x] `GameSystemServiceTest` prüft CRUD-Logik mit gemocktem Repository (4 Tests)
  - [x] GlobalExceptionHandler `GAME_SYSTEM_SCHEMA_INVALID` + `GAME_SYSTEM_NOT_FOUND`
- **Emittierte Komponenten:** `GameSystem` (JPA), `GameSystemRepository`, `GameSystemService`, `GameSystemController`, `RuleSchemaValidator`
- **Dateien:** `src/main/java/com/lwe/core/domain/GameSystem.java`, `com/lwe/core/repository/GameSystemRepository.java`, `com/lwe/core/service/GameSystemService.java`, `com/lwe/api/GameSystemController.java`, `com/lwe/rules/RuleSchemaValidator.java`, `src/test/resources/rules/{d20lite,twodicepool}.json`, `src/test/java/com/lwe/rules/RuleSchemaValidatorTest.java`, `src/test/java/com/lwe/core/service/GameSystemServiceTest.java`

### P1-T06: World + Entity Repository + REST-Endpoints
- **Status:** ✅
- **Aufwand:** 2 Tage
- **Abhängigkeiten:** P1-T05
- **Erledigt:** 2026-07-13
- **Beschreibung:** CRUD-Endpunkte für `worlds` und `world_members` mit Permission-Checks (Owner-only für Update/Delete, Cross-User Access Denied). Soft-Delete (`active`-Flag) für `worlds` + `entities`. V003-Migration.
- **Akzeptanzkriterien:**
  - [x] `POST /api/v1/worlds` erstellt Welt mit `owner_id` aus JWT + optionalem `game_system_id`
  - [x] `GET /api/v1/worlds` listet nur aktive Welten des Users
  - [x] `GET /api/v1/worlds/{id}` → `WORLD_ACCESS_DENIED` bei fremder Welt
  - [x] Nur `owner_id` darf Welt bearbeiten/löschen, sonst `403` (Soft-Delete via `active=false`)
  - [x] `POST /api/v1/worlds/{id}/members` — Mitglied einladen (`WORLD_MEMBER_ALREADY` bei Duplikat)
  - [x] `WorldServiceTest` — 6 Unit-Tests für CRUD + Permission-Checks + Soft-Delete
  - [x] `V003__world_softdelete.sql` — `active`-Spalte für `worlds` + `entities`
- **Komponenten:** `WorldService` (+ Test), `WorldController`, `World` (Entity), `WorldMember` (Entity), `WorldRepository`, `WorldMemberRepository`, `V003__world_softdelete.sql`
- **Bemerkungen:**
  - `World.getById` prüft Owner vor Zugriff → `WORLD_ACCESS_DENIED` für Nicht-Owner
  - Delete = `setActive(false)` + `save()` — nie physisch gelöscht
  - GameSystem-Referenz wird auf Aktivität geprüft (`WORLD_GAME_SYSTEM_INACTIVE`)
- **Dateien:** `src/main/java/com/lwe/core/domain/World.java`, `com/lwe/core/domain/WorldMember.java`, `com/lwe/core/repository/WorldRepository.java`, `com/lwe/core/repository/WorldMemberRepository.java`, `com/lwe/core/service/WorldService.java`, `com/lwe/api/WorldController.java`, `src/main/resources/db/migration/V003__world_softdelete.sql`

### P1-T07: WebSocket-Konfiguration (STOMP) und Test-Topic
- **Status:** ✅
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
- **Status:** ✅
- **Aufwand:** 0,5 Tage
- **Abhängigkeiten:** P1-T01 … P1-T07
- **Erledigt:** 2026-07-13
- **Beschreibung:** `docs/SMOKE-TEST.md` dokumentiert 13-Step-End-to-End-Test der Phase-1-API (Health, Register, Login, Game-System-CRUD, World-CRUD, Permission-Checks, WS-Event, Soft-Delete).
- **Akzeptanzkriterien:**
  - [x] `docs/SMOKE-TEST.md` dokumentiert komplette Sequenz (13 Schritte)
  - [x] Smoke-Test läuft von Hand via curl/jq
  - [x] M1 Trigger: Health UP + Register 201 + Login 200 + World 201 + WS Event + Delete 204
- **Dateien:** `docs/SMOKE-TEST.md`

### P1-T09: Backend i18n Grundgerüst (MessageSource)
- **Status:** ✅
- **Aufwand:** 1 Tag
- **Abhängigkeiten:** P1-T01
- **Erledigt:** 2026-07-13
- **Beschreibung:** `AcceptHeaderLocaleResolver` in `I18nConfig.java` konfiguriert. Unterstützte Locales: `de` (Default), `en` (Fallback). `Accept-Language`-Header wird korrekt ausgelesen; nicht unterstützte Sprachen fallen auf Default zurück. Siehe [`ADR/007`](ADR/007-internationalization-strategy.md).
- **Akzeptanzkriterien:**
  - [x] `messages_de.properties` + `messages_en.properties` + `validation_de.properties` + `validation_en.properties` existieren in `src/main/resources/i18n/`
  - [x] `I18nConfig.localeResolver()` als `AcceptHeaderLocaleResolver` registriert
  - [x] `Accept-Language: de` → Locale `de`, `Accept-Language: en-US` → `en`
  - [x] Unbekannte Locale (z. B. `fr`) → Fallback `de` (Default)
  - [x] `I18nConfigTest` — 4 Unit-Tests für Locale-Resolution (DE, EN, Unknown, No-Header)
- **Dateien:** `src/main/java/com/lwe/i18n/I18nConfig.java`, `I18nConfig.java`, `I18nConfigTest.java`

---

## Phase 2: Die Logik

### P2-T01: Rule-Engine Interface und Implementierung
- **Status:** ✅
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
- **Status:** ✅
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
- **Status:** ✅
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
- **Status:** ✅
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
- **Status:** ✅
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
- **Status:** ✅
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
- **Status:** ✅
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
- **Status:** ✅
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
- **Status:** ✅
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
- **Status:** ✅
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
- **Status:** ✅
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
- **Status:** ✅
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
- **Status:** ✅
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
- **Status:** ✅
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
- **Status:** ✅
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
- **Status:** ✅
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
- **Status:** ✅
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
- **Status:** ✅
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
- **Status:** ✅
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
- **Status:** ✅
- **Status:** 📋
- **Aufwand:** 1 Tag
- **Abhängigkeiten:** P3-T01 … P3-T10
- **Beschreibung:** End-to-End-Demo dokumentiert: Login → Welt erstellen → Charakter anlegen → Karte anzeigen → Token bewegen → Wurf → Chat. M3-Auslöser.
- **Akzeptanzkriterien:**
  - [ ] `docs/DEMO.md` dokumentiert den kompletten Flow
  - [ ] Demo läuft durch
- **Dateien:** `docs/DEMO.md`

### P3-T12: Frontend i18n-Durchgang + Sprachumschalter
- **Status:** ✅
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
- **Status:** ✅
- **Status:** ✅
- **Aufwand:** 1 Tag
- **Abhängigkeiten:** —
- **Erledigt:** 2026-07-13
- **Beschreibung:** Python-Service in `ai-bot/`. FastAPI + Pydantic-Settings. Health-Endpunkt. LLM-Unterstützung für **Ollama** (eigene JSON-API) **und vLLM** (OpenAI-kompatibel `/v1/chat/completions`). `pyproject.toml` mit FastAPI, httpx, Jinja2. Konfiguration via `AI_BOT_*`-Env-Vars. EventPoller-Skeleton.
- **Akzeptanzkriterien:**
  - [x] `uvicorn ai_bot.main:app --reload` startet auf Port 8000
  - [x] `GET /health` returns `200` + Modus/LLM-Typ
  - [x] Abstrakter `LLMClient` mit `OllamaClient` + `VLLMClient` (OpenAI-compat)
  - [x] `Settings` via Pydantic-Settings mit Prefix `AI_BOT_` für strikte Env-Validierung
  - [x] `.env` + `.env.example` mit neuen `AI_BOT_*` Variablen aktualisiert
- **Dateien:** `ai-bot/pyproject.toml`, `ai-bot/src/ai_bot/{main,config,llm_client,api_client,poller}.py`

### P4-T02: Event-Polling (Bot → Server)
- **Status:** ✅
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
- **Status:** ✅
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
- **Status:** ✅
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
- **Status:** ✅
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
- **Status:** ✅
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
- **Status:** ✅
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
- **Status:** ✅
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

## Phase 6: Welt-Tiefe & Ereignis-Logs

### P6-T01: Entity-Event-Log (entity_events)
- **Status:** 📋
- **Aufwand:** 2 Tage
- **Abhängigkeiten:** P1-T03
- **Beschreibung:** Tabelle `entity_events` mit entity_type/entity_id-Discriminator. Ereignis-Log für Regionen, Orte und NPCs. `EntityEventService` zum Publishen + Abfragen. REST-Endpunkte `POST/GET /api/v1/entity-events`. Siehe [`WORLD-DEPTH.md`](WORLD-DEPTH.md).
- **Akzeptanzkriterien:**
  - [ ] Migration `V020__entity_events.sql`
  - [ ] JPA-Entity `EntityEvent`, Repository, Service
  - [ ] `POST /api/v1/entity-events` erzeugt Event (auth, DM/Bot)
  - [ ] `GET /api/v1/entity-events?entityType=location&entityId=X` filtert
  - [ ] Index auf `(entity_type, entity_id, created_at DESC)`
- **Dateien:** `db/migration/V020__entity_events.sql`, `EntityEvent.java`, `EntityEventRepository.java`, `EntityEventService.java`, `EntityEventController.java`

### P6-T02: Regionen-Datenmodell + CRUD
- **Status:** 📋
- **Aufwand:** 2 Tage
- **Abhängigkeiten:** P6-T01
- **Beschreibung:** Tabelle `regions` mit Geschichte, Gefahrenlevel, Klima, Ressourcen, Fraktionen. JPA-Entity, CRUD-Endpunkte. Regionen werden pro Welt angelegt.
- **Akzeptanzkriterien:**
  - [ ] Migration `V021__regions.sql`
  - [ ] CRUD: `POST/GET/PATCH /api/v1/worlds/{id}/regions`
  - [ ] Region hat name, description, history, danger_level, climate, resources, factions, position
- **Dateien:** `V021__regions.sql`, `Region.java`, `RegionController.java`

### P6-T03: Orte-Datenmodell + CRUD
- **Status:** 📋
- **Aufwand:** 2 Tage
- **Abhängigkeiten:** P6-T02
- **Beschreibung:** Tabelle `locations` mit Typ, Geschichte, Wohlstand, Dienstleistungen. JPA-Entity, CRUD-Endpunkte. Orte gehören zu Regionen.
- **Akzeptanzkriterien:**
  - [ ] Migration `V022__locations.sql`
  - [ ] CRUD: `POST/GET/PATCH /api/v1/worlds/{id}/regions/{rId}/locations`
  - [ ] Location hat type, services, wealth, factions, position
- **Dateien:** `V022__locations.sql`, `Location.java`, `LocationController.java`

### P6-T04: NPC-Ort-Zuweisung + Services
- **Status:** 📋
- **Aufwand:** 2 Tage
- **Abhängigkeiten:** P6-T03
- **Beschreibung:** NPCs können via `PATCH /entities/{id}` einem Ort zugewiesen werden (`metadata_json.location_id` + `occupation`). Endpunkt `GET /locations/{id}/npcs` listet NPCs am Ort. `GET /locations/{id}/services` listet verfügbare Dienste.
- **Akzeptanzkriterien:**
  - [ ] NPC-Metadaten um `occupation`, `location_id`, `schedule`, `services_offered` erweiterbar
  - [ ] `GET /locations/{id}/npcs` filtert nach location_id
  - [ ] `GET /locations/{id}/services` aggregiert services_offered aller NPCs
  - [ ] Schedule-Prüfung: ist NPC zu dieser Tageszeit verfügbar?
- **Dateien:** `EntityService.java` (erweitert), `LocationNpcController.java`

### P6-T05: Wirtschaft & Preise
- **Status:** 📋
- **Aufwand:** 2 Tage
- **Abhängigkeiten:** P6-T04
- **Beschreibung:** Preiskalkulation basierend auf Orts-Wohlstand + NPC-Preis-Modifier. `GET /locations/{id}/market` zeigt Items + Preise.
- **Akzeptanzkriterien:**
  - [ ] Preisformel: `basispreis × (1 + (wealth - 5) × 0.1) × npc.price_modifier`
  - [ ] Markt-Endpunkt gibt Items + aktuelle Preise zurück
  - [ ] Integration mit InventoryService (Kauf/Verkauf)
- **Dateien:** `EconomyService.java`, `MarketController.java`

### P6-T06: KI-Kontextaufbau Regionen
- **Status:** 📋
- **Aufwand:** 1 Tag
- **Abhängigkeiten:** P6-T01 … P6-T04
- **Beschreibung:** AI-Bot lädt beim Prompt-Bau Entity-Events für Region + Location + NPC. Erweiterung der Prompt-Templates um Orts- und Regions-Kontext.
- **Akzeptanzkriterien:**
  - [ ] Bot lädt letzte 5 Entity-Events pro Entity
  - [ ] Prompt-Templates (Jinja2) um Orts-Informationen ergänzt
  - [ ] NPC-Schedule wird im Prompt referenziert („NPC ist tagsüber in der Schmiede")
- **Dateien:** `ai-bot/src/ai_bot/context_loader.py`, `prompts/*.j2` (erweitert)

### P6-T07: Einfache Quest-Generierung
- **Status:** 📋
- **Aufwand:** 3 Tage
- **Abhängigkeiten:** P6-T06
- **Beschreibung:** KI generiert Quests basierend auf Regionen-Zustand + Events. Quest-Typen: Töte-X, Bringe-Y, Eskortiere-Z. Quests werden in neuer Tabelle `quests` persistiert und können von Spielern angenommen werden.
- **Akzeptanzkriterien:**
  - [ ] Tabelle `quests`: id, title, description, type, objectives JSONB, rewards JSONB, giver_id, status
  - [ ] KI generiert Quest-Vorschlag → DM approved → persistiert
  - [ ] Spieler kann Quest annehmen und fortschritt verfolgen
  - [ ] Quest-Abschluss erzeugt Entity-Event (NPC-Chronik)
- **Dateien:** `V030__quests.sql`, `Quest.java`, `QuestService.java`, `QuestController.java`

---

## Phase 7: Architektur & Code-Qualität (Post-Mortem-Analyse)

### P7-T01: ObjectMapper zentralisieren
- **Status:** ✅
- **Aufwand:** 1h
- **Beschreibung:** 19 ad-hoc `new ObjectMapper()` durch zentralen `@Bean` in `JacksonConfig` ersetzt.
- **Dateien:** `JacksonConfig.java`, 7 Services (nach und nach per DI)

### P7-T02: @Transactional-Lücken schließen
- **Status:** ✅
- **Aufwand:** 0,5h
- **Beschreibung:** `WorldTimeService.tickAllWorlds()` + `MemoryCleanupJob.decayMemories()` + `IntentExecutor.execute()` mit `@Transactional` versehen.

### P7-T03: Exception-Handler-Vollständigkeit
- **Status:** ✅
- **Aufwand:** 0,5h
- **Beschreibung:** `GameSessionService.SessionException` in `GlobalExceptionHandler` aufgenommen. `RuntimeException`-Würfe in `AdminController` + `TimeController` durch saubere Fehlerbehandlung ersetzt.

### P7-T04: Dead Code beseitigen
- **Status:** ✅
- **Aufwand:** 0,5h
- **Beschreibung:** Leere `ai/` + `combat/` Packages entfernt. `TestWsController` mit `@Profile("dev")` versehen. 7 tote Frontend-Komponenten entfernt. `@dnd-kit/utilities` aus Dependencies entfernt. `@types/three` in devDependencies verschoben.

### P7-T05: Bot-Infrastruktur
- **Status:** ✅
- **Aufwand:** 1h
- **Beschreibung:** `Containerfile` pip-Reihenfolge gefixt. `conftest.py` unused imports bereinigt. HTTP-Fehler-Logging in `api_client.py` (13 Catch-Blöcke). 6 neue Tests (weather, memories).

### P7-T06: Frontend-Performance
- **Status:** ✅
- **Aufwand:** 2h
- **Beschreibung:** GameView-Monolith → RightPanel extrahiert. ChatPanel `React.memo`. FactionPage `useMemo`. N+1 Weather → `Promise.all`. `useKeyboardShortcuts` → useRef-basiert (kein Re-Register). 3 Pages auf `useApiGet` umgestellt.

### P7-T07: UI/UX-Polish
- **Status:** ✅
- **Aufwand:** 2h
- **Beschreibung:** `ErrorBoundary` in App.tsx. StatusBar + DM-Time-Controls + Fog-Toggle. Keyboard-Shortcuts (Esc/B/M). Cursor-fokussierter Zoom. Service-Cards in LocationDetail. SettingsPage (Sprache, Dice-Mode). Accessibility: aria-labels (11 Buttons) + aria-live (Chat, StatusBar).

---

## Phase 8: NPC-Bewusstsein & Erweitertes Entity-Modell

### P8-T01: Entity-Felder (Backstory, Age, XP, Standing)
- **Status:** ✅
- **Aufwand:** 2h
- **Beschreibung:** `V061__entity_extended.sql` — `backstory`, `age`, `experience_level`, `social_standing` auf `entities`. Backend + API + Frontend NPC-Profil aktualisiert. AI-Bot-Prompts enthalten neue Felder.

### P8-T02: Entity Memories + Relationships
- **Status:** ✅
- **Aufwand:** 3h
- **Beschreibung:** `V062__entity_memories.sql` — `entity_memories` + `entity_relationships` Tabellen. `MemoryService`, `RelationshipService`, `EntitySocialController` (GET/POST). Bot leitet Memories aus Events ab (combat/disrespect/helped).

### P8-T03: Gossip-System
- **Status:** ✅
- **Aufwand:** 1h
- **Beschreibung:** NPCs mit starken Erinnerungen (|sentiment|≥2) teilen diese mit ~20% Wahrscheinlichkeit per SPEAK-Intent mit anderen NPCs am selben Ort.

### P8-T04: Memory-Drift + Cleanup
- **Status:** ✅
- **Aufwand:** 0,5h
- **Beschreibung:** `MemoryCleanupJob` — täglicher Scheduled-Job: Sentiment driftet um ±1/Tag, Null-Erinnerungen >30 Tage werden gelöscht.

---

## Phase 9: Emergente Diplomatie & Intent-Execution

### P9-T01: IntentExecutor (NPCs handeln)
- **Status:** ✅
- **Aufwand:** 2h
- **Beschreibung:** `IntentExecutor.java` — dispatcht approved Intents: MOVE (Positionsupdate), ATTACK (Schaden), SPEAK (Chat-Event), CHANGE_RELATION (Faction-Diplomatie).

### P9-T02: ai_mode-Routing
- **Status:** ✅
- **Aufwand:** 1h
- **Beschreibung:** `NpcIntentService.create()` wertet `worlds.settings_json.ai_mode` aus: `autonom` → sofort ausführen, `suggest` → DM-Queue, `off` → ablehnen.

### P9-T03: Target-Event-Handling (Bot)
- **Status:** ✅
- **Aufwand:** 1h
- **Beschreibung:** Bot reagiert nicht nur auf source_entity_id, sondern auch auf target_entity_id. Ermöglicht NPC-Reaktionen, wenn sie Ziel einer Aktion wurden.

---

## Phase 10: Campaign-Polish & UX-Vervollständigung

### P10-T01: Start Combat UI
- **Status:** ✅
- **Aufwand:** 1h
- **Beschreibung:** GameView erhält einen "Start Combat"-Button mit Entity-Auswahl (welche NPCs/PCs nehmen teil?). POST /api/v1/combat/start mit ausgewählten IDs + optionaler mapId. CombatPage lädt bestehenden Kampf-Zustand beim Mount. Neue Migration V081 für map_id-Spalte. Neuer GET /api/v1/maps/{mapId} Endpunkt. StartCombatModal mit Checkbox-Liste + Map-Dropdown.

### P10-T02: Entity-Übersicht (NPC/PC-Liste)
- **Status:** ✅
- **Aufwand:** 1h
- **Beschreibung:** Seite `/worlds/:id/entities` mit Liste aller NPCs und PCs einer Welt. Filter nach Typ (PC/NPC), Text-Suche, Klick → NpcViewPage. "Create Entity"-Button mit EntityCreateModal. Delete-Button mit Bestätigung.

### P10-T03: Welten-Clone/Export
- **Status:** 📋
- **Aufwand:** 2h
- **Beschreibung:** Backend: `POST /worlds/{id}/clone` erzeugt Kopie einer Welt (inkl. Regionen, Orte, NPCs, Fraktionen). Frontend: "Clone"-Button im WorldEditor. Export als JSON-Download.

### P10-T04: Email-Verifikation
- **Status:** 📋
- **Aufwand:** 2h
- **Beschreibung:** Bei Registrierung `email_verified_at = null` setzen. Verifikationstoken generieren und speichern. `POST /auth/verify-email` mit Token. Ungültige Email → resenden. Frontend: VerifyEmailPage.

### P10-T05: Dashboard Pagination
- **Status:** 📋
- **Aufwand:** 1h
- **Beschreibung:** Backend: `GET /worlds/accessible` mit `page`/`size`-Parametern. Frontend: "Load more"-Button oder Infinite-Scroll.

### P10-T06: Onboarding für neue User
- **Status:** 📋
- **Aufwand:** 2h
- **Beschreibung:** Erstanmelde-Flow: Willkommensseite → "Erstelle deine erste Welt" → Tutorial-Tooltips in GameView. Checkliste für erste Schritte.

### P10-T07: Map Background Upload persistieren
- **Status:** 📋
- **Aufwand:** 2h
- **Beschreibung:** Backend: FileUploadController mit MultipartFile → Speicherung auf Disk (später S3). `world_maps.image_url` zeigt auf gespeicherte Datei. Frontend: Upload-UI im MapEditor speichert tatsächlich.

### P10-T08: Mobile Responsiveness
- **Status:** 📋
- **Aufwand:** 4h
- **Beschreibung:** Sidebar/RightPanel klappen auf <768px automatisch zu. GameView layout passt sich an. Touch-Unterstützung für Token-Drag. Map Canvas minimale Höhe anpassen.

### P10-T09: Soundeffekte (optional)
- **Status:** 📋
- **Aufwand:** 2h
- **Beschreibung:** Würfelgeräusche beim Roll (CSS Dice + 3D). Chat-Nachricht-Ton. Kampf-Aktion-Ton. Umschaltbar in Settings.

### P10-T10: JSON-Editor Syntax-Highlighting
- **Status:** 📋
- **Aufwand:** 2h
- **Beschreibung:** Ersetze das reine `<textarea>` im JSON-Editor durch einen einfachen Code-Editor (CodeMirror oder Monaco Editor light). Zeigt Syntax-Fehler direkt an.

---

## Phase 11: Architektur & Infrastruktur

### P11-T01: SessionController konsolidieren
- **Status:** 📋
- **Aufwand:** 1h
- **Beschreibung:** Zwei Controller mit überlappenden Funktionen: `SessionController` (publiziert nur Events) und `GameSessionController` (persistiert Sessions). `SessionController` entfernen oder auf `GameSessionService` umleiten.

### P11-T02: RuleEngine Plugin-Registry
- **Status:** 📋
- **Aufwand:** 2h
- **Beschreibung:** Engine-Erkennung per `className.contains("pool")`/`"fudge"` ist fragil. Stattdessen: `Map<DiceSystem, RuleEngine>` via `@PostConstruct` in einer zentralen Registry registrieren. Neue Engines registrieren sich selbst via `@Component` + Interface.

### P11-T03: GameSystem-Caching
- **Status:** 📋
- **Aufwand:** 2h
- **Beschreibung:** Jeder Wurf (RollService, CombatService) lädt das GameSystem aus der DB → N+1 Problem. Cache per `@Cacheable` auf `gameSystemRepository.findById()`. Redis ist bereits in `compose.prod.yml` konfiguriert.

### P11-T04: Event-Archivierung testen + aktivieren
- **Status:** 📋
- **Aufwand:** 1h
- **Beschreibung:** `EventArchiveJob` läuft täglich um 03:00 UTC, aber es gibt keinen Test und kein Monitoring. Test schreiben + Logging ergänzen + manuell triggerbaren Endpunkt `POST /admin/events/archive`.

### P11-T05: API-Rate-Limiting pro Endpunkt
- **Status:** 📋
- **Aufwand:** 2h
- **Beschreibung:** Aktuell nur globales Limit (100/IP/min) + Login-Limit (5/IP/min). Per-Endpunkt-Limits für world-creation, combat-actions, und AI-bot-endpoints.

### P11-T06: Health-Check für Abhängigkeiten
- **Status:** 📋
- **Aufwand:** 1h
- **Beschreibung:** Spring Boot Actuator `/actuator/health` zeigt nur den Status der App an. Erweitern um DB-Connectivity, Redis-Ping, AI-Bot-Connectivity (optional). Custom HealthIndicator.

### P11-T07: ~72 Map.of() → DTOs
- **Status:** 📋
- **Aufwand:** 6h
- **Beschreibung:** Alle Controller ersetzen ad-hoc `Map.of()`-Responses durch dedizierte Response-DTOs/Records. Ermöglicht OpenAPI-Schema-Generierung und Type-Safety. Betrifft ~27 Controller.

### P11-T08: Frontend Komponenten-Tests
- **Status:** 📋
- **Aufwand:** 4h
- **Beschreibung:** Vitest + Testing Library für kritische Komponenten: AuthForm, EntityCreateModal, ChatPanel, ActionBar, StatusBar. Grundlegende Render-Tests + Interaktions-Tests.

---

## NOCH OFFEN (Architektur-Risiken)

| ID | Was | Aufwand | Priorität |
|---|---|---|---|
| ⭕ | **~72 `Map.of()`-Responses → DTOs** | ~6h | Niedrig |
| ⭕ | **Frontend Komponenten-Tests** (Vitest + Testing Library) | ~4h | Niedrig |
| ⭕ | **🚀 Deploy-Workflow** (von dir ans Ende gestellt) | 0,5h | Ganz ans Ende |

---

## Statistik

| Phase | Tasks | Sum Aufwand |
|---|---|---|
| 1 | 9 (1 cancelled) | 10,5 Tage |
| 2 | 9 | 20,0 Tage |
| 3 | 12 | 21,0 Tage |
| 4 | 8 | 13,0 Tage |
| 5 | 8 | 18,0 Tage |
| 6 | 7 | 14,0 Tage |
| 7 (Qualität) | 7 | 7,0 Tage |
| 8 (NPC) | 4 | 6,5 Tage |
| 9 (Diplomatie) | 3 | 4,0 Tage |
| 10 (Campaign-Polish) | 10 | 19,0 Tage |
| 11 (Architektur) | 8 | 19,0 Tage |
| **Summe** | **85 (1 cancelled)** | **152,0 Tage** |

Mit Personalaufwand gerechnet. Bei ~20 effektiven Arbeitstagen/Monat entspricht das ~6,65 Monaten (vollzeit). Bei Nebenher-Betrieb ist dies entsprechend zu multiplizieren. Zuzüglich offener Risiken (~10 Tage).