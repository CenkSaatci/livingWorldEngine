# DSA-Spieltest „Gareth-Kampagne" (2026-09-12)

End-to-End-Spieltest über das Frontend mit 3 Accounts (Meister + 2 Spieler),
vollem DSA-5-Regelsatz (Attribute/Talente/Zauber/Liturgien/Vor- & Nachteile/
Pakete/Kampf). Politik: alles Gefundene sofort fixen, Bericht am Ende.

## Aufgebaute Spielwelt

- **System:** `DSA5 Playtest` v2 (Import via UI aus `docs/examples/dsa5-playtest.json`:
  8 Attribute, 59 Fertigkeiten (43 Talente + 10 Zauber + 6 Liturgien), 14 Merkmale,
  7 Pakete (Elf/Waldelf/Zwerg/Jäger/Magier/Geweihte/Krieger), 5 Fähigkeiten,
  5 Zustände, Steigerungstabelle A–D, Budgets 1100 AP)
- **Welt:** Aventurien (Template) → Region Mittelreich → Ort Gareth (city) →
  Fraktion Praios-Kirche → NSC Alrik Immerda (Gastgeber/Questgeber)
- **Kampagne:** Gareth-Kampagne (Fork-Welt, DSA5 Playtest v2, 2 Spieler als Mitglieder)
- **Charaktere:** Lysander Funkenflug (Magier, MU14/KL14/IN13, HP 16) und
  Brinja Eisenarm (Kriegerin, KK14/KO13, HP 19) — beide via Charakter-Wizard
- **Abenteuer:** „Der verschwundene Karren" (2 Nodes, gespielt + abgeschlossen)
- **Quest:** „Der verschwundene Karren" (fetch) + **Kampf:** Lysander/Brinja vs.
  Wegelagerer (Sieg, Writeback verifiziert) + **Session** + **Live-Chat** Spieler↔Spieler

## Regel-Korrektheit (gegen DSA 5 geprüft)

- 3W20-Talentproben (Einzelerfolge, Gesamt nur bei 3/3) ✓
- LeP `(KO+KK)/2+5`, AsP `(MU+KL+IN)/2`, INI `(MU+GE)/2` ✓ (Sheet + API verifiziert)
- Attribut-Basis 8, Staffelkosten 1/2/4, Paket-Mods (KK 13+1=14), Auto-Traits ✓
- Kampf: Initiative, AP-Verbrauch, Schaden mit Schadensart, Besiegt-Status, Writeback ✓

## Gefixt (16 Befunde, alle mit Tests + Live-Verifikation)

| # | Befund | Fix |
|---|---|---|
| 1 | Import strich backend-gültige Keys (traits/packages/conditions/…) still | `utils/importRules.ts` + 3 Tests |
| 2 | Wizard Edit→Save löschte `conditions[]` (kein from/to-Mapping) | opake Roundtrip-Erhaltung + 2 Tests |
| 3 | Dashboard-Platzhalter „Frontend-Skelett … Phase 3" | `app.no_worlds` (6 Locales) |
| 4 | `gameView.*`-Keys unübersetzt (rohe Keys in Aria-Labels) | 7 Keys × 6 Locales |
| 5 | Kampagnen-Mitglieder als UUID-Fragment | `username` in Member-API + UI |
| 6 | Entity-Liste: Fraktion als UUID-Fragment | Frontend-Namensauflösung |
| 8 | Kampf-HP hardcoded 10; Creation-HP Default 10 (LeP 15 z. B. ignoriert) | HP-Init aus `lep` (campaignId) + Kampf nutzt Entity-HP; 5 Tests |
| 9 | Debug-Rotquadrat (`MapCanvas`) im Produktionscode | entfernt |
| 10 | Kampf-Session bei Reload/Deep-Link verloren | `GET /combat/active` + Store-Fallback + Empty-State; 2 Tests |
| 11 | Chat zeigte System-Events als Roh-JSON | nur Events mit menschlichem Text |
| 12 | Keine Quest-Erstellung im Frontend | Erstellen-Dialog im QuestLog |
| 13 | Quest ohne objectives → 500 (NOT NULL) | Defaults `[]`/`{}` + Test |
| 14 | Adventure-Schreibops (Override/Nodes/Choices/Force/Inject) für Spieler offen (HIGH) | DM-Gate (`requireDmAdventure`); live 403/200; 2 Tests |
| 15 | NPC-Edit überschrieb metadata (Services/Preise gingen verloren) | Merge + Dienste-/Preis-Editor; live verifiziert |
| 16 | Diplomatie zeigte Fraktions-IDs | Namensauflösung |
| 17 | Quest create/delete für Mitglieder offen | DM-only (Status bleibt spielbar); live 403/201; Test |
| 18 | Karte 403 für Mitglieder (F5-Audit hatte nur Owner/PUBLIC) | `requireRead` in `WorldMapService.getMap`; 2 Tests; live 200/403 |
| 19 | Mobile Header gequetscht (Titel-Umbruch, abgeschnittene Buttons) | responsive Kürzungen (Dashboard/GameView) + Screenshots |

