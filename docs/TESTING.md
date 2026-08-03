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
- [ ] D20Lite-Template laden → Wizard zeigt 6 Attribute (staerke, geschick, etc.)
- [ ] Alle 10 Wizard-Schritte sind erreichbar (Vor/Zurück-Navigation)
- [ ] Schritt 10 (Übersicht) zeigt alle konfigurierten Daten

#### 13.1.2 Probe-Typ konfigurieren (Schritt 10)
- [ ] `d20_target` auswählen → Probe-Expression = `1d20+mod`
- [ ] `d100_threshold` auswählen → Probe-Expression = `1d100`
- [ ] `d20_3attr` auswählen → Probe-Expression = `3d20`
- [ ] Test-Würfel-Button funktioniert (zeigt Ergebnis an)

#### 13.1.3 Combat konfigurieren (Schritt 10)
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

#### 13.5.2 Import
- [ ] Import-Button im CharacterSheet-Header (Upload-Icon)
- [ ] Exportierte JSON-Datei auswählen
- [ ] Neuer Character wird erstellt
- [ ] Browser navigiert zum neuen Character-Sheet
- [ ] Alle Werte (Attribute, Skills, Items) korrekt übernommen
- [ ] attributes_json nach Import nicht doppelt encodiert (kein "{\\\\\"...")

#### 13.5.3 Edge-Cases
- [ ] Import ohne world_id → Toast "No world_id in import file"
- [ ] Import ungültiger JSON → Toast "Import failed"
- [ ] Export + Import eines Characters mit per-character Skills → Skills erhalten

---

### 13.6 Resting

**Vorbereitung:** Character in Welt mit D&D-System (resting konfiguriert)

#### 13.6.1 Short Rest
- [ ] Character hat niedrige HP (z.B. 10/50)
- [ ] `POST /entities/{id}/rest/short` aufrufen
- [ ] HP um heal_percent (50%) erhöht → 10 + 25 = 35
- [ ] AP auf maximum恢复了

#### 13.6.2 Long Rest
- [ ] Character hat niedrige HP + AP
- [ ] `POST /entities/{id}/rest/long` aufrufen
- [ ] HP = max (50/50)
- [ ] AP = max

#### 13.6.3 Ohne Resting-Config
- [ ] Welt mit System ohne resting-Konfiguration
- [ ] `POST /entities/{id}/rest/short` → HP unverändert
- [ ] `POST /entities/{id}/rest/long` → HP unverändert

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