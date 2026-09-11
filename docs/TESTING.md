# Testing-Strategie

> Wie LWE auf allen Ebenen getestet wird. Verbindlich für alle Phasen.

---

## 1. Test-Pyramide

```
            ┌───────────┐
            │    E2E     │   ← Playwright (Browser)
            │   (~5%)    │
            ├───────────┤
            │ Integration│   ← Spring Boot @SpringBootTest, Testcontainers
            │   (~25%)   │
            ├───────────┤
            │    Unit    │   ← JUnit 5 / pytest / vitest
            │   (~70%)   │
            └───────────┘
```

Anteile sind Richtwerte, keine harte Vorgabe. Wichtig: **jede Ebene hat ihren Zweck** — Unit-Tests ohne Dependencies für schnelles Feedback, Integration-Tests für Zusammenspiel realer Komponenten, E2E-Tests für kritische User-Flows.

---

## 2. Backend (Java / Spring Boot)

### 2.1 Unit-Tests
- **Framework:** JUnit 5, Mockito, AssertJ
- **Bereich:** Services, Rule-Engine, Expression-Parser, Validatoren, Utility-Klassen
- **Keine** Spring-Context, keine Datenbank — gemockte Dependencies
- **Naming:** `ClassNameTest` mit `@TestMethodName`-Konvention: `shouldReturnXWhenY`
- **Zeit:** < 10 s gesamte Suite

### 2.2 Integration-Tests
- **Framework:** `@SpringBootTest`, Testcontainers (PostgreSQL)
- **Bereich:** Repository-Methoden, End-to-End durch Controller → Service → Repository → DB
- **Datenbank:** Echte PostgreSQL-Instanz via Testcontainers (nicht H2 — JSONB-Verhalten soll realistisch sein)
- **Migrationen:** Flyway läuft automatisch vor Tests
- **Naming:** `*IT.java` (Suffix für Maven Failsafe-Plugin)
- **Zeit:** < 60 s gesamte Suite

### 2.3 Slicing-Tests
- `@WebMvcTest` für Controller-Layer (Service gemockt)
- `@DataJpaTest` für Repository-Layer (Testcontainers)
- Schneller als volle Integrationstests, isoliert Schicht

### 2.4 WebSocket-Tests
- `WebSocketStompClient` in Integrationstests
- STOMP-Connect mit JWT, Subscription und Receive prüfen

### 2.5 i18n-Tests
- Parametrisierter Test: gleicher Endpunkt mit `Accept-Language: de` bzw. `en`
- Assert: Fehlermeldung korrekt übersetzt
- Test für "missing translation key" (Fallback-Locale)

---

## 3. Frontend (React / TypeScript)

### 3.1 Unit-Tests
- **Framework:** Vitest + React Testing Library
- **Bereich:** Komponenten, Hooks, Utility-Funktionen
- **Mocking:** MSW (Mock Service Worker) für HTTP-Interceptor, keine echten Backend-Calls
- **Naming:** `*.test.tsx` / `*.test.ts` direkt neben Source-Datei
- **Zeit:** < 15 s gesamte Suite

### 3.2 Komponenten-Tests
- Rendering mit varying props, Store-State
- Benutzerinteraktion via `@testing-library/user-event`
- A11y-Prüfung: `jest-axe` für WCAG-Verletzungen pro Komponente

### 3.3 i18n-Tests
- Test, dass alle Keys in `de` und `en` vorhanden sind
- Vergleich der Key-Sets der Sprachen
- Render einer Beispiel-Komponente mit beiden Sprachen (snapshot optional)

### 3.4 E2E-Tests (Playwright)
- **Bereich:** Kritische User-Flows (Login, Welt erstellen, Würfel werfen, Spielbetreten)
- **Gegen:** Echtes Backend via `podman compose` (Test-Setup startet kompletten Stack)
- **Parallelität:** Worker-basiert, Tests isoliert per Test-User
- **Zeit:** < 5 min gesamte Suite
- **Ausführung:** In CI bei jedem PR (Phase 5+)

---

## 4. AI-Bot (Python)

### 4.1 Unit-Tests
- **Framework:** pytest, pytest-asyncio, httpx-Mock
- **Bereich:** Kontext-Loader, Prompt-Renderer, Parser für LLM-Output, Validierung
- **LLM:** Gemockt (`MockLLMClient`) — keine echten API-Calls in CI
- **Fixtures:** `tests/fixtures/npc_contexts/*.json`

### 4.2 Prompt-Tests
- Golden-File-Tests: gerenderter Prompt verglichen mit `tests/prompts/expected/*.txt`
- Schützt vor unbeabsichtigter Prompt-Drift durch Refactoring

### 4.3 Integration-Tests
- Starre den Bot mit gemocktem Backend auf
- Polling-Loop simuliern, Event aus_fixture_einspeisen, Intent-Output prüfen

---

## 5. Property-Based Tests (optional)

