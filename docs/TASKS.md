# Tasks

> Master-Task-Liste. ID-Referenzen (z. B. `P2-T04`) können in Commits, PRs und Reviews verwendet werden.

## Aktueller Projektstand (2026-09-12)

- **P28 Engine-Bausteine** ✅ · **P29 Spielgefühl + Pakete** ✅ · **P23 Schadenstypen** ✅ (T05 optional) · **P30 Charakter-Wizard** ✅ · **P31 E2E-Ausbau** ✅
- **P33 Backlog-Abbau & Härtung** ✅ (T33-01…11: E2E-Zustände/Schadensart, Welt-PUBLIC, Member-Quota, Fork inkl. Quests/Adventures/Choices, System-Shares, Bot-Runtime, DM-Queue Bulk+WS, Adventure-Inject, ADR-013; Final-Audit + Re-Audit ohne offene HIGH/MEDIUM)
- **Tests:** Backend 420 (`mvn -B test`) · Frontend 169 (`npx vitest run`) · E2E 11 (`npm run test:e2e`) · ai-bot 50 · `tsc`/Build grün
- **Audits:** P28, P23/P29, P30 und ein finales Gesamt-Audit — alle HIGH/MEDIUM-Findings gefixt, Rest bewusst zurückgestellt (siehe Notizen unten)
- **P27-Status:** komplett ✅ (Shares/Welt-PUBLIC und Fork-Lücken via P33; Bot-Runtime via T33-06; Bulk/WS/E2E via T33-07/08)
- **Offen (bewusst):** P34 ✅ abgeschlossen · P14-Rest (Editor-E2E; Inject-Choice ✅) · E2E-Backlog T32-T03 (Fork-Unabhängigkeit) · `attackMalus` ohne Attack-Roll-Modell · Fate „+1/Tod abwenden" · Conditions-Aktionssperren · `baseValues` schema-only
- **Nächste Schritte:** DSA-Spieltest (Welt+System+Kampagne, 2 Spieler, Interaktionen, UX-Report), danach P14-Editor-E2E

## Verifikation Alt-Phasen (2026-09-12)

> Alle Phasen 1–22 und 24–27 wurden per Evidenz (Code/Tests/Migrationen) gegen den Tracker geprüft.