## Phase 35 — umgesetzt (2026-09-12, alle Entscheidungen)

- **B9 Aufrunden:** `DerivedValueService` rundet auf (Anzeige + HP konsistent)
- **B1 Talent-FW:** 5. Wizard-Step mit AP-Kosten, Cap maxSkillValue, `skillsJson`
- **B7 Auto-HP:** Neuberechnung bei Attribut-Änderung (Schaden bleibt), mit campaignId
- **B8:** `createAdventure` DM-only
- **B6 i18n:** 9 Dateien (QuestLog, Market, AdventurePlay, Location-/RegionView, WorldMap, RegionPicker, FactionPage, Modals) + Markt-Leak (weltfremde NPC-Angebote) gleich mit gefixt
- **B2 Conditions-Editor:** Zustands-Katalog in Wizard-Step 8 (Name/Runden/Effekte)
- **B3 Zauber VOLL:** `POST /rolls/cast` (Probe + AsP/KaP-Abzug, Trait-Pflicht), Casting-Badges im Sheet, Kosten in `dsa5-playtest.json` (System v5)
- **B5 Chat-Historie:** V103 `chat_messages`, letzte 50 beim Betreten
- **B4 Handelsfenster:** Angebot/Gegenangebot/Annahme/Abbruch, atomarer Tausch, Item-Namen-Anreicherung

## Backlog-Status (Stand Runde 4)

B1–B9 sind umgesetzt (Phase 35); **B10 (Fraktionsverlinkung) in Runde 4 erledigt**.
Offen bleibt nur `systemWizard.json`-Übersetzung (GM-Tool, DE-Fallback) sowie die
ADR-013-Social-Tasks (P35-SM-01…04).

## Ursprüngliches Backlog (→ TASKS Phase 35)

- **B1:** Charakter-Erstellung ohne Talent-FW (alle FW 0; Steigern pro Skill existiert)
- **B2:** Conditions-Editor im System-Wizard (aktuell nur Erhaltung, kein Edit)
- **B3:** Zauber als First-Class-Mechanik (Schulen Freitext, kein AsP-Abzug/Slot-Enforcement)
- **B4:** Item-Transfer/Handel Spieler↔Spieler fehlt (nur NPC-Markt)
- **B5:** Chat ohne Historie (nur live via WS)
- **B6:** i18n-Retrofit Welt-Komponenten (FactionPage, Adventure-Play, QuestLog/Markt EN, EN-Enum-Werte)
- **B7:** HP-Neuberechnung bei Attribut-Steigerung/Level-Up
- **B8:** `createAdventure` für Mitglieder offen (Spieler-Agency? Entscheidung)
- **B9:** LeP-Anzeige bruchteilig (15.5 — Rundungsregel entscheiden)
- **B10:** Factions-Seite nicht aus Game View verlinkt (prüfen)

## Responsive (390px / 1920px geprüft)

- Dashboard, Charakterbogen, Quest-/Abenteuer-Panels: mobil sauber gestapelt ✓
- Game View mobil: Karte + Regions-Overlay ok, Sidebar/Chat per Toggle; Header war
  gequetscht → responsive gekürzt (#19) ✓
- Desktop 1920px: 3-Spalten-Layout (Sidebar/Karte/Chat) füllt die Breite ✓
- UI-Bauweise: React + Tailwind (Dark-Theme-Tokens), PixiJS-Karte, zustand-Stores,
  i18n (6 Locales), ReactFlow-Abenteuereditor

## Test-Artefakte (Dev-DB)

Accounts `playtest-meister/-spieler1/-spieler2@test.de` (Test123!) werden als
Testdaten behalten (gehören Playtest-Usern, `e2e-cleanup.sh` greift nur eigene
„E2E“-Artefakte an). Welt Aventurien +
Fork, Kampagne Gareth-Kampagne. Aufräumen bei Bedarf: `scripts/e2e-cleanup.sh`
(greift nur eigene Objekte des Login-Users).

## Betriebshinweis (Phase 38)

Kampagnen pinnen das Regelwerk als Snapshot (`rulesJsonSnapshot`). Nach Engine-/Content-
Aenderungen am System (T2–T7: `attack` at/pa, `blocks`, `fate`, `social`, `social_actions`)
muss der DM in der Kampagne **„System nachziehen"** (`POST /campaigns/{id}/pull-system`)
ausfuehren — sonst spielt die Kampagne weiter mit den alten Regeln.