Für ressourcenkritische Logik wie Dice-Expression-Parser und Rule-Engine:
- **jqwik** (Java) oder **hypothesis** (Python)
- Generiert tausende zufällige Eingaben
- Testet Invarianten (z. B.: „Probeergebnis ist immer im Wertebereich [Roll_min, Roll_max] + mod")

---

## 6. Last- und Performance-Tests

- **Framework:** k6 (skriptbar in JS)
- **Scope in Phase 5:** "100 gleichzeitige Spieler in einer Welt würfeln"
- **Metriken:** Latenz p95 < 200 ms, Fehlerquote < 0,1 %
- **Häufigkeit:** Manuell vor Release, später optional in CI-Nightly

---

## 7. Test-Abdeckung

- **Gemessen via:** JaCoCo (Backend), Vitest-Coverage (Frontend), pytest-cov (Bot)
- **Ziel (Richtwert, nicht hart):**
  - Backend: ≥ 80 % Line Coverage, ≥ 70 % Branch Coverage
  - Frontend: ≥ 75 % Line Coverage
  - Bot: ≥ 85 % Line Coverage (kritische Validierungslogik)
- **Ausnahmen:** DTO-Entities, reine Konfiguration, `main`-Methoden — nicht mitgezählt

---

## 8. CI-Anbindung (Phase 5)

```yaml
# .github/workflows/ci.yml (vereinfacht)
jobs:
  backend:
    runs-on: ubuntu-latest
    services:
      postgres: ... # oder testcontainers
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
      - run: ./mvnw verify   #_unit + integration via failsafe
  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: pnpm/action-setup
      - run: pnpm install
      - run: pnpm check       # lint + typecheck
      - run: pnpm test        # vitest
  bot:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/setup-python@v5
      - run: pip install -e ".[test]"
      - run: pytest
```

**Gates (PR-merge-blocking):**
- Alle Test-Suiten grün
- Coverage hinter Minimum
- i18n-Keys konsistent zwischen `de` und `en`

---

## 9. Samen-Daten (Seed)

- `db/migration/afterMigrate__seed.sql` (Flyway Callback) lädt Beispieldaten in Dev-Profil:
  - 1 Admin-User, 2 reguläre User
  - Beide Beispiel-Regelwerke (D20Lite, TwoDicePool)
  - 1 Demo-Welt mit NPCs und Adventure-Start
- In Produktion inaktiv (`profile != dev`)
- Erlaubt schnelles Ausprobieren ohne händisches Anlegen

---

## 10. Manuelle Tests

Für jeden Meilenstein wird ein **Smoke-Test-Skript** gepflegt:
- `docs/SMOKE-TEST.md` (Phase 1)
- `docs/DEMO.md` (Phase 3)
- `docs/LIVING-WORLD-DEMO.md` (Phase 4)
- `docs/USER-GUIDE.md` (Phase 5)

Diese werden vor jedem Meilenstein-Release per Hand durchgespielt.

---

## 11. Testlisten pro Phase

| Phase | Neue Tests |
|---|---|
| 1 | Unit für `JwtService`, `AuthService`, `RuleSchemaValidator`; Integration für `/api/auth`, `/api/worlds`, WebSocket |
| 2 | Unit für Rule-Engine, Dice-Parser; Integration für `/api/rolls`, Combat-Flow, Adventure |
| 3 | Vitest für alle Komponenten; `jest-axe` für A11y; E2E für Login-Flow |
| 4 | pytest für ContextLoader, LLM-Client (gemockt), Validator; Golden-File-Prompt-Tests |
| 5 | Last-Tests mit k6; E2E Suite mit echtem Container-Stack |

---

## 12. Verweise

- Integration in TASKS.md: jede("{ acceptance criteria")-Liste enthält Testanforderungen
- [`ADR/007`](ADR/007-internationalization-strategy.md) — i18n-Test-Prüfung
- [`ADR/005`](ADR/005-ki-validation-layer.md) — Validator-Schicht hat eigenenTestsatz

---

## 13. Manueller Testplan (Phase 17–21)

Gemeinsam durchzugehen mit DM + Spieler-Perspektive.

### 13.1 SystemWizard — Game System erstellen

**Vorbereitung:** Dashboard → Game Systems → "New System"

#### 13.1.1 Template laden
- [ ] D20Lite-Template laden (Wizard: „Load Template", JSON-Tab: „Load Template") → Wizard zeigt 6 Attribute (staerke, geschick, etc.)
- [ ] Alle 11 Wizard-Schritte sind erreichbar (Vor/Zurück-Navigation)
- [ ] Schritt 11 (Übersicht) zeigt alle konfigurierten Daten

#### 13.1.2 Probe-Typ konfigurieren (Schritt 11)
- [ ] `d20_target` auswählen → Probe-Expression = `1d20+mod`
- [ ] `d100_threshold` auswählen → Probe-Expression = `1d100`
- [ ] `d20_3attr` auswählen → Probe-Expression = `3d20`
- [ ] Test-Würfel-Button funktioniert (zeigt Ergebnis an)

#### 13.1.3 Combat konfigurieren (Schritt 11)
- [ ] "Kampfregeln aktivieren" checkbox → Combat-Felder erscheinen
- [ ] Initiative-Expression setzbar (z.B. `1d20+geschick`)
- [ ] Damage-Expression setzbar (z.B. `1d8+staerke`)
- [ ] AP pro Runde + AP Max setzbar

#### 13.1.4 Combat: Critical Hits
- [ ] Critical Hits Sektion aufklappbar
- [ ] Threshold (1-20) setzbar → default 20
- [ ] Multiplier (1-10) setzbar → default 2

#### 13.1.5 Combat: Saving Throws
- [ ] Saving Throws Sektion aufklappbar
- [ ] Base DC setzbar → default 8
- [ ] Proficiency Bonus Formel setzbar (z.B. `floor((attr-10)/2)`)

#### 13.1.6 Combat: Resting
- [ ] Resting Sektion aufklappbar
- [ ] Short Rest: Heal % setzbar (0.0 - 1.0) → default 0.5
- [ ] Short Rest: "Ressourcen wiederherstellen" checkbox
- [ ] Long Rest: "Vollständige Heilung" checkbox
- [ ] Long Rest: "Alle Ressourcen" checkbox

#### 13.1.7 Speichern & Validieren
- [ ] System speichern → Erfolgsmeldung
- [ ] System erscheint in der Liste
- [ ] Gespeichertes System editieren → Wizard zeigt alle Werte korrekt an
- [ ] JSON-Export des Systems → JSON enthält alle gesetzten Felder
- [ ] JSON-Import des exportierten Systems → Wizard zeigt gleiche Werte

#### 13.1.8 Beispiel-JSONs importieren
- [ ] `dnd5e.json` importieren → Wizard zeigt D&D-Konfiguration
- [ ] `coc7e.json` importieren → probeType = d100_threshold
- [ ] `dsa5.json` importieren → probeType = d20_3attr, 3er-Attribute

---

### 13.2 Character Edit & Sheet

**Vorbereitung:** Welt mit einem Game System erstellen → Character anlegen → Character-Sheet öffnen

#### 13.2.1 Sheet-Grundfunktionen
- [ ] Character-Sheet öffnet sich ohne Fehler
- [ ] Character-Name + Typ werden angezeigt
- [ ] Attribute werden aus rulesJson geladen (min/max korrekt)
- [ ] Derived Values (HP, AC) werden berechnet und angezeigt
- [ ] Skills werden aus rulesJson geladen

#### 13.2.2 Attribut inline editieren
- [ ] Klick auf Attribut-Wert (z.B. "15") → Eingabefeld erscheint
- [ ] Neuen Wert eingeben → Enter → Wert gespeichert
- [ ] Sheet aktualisiert sich (refetch) → neuer Wert sichtbar
- [ ] Escape bricht Editier-Modus ab → alter Wert bleibt
- [ ] Validierung: Wert < min → wird zurückgesetzt
- [ ] Validierung: Wert > max → wird zurückgesetzt
- [ ] Fehlgeschlagener Speicherversuch → Toast "Failed to save attribute"

#### 13.2.3 XP editieren
- [ ] XP-Wert klickbar → Eingabefeld erscheint
- [ ] Neuen Wert eingeben → Enter → gespeichert
- [ ] Level-Anzeige aktualisiert sich
- [ ] XP-Balken zeigt Fortschritt an

#### 13.2.4 Skill editieren (per-character override)
- [ ] Skill-Wert klicken (z.B. "+4") → Eingabefeld erscheint
- [ ] Neuen Wert eingeben (z.B. "5") → Enter → gespeichert
- [ ] "override"-Label erscheint neben dem Skill
- [ ] Skill ohne per-character Value zeigt normalen Wert ohne Label
- [ ] Fehlgeschlagener Speicherversuch → Toast "Failed to save skill"

#### 13.2.5 Skill-gesteuerte Probe
- [ ] ProbeRoller-Button neben Skill klicken
- [ ] Probe-Ergebnis erscheint (total, Erfolg/Fehlschlag)
- [ ] Probe nutzt per-character Skill-Wert (wenn gesetzt)
- [ ] Probe nutzt globalen Skill-Wert (wenn kein per-character)

#### 13.2.6 Abilities anzeigen
- [ ] Active-Abilities mit farbigem Hintergrund (`bg-accent/10`)
- [ ] Passive-Abilities mit grauem Hintergrund (`bg-bg-primary/30`)
- [ ] Active-Abilities zeigen AP-Kosten an
- [ ] Active-Abilities zeigen Effekt-Beschreibung
- [ ] Passive-Abilities zeigen keine AP-Kosten

#### 13.2.7 Ability Use (außerhalb Combat)
- [ ] "Use"-Button neben jeder Active-Ability
- [ ] Klick → Würfelergebnis (1-20) erscheint rechts neben dem Button
- [ ] Ergebnis verschwindet nach 3 Sekunden
- [ ] Passive-Abilities haben keinen "Use"-Button

#### 13.2.8 Formula Overrides
- [ ] "Formula Overrides" Sektion sichtbar
- [ ] Plus-Button → Eingabefelder erscheinen (Name + Wert)
- [ ] Neues Override hinzufügen (z.B. hp +2) → gespeichert
- [ ] Override erscheint in der Liste
- [ ] Override löschen (X-Button) → entfernt
- [ ] Derived Values berücksichtigen Override (hp erhöht sich um +2)

---

### 13.3 Kampf

**Vorbereitung:** Welt mit D&D-System + 2+ Charaktere (PC + NPC)

#### 13.3.1 Kampf starten
- [ ] Kampf-Button in Welt-Ansicht → Teilnehmer auswählen
- [ ] Initiative wird gewürfelt → Reihenfolge korrekt (höchste zuerst)
- [ ] Kampf-Bildschirm wird geöffnet
- [ ] Erster Teilnehmer in der Initiative-Liste markiert

#### 13.3.2 Kampf-UI
- [ ] Karte wird angezeigt (MapCanvas)
- [ ] Chat-Panel + Roll-Log sichtbar
- [ ] Initiative-Liste zeigt alle Teilnehmer mit Werten
- [ ] AP-Balken zeigt aktuelle/maximale AP des aktiven Actors

#### 13.3.3 Action ausführen
- [ ] Target auswählbar (Klick auf Teilnehmer-Name)
- [ ] Action-Button (z.B. "ACTION") klickbar → Aktion wird ausgeführt
- [ ] Schaden wird angezeigt (Chat + Log)
- [ ] AP des Actors reduziert sich (aus POST-Response, nicht auf 0)

#### 13.3.4 Action-Typen
- [ ] Action-Typen aus `rulesJson.combat.action_types` geladen
- [ ] Bonus-Aktion (wenn konfiguriert) separat klickbar
- [ ] Reaktion (wenn konfiguriert) separat klickbar
- [ ] Buttons disabled wenn Aktion bereits verbraucht

#### 13.3.5 AP-Verbrauch & Aktionen pro Runde
- [ ] Action verbraucht 1 AP → AP-Balken aktualisiert
- [ ] Bei 0 AP: Action-Buttons disabled
- [ ] `actions_per_turn` wird korrekt angezeigt (z.B. 1/1 action)

#### 13.3.6 Fähigkeiten im Kampf
- [ ] Fähigkeiten (abilities) des Actors werden in der ActionBar geladen
- [ ] Ability-Button klickbar → `POST /combat/{id}/ability` wird aufgerufen
- [ ] Schaden/Effekt der Ability wird angewendet

#### 13.3.7 Next Turn
- [ ] "Nächster Zug"-Button → nächster Teilnehmer ist dran
- [ ] AP des neuen Actors wird aufgefrischt (AP-Max)
- [ ] Runden-Zähler erhöht sich bei letztem Teilnehmer

#### 13.3.8 Kampf beenden
- [ ] "Beenden"-Button → Kampf wird beendet
- [ ] Status = ENDED
- [ ] Keine Aktionen mehr möglich

---

### 13.4 Inventory

**Vorbereitung:** Character mit Items (Waffe, Rüstung, etc.)

#### 13.4.1 Inventory-Seite öffnen
- [ ] Inventory über Navigation erreichbar
- [ ] Equip-Slots werden angezeigt (weapon, armor, helmet, accessory)
- [ ] Leere Slots zeigen Slot-Namen italic
- [ ] Backpack zeigt Items als Liste

#### 13.4.2 Items anzeigen
- [ ] Item-Name + Typ + Anzahl werden angezeigt
- [ ] Equipped Items haben "unequip"-Button
- [ ] Backpack-Items haben "equip"-Button
- [ ] Hover → Tooltip mit Details (Name, Type, Weight)

#### 13.4.3 Drag & Drop Equip
- [ ] Item aus Backpack greifen (Drag)
- [ ] Auf korrekten Equip-Slot ziehen (Drop) → Equip erfolgreich
- [ ] Equipped-Item erscheint im Slot
- [ ] Item verschwindet aus Backpack
- [ ] Falscher Slot (z.B. Waffe auf armor) → Equip trotzdem möglich

#### 13.4.4 Drag & Drop Unequip
- [ ] Equipped-Item greifen → aus Slot ziehen
- [ ] Item landet wieder im Backpack
- [ ] Slot ist leer
- [ ] Alternativ: "unequip"-Button klicken

#### 13.4.5 Equip-Button (Backpack → Slot)
- [ ] "equip"-Button auf Item klicken
- [ ] Item-Typ bestimmt Slot: WEAPON→weapon, ARMOR→armor, HELMET→helmet, ACCESSORY→accessory
- [ ] Equip erfolgreich
- [ ] CONSUMABLE/MISC Items → Fallback auf weapon (verkraftbar)

#### 13.4.6 Bonuses & AC
- [ ] Equipped Item mit Bonuses → Bonuses werden angezeigt
- [ ] AC aktualisiert sich nach Equip (um armor_class-Bonus erhöht)
- [ ] AC aktualisiert sich nach Unequip (Bonuses entfernt, AC sinkt)
- [ ] Mehrere Items mit gleichem Bonus → Werte werden addiert

#### 13.4.7 Error-Feedback
- [ ] API-Fehler beim Equip → Toast "Equip failed"
- [ ] API-Fehler beim Unequip → Toast "Unequip failed"

---

### 13.5 Character Export/Import

**Vorbereitung:** Character mit Attributen, Skills und Items

#### 13.5.1 Export
- [ ] Export-Button im CharacterSheet-Header (Download-Icon)
- [ ] Klick → JSON-Datei wird heruntergeladen
- [ ] Datei enthält: entity_type, name, attributes_json, inventory_json, skills_json
  (Hinweis: Export nutzt camelCase — `worldId`, `attributesJson`, …; `skills_json` existiert nicht)

#### 13.5.2 Import
- [ ] Import-Button im CharacterSheet-Header (Upload-Icon)
- [ ] Exportierte JSON-Datei auswählen
- [ ] Neuer Character wird erstellt
- [ ] Browser navigiert zum neuen Character-Sheet
- [ ] Alle Werte (Attribute, Skills, Items) korrekt übernommen
- [ ] attributes_json nach Import nicht doppelt encodiert (kein "{\\\\\"...")

#### 13.5.3 Edge-Cases
- [ ] Import ohne world_id → Toast "No worldId in import file"
- [ ] Import ungültiger JSON → Toast "Import failed"
- [ ] Export + Import eines Characters mit per-character Skills → Skills erhalten

---

### 13.6 Resting

**Vorbereitung:** Character in Welt mit D&D-System (resting konfiguriert)

#### 13.6.1 Short Rest
- [ ] Character hat niedrige HP (z.B. 10/50)
- [ ] `POST /worlds/{worldId}/entities/{id}/rest/short` aufrufen
- [ ] HP um heal_percent (50%) erhöht → 10 + 25 = 35
- [ ] AP auf maximum恢复了

#### 13.6.2 Long Rest
- [ ] Character hat niedrige HP + AP
- [ ] `POST /worlds/{worldId}/entities/{id}/rest/long` aufrufen
- [ ] HP = max (50/50)
- [ ] AP = max

#### 13.6.3 Ohne Resting-Config
- [ ] Welt mit System ohne resting-Konfiguration
- [ ] `POST /worlds/{worldId}/entities/{id}/rest/short` → HP unverändert
- [ ] `POST /worlds/{worldId}/entities/{id}/rest/long` → HP unverändert

#### 13.6.4 Dice-basierte Heilung
- [ ] Config: `"hp": "1d6"` für short_rest
- [ ] Short Rest → HP zwischen 11 und 16 (10 + 1W6)

---

### 13.7 Sicherheit

#### 13.7.1 EntityAbilityController Auth
- [ ] User A erstellt Welt + Character + Ability
- [ ] User B (anderer Account) ruft `POST /entities/{charId}/abilities/{abilityId}` auf
- [ ] → 403 oder Fehler, keine Zuweisung
- [ ] User A kann Ability zuweisen (funktioniert)

#### 13.7.2 Skills schützen
- [ ] User B ruft `PATCH /entities/{charId}/skills` auf fremden Character auf
- [ ] → 403 Access Denied
- [ ] User A (Besitzer) kann Skills editieren

#### 13.7.3 Character Edit schützen
- [ ] `PATCH /entities/{charId}/attributes` auf fremden Character → 403
- [ ] `PATCH /entities/{charId}/progression` auf fremden Character → 403
---

## 14. Manueller Testplan 3-Ebenen-Modell (Phase 24–26)

**Ziel:** Verifikation des kompletten Kampagnen-Flows (ADR-010): Welt ∥ System → Kampagne. Alle Checkpoints gegen ein frisches Backend (Port 8080) + Frontend-Build (Port 3000).

**Vorbereitung:**
1. Backend starten (`cd backend && mvn spring-boot:run` bzw. jar aus `backend/target/`)
2. Frontend bauen + serven (`npx vite build` + `npx serve -s dist -l 3000`)
3. Zwei Test-Accounts anlegen: `dm@test.de` + `spieler@test.de` (Register → Email-Verify via Console-Token)
4. DSA-System anlegen (Game Systems → New System → Template)
5. Eine Welt „Testwelt" anlegen (Welt-Name reicht, KEIN System wählbar!)

---

### 14.1 Welt-Erstellung ohne System (P26-T02)

#### 14.1.1 Welt anlegen
- [ ] Dashboard → "Erstellen" → Name „Testwelt" → Welt erscheint in Liste
- [ ] Kein Game-System-Dropdown im Create-Dialog (nur Name)
- [ ] Welt-Editor (`/worlds/{id}/edit`) hat KEINEN Game-System-Dropdown mehr
- [ ] Welt speichern → Toast „Gespeichert", keine Fehler

#### 14.1.2 Welt-Clone
- [ ] Welt-Editor → „Clone" → Kopie „Testwelt (Copy)" erscheint
- [ ] Kopie enthält Regionen/Locations/NPCs (falls angelegt)

---

### 14.2 Kampagne erstellen (P24-T05, P26-T01)

#### 14.2.1 Kampagnen-Sektion im Dashboard
- [ ] Dashboard zeigt Abschnitt „Kampagnen" unterhalb der Welten
- [ ] Ohne Kampagnen: Hinweis „Noch keine Kampagnen"
- [ ] Button „Kampagne erstellen" öffnet Modal

#### 14.2.2 Kampagne anlegen (DM = Ersteller)
- [ ] Modal: Name „Runde 1" + Welt „Testwelt" + System „DSA" → Erstellen
- [ ] Kampagne erscheint in der Kampagnen-Liste
- [ ] Klick auf Kampagne → Detailseite `/campaigns/{id}`
- [ ] Detailseite zeigt: Welt-Name, System-Name v{Version}, Mitglieder-Anzahl = 1
- [ ] Mitgliederliste enthält `dm@test.de` mit Rolle **DM** (Kronen-Icon)
- [ ] DM-Eintrag hat KEINEN Löschen-Button

#### 14.2.3 Kampagne ohne System ablehnen
- [ ] Modal: System leer → Erstellen-Button deaktiviert
- [ ] Welt leer → Erstellen-Button deaktiviert

#### 14.2.4 Kampagne mit inaktiver Welt-Zuordnung
- [ ] (Optional, nur via API) `POST /campaigns` mit fremder worldId → 404/403

---

### 14.3 Kampagnen-Mitglieder (P25-T04)

#### 14.3.1 Spieler hinzufügen (als DM)
- [ ] Detailseite → „Spieler hinzufügen" → „spiel" tippen → Suchvorschlag `spieler@test.de`
- [ ] Klick auf Vorschlag → Toast „Spieler hinzugefügt"
- [ ] Mitgliederliste zeigt jetzt `spieler@test.de` mit Rolle **Spieler** (User-Icon)
- [ ] Spieler-Eintrag hat Löschen-Button (Trash-Icon)

#### 14.3.2 Spieler entfernen (als DM)
- [ ] Trash-Icon am Spieler klicken → Toast „Spieler entfernt"
- [ ] Mitgliederliste: Spieler verschwunden
- [ ] Erneut hinzufügen für weitere Tests

#### 14.3.3 Nicht-DM darf keine Mitglieder verwalten
- [ ] Mit `spieler@test.de` einloggen → Kampagnen-Detail öffnen
- [ ] Such-Input vorhanden, aber add → Fehler-Toast „Spieler konnte nicht hinzugefügt werden" (oder 403)
- [ ] (Backend) Löschen eines Mitglieds → 403/`DM_REQUIRED`

#### 14.3.4 DM kann nicht entfernt werden
- [ ] (Backend) `DELETE /campaigns/{id}/members/{dmUserId}` → Fehler `DM_REMOVAL_DENIED`
- [ ] UI zeigt für DM keinen Löschen-Button

---

### 14.4 Kampagnen-Kontext & Welt-Einstieg (P26-T01)

#### 14.4.1 „In Welt starten"
- [ ] Detailseite → Button „In Welt starten" → Navigiert zu `/worlds/{worldId}`
- [ ] Header zeigt Kampagnen-Badge mit „Runde 1"
- [ ] Welt lädt normal (Karte, Regionen)

#### 14.4.2 Kampagnen-Kontext bei Session-Start
- [ ] In GameView: „Session starten" (SessionManager)
- [ ] (Backend) Session hat `campaignId` gesetzt
- [ ] `GET /api/v1/worlds/{worldId}/sessions` → Response enthält `campaignId`

#### 14.4.3 Kampagnen-Kontext bei Combat-Start
- [ ] Combat starten (Mind. 2 Entities) → Response enthält `campaignId`
- [ ] Kampf läuft mit System-Regeln der Kampagne (Action-Types aus DSA-rulesJson)
- [ ] ActionBar zeigt Aktionen aus Kampagnen-System (nicht Welt-Fallback)

---

### 14.5 Character Sheet & Proben im Kampagnen-Kontext (P25-T03)

#### 14.5.1 Sheet mit Kampagnen-System
- [ ] Character öffnen (`/characters/{id}`) — Sheet nutzt Regeln der Kampagne
- [ ] Attribute = DSA-Attribute (falls DSA-System, sonst Template-Attribute)
- [ ] Level-Berechnung aus XP der Kampagne korrekt

#### 14.5.2 Probe mit Kampagnen-System
- [ ] Skill-Probe würfeln → Ergebnis nutzt DSA-Probe-Typ (z. B. `d20_3attr` oder `d100_threshold`)
- [ ] (Backend) `POST /rolls/probe` mit `campaignId` → korrekte Expression

#### 14.5.3 Roll via Kampagnen-Kontext
- [ ] (Backend) `POST /rolls` mit `campaignId` → Engine aus Kampagnen-System
- [ ] Ohne `campaignId` (Welt ohne System) → D20-Fallback, kein Crash

---

### 14.6 Items & Abilities am System (P24-T01/T02, P25-T08)

#### 14.6.1 Item-CRUD
- [ ] (Backend) `GET /api/v1/game-systems/{id}/items` → leere Liste (200)
- [ ] (Backend) `POST /api/v1/game-systems/{id}/items` mit `{name:"Kurzschwert", type:"WEAPON", weight:1.5, value:10, bonusesJson:"{\"damage\":\"1d6\"}"}` → 201
- [ ] (Backend) `GET /api/v1/items/{id}` → Item mit `gameSystemId`
- [ ] (Backend) `PUT /api/v1/items/{id}` Name ändern → 200
- [ ] (Backend) `DELETE /api/v1/items/{id}` → 204
- [ ] (Backend) Invalid type (`"WAND"`) → 400 `INVALID_ITEM_TYPE`
- [ ] (Backend) Unbekanntes System → 404 `GAME_SYSTEM_NOT_FOUND`

#### 14.6.2 Ability-CRUD
- [ ] (Backend) `GET /api/v1/game-systems/{id}/abilities` → leere Liste
- [ ] (Backend) `POST /api/v1/game-systems/{id}/abilities` mit `{name:"Feuerball", type:"ACTIVE", apCost:2}` → 201
- [ ] (Backend) `GET /api/v1/abilities/{id}` → `gameSystemId` gesetzt
- [ ] (Backend) `DELETE /api/v1/abilities/{id}` → 204
- [ ] (Frontend, optional) Ability am Character zuweisen → `POST /entities/{charId}/abilities/{abilityId}` funktioniert unabhängig vom Welt-Match

#### 14.6.3 Alte Endpoints weg
- [ ] (Backend) `POST /api/v1/worlds/{worldId}/abilities` → 404 (Endpoints existieren nicht mehr)
- [ ] (Backend) `GET /api/v1/worlds/{worldId}/abilities` → 404

---

### 14.7 Security (Audit B1–B2)

#### 14.7.1 Kampagnen-Liste nur für Berechtigte (B1)
- [ ] User A (dm@test.de) sieht seine Kampagne
- [ ] User B (spieler@test.de, ohne Welt-Zugriff) loggt ein → Dashboard zeigt KEINE Kampagne von User A
- [ ] (Backend) `GET /api/v1/campaigns` als User B → leere Liste

#### 14.7.2 Kampagnen-Detail geschützt
- [ ] User B ohne Welt-Zugriff: `GET /api/v1/campaigns/{id}` → 403 `WORLD_ACCESS_DENIED`

#### 14.7.3 Combat campaignId (B2)
- [ ] (Backend) `POST /api/v1/combat/start` mit `campaignId` als Nicht-DM → 403 `WORLD_ACCESS_DENIED`
- [ ] (Backend) Als DM mit `campaignId` → 201, Response mit `campaignId`

#### 14.7.4 Session campaignId
- [ ] (Backend) `POST /api/v1/sessions/start` mit `campaignId` als Nicht-DM → 403
- [ ] (Backend) Als DM → 201, Response mit `campaignId`

#### 14.7.5 Roll mit fremdem campaignId
- [ ] (Backend) `POST /rolls` mit fremder campaignId → nicht-crash, Fallback oder 403 (kein 500)

---

### 14.8 Events & WebSocket (P25-T07)

#### 14.8.1 Events tragen campaignId
- [ ] Session starten (mit Kampagne) → `world_events`-Eintrag hat `campaignId`
- [ ] Combat starten (mit Kampagne) → `COMBAT_STARTED`-Event hat `campaignId`
- [ ] (Backend) `GET /api/v1/worlds/{id}/events?since=0` → Response-Feld `campaignId` vorhanden
  (Hinweis: API nutzt camelCase — kein `campaign_id`)

#### 14.8.2 WebSocket
- [ ] Zwei Browser: Welt A + Welt B geöffnet
- [ ] Combat in Welt A → nur Welt A empfängt `COMBAT_STARTED`
- [ ] Events ohne campaignId (z. B. Welt-Zeit) → `campaignId` leer/null, kein Bruch

---

### 14.9 Regression

#### 14.9.1 Bestehende Welten ohne Kampagne
- [ ] Alte Welt ohne Kampagne öffnen → GameView funktioniert (Welt-Fallback)
- [ ] Sheet/Combat ohne campaignId → D20-Fallback, keine Fehler
- [ ] Welt-Zeit (Time Advance/Pause) funktioniert weiter

#### 14.9.2 Resting
- [ ] Kurz-Rast (short) → AP-Regenerierung lt. System (Fallback)
- [ ] Lange Rast (long) → HP voll (Fallback oder Kampagnen-Regeln)

#### 14.9.3 Export/Import
- [ ] Character exportieren → JSON enthält camelCase-Felder (kein `world_id`-Mismatch)
- [ ] Character importieren → Roundtrip korrekt

#### 14.9.4 i18n
- [ ] Sprache auf DE/EN wechseln → Kampagnen-Texte (Sektion, Modal, Detail, Toasts) übersetzt
- [ ] FR/ES/IT/TR → Fallback auf EN, kein leeres Label

---

### 14.10 Testdaten-Checkliste (fürs Wochenende)

- [ ] 2 Accounts (`dm@test.de`, `spieler@test.de`)
- [ ] 1 Game-System (DSA oder D20Lite)
- [ ] 1 Welt „Testwelt" (mit 2+ Entities: 1 PC + 1 NPC)
- [ ] 1 Kampagne „Runde 1" (Welt + System, DM = dm@test.de)
- [ ] 1 Spieler-Mitglied (spieler@test.de)
- [ ] 1 Item (Kurzschwert) am System
- [ ] 1 Ability (Feuerball) am System

**Ergebnis-Erfassung:** Abgehakte Checkpoints + gefundene Fehler mit Reproduktionsschritten hier unten notieren (Datum, Ticket-Nummer).

---

## 15. P28/P29 Wizard-Engine Checkpoints

> Stand P31 + Audits. Backend-Tests: `mvn -B test` (402); Frontend: `npx vitest run` (169); E2E: `cd frontend && npm run test:e2e` (Playwright, 9 Tests — Login-Setup, Wizard-Pakete, Charakter-Wizard, Sheet-DSA, Kampf-Manöver, Kampf-Zustände/Schadensart, System-Pin, Save-Gate, Dashboard-Smoke). Voraussetzung: pm2 `lwe-frontend`/`lwe-backend` laufen, `npx playwright install chromium` einmalig; Zugang via `E2E_EMAIL`/`E2E_PASSWORD` (Default devbe).

### 15.1 Autorierung (SystemWizard)
- [x] **Automatisiert (Playwright):** Paket-Step zeigt gespeicherte Pakete (Name/Kosten/Auto-Merkmale/Restriktionen), Live-Vorschau rechnet („Kosten: 18 AP · staerke +1 · Nachtsicht"), Save öffnet die Übersicht erfolgreich
- [ ] P28: Budget/Kostenkurven/Traits/Steigerungs-Matrix/Tabellen-Derived/Pakete setzen → Save gated bei Fehlern (Paket-Name/Kosten/Mods + Traits + Attribute)
- [ ] P29-Pakete: Elf anlegen (18 AP, Mods `mut+1`, `gewandtheit+1`, Choice `klugheit/intuition −1`, Auto-Trait Nachtsicht, restricted Zwerg)
- [ ] Vorschau: Elf + Waldelf + Jäger wählen → Kosten 118 AP, Mods + Auto-Traits korrekt, Wahl-Gruppen auflösbar
- [ ] Warnungen erscheinen: Elf ohne Waldelf → „recommended"; Zwerg + Elf → „restricted" (rot)

### 15.2 Sheet (DSA-Referenz `docs/examples/dsa5.json`)
- [ ] Derived-Tabelle: MU14+KL13+IN12=39 → `sk` = 7
- [ ] `asp` erscheint nur mit Trait Zauberer (19,5)
- [ ] Ability „Angriff (AT)" zeigt Schadensart `slashing`

### 15.3 Kampf (P23/P29)
- [ ] Zustand „Wunde" (DM) → Probe-Malus −4; Tick beim Zugbeginn (nach `rounds` weg)
- [ ] Schicksalspunkt ausgeben → Re-Roll; bei 0 → 422 und Toast
- [ ] Manöver „Wuchtschlag" → ActionBar-Button, AP-Kosten 2, Schaden +4 (Log mit `(bludgeoning)`)
- [ ] System ohne Manöver → ActionBar unverändert
- [ ] Waffe (ausgerüstet, `damage_type: fire`) vs. Feuer-resistent (Metadaten) → halber Schaden; Verwundbar → doppelt; `damage_armor: 3` → −3 vor Resistenz

### 15.4 Regression
- [ ] Alte Systeme (D20Lite/TwoDicePool/Fudge/dnd5e/coc7e) laden/bauen unverändert
- [x] `docs/examples/dsa5.json` validiert gegen `DEFAULT_SCHEMA` + Sheet/Probe-Abnahme (Backend-Tests)

### 15.5 E2E-Backlog (Playwright)
- [x] Sheet-E2E (DSA-Referenz): sk=7, asp nur mit Zauberer, Rüstung (P31-T01)
- [x] Kampf-E2E: Session im UI starten → Manöver + AP-Gate (P31-T02); offen: Zustands-Tick + Schadensarten-Log
- [x] Wizard-Save-Gate-E2E: ohne Attribute blockiert mit Toast (P31-T03)
- [x] System-Pin-E2E: Version-Bump, Badge, UI-Pull (T32-T02)
- [x] CombatPage-Stabilität: Ursache war kein WS-Loop, sondern eine ResizeObserver-Schleife in `usePixiApp` (Border-Box statt Content-Box → Canvas wuchs kontinuierlich). Fix: `clientWidth/Height`; E2E klickt wieder ohne `force` (finaler Audit F1)