- **Vollständig umgesetzt:** P1 (T02 cancelled), P2, P8, P9, P12, P13, P15, P16, P17 (+C01–C05), P18 (T01–T04), P19, P20, P21, P24 (T04→P25-T06), P25, P26
- **Umgesetzt mit Teilständen:** P3 (T09 Fog-Persistenz, T11 DEMO.md, T12 Key-Test), P4 (T03 NPCContext-Modell, T05 Ollama-Pause, T08 Demo-Doku 📋), P5 (T02 Redis aktiv, T04 Admin-Audit-Log, T07 i18n-Gate, T08 M5-Nachweis), P6 (T04 Schedule, T05 Kauf/Verkauf, T06 Orts-Kontext, T07 Bot-Quests/„pending"), P7 (T01 ObjectMapper-Rest, T02 IntentExecutor-Transaktion), P10 (JSON-Export), P11 (T07 Map.of-Rest), Cleanup (C06 bewusst abweichend)
- **Weiterhin offen:** P14-T02–T04 (Editor/Play/Override-E2E + Inject-Choice), P22 (nur Konzept); P27 umgesetzt (Teilstände s. o.)
- **Tracker-Bug behoben:** doppelte `Status:`-Zeilen in P1–P4 entfernt (Karteileichen aus `818211e4`)
- **Offener Code-Bug (notiert, nicht P27):** `QuestLog` filtert Status „pending", den der DB-CHECK (`V030`) nicht zulässt

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
- **Dateien:** `backend/pom.xml`, `backend/backend/src/main/java/com/lwe/LweApplication.java`, `backend/backend/src/main/resources/application.yml`, `backend/backend/src/main/resources/application-dev.yml`, `scripts/setup.sh`

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
- **Dateien:** `backend/src/main/resources/db/migration/V001__initial.sql`, `backend/src/main/resources/application.yml` (flyway.baseline-version)

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
- **Dateien:** `backend/src/main/java/com/lwe/security/*`, `backend/src/main/java/com/lwe/core/domain/User.java`, `backend/src/main/java/com/lwe/core/domain/RefreshToken.java`, `backend/src/main/java/com/lwe/core/repository/*`, `backend/src/main/java/com/lwe/core/service/AuthService.java`, `backend/src/main/java/com/lwe/api/UserController.java`, `backend/src/main/java/com/lwe/api/GlobalExceptionHandler.java`, `backend/src/main/resources/db/migration/V002__auth.sql`

### P1-T05: Game-System Repository + JSON-Schema-Validator
- **Status:** ✅
- **Aufwand:** 2 Tage
- **Abhängigkeiten:** P1-T03, P1-T04
- **Erledigt:** 2026-07-13
- **Beschreibung:** JPA Entity `GameSystem` + `GameSystemRepository` + `GameSystemService` + `GameSystemController` mit CRUD und JSON-Schema-Validierung via `com.networknt:json-schema-validator`. Endpunkte `POST /api/v1/game-systems`, `GET /api/v1/game-systems/{id}`, `POST /api/v1/game-systems/{id}/validate`.
- **Akzeptanzkriterien:**
  - [x] `POST /api/v1/game-systems` persistiert nur nach erfolgreicher Validierung; invalide JSON/fehlende Felder → 400 + strukturierte Fehler
  - [x] `POST /api/v1/game-systems/{id}/validate` returns `{valid:true/false, errors:[...]}`
  - [x] 2 Test-Fixtures: `backend/src/test/resources/rules/d20lite.json` + `twodicepool.json` (beide schema-konform)
  - [x] `RuleSchemaValidatorTest` lädt beide Fixtures und prüft Validität, plus invalide JSON-Cases
  - [x] `GameSystemServiceTest` prüft CRUD-Logik mit gemocktem Repository (4 Tests)
  - [x] GlobalExceptionHandler `GAME_SYSTEM_SCHEMA_INVALID` + `GAME_SYSTEM_NOT_FOUND`
- **Emittierte Komponenten:** `GameSystem` (JPA), `GameSystemRepository`, `GameSystemService`, `GameSystemController`, `RuleSchemaValidator`
- **Dateien:** `backend/src/main/java/com/lwe/core/domain/GameSystem.java`, `com/lwe/core/repository/GameSystemRepository.java`, `com/lwe/core/service/GameSystemService.java`, `com/lwe/api/GameSystemController.java`, `com/lwe/rules/RuleSchemaValidator.java`, `backend/src/test/resources/rules/{d20lite,twodicepool}.json`, `backend/src/test/java/com/lwe/rules/RuleSchemaValidatorTest.java`, `backend/src/test/java/com/lwe/core/service/GameSystemServiceTest.java`

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
- **Dateien:** `backend/src/main/java/com/lwe/core/domain/World.java`, `com/lwe/core/domain/WorldMember.java`, `com/lwe/core/repository/WorldRepository.java`, `com/lwe/core/repository/WorldMemberRepository.java`, `com/lwe/core/service/WorldService.java`, `com/lwe/api/WorldController.java`, `backend/src/main/resources/db/migration/V003__world_softdelete.sql`

### P1-T07: WebSocket-Konfiguration (STOMP) und Test-Topic
- **Status:** ✅ (WebSocket-Config + Auth-Interceptor getestet; kein StompClient-IT — manueller Smoke TESTING §12)
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
  - [x] `messages_de.properties` + `messages_en.properties` + `validation_de.properties` + `validation_en.properties` existieren in `backend/src/main/resources/i18n/`
  - [x] `I18nConfig.localeResolver()` als `AcceptHeaderLocaleResolver` registriert
  - [x] `Accept-Language: de` → Locale `de`, `Accept-Language: en-US` → `en`
  - [x] Unbekannte Locale (z. B. `fr`) → Fallback `de` (Default)
  - [x] `I18nConfigTest` — 4 Unit-Tests für Locale-Resolution (DE, EN, Unknown, No-Header)
- **Dateien:** `backend/src/main/java/com/lwe/i18n/I18nConfig.java`, `I18nConfig.java`, `I18nConfigTest.java`

---

## Phase 2: Die Logik

### P2-T01: Rule-Engine Interface und Implementierung
- **Status:** ✅
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
- **Aufwand:** 1 Tag
- **Abhängigkeiten:** P2-T01 … P2-T07
- **Beschreibung:** End-to-End Integrationstests kombiniert: `Game-System hochladen → Welt erstellen → Charakter anlegen → Probe würfeln → Kampf → event-log prüfen`.
- **Akzeptanzkriterien:**
  - [ ] Test suite läuft via `cd backend && mvn test`
  - [ ] Test deckt beide Beispielwerke ab
  - [ ] M2 Trigger: Test suite grün
- **Dateien:** `backend/src/test/java/com/lwe/integration/RuleEngineFlowIT.java`

### P2-T09: Time Engine (Weltzeit & Kalender)
- **Status:** ✅ (Abweichung: Spalten in V001 statt V007, DayPhase als inneres Enum statt Calculator)
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
- **Status:** ✅ (Teilstand: Fog-Overlay; Persistenz/Polygon/Undo offen → A03)
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
- **Status:** ✅ (Teilstand: Flow läuft; `docs/DEMO.md` fehlt)
- **Aufwand:** 1 Tag
- **Abhängigkeiten:** P3-T01 … P3-T10
- **Beschreibung:** End-to-End-Demo dokumentiert: Login → Welt erstellen → Charakter anlegen → Karte anzeigen → Token bewegen → Wurf → Chat. M3-Auslöser.
- **Akzeptanzkriterien:**
  - [ ] `docs/DEMO.md` dokumentiert den kompletten Flow
  - [ ] Demo läuft durch
- **Dateien:** `docs/DEMO.md`

### P3-T12: Frontend i18n-Durchgang + Sprachumschalter
- **Status:** ✅ (Teilstand: i18n-Key-Konsistenztest fehlt; Sprachschalter in Settings)
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
- **Status:** ✅ (Teilstand: Kontext inline im Poller, kein NPCContext-Modell)
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
- **Status:** ✅ (Teilstand: kein Pausieren der Welt bei Ollama-down)
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
- **Status:** 📋 (Demo-Doku `docs/LIVING-WORLD-DEMO.md` fehlt)
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
- **Status:** ✅ (V010 + TenantInterceptor; dedizierter Cross-Tenant-Test offen)
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
- **Status:** ✅ (Teilstand: CacheManager in-memory, Redis-Dependency ungenutzt)
- **Aufwand:** 2 Tage
- **Abhängigkeiten:** P5-T01
- **Beschreibung:** Spring Cache mit Redis. Game-System und warm-world-status cachen. Invalidation bei Update.
- **Akzeptanzkriterien:**
  - [ ] Cache-Hit-Rate≥ 90 % bei Regelwerk-Lookups
  - [ ] Schreib-Clear invalidiert korrekte Keys
  - [ ] Redis optional via `SPRING_CACHE_TYPE=none` deaktivierbar
- **Dateien:** `CacheConfig.java`, `compose.yml` (Redis-Service)

### P5-T03: Event-Archivierung
- **Status:** ✅ (EventArchiveJob + Test + Admin-Endpoint)
- **Aufwand:** 2 Tage
- **Abhängigkeiten:** P5-T01
- **Beschreibung:** Scheduled Job (Spring `@Scheduled`) archiviert Events älter als 30 Tage. Tabelle `world_events_archive_v{YYYY_MM}`. Lesend/rückholbar.
- **Akzeptanzkriterien:**
  - [ ] Job läuft Tag 1× und ist idempotent
  - [ ] Archiv-Queries via Service-API
  - [ ] `world_events` Haupttabelle bleibt performant
- **Dateien:** `EventArchiveJob.java`, `EventArchiveService.java`, `db/migration/V011__events_archive.sql`

### P5-T04: Admin-Dashboard Backend
- **Status:** ✅ (Teilstand: kein Audit-Log für Admin-Aktionen)
- **Aufwand:** 3 Tage
- **Abhängigkeiten:** P5-T01
- **Beschreibung:** Endpunkte für Admin: User-Liste, Bot-Status (pro Welt), System-Metriken, Regelwerk-Uploads verwalten.
- **Akzeptanzkriterien:**
  - [ ] Nur Rolle `ADMIN` hat Zugriff
  - [ ] Endpunkte dokumentiert in [`API.md`](API.md)
  - [ ] Audit-Log aller Admin-Aktionen
- **Dateien:** `AdminController.java`, `AdminUserService.java`

### P5-T05: Admin-Dashboard Frontend
- **Status:** ✅ (AdminPage + Rollen-Guard)
- **Aufwand:** 3 Tage
- **Abhängigkeiten:** P5-T04
- **Beschreibung:** React-Seiten für Admin-Views. Routing ` /admin/*` mit Role-Guard.
- **Akzeptanzkriterien:**
  - [ ] User-Übersicht mit Statistik pro Welt
  - [ ] Bot-Status pro Welt als Heatmap
  - [ ] Admin-Aktionen (Deaktivieren, Reset Bot) vorhanden
- **Dateien:** `frontend/src/pages/admin/*`

### P5-T06: Production Containerfile + Nginx-Setup
- **Status:** ✅ (Containerfiles + Nginx + compose.prod; TLS in DEPLOYMENT.md)
- **Aufwand:** 2 Tage
- **Abhängigkeiten:** P3-T01, P4-T01
- **Beschreibung:** Multi-Stage `Containerfile`s für Backend (`backend/`), Frontend und Bot (gelesen von `podman build`). Nginx Reverse-Proxy liefert Frontend statisch aus und proxyt API/WS ans Backend.
- **Akzeptanzkriterien:**
  - [ ] `podman compose -f compose.prod.yml up` läuft komplett
  - [ ] Health-Checks für alle Services
  - [ ] Letsencrypt-Integration dokumentiert
  - [ ] Prod-Image ≤ 300 MB
- **Dateien:** `backend/Containerfile`, `frontend/Containerfile`, `ai-bot/Containerfile`, `nginx/nginx.conf`, `compose.prod.yml`

### P5-T07: CI/CD (GitHub Actions)
- **Status:** ✅ (CI/Deploy-Workflows; i18n-Key-Gate fehlt)
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
- **Status:** ✅ (USER-GUIDE vorhanden; M5-Demo-Nachweis fehlt)
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
- **Status:** ✅ (V020 + EntityEventService/Controller + Test)
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
- **Status:** ✅ (V021 + RegionService + Test)
- **Aufwand:** 2 Tage
- **Abhängigkeiten:** P6-T01
- **Beschreibung:** Tabelle `regions` mit Geschichte, Gefahrenlevel, Klima, Ressourcen, Fraktionen. JPA-Entity, CRUD-Endpunkte. Regionen werden pro Welt angelegt.
- **Akzeptanzkriterien:**
  - [ ] Migration `V021__regions.sql`
  - [ ] CRUD: `POST/GET/PATCH /api/v1/worlds/{id}/regions`
  - [ ] Region hat name, description, history, danger_level, climate, resources, factions, position
- **Dateien:** `V021__regions.sql`, `Region.java`, `RegionController.java`

### P6-T03: Orte-Datenmodell + CRUD
- **Status:** ✅ (V022 + LocationService + Test)
- **Aufwand:** 2 Tage
- **Abhängigkeiten:** P6-T02
- **Beschreibung:** Tabelle `locations` mit Typ, Geschichte, Wohlstand, Dienstleistungen. JPA-Entity, CRUD-Endpunkte. Orte gehören zu Regionen.
- **Akzeptanzkriterien:**
  - [ ] Migration `V022__locations.sql`
  - [ ] CRUD: `POST/GET/PATCH /api/v1/worlds/{id}/regions/{rId}/locations`
  - [ ] Location hat type, services, wealth, factions, position
- **Dateien:** `V022__locations.sql`, `Location.java`, `LocationController.java`

### P6-T04: NPC-Ort-Zuweisung + Services
- **Status:** ✅ (Teilstand: Schedule-Verfügbarkeit fehlt)
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
- **Status:** ✅ (Teilstand: keine Kauf/Verkauf-Integration mit Inventar)
- **Aufwand:** 2 Tage
- **Abhängigkeiten:** P6-T04
- **Beschreibung:** Preiskalkulation basierend auf Orts-Wohlstand + NPC-Preis-Modifier. `GET /locations/{id}/market` zeigt Items + Preise.
- **Akzeptanzkriterien:**
  - [ ] Preisformel: `basispreis × (1 + (wealth - 5) × 0.1) × npc.price_modifier`
  - [ ] Markt-Endpunkt gibt Items + aktuelle Preise zurück
  - [ ] Integration mit InventoryService (Kauf/Verkauf)
- **Dateien:** `EconomyService.java`, `MarketController.java`

### P6-T06: KI-Kontextaufbau Regionen
- **Status:** ✅ (Teilstand: Location/Schedule nicht im Bot-Prompt)
- **Aufwand:** 1 Tag
- **Abhängigkeiten:** P6-T01 … P6-T04
- **Beschreibung:** AI-Bot lädt beim Prompt-Bau Entity-Events für Region + Location + NPC. Erweiterung der Prompt-Templates um Orts- und Regions-Kontext.
- **Akzeptanzkriterien:**
  - [ ] Bot lädt letzte 5 Entity-Events pro Entity
  - [ ] Prompt-Templates (Jinja2) um Orts-Informationen ergänzt
  - [ ] NPC-Schedule wird im Prompt referenziert („NPC ist tagsüber in der Schmiede")
- **Dateien:** `ai-bot/src/ai_bot/context_loader.py`, `prompts/*.j2` (erweitert)

### P6-T07: Einfache Quest-Generierung
- **Status:** ✅ (Teilstand: Bot generiert keine Quests; UI filtert `pending`, DB-CHECK V030 kennt es nicht — Bug notiert)
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
- **Status:** ✅ (Teilstand: 6 ad-hoc `new ObjectMapper()` verblieben)
- **Aufwand:** 1h
- **Beschreibung:** 19 ad-hoc `new ObjectMapper()` durch zentralen `@Bean` in `JacksonConfig` ersetzt.
- **Dateien:** `JacksonConfig.java`, 7 Services (nach und nach per DI)

### P7-T02: @Transactional-Lücken schließen
- **Status:** ✅ (Teilstand: `IntentExecutor.execute()` ohne `@Transactional`)
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

### ✅ Welten-Clone/Export
- **Status:** ✅ (Teilstand: Clone fertig, JSON-Export fehlt)
- **Aufwand:** 2h
- **Beschreibung:** Backend: `POST /worlds/{id}/clone` erzeugt Kopie einer Welt (inkl. Regionen, Orte, NPCs, Fraktionen). Frontend: "Clone"-Button im WorldEditor. Export als JSON-Download.

### P10-T04: Email-Verifikation
- **Status:** ✅ (V082 + VerifyEmailPage)
- **Aufwand:** 2h
- **Beschreibung:** Bei Registrierung `email_verified_at = null` setzen. Verifikationstoken generieren und speichern. `POST /auth/verify-email` mit Token. Ungültige Email → resenden. Frontend: VerifyEmailPage.

### P10-T05: Dashboard Pagination
- **Status:** ✅ (PaginatedWorldResponse + Load-More)
- **Aufwand:** 1h
- **Beschreibung:** Backend: `GET /worlds/accessible` mit `page`/`size`-Parametern. Frontend: "Load more"-Button oder Infinite-Scroll.

### P10-T06: Onboarding für neue User
- **Status:** ✅ (WelcomePage + Register-Flow)
- **Aufwand:** 2h
- **Beschreibung:** Erstanmelde-Flow: Willkommensseite → "Erstelle deine erste Welt" → Tutorial-Tooltips in GameView. Checkliste für erste Schritte.

### P10-T07: Map Background Upload persistieren
- **Status:** ✅ (V080 + FileUploadController)
- **Aufwand:** 2h
- **Beschreibung:** Backend: FileUploadController mit MultipartFile → Speicherung auf Disk (später S3). `world_maps.image_url` zeigt auf gespeicherte Datei. Frontend: Upload-UI im MapEditor speichert tatsächlich.

### P10-T08: Mobile Responsiveness
- **Status:** ✅ (useMediaQuery + GameView)
- **Aufwand:** 4h
- **Beschreibung:** Sidebar/RightPanel klappen auf <768px automatisch zu. GameView layout passt sich an. Touch-Unterstützung für Token-Drag. Map Canvas minimale Höhe anpassen.

### P10-T09: Soundeffekte (optional)
- **Status:** ✅ (utils/sound + Settings-Toggle)
- **Aufwand:** 2h
- **Beschreibung:** Würfelgeräusche beim Roll (CSS Dice + 3D). Chat-Nachricht-Ton. Kampf-Aktion-Ton. Umschaltbar in Settings.

### P10-T10: JSON-Editor Syntax-Highlighting
- **Status:** ✅ (SyntaxHighlightedTextarea im GameSystemPage)
- **Aufwand:** 2h
- **Beschreibung:** Ersetze das reine `<textarea>` im JSON-Editor durch einen einfachen Code-Editor (CodeMirror oder Monaco Editor light). Zeigt Syntax-Fehler direkt an.

---

## Phase 11: Architektur & Infrastruktur

### P11-T01: SessionController konsolidieren
- **Status:** ✅ (GameSessionController + StompController)
- **Aufwand:** 1h
- **Beschreibung:** Zwei Controller mit überlappenden Funktionen: `SessionController` (publiziert nur Events) und `GameSessionController` (persistiert Sessions). `SessionController` entfernen oder auf `GameSessionService` umleiten.

### P11-T02: RuleEngine Plugin-Registry
- **Status:** ✅ (RuleEngine-Enum-Map in CombatService)
- **Aufwand:** 2h
- **Beschreibung:** Engine-Erkennung per `className.contains("pool")`/`"fudge"` ist fragil. Stattdessen: `Map<DiceSystem, RuleEngine>` via `@PostConstruct` in einer zentralen Registry registrieren. Neue Engines registrieren sich selbst via `@Component` + Interface.

### P11-T03: GameSystem-Caching
- **Status:** ✅ (@Cacheable("gameSystems"))
- **Aufwand:** 2h
- **Beschreibung:** Jeder Wurf (RollService, CombatService) lädt das GameSystem aus der DB → N+1 Problem. Cache per `@Cacheable` auf `gameSystemRepository.findById()`. Redis ist bereits in `compose.prod.yml` konfiguriert.

### P11-T04: Event-Archivierung testen + aktivieren
- **Status:** ✅ (EventArchiveJobTest + Admin-Endpoint)
- **Aufwand:** 1h
- **Beschreibung:** `EventArchiveJob` läuft täglich um 03:00 UTC, aber es gibt keinen Test und kein Monitoring. Test schreiben + Logging ergänzen + manuell triggerbaren Endpunkt `POST /admin/events/archive`.

### P11-T05: API-Rate-Limiting pro Endpunkt
- **Status:** ✅ (RateLimitProperties je Endpunktgruppe)
- **Aufwand:** 2h
- **Beschreibung:** Aktuell nur globales Limit (100/IP/min) + Login-Limit (5/IP/min). Per-Endpunkt-Limits für world-creation, combat-actions, und AI-bot-endpoints.

### P11-T06: Health-Check für Abhängigkeiten
- **Status:** ✅ (BotHealthIndicator + Actuator)
- **Aufwand:** 1h
- **Beschreibung:** Spring Boot Actuator `/actuator/health` zeigt nur den Status der App an. Erweitern um DB-Connectivity, Redis-Ping, AI-Bot-Connectivity (optional). Custom HealthIndicator.

### P11-T07: ~72 Map.of() → DTOs
- **Status:** ✅ (Teilstand: 17 `Map.of` in 8 Dateien verblieben)
- **Aufwand:** 6h
- **Beschreibung:** Alle Controller ersetzen ad-hoc `Map.of()`-Responses durch dedizierte Response-DTOs/Records. Ermöglicht OpenAPI-Schema-Generierung und Type-Safety. Betrifft ~27 Controller.

### P11-T08: Frontend Komponenten-Tests
- **Status:** ✅ (AuthForm/EntityCreateModal/ChatPanel/ActionBar/StatusBar-Tests)
- **Aufwand:** 4h
- **Beschreibung:** Vitest + Testing Library für kritische Komponenten: AuthForm, EntityCreateModal, ChatPanel, ActionBar, StatusBar. Grundlegende Render-Tests + Interaktions-Tests.

---

## Phase 12: Qualität & Robustheit

### P12-T01: Fehlende Service-Tests (QuestService, QuotaService, WorldMapService)
- **Status:** ✅
- **Aufwand:** 2h
- **Beschreibung:** Drei Services haben keine Tests:
  - `QuestService` — CRUD + Status-Update + Zugriffsprüfung
  - `QuotaService` — World-Limit-Prüfung, Member-Limit
  - `WorldMapService` — getOrCreate, update, getById
  **Pattern:** Mokende Repositories + `@ExtendWith(MockitoExtension.class)` (siehe bestehende Tests)

### P12-T02: ResponseEntity<?> durch typisierte Returns ersetzen
- **Status:** ✅
- **Aufwand:** 2h
- **Beschreibung:** 17/31 Controller geben `ResponseEntity<?>` zurück. Die Methode sollte den konkreten DTO-Typ deklarieren:
  - `QuestController` — verwendet `HashMap` statt DTO → `QuestResponse`
  - `LocationController`, `RegionController` — `ResponseEntity<?>` → `LocationResponse`
  - `GlobalExceptionHandler` — alle Handler auf `ApiError` umgestellt

### P12-T03: GlobalExceptionHandler auf ErrorResponse umstellen
- **Status:** ✅
- **Aufwand:** 0,5h
- **Beschreibung:** `Map.of("error", ...)` durch `ApiError.of(code, message)` ersetzt. Alle Exception-Handler retournieren `ResponseEntity<ApiError>`. Schema-Validation + Field-Validation auf `ApiError.schemaValidation()` / `ApiError.validationFailed()` umgestellt.

### P12-T04: Frontend-Testabdeckung erweitern (+5 Testdateien)
- **Status:** ✅
- **Aufwand:** 3h
- **Beschreibung:** Vitest-Tests für:
  - `InitiativeList` (3 Tests: leer, mit Teilnehmern, defeated)
  - `ApBar` (2 Tests: leer, AP-Anzeige)
  - **Setup:** i18n + Testing-Library ist bereits konfiguriert.

### P12-T05: QuestController DTO-Refactoring
- **Status:** ✅
- **Aufwand:** 0,5h
- **Beschreibung:** `HashMap` in `toResponse()` durch `QuestResponse`-Record ersetzen, analog zu den anderen Controllern.

### P12-T06: Skeleton-Loader für Dashboard + GameView
- **Status:** ✅
- **Aufwand:** 1h
- **Beschreibung:** Ladezustände visuell ansprechender gestalten:
  - Dashboard: Skeleton-Karten (graue Boxen mit Puls-Animation) statt "Loading…"-Text
  - Neue Komponente: `SkeletonCard`

### P12-T07: UI-Transitionen & Micro-Interaktionen
- **Status:** ✅
- **Aufwand:** 2h
- **Beschreibung:** Fehlende Animationen ergänzt:
  - Modal Open/Close über CSS-Animationen (`modal-overlay`, `modal-content`)
  - Toast Slide-In von rechts (`toast-slide`)
  - Page-Enter-Fade (`page-enter`)
  - CSS-Keyframes in `index.css`

---

## Phase 13: Campaign-Features (MVP-Lücken)

### P13-T01: Einladungssystem mit Token
- **Status:** ✅ (Abweichungen siehe Bemerkungen)
- **Aufwand:** 3h
- **Beschreibung:** DM erzeugt Einladungslink mit Token (`POST /worlds/{id}/invites`). Empfänger klickt Link → wird Member der Welt (`POST /worlds/join?token=...`). Token hat optionales Ablaufdatum (expiresAt, maxUses).
  - **Backend:** `world_invites` Tabelle (V085), `WorldInviteService` + `WorldInviteController`
  - **Frontend:** "Invite"-Button im WorldEditor → Modal mit Link (worldEditor.invites)
  - **Tests:** `WorldInviteServiceTest`
- **Bemerkungen (2026-09-06):**
  - Routen heißen `/invites` + `/join` (nicht `/invite` wie ursprünglich geplant)
  - Offen: "Pending Invites"-Liste im Dashboard, Controller-Test

### P13-T02: XP-System + Progression (Level/Shop)
- **Status:** ✅ (Abweichungen siehe Bemerkungen)
- **Aufwand:** 5h
- **Beschreibung:** Zwei Progression-Modelle via `rules_json.progression`:
  - **`mode: "level"`** — XP → Level laut Level-Tabelle → Attributspunkte + Ability-Slots
  - **`mode: "shop"`** — XP direkt gegen Attributspunkte eintauschbar (`spendPoints`, keine eigene UI)
  - **Level-Tabelle:** `[{level, xp, attribute_points, ability_slots}]` im Game-System
  - **Reset:** `LevelUpService.resetPoints` (kein separater REST-Endpunkt)
  - **Backend:** Migration `V086__entity_xp.sql`, `LevelUpService`, `PATCH /entities/{id}/progression`
  - **Frontend:** XP/Level + XP-Balken im CharacterSheet, DM-XP-Vergabe in NpcViewPage ("XP granted")
  - **Tests:** LevelUpServiceTest
- **Ersetzt:** Alten P13-T02 + P13-T06 (XP-Automatik entfällt)
- **Bemerkungen (2026-09-06):**
  - E2E-verifiziert: 350 XP → Level 2 (DnD-Skala), Sheet zeigt Level + XP
  - Offen: Level-Up-Modal, dedizierte Shop-UI, DM-only-Restriktion (aktuell: Owner editiert eigene XP)

### P13-T03: Kampf-Log im Chat
- **Status:** ✅
- **Aufwand:** 1h

### P13-T04: Character-Sheet als P&P-Bogen
- **Status:** ✅
- **Aufwand:** 3h

### P13-T05: HP/AP-Regeneration + Rest
- **Status:** ✅
- **Aufwand:** 2h

---

## Phase 14: Adventure Visual Editor + Live-DM

### P14-T01: Backend — Adventure Discovery + Override API
- **Status:** ✅ (Abweichungen siehe Bemerkungen)
- **Aufwand:** 3h
- **Beschreibung:** Adventures sind auffindbar und NPC-/ortsgebunden:
  - **Migration V088:** `adventures` um `location_id` und `giver_entity_id` erweitert
  - **Listen-Endpunkte:** `GET /api/v1/adventures?worldId=X`, `/by-location/{id}`, `/by-giver/{id}`
  - **DM-Override-Endpunkte:** `override-text`, `force-node/{nodeId}`, `inject-choice/{nodeId}`, `PATCH nodes/{nodeId}`, Nodes/Choices-CRUD
  - **WebSocket-Events:** `ADVENTURE_NODE_CHANGED`, `ADVENTURE_CHOICES_CHANGED` (publiziert)
- **Bemerkungen (2026-09-06):**
  - Offen: `override-skillcheck`-Endpunkt, `PATCH choices/{choiceId}`, Listen-Pfade `/locations/{id}/adventures` + `/entities/{id}/adventures` (stattdessen `/by-location`, `/by-giver`)

### P14-T02: Frontend — ReactFlow Adventure Editor
- **Status:** 📋 (Basis vorhanden, E2E ausstehend)
- **Aufwand:** 3h
- **Beschreibung:** Visueller Node-Graph-Editor für den DM (`AdventureEditorPage` mit `@xyflow/react`):
  - **Canvas:** ReactFlow mit Custom Nodes — **E2E-Verifikation der Toolbox/Connect/Preview-Features ausstehend**
  - **Nodes:** AdventureNodes als Karten im Graph (Titel, Text-Vorschau, Image)
  - **Edges:** Choices als Verbindungslinien mit Label
  - **Drag & Drop:** Neue Nodes aus einer Toolbox ziehen
  - **Connect:** Von Node zu Node ziehen = neue Choice
  - **Sidebar:** Bei Klick auf Node → Editor für Text/Image/SkillCheck/isEnd
  - **Choice-Editor:** Label, Target-Node, Skill-Check-JSON, Success/Failure-Node
  - **Location/NPC-Binding:** Dropdown zur Auswahl, welcher NPC/Location dieses Adventure zugeordnet ist
  - **Preview-Button:** DM klickt sich durch den Graph wie ein Spieler
  - **Save:** Änderungen werden via API persistiert

### P14-T03: Frontend — Adventure Discovery + Play Page
- **Status:** 📋 (Basis vorhanden, E2E ausstehend)
- **Aufwand:** 2,5h
- **Beschreibung:** Spieler finden und spielen Adventures:
  - **Discovery (NPC):** `NpcViewPage` zeigt NPC-Adventures (`/adventures/by-giver`) — **Start-Button-Flow ungeprüft**
  - **Discovery (Location):** `LocationDetail` zeigt Adventures (`/adventures/by-location`)
  - **Available-Badge:** GameView-Sidebar zeigt ob in der aktuellen Location ein Adventure startbar ist — **ungeprüft**
  - **Play Page:** Route `/worlds/{worldId}/adventures/{adventureId}` (`AdventurePlayPage`) — **E2E ausstehend**
  - **Node-Ansicht:** Text + optionales Bild
  - **Choice-Buttons:** Klick → `POST /adventures/{id}/advance`
  - **Skill-Check-Indikator:** "🎲 Geschicklichkeit 12" bei entsprechenden Choices
  - **Progress-Bar:** Node X von Y
  - **WebSocket-Empfang:** Live-Updates bei DM-Override

### P14-T04: Frontend — Live DM Override UI
- **Status:** 📋 (Basis vorhanden, E2E ausstehend)
- **Aufwand:** 1h
- **Beschreibung:** Während Spieler ein Adventure spielen, kann der DM über ein Overlay eingreifen (`LiveAdventurePanel` in GameView):
  - **Live-Status:** In der DM-Queue / GameView wird angezeigt, welcher Spieler welches Adventure spielt und an welchem Node — **ungeprüft**
  - **Override-Button:** "Edit Active Node" → Inline-Editor für Text (`override-text` verdrahtet ✅)
  - **Force-Node-Dropdown:** DM wählt einen Ziel-Node aus dem Graphen (`force-node` verdrahtet ✅)
  - **Inject-Choice-Form:** Fügt dynamisch eine Choice hinzu — **UI-Anbindung ungeprüft**
  - **WebSocket-Push:** Änderungen werden sofort an alle Spieler gesendet — **ungeprüft**

---

## NOCH OFFEN (Architektur-Risiken)

| ID | Was | Aufwand | Priorität |
|---|---|---|---|
| ✅ | **Deploy-Workflow** (`.github/workflows/deploy.yml`) | erledigt | — |

---

## Phase 15: System-Wizard 2.0 & Map-Editor UX

### P15-T01: Map-Editor Layout fix
- **Status:** ✅
- **Aufwand:** 1,0 Tag
- **Erledigt:** 2026-07-19
- **Beschreibung:** RightPanel wird durch Karte weggedrückt. Layout so umbauen, dass Map + RightPanel nebeneinander passen.

### P15-T02: GameView-Karte via HTML/CSS statt PixiJS
- **Status:** ✅
- **Aufwand:** 1,0 Tag
- **Erledigt:** 2026-07-19
- **Analyse:** PIXI-Renderer zeigt trotz korrekt geladener Daten nur Schwarz. Lösung: Hintergrund als CSS `<img>`, PixiJS nur für Grid/Overlays.
- **Akzeptanzkriterien:** Karte sichtbar im GameView. Grid + Overlays funktionsfähig.

### P15-T03: Wizard Step 0 — System-Charakter
- **Status:** ✅
- **Aufwand:** 1,5 Tage
- **Erledigt:** 2026-07-19
- **Beschreibung:** Neuer Step 0 mit Progression-Typ (Level/XP/Steigerung) + Checkboxen für Optionen (Magie, Psionik, Fernkampf, krit. Treffer, Rüstungs-Erschwernis). Auswahl steuert spätere Steps conditional.
- **Akzeptanzkriterien:** Nutzer wählt Progression-Typ → entsprechende UI in Step 5 erscheint. Optionen blenden Step 6 (Specials) ein/aus.

### P15-T04: Wizard Step 2 — Derived Values
- **Status:** ✅
- **Aufwand:** 1,0 Tag
- **Erledigt:** 2026-07-19
- **Beschreibung:** Neuer Step nach Attributen. Formel-Baukasten für abgeleitete Werte (HP, AP, MP, etc.). Dropdown für Attribut + Operator + Zahl/Feld.
- **Abhängigkeiten:** Step 1 (Attribute) muss vorher kommen
- **Akzeptanzkriterien:** Attribut aus Dropdown wählbar, Formel wird gespeichert und im Review angezeigt.

### P15-T05: Wizard Step 5 — Abilities
- **Status:** ✅
- **Aufwand:** 2,0 Tage
- **Erledigt:** 2026-07-19
- **Beschreibung:** Neuer Step für aktive/passive Fähigkeiten. Aktive: Name, Kosten (AP/MP), Würfelausdruck, Effekt. Passive: Name, Bonus (z.B. RK+1). Talent-Typ abhängig von Step 0 (Class-Feature/Talent).

### P15-T06: Wizard Step 6 — Progression (conditional)
- **Status:** ✅
- **Aufwand:** 1,0 Tag
- **Erledigt:** 2026-07-19
- **Beschreibung:** Step 5 wird je nach Step-0-Auswahl unterschiedlich dargestellt: Level-Tabelle (D&D), XP-Kosten (DSA), Steigerungswürfel (CoC).
- **Abhängigkeiten:** Step 0
- **Akzeptanzkriterien:** Je nach gewähltem Progression-Typ erscheint die passende UI.

### P15-T07: Wizard Step 7 — Specials (conditional)
- **Status:** ✅
- **Aufwand:** 1,5 Tage
- **Erledigt:** 2026-07-19
- **Beschreibung:** Magie/Psionik-Sektion, nur sichtbar wenn in Step 0 aktiviert. Zauber pro Stufe, Mana-Formel, Schulen/Domänen.
- **Abhängigkeiten:** Step 0
- **Akzeptanzkriterien:** Bei ☑ Magie erscheint der Magie-Step, sonst nicht.

### P15-T08: Conditionals-Engine
- **Status:** ✅
- **Aufwand:** 2,0 Tage
- **Erledigt:** 2026-07-19
- **Beschreibung:** Formel-Editor für konditionale Boni in Würfelausdrücken („wenn Stärke > 15 dann +2 auf Schaden"). Paradebeispiel DSA: „jeder Punkt Intuition über 8 gibt +1 auf Initiative".
- **Akzeptanzkriterien:** Bedingungen wie `if(attribut>X, +bonus, 0)` werden geparst und gespeichert. Später in der RuleEngine auswertbar.

### P15-T09: i18n für Wizard & Editor
- **Status:** ✅
- **Aufwand:** 1,0 Tag
- **Erledigt:** 2026-07-19
- **Beschreibung:** SystemWizard-Komponente vollständig übersetzen (DE/EN). MapEditor + WorldSettings ergänzen.
- **Akzeptanzkriterien:** Sprachwechsel übersetzt alle Wizard-Texte.

### P15-T10: Diverse UX-Fixes
- **Status:** ✅
- **Aufwand:** 1,0 Tag
- **Erledigt:** 2026-07-19
- **Beschreibung:** 1d2-1d100 Dropdown, Region-Farben speicherbar, Location verschiebbar, Wizard-Beschreibungen verbessern.
- **Akzeptanzkriterien:** Alle UX-Punkte aus dem Feedback umgesetzt.

### P15-T11: 3D-Würfel-Verbesserung (nice-to-have)
- **Status:** ✅
- **Aufwand:** 1,0 Tag
- **Erledigt:** 2026-07-19
- **Beschreibung:** Ersetze die aktuelle THREE.js-Darstellung (farbige Polyeder + Sprite-Label) durch echte 3D-Würfel mit korrekter Augenzahl auf allen Faces. Für d6: Dots pro Face (1-6). Für d4/d8/d10/d12/d20: Jeweilige Zahl (1-N) auf jedem Face, nicht nur das gewürfelte Ergebnis. Geometrien und Face-Mapping müssen korrekt sein (keine leeren Faces).
- **Akzeptanzkriterien:**
  - Alle Würfeltypen (d4/d6/d8/d10/d12/d20) zeigen korrekte Augenzahlen auf jedem Face
  - Keine schwebenden Sprite-Label mehr nötig
  - d10 als pentagonaler Trapezoeder (nicht CylinderGeometry)
  - Faces sind lesbar (Zahlen/Dots gut sichtbar)

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
| 12 (Qualität & Robustheit) | 7 | 11,0 Tage |
| 13 (Campaign-Features) | 5 | 12,0 Tage |
| 14 (Adventure Editor) | 4 | 9,5 Tage |
| 15 (Wizard 2.0 & Editor UX) | 11 | 14,5 Tage |
| **16 (Multi-System & RuleEngine)** | **10** | **20,0 Tage** |
| **Summe** | **123 (1 cancelled)** | **225,5 Tage** |

---

## Phase 16: Multi-System Character-Sheet & RuleEngine

### Kontext
Drei Zielsysteme (D&D 5e, CoC 7e, DSA 5) haben stark unterschiedliche Regelmechaniken. Der SystemWizard muss diese abbilden können, und der Character-Sheet + RuleEngine müssen die Konfiguration auswerten.

| Merkmal | D&D 5e | CoC 7e | DSA 5 |
|---|---|---|---|
| Attribute | 6 (STR/DEX/CON/INT/WIS/CHA) | 8 (STR/CON/DEX/APP/POW/INT/SIZ/EDU) | 8 (MU/KL/IN/CH/FF/GE/KO/KK) |
| Attribut-Range | 3–20 | 3–18 | 1–21 |
| Modifier | (Wert-10)/2 | — | — |
| Probe | d20 + Mod ≥ DC | d100 ≤ Fertigkeit% | 3d20 ≤ Attribut (3 Attribute) |
| Vorteile | Feats, Class Features | Perks | Vor-/Nachteile, SF |
| Aktionen | Action/Bonus/Reaction | 1 Aktion | 1 Aktion |
| Progression | Level + XP (Milestone) | Steigerungswürfe | XP + Talentstufen |
| Kampf | Multi-Attack, AC, HP | Wunden, Manöver | AT/PA, DP, Rüstung |

### P16-T01: System-Analyse + Beispiel-JSONs als Living Spec
- **Status:** ✅
- **Aufwand:** 1,0 Tag
- **Erledigt:** 2026-07-19
- **Beschreibung:** Erstelle 3 vollständige `rulesJson`-Konfigurationen für D&D 5e, CoC 7e und DSA 5 als **Living Spec** für alle Folgetasks. Jede Konfiguration wird manuell erstellt (nicht via Wizard) und deckt alle relevanten Regelmechaniken ab. Die Configs dienen als:
  - **Akzeptanzkriterien** für den Wizard (muss diese Felder produzieren können)
  - **Testdaten** für die Sheet-API und RuleEngine
  - **Dokumentation** der System-Unterschiede
- **Abhängigkeiten:** —
- **Akzeptanzkriterien:**
  - 3 valide `rulesJson`-Konfigurationen in `docs/examples/` abgelegt
  - Jede Config enthält: Attribute, Skills (mit 1–3 Attribut-Verknüpfungen), Abilities, Progression, Conditionals
  - Lücken-Report: welche Wizard-Felder fehlen noch?
- **Bemerkungen:** Die Configs werden nie gelöscht — jeder Code muss sie korrekt verarbeiten können. Bei Datenmodell-Änderungen werden sie aktualisiert.
- **Qualitäts-Check:** Dokumentation + Datenmodell-Review

### P16-T02: Wizard Step 5 erweitern — Probentyp + DSA-3er-Proben
- **Status:** ✅
- **Aufwand:** 2,0 Tage
- **Erledigt:** 2026-07-19
- **Beschreibung:** Skills aktuell an ein Attribut gekoppelt. Für DSA brauchen Skills **drei** Attribute. Ausserdem muss der **Probentyp** konfigurierbar sein:
  - `d20_target` (D&D): d20 + Mod ≥ Zielwert
  - `d100_threshold` (CoC): d100 ≤ Fertigkeit
  - `d20_3attr` (DSA): 3d20, je ≤ Attribut, Fehlschläge kompensieren
  - Der Würfelausdruck im Dice-Step wird automatisch aus dem Probentyp generiert
- **Backend:** `rulesJson.probe_type` (Default: `'d20_target'` für alte Systeme), `rulesJson.skill_attributes` (array statt single)
- **Frontend:** Wizard Step 2 (Skills) erlaubt mehrere Attribute + neues Dropdown für Probentyp im Dice-Step
- **Akzeptanzkriterien:**
  - Skill kann 1–3 Attribute haben (je nach Probentyp)
  - Probentyp wird in `rulesJson` gespeichert
  - Alte Systeme ohne `probe_type` → Default `d20_target`
  - Frontend-Tests
- **Bemerkungen:** Default-Werte für `probe_type` stellen sicher dass bestehende Systeme nicht brechen.
- **Qualitäts-Check:** TDD, i18n, UI/UX-Review, Migration alter Systeme geprüft

### P16-T03: Wizard Step 8 erweitern — Action Economy
- **Status:** ✅
- **Aufwand:** 1,5 Tage
- **Erledigt:** 2026-07-19
- **Abhängigkeiten:** P16-T01
- **Beschreibung:** System kann konfigurieren welche Aktions-Typen es gibt und wie viele pro Runde:
  - `actionTypes`: Liste der Typen (`["action"]`, `["action", "bonus_action"]`, usw.)
  - `actionsPerTurn`: Anzahl pro Typ (`{ action: 1 }`, `{ action: 1, bonus_action: 1 }`)
  - Abilities/Abilities bekommen Feld `actionCost: { type, amount }`
  - DSA-Sonderfertigkeit "Nachladen": `actionCost: { type: "action", amount: -1 }` (reduziert Kosten)
  - D&D "Extra Attack": erlaubt 2 Angriffe pro Action → Feld `multiAttack: number`
- **Backend:** `rulesJson.combat.action_types` + `rulesJson.combat.actions_per_turn`
- **Frontend:** Wizard Step 2 (Combat Dice) um Action-Sektion erweitern. Ability-Step zeigt Action-Dropdown.
- **Akzeptanzkriterien:**
  - Action-Config speicherbar und im Review sichtbar
  - Pro Ability kann Action-Typ + Kosten gewählt werden
  - Frontend-Tests + TypeScript
- **Qualitäts-Check:** TDD, Datenmodell-Review, i18n

### P16-T04: Ability-Tags + Vor-/Nachteile
- **Status:** ✅
- **Aufwand:** 1,5 Tage
- **Erledigt:** 2026-07-19
- **Beschreibung:** Abilities bekommen Tags für bessere Filterung im Kampf-UI:
  - `tags: string[]` (z.B. `["attack", "ranged", "magic"]`, `["defensive", "concentration"]`)
  - Vor-/Nachteile (DSA) / Feats (D&D) / Perks (CoC) als spezielle Abilities:
    - `category: 'ability' | 'advantage' | 'perk'` 
    - Können Conditionals referenzieren (z.B. Vorteil "Zäher Hund": +2 auf KO-Proben)
- **Backend:** `rulesJson.abilities[].tags`, `rulesJson.abilities[].category`
- **Frontend:** Wizard Step 5 (Abilities) um Tag-Eingabe + Kategorie-Dropdown erweitern. Review zeigt Tags.
- **Akzeptanzkriterien:**
  - Tags speicherbar und im Review sichtbar
  - Kategorie-Auswahl für normale Fähigkeiten / Vorzüge / Nachteile
  - Frontend-Tests
- **Qualitäts-Check:** TDD, UI/UX-Review

### P16-T05: Modifier-Engine (Backend)
- **Status:** ✅
- **Aufwand:** 2,0 Tage
- **Erledigt:** 2026-07-24
- **Beschreibung:** Automatische Modifier-Berechnung für Systeme die das unterstützen:
  - `modifierFormula: '(value - 10) / 2'` (D&D) → wird auf jedes Attribut angewendet
  - `derivedValueFormula` in Conditionals auswerten:
    - HP = Basis + Attribut × Multiplikator
    - AC = 10 + DEX-Modifier + Rüstung
  - Backend evaluiert die Formeln beim Character-Request
- **Backend:** `ModifierService` + **`FormulaEvaluator`** — sicherer Recursive-Descent-Parser:
  - Erlaubte Operatoren: `+`, `-`, `*`, `/`, `()`, `min()`, `max()`, `floor()`
  - Erlaubte Variablen: nur Attribut-Namen aus `rulesJson.attributes`
  - **Kein `eval`, kein `ScriptEngine`, keine dynamischen Funktionsaufrufe**
  - Verhindert Code-Injection von Haus aus
- **Akzeptanzkriterien:**
  - Modifier-Formeln werden korrekt berechnet (D&D: attr 15 → +2)
  - Derived-Formeln funktionieren (DSA: `(KO+KK)/2+5` → LP)
  - Sicherheit: nur erlaubte Variablen + Operatoren, keine Injection
  - Unit-Tests für FormulaEvaluator (Grenzfälle, Division durch 0, etc.)
- **Bemerkungen:** FormulaEvaluator ist das Herzstück. Sheet-API + RuleEngine bauen darauf auf. Je sauberer das hier ist, desto einfacher werden T06–T08.
- **Qualitäts-Check:** TDD, Security-Review, Code-Qualität

### P16-T06: Character-Sheet API (Backend)
- **Status:** ✅
- **Aufwand:** 3,0 Tage
- **Erledigt:** 2026-07-24
- **Beschreibung:** REST-API für Character-Sheet — **funktioniert für PCs und NPCs gleichermassen** (NPCs haben nur einfachere Progression):
  - `GET /entities/{id}/sheet` — berechnet alle Werte aus `rulesJson` + `attributesJson`
  - Wendet FormulaEvaluator auf Derived-Formeln, Modifier und Conditionals an
  - Gibt berechnete Attribute (mit Modifiern), Skills, Abilities, Derived Values zurück
  - Berücksichtigt Progression (Level, XP, Talentstufen)
  - NPCs: liefert Basis-Werte ohne Progression
  - Alte Systeme ohne `probe_type` oder `modifierFormula`: Default-Werte verwenden, Sheet trotzdem ausliefern
- **Backend:** `CharacterSheetService`, `FormulaEvaluator` (aus T05)
- **Akzeptanzkriterien:**
  - Sheet-API gibt konsistente Werte zurück
  - D&D: Modifier = (attr-10)/2, HP = Basis + CON-Mod x Level
  - CoC: HP = (STR+CON)/2, Sanity = POW×5
  - DSA: LP = (KO+KK)/2+5
  - Integrationstests für jedes Beispielsystem
- **Qualitäts-Check:** TDD, Architektur-Review

### P16-T07: Character-Sheet UI (Frontend) — Read-Only first
- **Status:** ✅
- **Aufwand:** 3,0 Tage
- **Erledigt:** 2026-07-24
- **Beschreibung:** Dynamisches Character-Sheet — **zunächst Read-Only**. Editieren kommt in einer späteren Iteration:
  - Ruft `GET /entities/{id}/sheet` ab
  - Zeigt alle berechneten Werte: Attribute (mit Modifiern), Skills, Abilities, Derived Values
  - Proben-Würfel-Button bei Skills — verwendet Probentyp aus `rulesJson`:
    - D&D: `1d20+staerke`, zeigt `≥ DC` Erfolg/Fehlschlag
    - CoC: `1d100`, vergleicht mit Skill-Wert
    - DSA: `3d20`, zeigt Einzelergebnisse pro Attribut
  - Abilities gefiltert nach Tags (im Kampf: nur `attack`-Abilities)
  - Conditionals als Tooltip/Hinweis: "Stärke > 15: +2 auf Schaden"
  - Kampf-UI: Aktions-Typen visualisieren, verbleibende Aktionen anzeigen
- **Bemerkungen:** Read-Only reduziert Komplexität und UI-Tests. Editieren wird ein eigener Task in Phase 17.
- **Qualitäts-Check:** TDD, UI/UX-Review, i18n, Responsive-Test

### P16-T08: RuleEngine — Conditionals auswerten + Roll-API v2
- **Status:** ✅
- **Aufwand:** 2,0 Tage
- **Erledigt:** 2026-07-24
- **Beschreibung:** Backend wertet `conditionals` aus `rulesJson` aus und erweitert die Würfel-API:
  - `ConditionEvaluator` wendet Bedingungen auf Proben an:
    - `gt/gte/lt/lte/eq`: einfache Vergleiche → Bonus/Malus
    - `per_point`: für jeden Punkt über/unter Schwellwert, Bonus × Differenz
    - Ergebnis: modifizierter Würfelwert + **Erklärung** (welche Bedingungen aktiv)
  - Erweiterte Roll-API:
    - `POST /rolls/probe` — würfelt eine System-Probe:
      - Input: `{ entityId, skillName, target (DC), advantage/disadvantage }`
      - Ermittelt Probentyp aus `rulesJson`, berechnet Modifier, wendet Conditionals an
      - Liefert: Einzelwürfe, Gesamtergebnis, Erfolg/Fehlschlag, aktive Conditionals
    - `POST /rolls/advantage` — D&D Vorteil/Nachteil: 2d20, wähle höher/niedriger
- **Backend:** `ConditionEvaluator`, `ProbeService` (erweitert `RollController`)
- **Akzeptanzkriterien:**
  - Conditionals werden bei Sheet-API und Würfel-API ausgewertet
  - Per-Point-Boni funktionieren (DSA: IN>8 → +1 Initiative pro Punkt)
  - Erklärung der aktiven Boni wird mitgeliefert
  - `POST /rolls/probe` funktioniert für alle 3 Probentypen
  - Unit-Tests für alle Operator-Typen
- **Qualitäts-Check:** TDD, Code-Qualität, Security

### P16-T09: Beispiel-Systeme validieren + Integrationstests
- **Status:** ✅
- **Aufwand:** 2,0 Tage
- **Erledigt:** 2026-07-24
- **Beschreibung:** 
  - Validiere dass die 3 Beispiel-JSONs aus T01 im Wizard geladen und gespeichert werden können
  - Erstelle für jedes System einen Test-Character im Wizard
  - Integrationstests: Character erstellen → Sheet laden → Probe würfeln → Conditional auswerten
  - Dokumentiere jede Konfiguration mit Screenshots
- **Akzeptanzkriterien:**
  - Alle 3 Beispiel-JSONs aus T01 sind via Wizard importierbar
  - Character-Sheet + Proben funktioniert für jedes System
  - Integrationstests grün
- **Qualitäts-Check:** Funktionaler Test, UI/UX-Review, i18n

### P16-T10: Qualitätssicherung + Bugfixes
- **Status:** ✅
- **Aufwand:** 2,0 Tage
- **Erledigt:** 2026-07-24
- **Beschreibung:** 
  - Bug-Hunting-Session: Edge-Cases in allen 3 Systemen testen
  - Code-Qualität: Prüfe auf die bekannten Muster (keine raw Maps, Exception-Handler konsistent, i18n vollständig)
  - UI/UX: Prüfe Dark/Light/Cyber-Themes, Scrollverhalten, mobile Ansatz
  - Performance: Ladezeiten des Character-Sheets prüfen (viele Conditionals)
  - Nachbesserungen aus den Reviews
- **Akzeptanzkriterien:**
  - Alle gefundenen Bugs gefixt
  - Keine offenen Code-Qualität-Mängel
  - Alle Themes funktionieren
- **Qualitäts-Check:** Full-Suite-Run (86+ Tests)

---

## Phase 17: Character-Edit, Inventory & Kampf-UI

Nach dem Read-Only-Sheet (P16-T07) folgen Editieren, Inventory und Kampf.

### P17-T01: Character Edit API + UI
- **Status:** ✅
- **Aufwand:** 2,0 Tage
- **Erledigt:** 2026-07-24
- **Beschreibung:** Attribut-Werte editierbar machen + XP/Level bearbeiten:
  - `PATCH /entities/{entityId}/attributes` — einzelne Attribut-Werte setzen (merged in attributesJson)
  - `PATCH /entities/{entityId}/progression` — XP/Level/Talentstufen setzen
  - Frontend: Inline-Edit für Attribut-Werte, XP-Balken editierbar
  - Validation gegen `rulesJson.attributes[].min/max`
  - Nur Character-Besitzer + DM dürfen editieren
  - Sheet-API aktualisiert sich nach Speichern (refetch)
- **Akzeptanzkriterien:**
  - Attribut-Wert klickbar → editierbar → gespeichert
  - Validation: min/max aus rulesJson
  - XP/Level editierbar → Derived Values passen sich an
  - Frontend-Tests + TypeScript
- **Qualitäts-Check:** TDD, Security (Access-Check), i18n
- **Hinweis:** Formel-Override (`PATCH /entities/{entityId}/override`) war optional und wurde nicht implementiert. Bei Bedarf separater Task.

### P17-T02: Inventory API
- **Aufwand:** 2,0 Tage
- **Status:** ✅
- **Erledigt:** 2026-07-24

### P17-T03: Inventory UI
- **Aufwand:** 2,0 Tage
- **Status:** ✅
- **Erledigt:** 2026-07-24

### P17-T04: Kampf-UI verbessern
- **Aufwand:** 2,0 Tage
- **Status:** ✅
- **Erledigt:** 2026-07-24

### P17-T05: Character Export/Import
- **Aufwand:** 1,0 Tag
- **Status:** ✅
- **Erledigt:** 2026-07-24
- **Nachreichung Import:** 2026-07-24
  - `POST /worlds/{worldId}/entities/import` — erstellt Entity aus JSON-Export
  - Import-Button im CharacterSheetPage-Header (JSON-Datei upload)
  - i18n DE/EN

### P17-T06: Qualitätssicherung
- **Aufwand:** 2,0 Tage
- **Status:** ✅
- **Erledigt:** 2026-07-24

---

## Phase 17 Statistik
| Phase | Tasks | Sum Aufwand |
|---|---|---|
| 17 (Character-Edit & Kampf) | 6 | 11,0 Tage |

Mit Personalaufwand gerechnet. Bei ~20 effektiven Arbeitstagen/Monat entspricht das bei Vollzeit ~5,5 Monaten gesamt.

---

## Phase 17 Cleanup (Review-Nacharbeiten)

Nach Abschluss der Review vom 2026-07-24 identifizierte und behobene Mängel:

### P17-C01: Formel-Override implementiert
- **Status:** ✅ (2026-07-24)
- **Beschreibung:** Fehlendes Feature aus P17-T01-Spezifikation nachgereicht.
  - `PATCH /entities/{entityId}/override` — speichert Overrides in `metadata_json.formula_overrides`
  - `CharacterSheetService` wendet Overrides additiv auf Derived Values an
  - Frontend: `FormulaOverrides`-Komponente im CharacterSheet (Hinzufügen/Entfernen)
- **Aufwand:** ~2 h

### P17-C02: Item-Typ → Slot Mapping beim Equip-Button
- **Status:** ✅ (2026-07-24)
- **Beschreibung:** Der "equip"-Button in `ItemCard.tsx` sendete immer `slot='weapon'`.  
  Fix: Mapping via Item-Typ — `WEAPON→weapon, ARMOR→armor, HELMET→helmet, ACCESSORY→accessory`.
- **Aufwand:** ~15 min

### P17-C03: Error-Feedback in leeren catch-Blöcken
- **Status:** ✅ (2026-07-24)
- **Beschreibung:** `InventoryPage` hatte 3 leere `catch { /* */ }`-Blöcke.  
  Fix: Toast-Nachrichten via `useToast` bei Fehlern in handleEquip/handleUnequip.
- **Aufwand:** ~15 min

### P17-C04: InventoryPage Tests
- **Status:** ✅ (2026-07-24)
- **Beschreibung:** `InventoryPage.test.tsx` mit 3 Tests: Loading-State, Empty-State, gerenderte Items + Bonuses.
- **Aufwand:** ~30 min

### P17-C05: TASKS.md bereinigt
- **Status:** ✅ (2026-07-24)
- **Beschreibung:** Formel-Override aus P17-T01-Beschreibung entfernt, Import-Nachreichung dokumentiert, Cleanup-Tasks eingefügt.
- **Aufwand:** ~10 min

### P17-C06: Abilities-UI (verschoben)
- **Status:** ⏭️ ersetzt durch P20 (Abilities-UI)
- **Beschreibung:** Der `abilitiesComingSoon`-Placeholder im CharacterSheet bleibt bestehen.  
  Geplant: Abilities aus der Datenbank laden + anzeigen (aus P16 vorbereitet).

---

## Phase 18: System-Validierung & Beispiel-Dateien

Nach der Analyse der 3 Beispielsysteme (D&D 5e, CoC 7e, DSA 5) gegen die aktuelle Implementierung identifizierte Lücken und behobene Mängel.

### P18-T01: Example JSONs korrigiert
- **Status:** ✅ (2026-07-24)
- **Beschreibung:** Drei Probleme in den Beispiel-JSONs:
  - `_gaps` und `_comment` (nicht im Schema) entfernt → `description` stattdessen
  - `probeType` fehlte in coc7e.json (`d100_threshold`) und dsa5.json (`d20_3attr`)
  - `modifierFormula` fehlte in dnd5e.json → `"floor((attr-10)/2)"`
- **Aufwand:** ~20 min
- **Qualitäts-Check:** Alle 3 JSONs passieren jetzt die Schema-Validierung

### P18-T02: ProbeType Auto-Detect
- **Status:** ✅ (2026-07-24)
- **Beschreibung:** `ProbeService` erkennt `probeType` jetzt automatisch aus `dice_mechanics.probe`:
  - `1d100` → `d100_threshold`
  - `3d20` → `d20_3attr`
  - Sonst → `d20_target` (default)
  - Explizites `probeType` im JSON überschreibt Auto-Detect
- **Aufwand:** ~30 min
- **Qualitäts-Check:** Alle Backend-Tests grün (204)

### P18-T03: RULES-SCHEMA.md aktualisiert
- **Status:** ✅ (2026-07-24)
- **Beschreibung:** Vollständiges Schema aus `RuleSchemaValidator.DEFAULT_SCHEMA` übernommen.
  - `probeType`, `modifierFormula`, `derived_values`, `conditionals`, `abilities` dokumentiert
  - `skill.attributes`-Array dokumentiert (Multi-Attribute für DSA 3er-Proben)
  - Probe-Typen-Tabelle + Auto-Detect dokumentiert
- **Aufwand:** ~30 min

### P18-T04: TASKS.md Phase 18 Abschnitt
- **Status:** ✅ (2026-07-24)
- **Beschreibung:** Dieser Abschnitt.

## Phase 18 Ausblick (Architektur-Entscheidungen für später)

Folgende Themen wurden analysiert, aber nicht umgesetzt — sie erfordern Architektur-Entscheidungen:

### Per-Character Skill-Werte
- **Problem:** DSA und CoC definieren Skill-Boni global im `rulesJson`. Alle Charaktere haben die gleichen Skill-Werte.
- **DSA:** Talentwert (FW) ist charakterspezifisch (1-20), muss pro Entity gespeichert werden.
- **CoC:** Skill-Startwerte sind berufsabhängig (Occupation-System), nicht global.
- **Lösungsansatz:** Neues `skills_json`-Feld auf `entities`-Tabelle (JSONB mit `{ "skillName": value }`). `ProbeService` überschreibt globale Boni mit per-Character-Werten.
- **Status:** 🔜 (Architektur-Entscheidung nötig)

### Combat-System für CoC/DSA
- Das Kampfsystem ist D&D-zentriert (Initiative, AP, HP). CoC hat vereinfachte Kämpfe, DSA verwendet AT/PA mit Aktionen.
- **Status:** 🔜 (Phase 18/19)

### Abilities-UI
- `abilitiesComingSoon`-Placeholder im CharacterSheet ersetzen.
- **Status:** 🔜 (Phase 18)

---

## Phase 18 Statistik
| Phase | Tasks | Sum Aufwand |
|---|---|---|
| 18 (System-Validierung & Docs) | 4 | ~1,5 Tage |

---

## Phase 19: Per-Character Skill-Werte & Unified Proben-System

Probendefinitionen (Talente, Fähigkeiten, Proben, Attacken) beziehen sich immer auf den Charakter — nicht auf das globale Regelwerk. `rulesJson.skills[].bonus` wird zum Startwert/Default, den der Charakter überschreiben kann.

### P19-T01: DB-Migration — `skills_json` auf `entities`
- **Status:** ✅ (2026-07-24, TDD: RED→GREEN→REFACTOR)
- **Aufwand:** 0,5 Tage
- **Beschreibung:** 
  - Neue Column `skills_json` (JSONB, nullable) auf `entities`-Tabelle via Flyway
  - Format: `{ "Athletik": 4, "Wahrnehmung": 2, "Heimlichkeit": 0 }`
  - Optional: Wertebereich konfigurierbar (min/max aus `rulesJson.skills[].type`)
- **Akzeptanzkriterien:**
  - Migration läuft auf bestehender DB
  - Field auf `GameEntity`-Domain gemappt
  - Getter/Setter vorhanden
- **Qualitäts-Check:** Migration-Test, Unit-Tests

### P19-T02: Backend — Per-Character Skill-Overrides in ProbeService
- **Status:** ✅ (2026-07-24, TDD: RED→GREEN→REFACTOR)
- **Aufwand:** 1,0 Tage
- **Beschreibung:** Der `ProbeService.executeProbe()` merged globale Skill-Boni mit per-Character-Overrides:
  1. Lade `entity.skillsJson`
  2. Wenn Skill-Name in `skillsJson` existiert → use per-character value als `skillBonus`
  3. Sonst → use `rulesJson.skills[].bonus` als Fallback
  - `PATCH /entities/{entityId}/skills` — einzelne Skill-Werte setzen (merged in `skillsJson`)
  - `GET /entities/{entityId}/sheet` — `skillsJson`-Werte im `SheetResponse` mitsenden
  - CharacterSheetService zeigt effektiven Skill-Wert (Fallback → Override)
- **Akzeptanzkriterien:**
  - ProbeService verwendet per-Character-Werte wenn vorhanden
  - Fallback auf globalen Bonus wenn Character keinen Wert hat
  - API-Endpunkt zum Setzen einzelner Skill-Werte
  - SheetResponse enthält effektiven Skill-Wert + Herkunft (global/character)
  - Alle 3 Probentypen (d20_target, d100_threshold, d20_3attr) verwenden den Mechanismus
- **Qualitäts-Check:** TDD, Integrationstests, Security

### P19-T03: Frontend — Skill-Werte editierbar im CharacterSheet
- **Status:** ✅ (2026-07-24, TDD: RED→GREEN→REFACTOR)
- **Aufwand:** 1,0 Tage
- **Beschreibung:**
  - Skill-Liste im CharacterSheet zeigt effektiven Wert + (edit icon)
  - Inline-Edit wie bei Attributen (klick → Input → Speichern)
  - `PATCH /entities/{entityId}/skills` wird beim Speichern aufgerufen
  - Bei erfolgreichem Speichern: refetch des Sheets
  - i18n DE/EN
- **Akzeptanzkriterien:**
  - Skill-Wert klickbar → editierbar → gespeichert
  - Neue Werte erscheinen sofort im Sheet
  - Fallback zu globalem Wert bei leerem `skillsJson`
- **Qualitäts-Check:** TDD, UI-Test, i18n

### P19-T04: Integrationstests — Per-Character Proben
- **Status:** ✅ (2026-07-24, TDD: RED→GREEN→REFACTOR)
- **Aufwand:** 1,0 Tage

### P19-R01: Review-Nacharbeiten
- **Status:** ✅ (2026-07-24)
- **Beschreibung:** Nach Code-Review der Phase 19 behobene Mängel:
  - `SkillRow` Toast-Feedback bei Fehlern (`useToast` + `toast.error`)
  - `SkillRow` extra GET-Request entfernt (lokaler State via `skillOverrides`)
  - Irreführender Kommentar im Integrationstest korrigiert
  - `DATA-MODEL.md` um `skills_json` ergänzt
- **Beschreibung:**
  - Erstelle Charakter in Welt mit D&D, CoC, DSA System
  - Setze per-Character Skill-Werte via `PATCH /entities/{entityId}/skills`
  - Führe Probe via `POST /rolls/probe` aus → prüfe korrekten Skill-Wert
  - Prüfe Fallback: leeres `skillsJson` → globaler Bonus
  - Prüfe Edge-Cases: nicht-existenter Skill, Wert außerhalb Range
- **Akzeptanzkriterien:**
  - Alle 3 Systeme funktionieren mit per-Character Werten
  - Fallback-Mechanismus korrekt
  - Keine Regression (204 Tests)
- **Qualitäts-Check:** Integrationstests grün

---

## Phase 20: Abilities-UI

**Architektur-Entscheidung:** Abilities werden analog zu Skills aus `rulesJson.abilities[]` geparst und dynamisch im SheetResponse ausgeliefert — nicht aus der `entity_abilities`-Tabelle. Die DB-Tabelle bleibt für zukünftige Level-Up-Unlocks (Phase 21+).

### P20-T01: SheetResponse um `rulesJson.abilities[]` erweitern
- **Status:** ✅ (2026-07-24, TDD: RED→GREEN→REFACTOR)
- **Aufwand:** 1,0 Tage
- **Beschreibung:** Backend: Abilities aus `rulesJson.abilities[]` parsen und im SheetResponse ausliefern:
  - Neues `SheetResponse.AbilityInfo { name, type, apCost, effect, diceExpression }`
  - `CharacterSheetService` parst `rulesJson.abilities[]` (analog zu Skills)
  - `parseRules()` existiert bereits — `abilities`-Array daraus lesen
  - Active/Passive-Type unterscheiden
- **Akzeptanzkriterien:**
  - SheetResponse enthält `abilities[]` mit Name, Typ, AP-Kosten, Effekt
  - Alle 3 Beispielsysteme haben korrekte Abilities im Response
  - Fallback: leeres/fehlendes `abilities[]` → leere Liste
- **Qualitäts-Check:** TDD, Integrationstests

### P20-T02: Frontend — Abilities im CharacterSheet anzeigen
- **Status:** ✅ (2026-07-24, TDD: RED→GREEN→REFACTOR)
- **Aufwand:** 1,0 Tage
- **Beschreibung:** Den `abilitiesComingSoon`-Placeholder ersetzen:
  - `useSheet.ts`: `AbilityInfo`-Interface + Daten aus SheetResponse
  - CharacterSheet: Abilities-Sektion unter Conditionals
  - Darstellung: Name, Typ-Badge (ACTIVE/PASSIVE), AP-Kosten, Effekt-Beschreibung
  - Active-Abilities hervorgehoben (mit "Use"-Button), Passive grau hinterlegt
  - i18n DE/EN
- **Akzeptanzkriterien:**
  - Abilities werden korrekt angezeigt
  - Active/Passive visuell unterscheidbar
  - Bei leerer Liste: Hinweis statt Placeholder
- **Qualitäts-Check:** TDD, Frontend-Tests, i18n

### P20-T03: Ability-Nutzung aus dem CharacterSheet
- **Status:** ✅ (2026-07-24, TDD: RED→GREEN→REFACTOR)
- **Aufwand:** 1,0 Tage
- **Beschreibung:** "Use"-Button für Active-Abilities:
  - **Im Combat:** Ruft `POST /combat/{sessionId}/ability` auf (bestehender Endpoint)
  - **Außerhalb Combat:** Würfelt via `diceExpression` der Ability (freier Wurf über Frontend-RNG)
  - Ergebnis als Toast (Combat) oder ProbeRoller-ähnliches Popup (free)
  - Deaktiviert wenn kein Target gewählt (im Combat)
  - Analog zu `ProbeRoller` aber mit Ability-Kontext
- **Akzeptanzkriterien:**
  - Active-Ability im Combat nutzbar (via bestehendem Endpoint)
  - Free-Roll außerhalb Combat mit Würfel-Animation
  - Passive-Abilities haben keinen Use-Button
- **Qualitäts-Check:** TDD, UI-Test

---

## Phase 17/19 Cleanup (Review-Nacharbeiten)

### C01: API.md — fehlende Endpunkte dokumentieren
- **Status:** ✅ (API.md-Endpunkte ergänzt)
- **Aufwand:** 30 min
- **Beschreibung:** Drei Endpunkte sind nicht in API.md:
  - `PATCH /entities/{entityId}/override` — Formel-Overrides
  - `PATCH /entities/{entityId}/skills` — Per-Character Skill-Werte
  - `POST /worlds/{worldId}/entities/import` — Character importieren
- **Akzeptanzkriterien:**
  - Alle 3 Endpunkte mit Request/Response dokumentiert
  - Fehlercodes dokumentiert

### C02: FormulaOverrides i18n
- **Status:** ✅ (FormulaOverrides i18n DE/EN)
- **Aufwand:** 15 min
- **Beschreibung:** Die `FormulaOverrides`-Komponente verwendet hartcodierte Labels statt i18n-Keys:
  - `t('sheet.overridesTitle')`, `t('sheet.overridesName')`, `t('sheet.overridesValue')`
  - DE/EN in `character.json` ergänzen
- **Akzeptanzkriterien:**
  - Alle Labels via `t()` übersetzt
  - DE + EN vorhanden

### C03: Passive Abilities in EntityAbilityController anzeigen
- **Status:** ✅ (optionaler Ability-Typ-Filter)
- **Aufwand:** 30 min
- **Beschreibung:** `EntityAbilityController.list()` filtert mit `.filter(a -> "ACTIVE".equals(a.type))` → passive Abilities werden aus der API-Antwort entfernt.
  - Fix: Beide Typen ausliefern, Frontend filtert selbst
  - Oder: Optionalen `type`-Query-Parameter für Filter
- **Akzeptanzkriterien:**
  - Passive Abilities erscheinen in der API-Antwort
  - Bestehende Nutzer (ActionBar) brechen nicht

### C04: FormulaOverrides Test
- **Status:** ✅ (CharacterSheet.test)
- **Aufwand:** 30 min
- **Beschreibung:** Frontend-Test für die FormulaOverrides-Komponente:
  - Rendert ohne Overrides → "No overrides" message
  - Kann Override hinzufügen
  - Kann Override entfernen
- **Akzeptanzkriterien:**
  - Test existiert und ist grün

### C05: CharacterSheetService Test-Coverage
- **Status:** ✅ (CharacterSheetServiceTest umfangreich)
- **Aufwand:** 1,0 Tag
- **Beschreibung:** Aktuell nur 2 Tests für den SheetService:
  - Test mit per-character skill values (vorhanden)
  - Test für derivedValues + conditionals (vorhanden)
  - Fehlt: Abilities, leeres rulesJson, Entity ohne World, Sheet ohne GameSystem
- **Akzeptanzkriterien:**
  - Mindestens 2 zusätzliche Tests
  - Edge-Cases abgedeckt

### C06: InventoryService.useConsumable via RollService
- **Status:** ✅ (bewusste Abweichung: direkte DiceExpression, System-Mods sollen nicht gelten — Code-Kommentar)
- **Aufwand:** 1,0 Tag
- **Beschreibung:** `useConsumable()` verwendet `new DiceExpression(effect.heal)` direkt statt `rollService.executeRoll()`:
  - Ignoriert System-spezifische Würfelmechaniken (Pool, Fudge, etc.)
  - Fix: `RollService` injizieren und `executeRoll` nutzen
- **Akzeptanzkriterien:**
  - useConsumable respektiert System-Würfelmechanik
  - Bestehende Tests bleiben grün

---

## Phase 21: Combat-Mechanics vertiefen

### P21-T01: Critical Hits, Saving Throws, Resting im Schema
- **Status:** ✅ (2026-07-24)
- **Aufwand:** 1,0 Tage
- **Beschreibung:** Erweiterung des `dice_mechanics.combat`-Blocks:
  - `critical_hit`: `{ "threshold": 20, "multiplier": 2 }`
  - `saving_throws`: `{ "base_dc": 8, "proficiency_bonus": "floor((attr-10)/2)" }`
  - `resting`: `{ "short_rest": { "heal_percent": 0.5, "recover_resources": true }, "long_rest": { "full_heal": true, "recover_all": true } }`
  - `damage_types` bewusst nicht auf Systemebene — gehören zu Items/Fähigkeiten (siehe P21-T02)
- **Akzeptanzkriterien:**
  - Schema-Validierung akzeptiert neue Felder
  - Beispiel-JSONs können erweitert werden
- **Qualitäts-Check:** Schema-Tests, Beispiel-JSONs validieren

### P21-T02: SystemWizard Combat-Step erweitern
- **Status:** ✅ (2026-07-24)
- **Aufwand:** 1,0 Tage
- **Beschreibung:** UI-Step für die neuen Combat-Felder (Critical Hits, Saving Throws, Resting):
  - Critical Hit Konfiguration (Threshold + Multiplier)
  - Saving Throw Basis-DC + Proficient-Bonus-Formel
  - Resting-Toggle (short/long rest mit Konfiguration)
  - Alles optional (nur anzeigen wenn Combat enabled)
- **Hinweis:** `damage_types` wird NICHT auf Systemebene konfiguriert — Schadensarten sind eine Eigenschaft von Items/Waffen/Fähigkeiten und werden dort pro Item definiert (z.B. `ability.damageType` oder `item.metadata_json.damage_type`).
- **Akzeptanzkriterien:**
  - Alle neuen Felder im Wizard setzbar
  - BuildRulesJson erzeugt korrektes JSON
  - i18n DE/EN
- **Qualitäts-Check:** UI-Test, Export-Test

### P21-T03: Resting-Mechanik im Backend
- **Status:** ✅ (2026-07-24, TDD: RED→GREEN→REFACTOR)
- **Aufwand:** 1,0 Tage
- **Beschreibung:** Server-seitige Auswertung der Resting-Konfiguration:
  - `POST /entities/{entityId}/rest/short` — kurze Rast (HP-Heilung, Ressourcen-Teilregeneration)
  - `POST /entities/{entityId}/rest/long` — lange Rast (volle Heilung, alle Ressourcen)
  - Konfiguration über `rulesJson.dice_mechanics.combat.resting`
  - Bestehenden `POST /entities/{entityId}/rest`-Endpoint anpassen/erweitern
- **Akzeptanzkriterien:**
  - Short/Long Rest funktionieren laut Config
  - D&D: Short Rest = 50% HP + Ressourcen, Long Rest = Full Heal
  - CoC: (kein Resting, Config leer)
  - DSA: 8h Schlaf = 1W6 LP (optional)
- **Qualitäts-Check:** TDD, Integrationstests

---

## Phase 22: Social Mechanics (Konzeptphase)

**Ziel:** Definition eines sozialen Regel-Subsystems, das im SystemWizard konfiguriert und vom Server ausgewertet werden kann.

### P22-T01: Konzeptdefinition
- **Status:** 🔜 (Konzept)
- **Aufwand:** — (noch nicht geschätzt)
- **Beschreibung:** Ausarbeitung des `social`-Blocks im rulesJson-Schema:
  - **NPC-Reaktion:** `2d6+charisma` → Tabelle mit Ergebnissen (hostil/neutral/freundlich)
  - **Morale:** Moral-Check bei 50% Verlusten, etc.
  - **Reputation:** Fraktions-Ruf (-20 bis +20), beeinflusst Reaktionen
  - **Social Conflict:** erweitertes Überzeugungs-System (optional)
  - **Beziehungen:** Faction-Relationship-Änderungen durch soziale Aktionen
- **Status der Umsetzung:** Nach Phase 21, sobald Combat-Mechanics stabil sind.

---

## Phase 23: Items, Abilities & Damage System

Schadensarten (`damage_type`) gehören nicht auf Systemebene, sondern zu Items, Waffen und Fähigkeiten. Diese Phase definiert das Zusammenspiel.

### P23-T01: `damageType` auf `rulesJson.abilities[]`
- **Status:** ✅ (Schema + SheetResponse.AbilityInfo.damageType; `CharacterSheetServiceTest`/`RuleSchemaValidatorTest`) — Hinweis: Sheet-Anzeige; Kampf-Schadensart von Entity-Fähigkeiten kommt aus `effects_json.damageType` (kein Slash-Fallback)
- **Aufwand:** 0,5 Tage
- **Beschreibung:** Jede Ability erhält ein optionales `damageType`-Feld:
  - `rulesJson.abilities[].damageType`: `"slashing" | "piercing" | "bludgeoning" | "fire" | ...`
  - Wird im `SheetResponse.AbilityInfo` ausgeliefert
  - Schema-Validierung ergänzen
- **Akzeptanzkriterien:**
  - abilities[] mit damageType werden schema-valide akzeptiert
  - Ohne damageType: null/leer (Slash-Fallback in CombatService)
  - Beispiel: D&D 5e "Angriff (Nahkampf)" = slashing, "Angriff (Fernkampf)" = piercing
- **Qualitäts-Check:** Schema-Tests, Unit-Tests

### P23-T02: `damage_type` auf Items
- **Status:** ✅ (`items.metadata_json.damage_type` → `InventoryEntry.damageType`; ActionBar sendet die ausgerüstete Waffe als `itemId`; CombatService nutzt den Typ nur für Items im Inventar des Actors (Case-insensitiv); Badge in `ItemCard`)
- **Aufwand:** 0,5 Tage
- **Beschreibung:** Items erhalten ein `damage_type`-Feld in ihrer JSONB-Metadaten:
  - `items.metadata_json.damage_type` für Waffen-Items
  - `InventoryService` liefert damage_type im InventoryResponse mit aus
  - CombatService nutzt Waffen-damage_type wenn vorhanden, sonst Ability-damageType
- **Akzeptanzkriterien:**
  - Items mit damage_type werden korrekt ausgeliefert
  - Combat-Aktionen priorisieren Waffen-damage_type über Ability-damageType
- **Qualitäts-Check:** TDD, Integrationstests

### P23-T03: SystemWizard — Abilities-Step um damageType erweitern
- **Status:** ✅ (Select im Abilities-Step, Roundtrip-Test in `gameSystem.test.ts`, i18n DE/EN)
- **Aufwand:** 0,5 Tage
- **Beschreibung:** Im bestehenden Abilities-Step (5a) ein Dropdown für damageType hinzufügen:
  - Auswahl aus vordefinierter Liste
  - Optional (kein damageType = leer)
  - Serialisierung in `buildRulesJson` + Deserialisierung in `parseRulesToWizard`
- **Akzeptanzkriterien:**
  - damageType im Wizard setzbar
  - Export/Import korrekt
  - i18n DE/EN
- **Qualitäts-Check:** UI-Test, Export-Test

### P23-T04: Combat Damage-Type Auswertung (Resistenz/Vulnerabilität)
- **Status:** ✅ (`metadata_json.damage_resistances`/`damage_vulnerabilities` in `applyDamageModifiers`; Ability-Typ aus `effects_json.damageType`; Manöver-`damageType`; Tests in `CombatServiceTest`)
- **Aufwand:** 1,0 Tage
- **Beschreibung:** `CombatService.executeAction()` und `useAbility()` berücksichtigen damageType:
  - `damage_types`-Liste auf der Welt/dem System als Referenz (nicht als Konfiguration)
  - Entities können `metadata_json.damage_resistances` und `damage_vulnerabilities` haben
  - Resistenz: Schaden halbiert. Verwundbarkeit: Schaden verdoppelt.
  - Fallback: kein damageType → normaler Schaden
- **Akzeptanzkriterien:**
  - Feuer-Ability gegen Feuer-resistenten Gegner → halber Schaden
  - Kälte-Ability gegen Kälte-verwundbaren Gegner → doppelter Schaden
  - Ability ohne damageType → normaler Schaden
- **Qualitäts-Check:** TDD, Integrationstests

### P23-T05: Skill-Kategorien (optional)
- **Status:** ⏭️ zurückgestellt (optional, keine Gameplay-Auswirkung; nicht blockierend für P29 — aufnehmen, wenn Sheet-Gruppierung gewünscht)

> **Offenes Ticket (finaler Audit F8):** globale Game-Systeme haben keinen Owner — jeder authentifizierte User kann sie per PATCH/DELETE ändern (Bestand vor P28, mit der neuen Mächtigkeit der Regeln relevant). Gehört in eine Berechtigungs-Phase (P32+).

> **Bewusst zurückgestellt (P23/P29-Audit):** `baseValues` in Paketen ist weiterhin schema-only (auch nach P30 — der Charakter-Wizard wendet sie noch nicht an); `attackMalus` wirkt erst mit Attack-Roll-Modell; Fate-Reroll ist client-vertrauensbasiert; Spieler-Rollensicht auf Zustände; Aktions-Sperren bei Zuständen.
- **Aufwand:** 0,5 Tage
- **Beschreibung:** Skills erhalten optionales `category`-Feld:
  - `rulesJson.skills[].category`: `"strength" | "dexterity" | "knowledge" | "social" | "combat" | ...`
  - Dient der Gruppierung im CharacterSheet (Filter/Sektionen)
  - Keine gameplay-Auswirkung
- **Akzeptanzkriterien:**
  - Skills mit category werden schema-valide akzeptiert
  - CharacterSheet gruppiert/filtert nach Kategorie (optional)

---

## Gesamtstatistik

> Historischer Stand der jeweiligen Planung. Aktueller Stand: siehe „Aktueller Projektstand" oben und die Phasen-Status.

| Phase | Tasks | Sum Aufwand |
|---|---|---|
| 17 (Character-Edit & Kampf) | 6 | 11,0 Tage |
| 17 Cleanup (Review) | 6 | ~1,5 Tage |
| 18 (System-Validierung & Docs) | 4 | ~1,5 Tage |
| 19 (Per-Character Skills) | 4 | ~3,5 Tage |
| 20 (Abilities-UI) | 3 | ~3,0 Tage |
| Cleanup (C01-C06) | 6 | ~4,0 Tage |
| 21 (Combat vertiefen) | 3 | ~3,0 Tage |
| 22 (Social Mechanics) | 1 (Konzept) | — |
| 23 (Items, Abilities & Damage) | 5 | ~3,5 Tage |
| Audit-Cleanup (A01-A05) | 5 | ~2,5 Tage |

---

## Audit-Cleanup (aus API-Audit 2026-07-25)

Nach dem vollständigen API-Audit identifizierte Restpunkte — Feature-Gaps, keine Bugs:

### A01: Ability-Roundtrip + Use-Verhalten (Modell A)
- **Status:** ✅ (2026-07-25)
- **Aufwand:** 1,0 Tage
- **Beschreibung:** Backend-Endpoints existieren (`POST/GET /worlds/{worldId}/abilities`, `GET/PUT/DELETE /abilities/{id}`), aber es gibt **kein Frontend** zum Erstellen/Verwalten von Abilities.
  - Ability-Verwaltungsseite oder Integration in den SystemWizard
  - Zuweisung von Abilities an Entities (POST /entities/{id}/abilities/{abilityId}) per UI
- **Aktualisierung (2026-07-25):** Modell A beschlossen — Abilities wirken **direkt aus rulesJson im Sheet** (wie Skills, kein Zuweisungs-UI nötig). Der SystemWizard-Step 5a wird um fehlende Detailfelder (description, apCost, effectsJson, targetType) erweitert.
- **⚠️ Vermerk — Modell B (Pro-Character-Zuweisung):** Für realistisches P&P (D&D-Klassen, DSA-Sonderfertigkeiten) brauchen verschiedene Characters unterschiedliche Abilities. Die Welt-Tabelle (`entity_abilities`) + Zuweisungs-UI wird als Teil von **Phase 23 (Items, Abilities & Damage)** geplant — dort zusammen mit dem Damage-System.
- **Akzeptanzkriterien:**
  - SystemWizard Step 5a: Abilities mit allen Detailfeldern definierbar
  - Sheet zeigt Abilities vollständig (Name, Typ, AP, Effekt, damageType später)
  - Use-Verhalten funktioniert
  - Active/Passive-Typen korrekt
- **Qualitäts-Check:** TDD, i18n

### A02: Inventory add/remove/use ohne UI
- **Status:** 🔜
- **Aufwand:** 1,0 Tage
- **Beschreibung:** Backend-Endpoints existieren (`POST /inventory/add`, `/remove`, `/use/{itemId}`), aber die InventoryPage erlaubt nur Equip/Unequip per Drag & Drop.
  - "Item hinzufügen"-Dialog (Item aus Katalog wählen, Menge)
  - Item entfernen (Menge/ganz)
  - Consumables benutzen (`POST /use/{itemId}`) mit Effekt-Anzeige
- **Akzeptanzkriterien:**
  - Items hinzufügen/entfernen via UI
  - Consumable-Nutzung zeigt Heilung/Schaden
  - Mengen korrekt aktualisiert
- **Qualitäts-Check:** TDD, i18n

### A03: Fog of War Backend-Anbindung
- **Status:** 🔜
- **Aufwand:** 0,5 Tage
- **Beschreibung:** `FogController` (`POST /fog/toggle`, `GET /fog/status`) existiert, aber `fogStore` ist reiner lokaler State — keine Server-Synchronisation.
  - Fog-Zustand pro Welt im Backend persistieren
  - WebSocket-Broadcast bei Fog-Änderungen
  - StatusBar-Button nutzt den Endpoint
- **Akzeptanzkriterien:**
  - Fog-Toggle speichert Zustand
  - Andere Clients sehen Fog-Änderung live
- **Qualitäts-Check:** TDD

### A04: World-Time-Controls vervollständigen
- **Status:** 🔜
- **Aufwand:** 0,5 Tage
- **Beschreibung:** StatusBar ruft nur `POST /time/pause` und `POST /time/advance` auf. `/time/resume`, `/time/set`, `PATCH /time/mode` existieren, werden aber nicht genutzt.
  - Resume-Button (bzw. Pause/Resume-Toggle)
  - Zeit setzen (DM) + Modus-Umschaltung
- **Akzeptanzkriterien:**
  - Pause/Resume funktioniert als Toggle
  - Modus-Umschaltung (auto/manual/hybrid) via UI
- **Qualitäts-Check:** TDD, i18n

### A05: Adventure-Editor UX vervollständigen
- **Status:** 🔜
- **Aufwand:** 0,5 Tage
- **Beschreibung:** `POST /adventures/{id}/inject-choice` und `POST /adventures/{id}/abandon` existieren ohne UI. Node-Edit funktioniert jetzt (T7), aber:
  - Choice-Injection im Live-Adventure-Panel
  - Abandon-Button im Adventure-Play
  - Fehler-Toasts bei fehlgeschlagenen Node-Saves (leere catches)
- **Akzeptanzkriterien:**
  - Choice-Injection per UI
  - Abandon-Button funktioniert
  - Keine stillen Fehler mehr
- **Qualitäts-Check:** TDD, i18n

### A06: Audit-Follow-up 3-Ebenen-Modell
- **Status:** ✅ (mit P26-T02 umgesetzt)
- **Aufwand:** 0,5 Tage
- **Beschreibung:** Audit-Befund B7: `WorldEditorPage` zeigte weiterhin einen Game-System-Dropdown und sendete `gameSystemId` beim PATCH `/worlds/{id}`. Das Backend ignoriert das Feld seit P24 (Record ohne gameSystemId) — die UI suggerierte eine Zuordnung, die nicht mehr gespeichert wird.
  - System-Auswahl aus dem Welt-Editor **entfernt** (Welten sind systemunabhängig, Zuordnung erfolgt in der Kampagne)
  - GameSystem-Typ + API-Fetch entfernt
- **Akzeptanzkriterien:**
  - Welt-Editor hat keinen Game-System-Dropdown mehr ✅
  - Kein `gameSystemId` im World-PATCH-Request ✅
- **Qualitäts-Check:** TDD, i18n

## Phase 24: Datenmodell-Umbau — 3-Ebenen-Modell (System ∥ Welt → Kampagne)

> Siehe [ADR-010](ADR/010-three-tier-model-system-world-campaign.md). Systeme und Welten werden entkoppelt; Items/Abilities wandern zum System; neue Kampagne verbindet Welt × System.

### P24-T01: `items.world_id` → `game_system_id`
- **Status:** ✅ (2026-09-06 verifiziert: V090, `GameItem.gameSystemId`, Tests grün)
- **Aufwand:** 0,5 Tage
- **Beschreibung:**
  - Migration V090: `items` Spalte `world_id` → `game_system_id REFERENCES game_systems(id) ON DELETE CASCADE`
  - `GameItem.java`: Feld `worldId` → `gameSystemId`
  - `InventoryService`: Zugriffe + Repos anpassen
- **Akzeptanzkriterien:**
  - Migration läuft auf bestehender DB
  - Items sind System-zugeordnet
  - Alle Tests grün
- **Qualitäts-Check:** TDD, Migration-Test

### P24-T02: `abilities.world_id` → `game_system_id`
- **Status:** ✅ (2026-09-06 verifiziert: V091, Tests grün)
- **Aufwand:** 0,5 Tage
- **Beschreibung:**
  - Migration V091: `abilities` Spalte `world_id` → `game_system_id`
  - `Ability.java`: Feld anpassen
  - `AbilityService`: Zugriffe anpassen
- **Akzeptanzkriterien:**
  - Migration läuft
  - Abilities sind System-zugeordnet
  - Tests grün
- **Qualitäts-Check:** TDD

### P24-T03: `campaigns`-Tabelle + Domain
- **Status:** ✅ (2026-09-06 verifiziert: V092/V093, `Campaign.java` + Repository, Tests grün)
- **Aufwand:** 0,5 Tage
- **Beschreibung:**
  - Migration V092: `campaigns(id, world_id FK, game_system_id FK, name, settings_json, state_json, created_at, updated_at)`
  - `Campaign.java` Domain + `CampaignRepository`
  - Indizes auf world_id + game_system_id
- **Akzeptanzkriterien:**
  - Tabelle + Domain existieren
  - Migration-Test grün
- **Qualitäts-Check:** TDD

### P24-T04: `worlds.game_system_id` entfernen
- **Status:** ⏭️ erledigt via P25-T06 (V097)
- **Aufwand:** 0,5 Tage
- **Beschreibung:**
  - Migration V093: Spalte `game_system_id` aus `worlds` entfernen
  - `World.java`: Feld + Getter/Setter entfernen
  - `WorldController`/`WorldService`: Create/Update ohne gameSystemId
- **Akzeptanzkriterien:**
  - Welten systemunabhängig anlegbar
  - Bestehende API-Aufrufer angepasst
- **Qualitäts-Check:** TDD

### P24-T05: Campaign-CRUD-API
- **Status:** ✅ (2026-09-06 verifiziert: `CampaignController` voll CRUD, Access-Checks, E2E-create + Berechtigungen getestet)
- **Aufwand:** 0,5 Tage
- **Beschreibung:**
  - `CampaignController` + `CampaignService`:
    - `POST /api/v1/campaigns` (worldId, gameSystemId, name)
    - `GET /api/v1/campaigns` (eigene Kampagnen)
    - `GET/PATCH/DELETE /api/v1/campaigns/{id}`
  - Access-Check: nur Owner/World-Member
- **Akzeptanzkriterien:**
  - CRUD funktioniert mit korrekten Fehlercodes
  - Nur Berechtigte sehen Kampagnen
- **Qualitäts-Check:** TDD, Security

---

## Phase 25: Kampagnen-Integration (Backend)

### P25-T01: RulesLoader auf Kampagnen-Kontext
- **Status:** ✅ (2026-09-06 verifiziert: `loadRulesByCampaign` + `loadSystemByCampaign`, Tests grün)
- **Aufwand:** 0,5 Tage
- **Beschreibung:** `RulesLoader` bekommt `loadRulesByCampaign(campaignId)`:
  - Kampagne → gameSystemId → rulesJson
  - Alte `loadRules(World)`-Überladung entfernen
- **Akzeptanzkriterien:** Kampagnen-Kontext liefert korrektes System; Tests grün
- **Qualitäts-Check:** TDD

### P25-T02: CombatService + LevelUpService auf Kampagne
- **Status:** ✅ (2026-09-06 verifiziert: Combat-Start mit `campaignId` per UI → 201, `addXp` mit Kampagnen-Kontext, Tests grün)
- **Aufwand:** 1,0 Tage
- **Beschreibung:** Alle `world.getGameSystemId()`-Stellen (CombatService 4×, LevelUpService 2×) auf Kampagnen-Kontext umstellen:
  - Combat startet mit `campaignId` statt world-basiertem System
  - `StartRequest` erweitert
- **Akzeptanzkriterien:** Kampf nutzt System der Kampagne; Tests grün
- **Qualitäts-Check:** TDD, Integrationstests

### P25-T03: CharacterSheet + ProbeService auf Kampagne
- **Status:** ✅ (2026-09-06 verifiziert: Sheet/Probe mit `campaignId`, echte Probe-Ergebnisse per UI, Tests grün)
- **Aufwand:** 0,5 Tage
- **Beschreibung:** `CharacterSheetService`/`ProbeService` laden System über Kampagne statt Welt:
  - Sheet-Request bekommt campaignId (oder Entity → Kampagne)
- **Akzeptanzkriterien:** Sheet/Proben nutzen Kampagnen-System; Tests grün
- **Qualitäts-Check:** TDD

### P25-T04: Session an Kampagne binden
- **Status:** ✅ (2026-09-06 verifiziert: V094/V095/V096, `campaignId` in Session/Combat/Events, Tests grün)
- **Aufwand:** 0,5 Tage
- **Beschreibung:**
  - Migration V094: `sessions.campaign_id` (statt world-only)
  - `SessionManager` nutzt Kampagnen-Kontext
- **Akzeptanzkriterien:** Session gehört zur Kampagne; Tests grün
- **Qualitäts-Check:** TDD

### P25-T05: Item-CRUD am System
- **Status:** ✅ (2026-09-06 verifiziert: Item-CRUD per curl + UI-Inventar (equip/unequip), Tests grün)
- **Aufwand:** 0,5 Tage
- **Beschreibung:**
  - `GET/POST /api/v1/game-systems/{id}/items` (Items eines Systems listen/anlegen)
  - `GET/PUT/DELETE /api/v1/items/{id}`
  - Item-Erstellung mit type/weight/value/bonusesJson/metadataJson
- **Akzeptanzkriterien:** Items systemweit verwaltbar; Tests grün
- **Qualitäts-Check:** TDD, Security

### P25-T06: `worlds.game_system_id` entfernen
- **Status:** ✅ (2026-09-11 umgesetzt: V097 droppt die Spalte; `World`-Feld/Getter/Setter/Ctor-Param,
  `WorldService.create/update`-Parameter, Controller-Records und `WorldInfoResponse`-Feld entfernt;
  `RulesLoader.loadSystem(World)` → null, `LevelUpService` nur noch Kampagnen-Kontext;
  ActionBar ohne Welt-Fallback; Frontend-Typen optional; alte `gameSystemId`-Clients werden
  per `@JsonIgnoreProperties` toleriert. Tests: Migration-Test + `WorldCreateWithoutSystemIT`,
  PerCharacter/LevelUp auf Kampagnen umgestellt, Suite grün.)
- **Aufwand:** 0,5 Tage
- **Beschreibung:**
  - Migration V093: Spalte `game_system_id` aus `worlds` entfernen
  - `World.java`: Feld + Getter/Setter entfernen
  - `WorldController`/`WorldService`: Create/Update ohne gameSystemId
- **Akzeptanzkriterien:**
  - Welten systemunabhängig anlegbar
  - Bestehende API-Aufrufer angepasst
- **Qualitäts-Check:** TDD

---

## Phase 26: Frontend 3-Ebenen

### P26-T01: Kampagnen-CRUD-UI
- **Status:** ✅
- **Aufwand:** 1,5 Tage
- **Beschreibung:**
  - Dashboard: Kampagnen-Sektion (Liste + Erstellen-Modal)
  - Erstellen: Welt wählen + System wählen + Name
  - Kampagnen-Detail: Einstieg in Welt mit System-Kontext
- **Akzeptanzkriterien:** Kampagne anlegen/öffnen; Welt+System-Kombination korrekt
- **Qualitäts-Check:** TDD, i18n, UI-Tests

### P26-T02: Welt-Erstellung ohne System
- **Status:** ✅ (inkl. A06)
- **Aufwand:** 0,5 Tage
- **Beschreibung:** Welt-Wizard: `game_system_id`-Auswahl entfernen (Welten sind systemunabhängig)
- **Akzeptanzkriterien:** Welt ohne System anlegbar; bestehende Welten editierbar
- **Qualitäts-Check:** TDD, i18n

### P26-T03: Combat/Session/ActionBar auf Kampagnen-Kontext
- **Status:** ✅ (Stand 2026-09-11: SessionManager/StartCombatModal/useSheet/ProbeRoller/SkillList senden campaignId; ActionBar lädt action_types AUSSCHLIESSLICH aus Kampagnen-Kontext (Welt-Fallback entfernt); Combat-Start + Session-Start mit campaignId per UI E2E-verifiziert; Badge in GameView.)
- **Aufwand:** 1,0 Tage
- **Beschreibung:**
  - Combat starten aus Kampagne (campaignId statt world→System)
  - ActionBar lädt action_types über Kampagnen-System
  - Session-Start über Kampagne
- **Akzeptanzkriterien:** Kampf/System-Einstellungen funktionieren im Kampagnen-Kontext
- **Qualitäts-Check:** TDD, UI-Tests

---

## Phase 27: Shared Universes & DM-Workflow

> Geteilte Templates (Systeme/Welten mit Sichtbarkeit), Kampagnen als geforkte Universen, Bot pro Kampagne. Siehe [`ADR/011`](ADR/011-shared-universes-visibility.md). Entscheidungen (2026-09-11): Clone-on-Create (kein Copy-on-Write), Sichtbarkeit privat/Einladungsliste/öffentlich, Version-Pinning mit manuellem Nachziehen, Bot pro Kampagne (Abo-Gate später).

### P27-T01: Visibility-Modell + Ownership (Systeme + Welten)
- **Status:** ✅ Teilstand (V098: `game_systems.owner_id/visibility`, Backfill Owner/PUBLIC; Owner/Admin-Schreibschutz inkl. Abilities/Items; Lese-Guard inkl. Kampagnen-Zugriff; Kampagnen nur mit eigenen/PUBLIC/Legacy-Systemen (F8); Grant/Clone-Policy. Offen: `INVITE_ONLY`-Shares, PUBLIC-Lesepfad für Welten)
- **Aufwand:** 1 Tag
- **Beschreibung:**
  - `visibility` (`PRIVATE`/`INVITE_ONLY`/`PUBLIC`) auf `game_systems` + `worlds` (Migration)
  - `owner_id` auf `game_systems` (Migration; Bestandsrows → definierter Migrations-Owner, dokumentieren)
  - Listen filtern: eigene + `PUBLIC` + explizit geteilte (`world_members` bzw. System-Freigaben)
  - Ändern/Löschen nur Ersteller (403 für Fremde, auch bei geratenen IDs)
  - Einladungsliste nutzt bestehende Member-Mechanismen (Welt-Einladungslinks)
- **Akzeptanzkriterien:** Sichtbarkeits-Matrix per API verifiziert (privat/invite/public × owner/fremd); Fremd-Edit → 403; Tests grün
- **Qualitäts-Check:** TDD, Security

### P27-T02: Campaign-Rollen härten (Spieler + Spielleiter)
- **Status:** ✅ (Rollen-Validierung `INVALID_ROLE` 400, `PATCH /campaigns/{id}/members/{userId}` Promote/Demote, Invariante „letzter DM" als `LAST_DM` 409 mit Pessimistic-Lock; Abweichung vom alten Wortlaut: Degradierung liefert `LAST_DM` statt `DM_REMOVAL_DENIED`)
- **Aufwand:** 0,5 Tage
- **Beschreibung:**
  - Rollen-Enum `DM`/`PLAYER` statt freiem String (Backend-Validierung in `addMember`)
  - DM kann Mitglieder zu DMs befördern / degradieren
  - Invariante: mindestens 1 DM pro Kampagne (`DM_REMOVAL_DENIED` auch bei Degradierung des letzten DMs)
- **Akzeptanzkriterien:** Multi-DM-Kampagne funktioniert (2 DMs verwalten Mitglieder); letzter DM nicht entfernbar/degradierbar; Tests grün
- **Qualitäts-Check:** TDD, Security

### P27-T03: Fork bei Kampagnen-Erstellung (eigenes Universum)
- **Status:** ✅ Teilstand (Fork beim Kampagnen-Start inkl. Feldlücken-Fix Skills/XP/HP/AP/Abilities/Fraktionsanführer/Hauptstadt/Wetter; Mirroring Campaign→World-Rollen (nur Fork-Welten); Quota zählt Forks nicht; Kampagne-Delete soft-deletet Fork. Offen/dokumentiert: Quests/Adventures werden nicht mitgeklont, Mirroring umgeht Welt-Member-Quota)
- **Aufwand:** 1 Tag
- **Beschreibung:**
  - Kampagne anlegen = Welt tief kopieren (`WorldService.clone`: Regionen, Orte, NPCs, Fraktionen, Entities, Karten), Kampagne zeigt auf den Fork
  - UI-Hinweis „erstellt eigene Kopie" im Erstellen-Modal; Template bleibt unverändert (per Test nachweisen: Template-Entities vs. Fork-Entities divergieren)
  - Geteilte Kampagnen-Welten lassen sich später über P27-T01-Mechanismus weiterteilen
- **Akzeptanzkriterien:** Template nach Kampagnen-Aktionen unverändert; Fork unabhängig bespielbar; Fraktionsführer-Tod im Fork betrifft Template nicht; Tests grün
- **Qualitäts-Check:** TDD, E2E (Playwright: Template → Kampagne → NPC töten → Template prüfen)

### P27-T04: Bot pro Kampagne konfigurierbar
- **Status:** 🔄 Teilstand (Kampagnen-`settingsJson` mit validiertem `bot.mode` (autonom|suggest|off) via PATCH; Intents tragen `campaign_id` (V101) und nutzen Kampagnen-Modus vor Welt-Modus; UI-Select mit Welt-Fallback. Offen: Bot-Runtime pollt weiter Welt-weit — Multi-Tenant-Polling braucht einen internen Welten-Endpoint/Zuordnung (separates Ticket))
- **Aufwand:** 1 Tag
- **Beschreibung:**
  - Bot-Konfiguration in `campaigns.settings_json` (`{"bot": {"mode": "autonom|suggest|off"}}`, keine neue Spalte)
  - Bot liest Konfiguration pro Kampagne (statt nur Welt-`ai_mode`); `off` pausiert Polling für die Kampagne
  - Kampagnen-UI: Bot-Schalter (an/aus/Modus) in Kampagnen-Detail
  - Abo-Gate nur als Stub/Flag vorbereiten (keine Logik — aktuell unwichtig)
- **Akzeptanzkriterien:** Bot reagiert nur in Kampagnen mit aktivem Modus; `off` → keine Intents; Tests (MockLLMClient) grün
- **Qualitäts-Check:** TDD

### P27-T05: System-Versionierung (Pin + Nachziehen)
- **Status:** ✅ (Option B/Snapshot: V100 `campaigns.rules_json_snapshot`+`game_system_version`; `RulesLoader` bevorzugt Pin; `POST /campaigns/{id}/pull-system` (DM) zieht nach; UI-Badge + Update-Button; Tests. Hinweis: Abilities/Items bleiben system-gebunden — Versionierung betrifft `rulesJson`)
- **Aufwand:** 1 Tag
- **Beschreibung:**
  - Kampagnen pinnen System-Version zum Erstellungszeitpunkt (stabile Regeln während der Kampagne)
  - „Update verfügbar"-Anzeige bei neuerer Version desselben Systems (Namens-Match + Versionsvergleich)
  - Manuelles Nachziehen (Button; Re-Point auf neue Version + Validierung); automatisiertes Nachziehen explizit später
- **Akzeptanzkriterien:** Laufende Kampagne bleibt auf alter Version; Update-Flow per UI + API verifiziert; Tests grün
- **Qualitäts-Check:** TDD

### P27-T06: DM-Queue fertig + Abnahme
- **Status:** 🔄 Teilstand: DM-Gate fuer List/Approve/Reject + 403-Hide im Panel erledigt; offen: Bulk/Filter, WS-Liveupdate statt 5s-Polling, E2E Bot→Queue→Entscheid)
- **Aufwand:** 1 Tag
- **Beschreibung:**
  - Aktionsliste: Bulk-Freigabe/Ablehnung, Filter (Typ/Status), WS-Live-Update statt 5s-Polling wo sinnvoll
  - E2E nach TESTING-Plan (Bot-Intent → Queue → DM-Entscheid → Welt-Effekt sichtbar)
  - Doku final: ARCHITECTURE-Sharing-Modell gegenprüfen, API-Visibility-Parameter + Fork-Verhalten dokumentieren
- **Akzeptanzkriterien:** DM arbeitet Queue vollständig per UI ab; Doku ohne Divergenzen; Tests grün
- **Qualitäts-Check:** TDD, E2E (Playwright)

---

## Gesamtstatistik (aktualisiert)

> Historischer Stand der jeweiligen Planung. Aktueller Stand: siehe „Aktueller Projektstand" oben und die Phasen-Status.

| Phase | Tasks | Sum Aufwand |
|---|---|---|
| 24 (Datenmodell 3-Ebenen) | 5 | ~2,5 Tage |
| 25 (Kampagnen-Integration) | 5 | ~3,0 Tage |
| 26 (Frontend 3-Ebenen) | 3 | ~3,0 Tage |
| 27 (Shared Universes & DM-Workflow) | 6 | ~5,5 Tage |
| 28 (Generische Engine-Bausteine) | 6 | ~6,0 Tage |
| 29 (Spielgefühl + Pakete) | 6 | ~6,0 Tage |
| 30 (Charakter-Wizard) | 4 | ~4,0 Tage |
| 31 (E2E-Ausbau) | 3 | ~1,0 Tag |

**Stand 2026-09-11:** P28–P31 abgeschlossen, alle Suiten grün (362/168/7), Audits (P28, P23/P29, P30, final) abgearbeitet.

---

## Phase 28: Generische Engine-Bausteine

> Wizard bildet generische Mechanik ab (kein DSA-Klon). Referenz: DSA-Heldenerschaffung + Grundregeln (Regelwiki). Leitprinzip: **Engine, nicht Inhalt**. Siehe [`ADR/012`](ADR/012-generic-wizard-engine.md). **Detail-Design (Datenmodell, Backend, UI/UX, Tests): [`WIZARD-PLAN.md`](WIZARD-PLAN.md) §3.** Voraussetzung: P23 (`damageType`) vor R4-bezogenen Arbeiten.

### P28-T01: Schema + Wire-Format öffnen
- **Status:** ✅ (2026-09-11: DEFAULT_SCHEMA akzeptiert die neuen Keys; Roundtrip-Tests; alte Systeme unverändert; committed `cb50ac9`)
- **Aufwand:** 1 Tag
- **Beschreibung:**
  - Neue Top-Level-Keys im Backend-Schema (`additionalProperties: false` beachten): `creationBudget`, `attributeCosts`, `packages`, `traits`, `advancement`, erweiterte `derived_values`-Einträge — strikt abwärtskompatibel (alte Systeme validieren weiter)
  - `toRulesJson`/`fromRulesJson` für alle neuen Blöcke; Backend-Konsumenten (RulesLoader, ProbeService, Sheet, LevelUp, Validator) kennen die Keys
- **Akzeptanzkriterien:** Altes D20Lite-System validiert + läuft unverändert; neues Minimalbeispiel mit allen Keys validiert; Tests grün
- **Qualitäts-Check:** TDD
- **Bemerkungen (2026-09-11):**
  - Vertagt (bewusst): strenge Shapes für `packages`/`traits`/`advancement`-Einträge — kommen in T03/T04 mit ihren Features (permissive Container bis dahin)
  - Backend-Konsumenten brauchten keine Änderung: alle Reads sind bereits defensiv (`.path()`/Defaults)
  - Kein UI (reine Grundlage)

### P28-T02: AP-Budget + Caps + Attribut-Kostenkurven
- **Status:** ✅ (2026-09-11: Schema-Shapes, `calcBudget`/`attrPointCost` (11 Tests), Budget-Step mit Caps/Kurven-Editor/BudgetBar, Save-Block bei Überziehung; Browser-Smoke ok; committed `c88f992`)
- **Aufwand:** 1 Tag
- **Beschreibung:**
  - `creationBudget`: AP-Topf, Maxima (Attribute gesamt/einzeln, Skills, Kampf, Zauber), Startwerte, Schicksalspunkte-Basis
  - Kostenstaffel pro Attribut (z. B. 15 AP bis 14, dann 30/45/60/… wie DSA)
  - Wizard-Step mit Live-Kostenanzeige + Budget-Balken; Budget-Gate im Frontend (Prüfbericht), Backend validiert das Schema (keine Budget-Prüfung — s. Bemerkungen)
- **Akzeptanzkriterien:** 100-AP-Paket nachbaubar (DSA: 8×8 Start, Summe ≤ Max); Überziehung wird rot + blockiert Save; Tests grün
- **Qualitäts-Check:** TDD
- **Bemerkungen (2026-09-11) — vertagte Teile:**
  - **Per-Attribut-Kostenkurven-Editor (UI):** Modell (`attribute.costs`) + Serialisierung existieren, Wizard editiert nur die globale Kurve (`attributeCosts.default`). Nachziehen, sobald ein System unterschiedliche Kurven pro Attribut braucht
  - **Kosten-Vorschau pro Attribut-Zeile:** nur Aggregat-BudgetBar; Zeilen-Vorschau („8→14 = 90 AP") nachrüsten mit dem Heldenbau
  - **Budget-Enforcement im Backend:** bewusst nicht — System-Save validiert nur Shapes; Durchsetzung gehört in den Heldenbau (später)
  - **`calcBudget` zählt nur Attribute:** Skills/Traits/Zauber fließen in T03/T04 in die Summe ein; bis dahin kann die Anzeige zu niedrig sein
  - **Cap-Prüfung:** `maxAttrValue` & Co. werden validiert (Shape), aber (noch) nicht im Wizard erzwungen

### P28-T03: Traits-Katalog (Vor-/Nachteile)
- **Status:** ✅ (2026-09-11: Schema streng (kind/effects/costs), Sheet wendet gewählte Traits aus `metadataJson.traits` an (Attribut-/Derived-Effekte, Tier-Suffix wird ignoriert), Wizard-Step „Merkmale" mit Katalog-Editor + Dangling-Warnung; committed `e544bd5`)
- **Aufwand:** 1,5 Tage
- **Beschreibung:**
  - `traits[]`: Name, Art (Vorteil/Nachteil), Kosten fest oder gestaffelt (Stufen wie I–III), Prerequisites (Traits/Spezies/Kultur), Exklusionen („nicht: X"), Effekt-Hooks (Basiswert-Boni, Freischaltungen)
  - Konfigurierbare Schranke (z. B. max. 80 AP Vorteile, DSA-Referenz)
  - Wizard-UI: Katalog mit Filter, Kosten-Summe, Konflikt-Warnung bei Exklusionen
- **Akzeptanzkriterien:** Glück-II-äquivalent (gestaffelt) + Ausschluss-Verletzung wird erkannt; Traits wirken auf Derived (z. B. AsP nur mit Zauberer-Äquivalent); Tests grün
- **Qualitäts-Check:** TDD

### P28-T04: Steigerung (Spalten, Matrix, Aktivierung)
- **Status:** ✅ (2026-09-11: Schema streng, Max-Regel 422 `SKILL_MAX_EXCEEDED` bei PATCH skills mit `campaignId`, Sheet liefert `advanceCost` (Aktivierung/Matrix), Wizard-Spalte+Matrix-Editor; committed `3bd305a`)
- **Aufwand:** 1,5 Tage
- **Beschreibung:**
  - Kosten-Spalte pro Skill (A/B/C/D-Äquivalent), Aktivierungskosten (Zauber/Liturgien vs. auto-aktive Talente), globale Kostenmatrix pro Stufe
  - Max-Regel: Skill ≤ höchstes beteiligtes Attribut +2 (Backend-Enforcement, 422 mit Code)
  - Sheet zeigt Steigerungskosten-Vorschau pro Skill
- **Akzeptanzkriterien:** Stufenweises Steigern mit korrekten Kosten; Aktivierung neuer Skills kostet; Max-Verletzung 422; Tests grün
- **Qualitäts-Check:** TDD
- **Bemerkungen (2026-09-11) — vertagte Teile:**
  - **Globale `activationCosts`-Map gestrichen:** Skills haben keinen Typ (spell/liturgy/…) — stattdessen `activationCost` pro Skill; Map wäre toter Config gewesen
  - **Max-Regel gilt für den gespeicherten Override-Wert**, nicht die Sheet-Gesamtanzeige inkl. Attributs-Modifikator (Heldenbau präzisiert)
  - **Attribute aus `rules`-Defaults** fließen nicht in die Max-Regel ein, wenn die Entity keine Attribute gespeichert hat (Audit-Befund)
  - Legacy-Feld `attribute` (Singular) wird von der Max-Regel nicht gelesen (Schema erlaubt es)
  - Steigerungs-Vorschau im Sheet nur als Badge; keine AP-Ledger-Deduktion (Heldenbau)

### P28-T05: Derived deluxe (Tabellen + Bedingungen)
- **Status:** ✅ (2026-09-11: `input`+`table`-Lookup mit Fehlergrund, `requiresTrait` lässt Eintrag weg, `DerivedValueInfo.error` mit Grund statt „(Fehler)"-Suffix, Wizard Tabellen-Modus + Merkmal-Select, Sheet zeigt Fehler mit Tooltip; committed `a282476`)
- **Aufwand:** 1 Tag
- **Beschreibung:**
  - `derived_values`-Einträge mit Tabellen-Lookup (Wertebereich → Ergebnis, z. B. SK/ZK-Summe) und `requiresTrait` (nur mit Vorteil/X vorhanden)
  - Spezies-Basis als Variable (Grundwert + Formel)
  - FormulaEvaluator-Erweiterung per TDD (TDD-Pflicht: jede neue Syntax mit Parser-Tests)
  - Wizard-Editor für Tabellen + Vorschau mit Beispiel-Attributen
- **Akzeptanzkriterien:** SK-Tabellen-Äquivalent rechnet korrekt; AsP-Äquivalent ohne Trait → als fehlend markiert (statt „(Fehler)"-Überraschung nur mit Erklärung); Tests grün
- **Qualitäts-Check:** TDD
- **Bemerkungen (2026-09-11) — vertagte Teile:**
  - **Keine FormulaEvaluator-Syntaxerweiterung nötig** — Tabellen/Bedingungen sind neue Semantik (input + table), kein neuer Parser
  - **Wizard-Vorschau mit Beispiel-Attributen gestrichen** (bräuchte JS-Zwillings-Evaluator); Tabellen-Editor zeigt Zeilen, Backend rechnet
  - **Tabellen-Überlappung = First-Match** (Plan wollte Fehler; Lücke bleibt Fehler) — siehe Audit P28
  - **Spezies-Basis als Variable** hängt an P29-Paketen (noch nicht vorhanden)
  - Override + Trait-Effekt auf denselben Derived-Wert addieren sich (gewollt, additiv)

### P28-T06: Abnahme Engine-Bausteine
- **Status:** ✅ (2026-09-11: `p28-reference.json` validiert + Sheet rechnet Tabelle/Traits/Advancement; Doku entdupliziert; committed `8876435`)
- **Aufwand:** 0,5 Tage
- **Beschreibung:**
  - Referenz-System nutzt alle P28-Blöcke und validiert; Doku (RULES-SCHEMA, Wizard-Hilfe) aktuell
  - Regression: alte Systeme (D20Lite/TwoDicePool/Fudge) laufen unverändert (E2E-Stichprobe)
- **Akzeptanzkriterien:** Referenz-E2E grün; Doku ohne Divergenzen
- **Qualitäts-Check:** E2E (Playwright)

---

## Phase 29: Spielgefühl + Pakete

> R-Reihenfolge: Zustände → Schicksal → Manöver → Rüstung. Pakete (G4) hier, nicht in P28. DSA-Content als Abnahme. **Detail-Design: [`WIZARD-PLAN.md`](WIZARD-PLAN.md) §4.**

### P29-T01: Zustände/Status-Engine
- **Status:** ✅ (ConditionService: Katalog+Instanzen, Probe-/Schadens-Mod, Tick bei nextTurn+Combatstart, Katalog-`rounds` als Default, DM-Gate fürs Anwenden/Entfernen, Lock-Read; Sheet-Badges; `d490fce`) — Teilstand: Aktions-Sperren noch nicht abgebildet, Vergeben ist DM-only (Spec), Spieler-Rollensicht später
- **Aufwand:** 1,5 Tage
- **Beschreibung:**
  - Generische Conditions mit mechanischen Effekten (Modifikatoren, Aktions-Sperren, Tick-Auflösung) — DSA-Zustände wie D&D-Conditions aus denselben Bausteinen
  - UI: Status-Badges am Charakter/Token, Vergeben/Entfernen (DM), Anzeige im Sheet
- **Akzeptanzkriterien:** Zustand modifiziert Probe/Schaden nachweislich; Ablauf-Timing korrekt; Tests grün
- **Qualitäts-Check:** TDD

### P29-T02: Schicksalspunkte
- **Status:** ✅ (spendFatePoint + Re-Roll im ProbeRoller; Pessimistic-Lock gegen Doppelausgabe; `4a9df44`) — Teilstand: „+1 Bonus"/„Tod abwenden" noch nicht implementiert; Reroll ist client-vertrauensbasiert (wie alle Würfe), Server-Kopplung später
- **Aufwand:** 0,5 Tage
- **Beschreibung:**
  - Meta-Währung pro Charakter: Neu würfeln, +1 Bonus, Tod abwenden; Startwert + Refresh-Regel aus `creationBudget`
  - UI: Punkte-Anzeige + Ausgeben-Button im Sheet/Probe-Modal
- **Akzeptanzkriterien:** Ausgeben wirkt (Re-Roll), kein Ausgeben bei 0; Tests grün
- **Qualitäts-Check:** TDD

### P29-T03: Kampfmanöver-Framework
- **Status:** ✅ (`dice_mechanics.combat.maneuvers[]`: apCost+effects[dmg]; POST /combat/{id}/maneuver + ActionBar-Buttons mit AP-Gate; TDD Backend 350/Frontend 150 zum Task-Abschluss, heute 362/168 (plus E2E 7) — Teilstand: `attackMalus` ohne Wirkung (kein Attack-Roll-Modell), voller Playwright-E2E im E2E-Paket nach P29
- **Aufwand:** 1,5 Tage
- **Beschreibung:**
  - Generisches Tausch-Prinzip: Angriffsmalus gegen Effekt (Schaden+, Spezial) — Wuchtschlag/Finte als Content, Framework als Engine
  - Manöver als System-Konfiguration (Voraussetzung, Kosten, Effekt-Formel); ActionBar zeigt verfügbare Manöver
- **Akzeptanzkriterien:** Manöver mit Malus/Effekt E2E verifiziert; ohne Manöver alles wie bisher; Tests grün
- **Qualitäts-Check:** TDD, E2E (Playwright)

### P29-T04: Rüstung + Schadenstypen
- **Status:** ✅ (P23-T01–T04 als Basis; flache Rüstung `metadata_json.damage_armor` vor Resistenz/Vulnerabilität; Sheet zeigt Rüstung, Kampf-Log mit Schadensart; Zonen bewusst nicht (optional); voller Playwright-E2E im E2E-Paket nach P29)
- **Aufwand:** 1 Tag
- **Beschreibung:**
  - Rüstungswerte (Zonen optional), Schadenstypen, Resistenzen/Vulnerabilitäten in der Schadensberechnung
  - **Abhängigkeit:** P23 (`damageType`) muss fertig sein — sonst zurückstellen
  - UI: Rüstung am Charakter/Inventar sichtbar, Schaden nach Typ aufgeschlüsselt im Log
- **Akzeptanzkriterien:** Resistenz halbiert, Vulnerabilität verdoppelt (E2E); Tests grün
- **Qualitäts-Check:** TDD, E2E (Playwright)

### P29-T05: Pakete (Spezies/Kultur/Profession)
- **Status:** ✅ (`packages[]` Schema+PkgDef: kind/cost/attributeMods (Fest + Choice-Gruppen `["MU","KK"]`/`"*"`)/autoTraits/baseValues/recommended/restricted; Wizard-Step 12 mit Editor + Live-Vorschau (Mods, Kosten, Auto-Traits, Fehler/Warnungen); Helper `resolvePackageMods`/`packageSelectionIssues` etc. mit Elf-Abnahme-Tests; i18n DE/EN)
- **Aufwand:** 1,5 Tage
- **Beschreibung:**
  - `packages[]`: Typ, AP-Kosten, Attribut-Mods mit Choice-Gruppen („MU *oder* KK −1", „eine beliebige +1"), Basiswerte, Auto-Traits, empfohlene/eingeschränkte Folge-Pakete (Kultur-Restriktionen)
  - Wizard-Schritt mit Auswahl + Live-Vorschau (Kosten, Mods, Warnung bei untypischen Kombinationen)
  - Validierung: Choice-Gruppen exakt 1 Treffer, Auto-Traits ohne Zusatzkosten
- **Akzeptanzkriterien:** Elf-Äquivalent (Mods + Auto-Vorteil + 18 AP) nachbaubar; untypische Kombi warnt; Tests grün
- **Qualitäts-Check:** TDD

### P29-T06: DSA-Referenzcontent + Abnahme
- **Status:** ✅ `docs/examples/dsa5.json` auf P23/P28/P29-Format (Budget, Staffeln, Traits, Matrix, Tabellen-Derived, Pakete inkl. 100-AP-Profession, Zustände, Manöver, Schadensarten); Abnahme-Tests: Schema, Sheet (sk-Tabelle 7, asp requiresTrait), 3W20 mit FW-Ausgleich, Paket-Auswahl (Elf/Waldelf/Jäger, restricted); TESTING.md §15 Checkpoints
- **Aufwand:** 1 Tag
- **Beschreibung:**
  - `docs/examples/dsa5.json` auf neues Format heben (Budget, Staffeln, Traits, Matrix, Tabellen-Derived, Pakete)
  - E2E-Heldenbau nach DSA-Regeln: 100-AP-Paket, 3W20-Probe mit FW-Ausgleich, Basiswerte inkl. Tabellen
  - Doku final (RULES-SCHEMA, TESTING-Checkpoints für P28/P29)
- **Akzeptanzkriterien:** DSA-E2E grün; Doku ohne Divergenzen
- **Qualitäts-Check:** E2E (Playwright)

---
## Phase 30: Charakter-Wizard (Konsument der P28/P29-Engine)

> Ziel: Pakete/Budget/Traits werden beim Erstellen eines PCs tatsächlich angewendet (Live-Vorschau + Speichern als Entity). Reine Frontend-Engine auf bestehenden Endpunkten (`POST /worlds/{id}/entities`) — kein Backend-Sonderweg. Abhängigkeit: P28/P29 (erledigt).

### P30-T01: Build-Helper (Pakete + Attribut-Kauf + Traits → Kosten/Endwerte)
- **Status:** ✅ (`CharacterBuild`, `buildCost`, `buildFinalAttributes`, `buildFinalTraits`, `buildIssues`; 4 Tests in `characterBuild.test.ts`)
- **Beschreibung:** `CharacterBuild`-Typ + pure Helper in `gameSystem.ts`:
  - Endattribute = gekaufter Wert + Paket-Mods (Kaufkosten auf dem gekauften Wert, Mods danach)
  - AP-Aufschlüsselung (Attribute/Traits/Pakete) gegen `creationBudget`, Auto-Traits kostenlos (im Paketpreis)
  - Issues: Auswahl-Fehler (P29), Attributgrenzen, Budget-Überschreitung, Trait-Exklusionen
  - Trait-Tiers (`Hohe Lebenskraft III`), End-Traits = gewählte + Auto-Traits (dedupliziert)
- **Akzeptanzkriterien:** Elf-Beispiel: 18 AP Paket + Attributkauf korrekt; Budget-Over wird Fehler; Tests grün
- **Qualitäts-Check:** TDD (Vitest)

### P30-T02: Wizard-UI — Pakete + Attribute mit BudgetBar
- **Status:** ✅ (`CharacterWizard.tsx`: Steps Pakete/Attribute mit Live-Budget, Choice-Auswahl, Mod-Vorschau; Entry-Button in `EntityListPage` nur bei System mit `creationBudget`/`packages`; i18n DE/EN)
- **Beschreibung:** `CharacterWizard`-Modal (Steps: Pakete → Attribute → Traits → Übersicht), Live-Vorschau, Warnen statt blockieren; Entry in `EntityListPage`, nur wenn aktive Kampagne ein System mit `creationBudget`/`packages` hat
- **Akzeptanzkriterien:** Attribut-Stepper live gegen Budget; Paket-Mods sichtbar; i18n DE/EN
- **Qualitäts-Check:** UI-Test (Vitest) + tsc

### P30-T03: Traits + Übersicht + Speichern
- **Status:** ✅ (Trait-Checkboxen mit Tiers + Auto-Trait-Chips, Übersicht mit Kosten/Issues/Warnungen, Save gated; Entity-Payload: Endwerte in `attributesJson`, `metadataJson.traits` + `package_selections` + `fate_points`; 2 Komponententests)
- **Beschreibung:** Trait-Auswahl (Tiers, automatische aus Paketen), Übersicht mit Endwerten/Kosten/Issues; Save legt PC an (`attributesJson` = Endwerte, `metadataJson.traits` = End-Traits, `metadataJson.package_selections`)
- **Akzeptanzkriterien:** Speichern erzeugt PC; Sheet zeigt Traits/Attribute; Tests grün
- **Qualitäts-Check:** TDD

### P30-T04: Abnahme — DSA-Heldenbau E2E
- **Status:** ✅ (Audit-Fixes: Caps `maxAttrValue`/`maxAttrTotal`/`maxAdvantageAp` + Endwert-Prüfung nach Mods + `requires`-Prüfung; Kampagnen-Reconcile beim Laden + Welt-Guard; Race/Stale im Rules-Fetch; Tier-Default, Close-während-Save, Escape/Dialog-Semantik, Code-i18n; E2E-Cleanup. Playwright `character-wizard.spec.ts`: System/Kampagne seeden → Wizard (Elf + Choice + Attributkauf + Zauberer) → Save → Sheet-API prüft mut 10 / klugheit 7 / fate 3; campaignStore persistiert aktive Kampagne reload-safe; Login-Setup nutzt gültigen State bei 429)
- **Beschreibung:** Playwright: System/Welt/Kampagne per API seeden → Charakter-Wizard → Elf+Waldelf, Attribute kaufen, Trait wählen → Speichern → Sheet prüft Werte; TESTING-Checkpoint
- **Akzeptanzkriterien:** E2E grün; Doku aktualisiert
- **Qualitäts-Check:** E2E (Playwright)

---

## Phase 31: E2E-Ausbau (TESTING §15.5)

### P31-T01: Sheet-E2E (DSA-Referenz)
- **Status:** ✅ (`sheet-dsa.spec.ts`: sk=7 im UI, asp nur mit Zauberer (19.5), Rüstung 3 aus `damage_armor`; ohne Trait/Rüstung kein Badge)
- **Beschreibung:** Playwright: dsa5.json-System + Welt + Kampagne + Entity per API seeden → Charakter-Sheet im UI zeigt sk=7, asp nur mit Zauberer, Rüstung
- **Qualitäts-Check:** E2E

### P31-T02: Kampf-E2E (Manöver/AP/Zustände/Schadensarten)
- **Status:** ✅ (`combat-maneuver.spec.ts`: Kampfstart im UI → Ziel wählen → Wuchtschlag (Kosten 2) → AP-Gate sperrt Button; Zustands-/Schadensarten-Log weiter manuell)
- **Beschreibung:** Playwright: Kampf starten, Manöver-Button (AP-Gate), Zustands-Tick, Schadensarten-Log
- **Qualitäts-Check:** E2E

### P31-T03: Save-Gate-E2E + finale Abnahme
- **Status:** ✅ (`wizard-save-gate.spec.ts`: ohne Attribute bleibt Modal offen + Toast „Mindestens ein Attribut anlegen…", kein System angelegt) — Gesamt-Audit folgt
- **Beschreibung:** Playwright: neues System ohne Attribute → Speichern blockiert; Gesamt-Audit mit allen Suiten (Backend/Frontend/E2E)
- **Qualitäts-Check:** E2E + Audit

---

---

## Phase 32: Content- & E2E-Ausbau (nach P27-Audit)

### T32-T01: Beispiel-Content dnd5e/coc7e auf P23/P28-Niveau
- **Status:** ✅ (`creationBudget`/`conditions`/`damageType` ergänzt, `description` entfernt; Schema-Validierung beider Beispiele + Frontend-Roundtrip-Test)

### T32-T02: Playwright System-Pin + Nachziehen
- **Status:** ✅ (`system-pin.spec.ts`: Regeln ändern → Version bumpt, Kampagne gepinnt → Badge → UI-Pull → Pin aktualisiert)

### T32-T03: Weitere E2E-Lücken (Backlog)
- **Status:** ⏳ (Zustands-Tick + Schadensarten-Log im Kampf; Bot-Intent→Queue→DM-Entscheid; Fork-E2E Template-unverändert)

---

## Phase 33: Backlog-Abbau & Härtung (Plan 2026-09-12)

> Ziel: offene Teilstände aus P27/P31/P32 schließen + alte Lücken (P14/P22). Reihenfolge = Risiko × Nutzen: erst schnelle Härtung, dann Fork-Vervollständigung, dann Bot-Runtime, dann DM-Queue; P14/P22 zuletzt. Nach jedem Block (3–5 Tasks) Audit + Findings-Fix, Abschluss mit Gesamt-Audit über alle Suiten.

### T33-01: E2E — Zustands-Tick + Schadensarten-Log
- **Status:** ✅ (`combat-damage.spec.ts`: Feuer-Waffe → Log „(fire)"; Rüstung 100 deterministisch 0 Schaden; `rounds:2` tickt über zwei Züge; Session-Cleanup. Resistenz-Mathematik bleibt Unit-getestet — E2E belegt Pipeline+Armor)
- **Aufwand:** 0,5 Tage
- **Beschreibung:** Neue Spec `frontend/e2e/combat-damage.spec.ts`: System mit `conditions` (Wunde: probe −4, rounds 2) + Waffe `damage_type: fire` + NPC mit `damage_resistances:["fire"]`/`damage_armor` per API seeden; Kampf im UI starten; Zustand per API (DM) auf den Actor legen → Probe/Angriff zeigt reduzierten Schaden im Log (`(fire)`, halbiert), nach 2× `next-turn` ist der Zustand weg (Sheet-Badges/API prüfen).
- **Akzeptanz:** E2E grün; Zustand tickt nach `rounds`; Resistenz halbiert sichtbar im Log
- **Abhängigkeit:** keine · **Qualitäts-Check:** Playwright

### T33-02: Welt-PUBLIC-Lesepfad (T01-Rest, V098-Spalte nutzen)
- **Status:** ✅ (`WorldAccess.requireRead` + PRIVATE/PUBLIC-Semantik; Reads: World/Entities/Regions/Locations/Quests/Map; `findAccessibleByUserId` inkl. PUBLIC, PRIVATE-Mitglieder raus; `WorldInfoResponse.visibility`; UI Badge + Sichtbarkeits-Select (owner-gated); IT `WorldVisibilityQueryIT`. Offen: Dashboard-FILTER nach Sichtbarkeit (nur Badge))
- **Aufwand:** 0,5 Tage
- **Beschreibung:** `WorldAccess.requireRead(worldId,userId)` (Owner ∪ Member ∪ PUBLIC; PRIVATE = Owner only) einführen und alle reinen LESEpfade darauf umstellen (WorldService.getById, Locations/Regions/Entities-Reads, Sheet/Probe bleiben requireAccess = Schreiben/Spielen). `WorldService.listAccessible` um PUBLIC-Welten ergänzen; `WorldInfoResponse.visibility` liefern; UI: Badge/Filter „Öffentlich" im Dashboard; Dokumentation in ADR-011 ergänzen.
- **Akzeptanz:** Fremder liest PUBLIC-Welt (200) und PRIVATE nicht (403); Owner/Member unverändert; Tests grün
- **Abhängigkeit:** — · **Qualitäts-Check:** TDD

### T33-03: Member-Quota beim Campaign-Mirroring
- **Status:** ✅ (`syncWorldMember` prüft `checkCanAddMember` vor Neuanlage → `WORLD_MEMBER_LIMIT` 403; Rollen-Updates bleiben frei; Test)
- **Aufwand:** 0,25 Tage
- **Beschreibung:** `CampaignMemberService.syncWorldMember` prüft vor dem Anlegen eines NEUEN World-Members `quotaService.checkCanAddMember(worldId, plan des Fork-Owners)`; bei Limit → `CampaignException("WORLD_MEMBER_LIMIT")`; Rollen-Updates bestehender Member bleiben erlaubt. Plan-Ermittlung wie in WorldService (Owner des Fork-Welts).
- **Akzeptanz:** Kampagnen-Add über Welt-Limit schlägt sauber fehl (403), keine Orphan-Member; Tests
- **Abhängigkeit:** — · **Qualitäts-Check:** TDD

### T33-04: Fork vervollständigen (Quests + Adventures)
- **Status:** ✅ (Quests inkl. Giver/Location-Remap; Adventures + Nodes + **Choices/Edges** inkl. Start-/Skill-Check-Knoten-Remap; Test. AdventureProgress bewusst nicht kopiert — Forks starten frisch, im Code dokumentiert; Quest-objectives/-rewards-IDs bleiben bekannte Einschränkung)
- **Aufwand:** 1 Tag
- **Beschreibung:** Repo-/Modell-Analyse, dann `WorldService.cloneWorld`: Quests (`quests.world_id`) und Adventures (`adventures.world_id`, Nodes/Progress) mitkopieren; Referenzen auf Entities/Locations via vorhandene `entityIdMap`/`locationIdMap` remappen (Giver/Location/Owner-Felder prüfen); Bewusst NICHT kopieren: History-Tabellen (Events, Intents, Sessions) — im Code dokumentieren.
- **Akzeptanz:** Fork enthält alle Template-Quests/Adventures mit korrekten Referenzen; Template bleibt unberührt; Tests + E2E-Erweiterung in T33-01-Umfeld
- **Abhängigkeit:** — · **Qualitäts-Check:** TDD

### T33-05: System-Shares (`INVITE_ONLY` scharf schalten)
- **Status:** ✅ (V102 `game_system_shares` statt V103; Shares in canRead/listVisible/requireUsable/Ability+Item-Reads; GET/POST/DELETE `/game-systems/{id}/shares` mit E-Mail/Username-Anzeige; Selbst-Share und PUBLIC-Downgrade abgelehnt (`GAME_SYSTEM_SHARE_SELF`/`GAME_SYSTEM_PUBLIC_NO_SHARE_NEEDED` 422); UI-Dialog; IT für Query)
- **Aufwand:** 1 Tag
- **Beschreibung:** Migration V103 `game_system_shares(system_id,user_id,role)`; `GameSystemService.canRead/findVisibleForUser` um Shares erweitern; Endpunkte `POST/DELETE /game-systems/{id}/shares` (Owner/Admin, Ziel-User per E-Mail/Username auflösen) + `GET .../shares`; Fehlercodes `GAME_SYSTEM_SHARE_EXISTS/NOT_FOUND`; UI: „Teilen"-Dialog im GameSystemPage (Owner), Badge `INVITE_ONLY`; Wizard/Kampagnen-Auswahl zeigt gesharte Systeme.
- **Akzeptanz:** Geshartes PRIVATE-System für Ziel lesbar/nutzbar, andere nicht; Owner kann entziehen; Tests (Service + Migration)
- **Abhängigkeit:** T33-02-Muster (Read-Policy) · **Qualitäts-Check:** TDD

### T33-06: Bot-Runtime pro Kampagne (T04-Rest)
- **Status:** ✅ (Interner `GET /api/v1/bot/worlds` (Rolle BOT/ADMIN): Welten+Kampagnen+`botMode`; Poller nutzt ihn mit `/worlds`-Fallback, überspringt `off`-Kampagnen, sendet `campaign_id` aus den Events mit; Tests: BotContextService + respx/pytest (ai-bot 50))
- **Aufwand:** 1 Tag
- **Beschreibung:** Interner Endpoint `GET /api/v1/bot/worlds` (nur Rolle BOT, Service-Token): Welten + Kampagnen + `bot.mode` + NPCs (statt `listOwned`); `ai-bot`-Poller: pro Kampagne iterieren, `off` überspringen, `campaignId` beim Intent mitsenden; respx-Tests + Compose-Doku.
- **Akzeptanz:** Bot verarbeitet nur erlaubte Welten/Kampagnen, `off` pausiert; pytest grün; Intents tragen `campaignId`
- **Abhängigkeit:** — · **Qualitäts-Check:** TDD (Java + pytest)

### T33-07: DM-Queue — Bulk + Filter
- **Status:** ✅ (`GET /npc-intents?worldId&status&type`; `POST /npc-intents/bulk {ids,action,reason}` mit Teil-Fehler-Report und DM-Gate; UI: Typ-Filter + Mehrfachauswahl + Bulk-Buttons; Tests)
- **Aufwand:** 0,5 Tage
- **Beschreibung:** `POST /npc-intents/bulk {ids[],action:"approve|reject",reason?}` (DM-gated, transaktional, Teil-Fehler als Ergebnisliste); `GET /npc-intents?worldId&status&type` mit Filtern; UI: Mehrfachauswahl + „Alle freigeben/ablehnen", Filter-Dropdown.
- **Akzeptanz:** Bulk über 3 Intents in einem Call; nur DM; Tests (Service) + UI-Test
- **Abhängigkeit:** T06-Gate (erledigt) · **Qualitäts-Check:** TDD

### T33-08: DM-Queue — WS-Liveupdate + E2E
- **Status:** ✅ (`useWorldSocket` meldet `NPC_INTENT_*` per CustomEvent → Panel refetcht sofort (Polling-Fallback bleibt); `dm-queue.spec.ts`: Panel erscheint <3 s, Bulk-Approve leert Queue)
- **Aufwand:** 0,5 Tage
- **Beschreibung:** `useWorldSocket`: `NPC_INTENT_PROPOSED/APPROVED/REJECTED` → `dmQueueStore`/Event-Refetch statt 5s-Polling (Polling als Fallback behalten); Playwright: Bot-Intent per API (BOT-frei: DM? Intent per API mit DM-Token) seeden → Panel erscheint live → Approve → verschwindet.
- **Akzeptanz:** Panel aktualisiert ohne Reload <1s; E2E grün
- **Abhängigkeit:** T33-07 · **Qualitäts-Check:** E2E

### T33-09: P14-Lücke — Adventure Inject-Choice + E2E
- **Status:** ✅ (Inject-Choice-UI im LiveAdventurePanel (Quell-/Ziel-Node + Label); E2E `adventure-inject.spec.ts`; Fund-Fix: `forceNode`/`injectChoice` publizierten die Adventure-ID als worldId → FK-Crash 500, jetzt Welt-ID + Test)
- **Aufwand:** 1 Tag
- **Beschreibung:** `inject-choice`-UI im `LiveAdventurePanel` (Choice-Text + Ziel-Node, nutzt vorhandenen Backend-Endpoint), Override-Skillcheck-Endpoint dokumentieren/verdrahten; Playwright: Adventure-Editor öffnen, Node anlegen/verbinden, Play-Page durchspielen (inkl. Override), 1 Spec.
- **Akzeptanz:** E2E-Flow grün; API.md ergänzt
- **Abhängigkeit:** — · **Qualitäts-Check:** E2E

### T33-10: P22 — Social-Mechanics-Konzept
- **Status:** ✅ (`ADR-013`: soziale Proben = Skill-Probe + Beziehungs-Modifikator, Furcht/Moral als Conditions, CHANGE_RELATION bleibt DM/Bot; Folge-Tasks P34 skizziert)
- **Aufwand:** 0,5 Tage
- **Beschreibung:** `docs/ADR/013-social-mechanics.md`: Beziehungsachse (RelationshipService existiert) → soziale Proben (Intimidate/Taunt als generische `social_actions` im rulesJson?), Furcht/Moral als Conditions-Reuse, Abgrenzung Content vs. Engine. Nur Konzept + TASKS-Ausblick, keine Implementierung.
- **Akzeptanz:** ADR akzeptiert; Folge-Tasks skizziert
- **Abhängigkeit:** — · **Qualitäts-Check:** Review

### T33-11: Abschluss — Gesamt-Audit mit allen Suiten
- **Status:** ✅ (Final-Audit NO-GO-Befunde gefixt (inkl. Re-Audit N1–N3): **advance**-Guard, Social-Writes via `requireWriteAccess`, Entity-Events mit Welt-Access (BOT/ADMIN-Ausnahme), Adventure-Statuscodes 404/422; **F1 CRITICAL** Write-Guards fuer Entity/Region/Location/Quest auf PUBLIC-Welten + Regression-IT `PublicWorldWriteAccessIT`; **F2 HIGH** Adventure-Node-Zugehoerigkeit in addChoice/forceNode/injectChoice; **F3** Pessimistic-Lock fuer Intent-Status; **P1–P3** Adventure-Reads/Events/Social-Endpunkte mit Access-Check; **P4** Quest-Typ-Validierung (400); Map-Read fuer PUBLIC; Doku/API/ERROR-CODES synchron. Offene Alt-Tickets s. P34-Sektion)
- **Aufwand:** 0,5 Tage
- **Beschreibung:** Read-only Audit über Phase-33-Diff; alle Suiten (Backend/Frontend/E2E/ai-bot) + Build; Findings fixen; TESTING/TASKS/API/ERROR-CODES final angleichen.
- **Akzeptanz:** Audit ohne offene HIGH/MEDIUM; alles gepusht
- **Qualitäts-Check:** Audit

> **Audit-Rhythmus:** Nach T33-01…T33-03 (Block A) → Audit; nach T33-04/T33-05 → Audit; nach T33-06…T33-08 (Block C) → Audit; T33-09/T33-10 optional; T33-11 Gesamt-Audit.


---

## Phase 34: Security-Tickets aus dem Phase-33-Audit (nicht blockierend, aber zeitnah)

> Aus dem Final-Audit: keine Regressionen, aber HIGH-Risiko in bestehender Fläche. Alle drei Tickets unabhängig — beliebige Reihenfolge. Abschluss-Gate: ein Mini-Audit nach allen drei + Suiten grün (Backend/Frontend/E2E/ai-bot).

### P34-T01: Adventure-Discovery `by-location`/`by-giver` absichern
- **Evidenz:** `AdventureController.java:45-55` ohne `@AuthenticationPrincipal`; `AdventureService.java:59-65` ohne userId/`requireRead` (`listByWorld` hat `requireRead`, P1-Audit). `Adventure` trägt `worldId` → kein neues Repository nötig.
- **Fix (TDD):** `listByLocation(locationId, userId)`, `listByGiver(giverEntityId, userId)`; pro Adventure `worldAccess.requireRead(worldId, userId)`, bei `WorldAccessException` Eintrag überspringen. Unbekannte ID und fehlender Zugriff → beide leere Liste (kein Existenz-Orakel). Controller reicht `user.getId()` durch.
- **Tests:** `AdventureServiceTest` — Fremder → leer; Member → Treffer; Adventures aus zwei Welten (eine fremd) → nur eigene; unbekannte ID → leer. Live-Probe devbe (fremde Location → `[]`).
- **Doku:** Verhalten in `API.md` vermerken (kein neuer Error-Code).
- **Status:** ✅ (TDD: Fremder → leer, Member → Treffer, gemischte Welten gefiltert, unbekannt → leer; Live-Probe `[]` + 403 ohne Token)

### P34-T02: E2E-Hygiene (Orphans + Cleanup)
- **Evidenz:** 8/9 Specs haben `afterAll` (Entities/Campaigns/Systeme), nur `wizard-save-gate.spec.ts` keins; keine Spec löscht Welten direkt — `CampaignService.delete:130-142` deaktiviert die Fork-Welt mit, Template bleibt. Übrig bleiben: inaktive Systeme/Items, NPC-Intents ohne Kampagne, Test-Artefakte des E2E-Users. `TESTING.md:703` dokumentiert die Altlasten bereits.
- **T34-02a:** `wizard-save-gate.spec.ts` `afterAll`-Cleanup nachrüsten (Campaign/System, Muster `system-pin.spec.ts:23-25`).
- **T34-02b:** `scripts/e2e-cleanup.sh` — mit E2E-User-Token nur eigene Artefakte aufräumen (inaktive Systeme, verwaiste Welten/Intents auflisten + löschen/deaktivieren); `TESTING.md`-Anleitung ergänzen (vor DB-Reset/Release laufen lassen).
- **Akzeptanz:** Voller E2E-Lauf hinterlässt keine *aktiven* Artefakte des E2E-Users (Nachweis via `GET /worlds`, `/game-systems`); Skript einmal trocken gegen Dev-DB laufen lassen.
- **Status:** ✅ (T34-02a entfällt — Save-Gate erzeugt keine Artefakte; T34-02b `scripts/e2e-cleanup.sh` + TESTING-Doku, Dry-Run verifiziert)

### P34-T03: Bulk-Tx-Garantie präzisieren
- **Evidenz:** `NpcIntentService.bulk:131-153` eine `@Transactional`; `approve`/`reject` per Selbstaufruf (Proxy umgangen); `IntentExecutor.execute` (DB-Side-Effects + Events) läuft innerhalb der Tx — ein DB-Fehler rollt alles zurück, inkl. bereits als erfolgreich gemeldeter Einträge.
- **Design-Entscheid (fix):** Bulk-Methode nicht-transaktional; pro Eintrag eigene Tx via `TransactionTemplate` (REQUIRES_NEW). `approve`-/`reject`-/Executor-Semantik unverändert (Executor pro Eintrag in dessen Tx — wie heute beim Einzel-Approve). Folge: Teilerfolg möglich, logische wie DB-Fehler betreffen nur den Eintrag; Client wertet `BulkResult[]` aus.
- **T34-03a:** Umbau + Kommentar aktualisieren. Tests: Teilerfolg (Eintrag 1 approved, Eintrag 2 `INTENT_NOT_PENDING` → Eintrag 1 bleibt approved); ungültige Action → pro-Eintrag-Fehler ohne Seiteneffekt. DM-Queue-E2E bleibt grün.
- **Doku:** `API.md` Bulk-Semantik (Teilerfolg möglich).
- **Status:** 📋
