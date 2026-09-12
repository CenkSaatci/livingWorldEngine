# Manueller Testplan — Feature-Abnahme & Verbesserungssuche

> **Zweck:** Gemeinsam (Du klickst in der UI, ich beobachte Backend-Log/DB/API parallel und fixe sofort)
> gehen wir alle implementierten Features durch, finden Bugs, UX-Schwächen, Inkonsistenzen und
> Verbesserungsquellen. Kein Automatisierungsersatz — Ergänzung zu Backend- (501), Frontend- (189)
> und E2E-Tests (11).
>
> **Stand:** Phase 38 (T0–T7) + alle vorherigen Phasen. Änderbar — Funde und Status werden hier gepflegt.

---

## 0. So arbeiten wir

1. **Block für Block** (unten in Reihenfolge oder nach Wunsch). Pro Fall: `[ ]` → `[x]` bei bestanden,
   `[!]` bei Fund, `[~]` bei bewusst übersprungen.
2. **Rollen:** Du testest UI (2 Browser/Profile für Spieler-Sichten). Ich prüfe parallel API/DB,
   schaue ins Backend-Log (`pm2 logs lwe-backend --lines 50`) und fixe gefundene Bugs direkt
   (mit Test + Regression), wenn sie klein sind; große Funde wandern in die Fundliste.
3. **Funde:** ID `QA-###` mit Block, Fall, Schwere, Repro, Vorschlag. Schweregrade:
   - **BLOCKER** — Flow nicht abschließbar
   - **HIGH** — falsches Ergebnis / Datenverlust / Rechteproblem
   - **MEDIUM** — Umweg, unklare Meldung, Inkonsistenz zwischen Systemen
   - **LOW** — Kosmetik, Text, Tippfehler, i18n
   - **IDEE** — UX/Feature-Verbesserungsvorschlag (kein Bug)
4. **Nach jedem Block:** Ich lasse Backend/Frontend/E2E laufen (Regression), wir re-testen Funde.
   Am Ende: Konsolidierung → Backlog (Phase 39) priorisiert nach Funden.
5. **Stop-Regel:** Wenn ein BLOCKER auftaucht, unterbrechen wir den Block, ich fixe, dann weiter.

### Worauf wir besonders achten (Verbesserungsquellen)
- Versteht man **Fehlermeldungen** ohne Handbuch? (Code sichtbar? Klartext? Was ist zu tun?)
- **Anzahl Klicks** für Alltagsaufgaben (Probe, Schaden, Quest anlegen, Systemwechsel).
- **Konsistenz** zwischen den drei Systemen (gleiche Konzepte = gleiche UI/Sprache).
- **Leere Zustände / Ladezustände**: Was sieht ein neuer Nutzer ohne Daten?
- **Rückmeldung**: Passiert nach Klick sichtbar etwas (Toast, Log, WS, Badge)?
- **Mobile/Touch**, Tastatur/Fokus, Kontraste.
- **Zahlen-Plausibilität** (AP/AsP/LEP/Fate, Schadens-Modifikatoren, Caps).
- **Daten-Robustheit**: Backend-Neustart mitten im Flow, Doppelklick, zwei Spieler gleichzeitig.

---

## 1. Testdaten

### 1.1 Accounts & Zugänge

| Rolle | Account | Zweck |
|---|---|---|
| DM/Owner | `playtest-meister@test.de` | Welt/Kampagne/Abenteuer, DM-Queue, Systeme |
| Spieler 1 | `playtest-spieler1@test.de` | PC Lysander/QA-Mira |
| Spieler 2 | `playtest-spieler2@test.de` | PC Brinja/QA-Torben |
| Admin (falls vorhanden) | siehe AdminPage-Zugang | Admin-Block A |
| E2E/Dev | `devbe@test.de` | Systeme importieren, Aufräumen |

> Passwörter wie bei Anlage (i. d. R. `Test123!`). Falls Login klemmt: Ich setze per API einen
> neuen Login oder Token und gebe dir den Link.

### 1.2 Empfehlung: Frische QA-Welt + bestehende Gareth-Kampagne

- **Neu (empfohlen für saubere Flows):** Ich lege in ~5 Minuten an (sag „Setup QA"):
  - System **QA-DSA5** aus `docs/examples/dsa5-playtest.json` (v6: attack at/pa, blocks, fate, social, baseValues-Beispiel)
  - Welt **QA-Gareth**, Kampagne **QA-Kampagne** (System gepinnt), Karte + 3 Regionen + 2 Orte
  - NPCs: **QA-Alrik** (Händler, Services), **QA-Räuber** (Kampf), **QA-Wache**
  - PCs per Wizard: **QA-Mira** (Spieler 1), **QA-Torben** (Spieler 2)
  - **QA-Schmiede** (dnd5e.json) mit PC QA-Aragorn + NPC QA-Ork
  - **QA-Arkham** (coc7e.json) mit PC QA-Investigator + NPC QA-Kultist
- **Bestand (Regression/Weiterführung):** Gareth-Kampagne (`a7740a16-…`), PCs Lysander/Brinja,
  läuft auf System v6 (per „System nachziehen").

### 1.3 Preflight (S-Block)

- [ ] `pm2 list` zeigt `lwe-backend`, `lwe-frontend`, `discord-bot` online
- [ ] Backend gesund: `curl -s localhost:8080/api/v1/game-systems` liefert 401/403 (nicht 5xx)
- [ ] Frontend erreichbar: http://localhost:5173
- [ ] Test-Accounts eingeloggt (2 Browser/Profile)
- [ ] Aufräum-Kommando bekannt: `E2E_EMAIL=devbe@test.de E2E_PASSWORD='…' ./scripts/e2e-cleanup.sh --dry-run`

---

## Block S — Setup & Grundlagen

#### S-01 ★ Login/Logout
- **Ziel:** Anmeldung funktioniert, Session bleibt nach Reload.
- **Schritte:** 1) Login Meister. 2) F5. 3) Logout.
- **Erwartet:** Dashboard mit Weltliste; nach Reload eingeloggt; Logout → Welcome/Login.
- **Fund:**

#### S-02 Registrierung + E-Mail-Verify-Banner
- **Ziel:** Neuer Account kann sich registrieren; Verify-Zustand sichtbar.
- **Schritte:** 1) Ausloggen. 2) Registrieren (neue Mail). 3) Dashboard ansehen (Banner?). 4) Verify-Link falls vorhanden.
- **Erwartet:** Registrierung ok, Banner nur bei unbestätigter Mail (kein Dauer-Banner nach Verify).
- **Fund:**

#### S-03 Passwort vergessen/Reset
- **Ziel:** Reset-Flow endet im Login.
- **Schritte:** 1) „Passwort vergessen" → Mail/Token-Flow. 2) Reset durchführen. 3) Mit neuem Passwort einloggen.
- **Erwartet:** Klare Meldungen, kein User-Enumeration-Leak (gleiche Antwort für unbekannte Mail).
- **Fund:**

#### S-04 Dashboard-Wahrheit
- **Ziel:** Dashboard zeigt nur passende Welten/Kampagnen des Users (inkl. Mitgliedschaften).
- **Schritte:** 1) Meister-Dashboard ansehen. 2) Spieler-Dashboard ansehen. 3) Karte „Welt erstellen" + Kampagnen-Karten prüfen.
- **Erwartet:** Owner sieht alle; Spieler nur Mitglieds-Welten; Karten zeigen Welt/System-/Kampagnennamen; Links funktionieren.
- **Fund:**

#### S-05 Leerer-Zustand-Tour
- **Ziel:** Neue Nutzer sehen hilfreiche Empty States.
- **Schritte:** 1) Als frischer Account einloggen. 2) Dashboard, Weltenliste, Systemliste, Kampagnenliste öffnen.
- **Erwartet:** Überall Erklärtext + CTA (kein weißer Bildschirm, keine `undefined`-Reste).
- **Fund:**

---

## Block A — Auth, Profil, Admin

#### A-01 Rate-Limit Login
- **Ziel:** Brute-Force-Schutz greift mit klarer Meldung.
- **Schritte:** 1) Falsches Passwort ~10× schnell hintereinander.
- **Erwartet:** Ab irgendwann `AUTH_RATE_LIMITED` (429) mit verständlichem Text; kein Crash; nach Wartezeit wieder normal.
- **Fund:**

#### A-02 Passwort ändern (Settings)
- **Ziel:** Änderung wirkt sofort, alte Session wird beendet.
- **Schritte:** 1) Settings → Passwort ändern. 2) Ausloggen, neu einloggen mit neuem Passwort. 3) Alte Refresh-Session prüfen (zweiter Browser).
- **Erwartet:** Neues Passwort gilt; alte Session wird abgewiesen oder sauber erneuert (kein Zombie-Zugriff).
- **Fund:**

#### A-03 Profil: Username/Sprache
- **Ziel:** Sprache umschalten (de→en) ohne Reload-Bruch; Username-Änderung erscheint in Mitgliederlist/Beziehungen.
- **Schritte:** 1) Settings → Sprache en. 2) Navigation prüfen. 3) Username ändern; Weltmitgliederliste ansehen.
- **Erwartet:** UI wechselt (GM-Tool darf auf DE zurückfallen); Username überall aktualisiert.
- **Fund:**

#### A-04 AdminPage (falls Admin-Konto)
- **Ziel:** Adminfunktionen sichtbar/unterscheidbar; Nicht-Admin sieht nichts.
- **Schritte:** 1) Als Admin AdminPage öffnen (Nutzer-/Audit-Log-Ansicht). 2) Als Nicht-Admin URL direkt aufrufen.
- **Erwartet:** Admin sieht Daten; Nicht-Admin 403/Weiterleitung; Audit-Einträge zeigen Aktionen (Roll, Queue, System-Änderung).
- **Fund:**

#### A-05 Autologout/Access-Expiry
- **Ziel:** Abgelaufener Access-Token wird transparent erneuert (Refresh), nicht als Fehler sichtbar.
- **Schritte:** 1) 30+ Minuten untätig sein oder Access-Token künstlich ablaufen lassen. 2) Beliebige Aktion.
- **Erwartet:** Aktion klappt ohne Re-Login (Refresh greift) oder klare „Sitzung abgelaufen"-Meldung.
- **Fund:**

#### A-06 Logout im zweiten Tab
- **Ziel:** Logout wirkt global.
- **Schritte:** 1) Zwei Tabs eingeloggt. 2) In Tab 1 Logout. 3) Tab 2 Aktion ausführen.
- **Erwartet:** Tab 2 wird ausgeloggt/zu Login geführt (kein Geist-Zustand).
- **Fund:**

#### A-07 Unbekannte Route / 404
- **Ziel:** Kein kaputter Screen bei falscher URL.
- **Schritte:** 1) URL `/gibtsnicht` und `/worlds/xxx` (fremde ID) öffnen.
- **Erwartet:** „Nicht gefunden"-Zustand mit Link zurück; keine Console-Errors-Lawine.
- **Fund:**

#### A-08 Ideen-Sweep Auth
- **Ziel:** Freie Beobachtung.
- **Schritte:** Login-Seite, Register, Verify, Reset durchklicken und notieren, was fehlt/verwirrt.
- **Erwartet:** Notizen.
- **Fund:**

---

## Block B — Game-Systeme & Wizard

> Testobjekt: `docs/examples/dsa5-playtest.json` (v6). Import: Systeme-Seite (Import/Neuanlage), Wizard editieren.

#### SYS-01 ★ Import DSA5-Playtest
- **Ziel:** Import erzeugt valides System (alle neuen Felder: attack value at, difficulties, casting, blocks, fate, social, social_actions, baseValues).
- **Schritte:** 1) System importieren oder Wizard öffnen. 2) Speichern. 3) Seite neu laden.
- **Erwartet:** Kein Schema-Fehler; nach Reload sind Attack-Quelle `at`/`pa`, `blocks`, `fate`, `social_actions` unverändert da.
- **Fund:**

#### SYS-02 Wizard Schritt Attribute & Derived Values
- **Ziel:** Attribute/Formeln/Tabellen korrekt editierbar.
- **Schritte:** 1) Attribut ändern (min/max/default). 2) Derived Value mit Formel `(a+b)/2+5`. 3) Derived Value mit Tabelle (z. B. sk min/max). 4) Probe-Wert unten live prüfen.
- **Erwartet:** Werte/Formeln korrekt; ungültige Formel zeigt Validierungsfehler, blockiert Speichern nicht still.
- **Fund:**

#### SYS-03 Wizard Skills (FW, Attribute, Kosten, Casting)
- **Ziel:** Fertigkeiten inkl. Zauberressourcen pflegbar.
- **Schritte:** 1) Skill mit 3 Attributen anlegen. 2) Casting: resource `asp`, cost 3, requiresTrait „Zauberer", restore `long`. 3) Speichern + Reload.
- **Erwartet:** Alles persistiert; Editor zeigt Werte wieder.
- **Fund:**

#### SYS-04 Wizard Kampf: Action-Types, Manöver, Attack-Gate
- **Ziel:** Generisches Angriffsmodell pflegbar.
- **Schritte:** 1) Combat aktivieren. 2) Attack-Quelle auf `value: at`, Ziel `pa`, Würfel `1d20`, Vergleich `lte`. 3) Manöver „Wuchtschlag" apCost 2, attackMalus 4, Effekt damage +3. 4) Speichern + Reload.
- **Erwartet:** Angriffswurf-Editor (Quelle-Auswahl Attribut/Wert/Fertigkeit) vorhanden, Werte persistiert.
- **Fund:**

#### SYS-05 Wizard Difficulties
- **Ziel:** Schwierigkeitsgrade (multiplier/delta) anlegen/ändern/reload.
- **Schritte:** 1) `hard {multiplier:0.5}`, `erschwert {delta:2}` anlegen. 2) Speichern + Reload. 3) Zeile löschen + speichern.
- **Erwartet:** Liste korrekt, keine Geist-Zeilen.
- **Fund:**

#### SYS-06 Wizard Zustände + Blocks
- **Ziel:** `conditions[].blocks` editierbar.
- **Schritte:** 1) Zustand „Betäubt" mit blocks `ATTACK,MOVE,DEFEND,MANEUVER,ABILITY`. 2) Speichern + Reload. 3) Tippfehler/Leerzeichen testen.
- **Erwartet:** Komma-Liste robust; Reload zeigt Blocks; leere Einträge fallen raus.
- **Fund:**

#### SYS-07 Wizard Fate + Social Actions
- **Ziel:** Neue Regelblöcke editierbar.
- **Schritte:** 1) Budget-Step: Fate „Bonus je Punkt"=1, „Tod abwenden"=1. 2) Soziale Aktion „Freundlich bitten" (Skill Überreden, Gewicht 1, onSuccess Beeindruckt 3 Runden, onFailure Verärgert). 3) Speichern + Reload.
- **Erwartet:** Persistiert 1:1; Effekt-Zeilen bleiben erhalten.
- **Fund:**

#### SYS-08 Wizard Pakete (Spezies/Kultur/Profession, Choices, baseValues)
- **Ziel:** Paket-Editor inkl. Basiswerten.
- **Schritte:** 1) Paket „Waldelf" mit Attribut-Mod + Choice + autoTrait + baseValue Klettern 4. 2) Vorschau-Kosten beobachten. 3) Speichern + Reload.
- **Erwartet:** Choice-Auflösung, Kosten, Auto-Traits korrekt; baseValues persistent.
- **Fund:**

#### SYS-09 Validierung: kaputte Systeme werden abgewiesen
- **Ziel:** Schema schützt vor Müll (T7-Fix prüfen).
- **Schritte:** 1) Root-Feld `"name": "X"` ins JSON schmuggeln (Import/API). 2) `attack.dice` = `"1d20+2"`. 3) `probeType` ungültig.
- **Erwartet:** Klarer Validierungsfehler mit Pfad; gültige Felder davor bleiben nutzbar; kein 500.
- **Fund:**

#### SYS-10 Wizard-Speichern ohne Pflichtfelder (Save-Gate)
- **Ziel:** Gate ist hilfreich, nicht nervig.
- **Schritte:** 1) Attribute komplett leeren. 2) Speichern.
- **Erwartet:** Prüfbericht-Toast nennt konkret fehlende Felder; Navigation möglich.
- **Fund:**

#### SYS-11 System-Versionierung & Clone
- **Ziel:** Änderung erhöht Version; Clone kopiert alles.
- **Schritte:** 1) System speichern (Änderung) → Version beobachten. 2) Clone „QA-DSA5 (Kopie)". 3) Kopie öffnen.
- **Erwartet:** Version monoton +1; Clone vollständig (neue Felder enthalten).
- **Fund:**

#### SYS-12 System-Pin + „System nachziehen" ★
- **Ziel:** Kampagnen-Snapshot isoliert Änderungen; Nachziehen übernimmt sie.
- **Schritte:** 1) In QA-Kampagne eine Skill-Probe (Überreden). 2) System ändern (z. B. `at`-Formel +1) + speichern. 3) Probe in Kampagne → Verhalten alt? 4) „System nachziehen" (DM). 5) Probe erneut.
- **Erwartet:** Vor Nachziehen altes Verhalten (Snapshot), danach neues; Versionsanzeige aktualisiert.
- **Fund:**

#### SYS-13 Beispielsysteme DnD/CoC importieren
- **Ziel:** Beide validieren und laden (Generik-Nachweis).
- **Schritte:** 1) `dnd5e.json` als System importieren. 2) `coc7e.json` importieren. 3) Jeweils Wizard öffnen, Attack-Config ansehen.
- **Erwartet:** Beide ohne Fehler; DnD Attack `attribute/gte/ac`, CoC `skill`/`lte`.
- **Fund:**

#### SYS-14 System löschen/deaktivieren
- **Ziel:** Soft-Delete versteckt System, laufende Kampagne bleibt handlungsfähig.
- **Schritte:** 1) Test-Kopie löschen/deaktivieren. 2) Systemliste filtern. 3) Kampagne mit dem System öffnen (falls verknüpft).
- **Erwartet:** Kein hartes Löschen; verknüpfte Kampagne bleibt mit Snapshot spielbar; Liste ohne Geist.
- **Fund:**

#### SYS-15 System-Shares (falls UI vorhanden)
- **Ziel:** Teilen mit Nutzer funktioniert lesend; Fremd-Zugriff ohne Share verboten.
- **Schritte:** 1) System mit Spieler teilen. 2) Spieler öffnet System. 3) Nicht-geteiltes System als Spieler direkt aufrufen.
- **Erwartet:** Geteilt: lesbar; ungeteilt: 403/DENIED ohne Existenz-Leak.
- **Fund:**

#### SYS-16 GM-Tool-i18n
- **Ziel:** systemWizard auf Englisch nutzbar, Fallback akzeptabel.
- **Schritte:** 1) Sprache en. 2) Wizard durchsteppen; fehlende Keys notieren (sichtbare Platzhalter?).
- **Erwartet:** Bedienbar; nur bekannte Lücken (Fallback DE) — neue Platzhalter = Fund.
- **Fund:**

#### SYS-17 Import/Export-Roundtrip
- **Ziel:** Export JSON → Import ergibt identisches System.
- **Schritte:** 1) System exportieren (falls Button; sonst `GET /game-systems/{id}`). 2) Neues System daraus importieren. 3) Diff auf Schlüssel.
- **Erwartet:** Keine verlorenen Felder (inkl. fate/social/blocks/baseValues).
- **Fund:**

#### SYS-18 Ideen-Sweep Wizard
- **Ziel:** Freie UX-Beobachtung.
- **Schritte:** Alle Steps einmal schnell durchklicken; notieren was verwirrt/fehlt (Hilfetexte, Reihenfolge, Validierungsnähe).
- **Erwartet:** Notizen.
- **Fund:**

---

## Block C — Welt, Karte, Fraktionen, Regionen, Zeit

> Daten: QA-Welt neu anlegen (Meister). Bestandsvergleich Gareth optional.

#### WLT-01 ★ Welt erstellen (leer + aus Vorlage)
- **Ziel:** Erstellung inkl. Sichtbarkeit/Quota.
- **Schritte:** 1) Welt „QA-2" leer anlegen. 2) Welt aus Vorlage/Clone anlegen (falls Option). 3) Einladungen/Mitglieder prüfen.
- **Erwartet:** Welt erscheint im Dashboard; Owner = Ersteller; Sichtbarkeit wie gewählt.
- **Fund:**

#### WLT-02 World Editor: Beschreibung/Einstellungen
- **Ziel:** Grundeinstellungen persistieren (Zeit, Chat-Log-Toggle, Sichtbarkeit).
- **Schritte:** 1) Weltzeit setzen. 2) Kampf-Chat-Log an/aus. 3) Reload.
- **Erwartet:** Werte bleiben; Toggle beeinflusst später den Kampf-Chat (merken für FGT-15).
- **Fund:**

#### WLT-03 Regionen
- **Ziel:** Region anlegen/editieren/löschen; Polygon/Position speichern.
- **Schritte:** 1) Region „Nordmark" (Beschreibung, Gefahr, Klima). 2) Auf Karte zeichnen. 3) Reload.
- **Erwartet:** Geometrie/Text bleiben; Listenansicht konsistent.
- **Fund:**

#### WLT-04 Orte + Hauptstadt
- **Ziel:** Ort in Region, Hauptstadt-Flag, Typ/Service-Daten.
- **Schritte:** 1) Ort „QA-Dorf" als Hauptstadt setzen. 2) Region ansehen.
- **Erwartet:** Hauptstadt-Marker sichtbar; nach Fork (R-10) Flag erhalten.
- **Fund:**

#### WLT-05 Fraktionen + Beziehungen
- **Ziel:** Fraktion anlegen, Relation zwischen Fraktionen setzen.
- **Schritte:** 1) „Rat" und „Räuberbande". 2) Relation `WAR` setzen. 3) FactionPage ansehen.
- **Erwartet:** Relation farbig/ikonisch korrekt; persistiert nach Reload.
- **Fund:**

#### WLT-06 Karte: Pan/Zoom, Marker
- **Ziel:** Karte bedienbar; Marker öffnen Details.
- **Schritte:** 1) Pan/Zoom (Mausrad, Touch). 2) Marker Ort/NPC/Region anklicken.
- **Erwartet:** Kein Springen/Flackern; Detail-Links korrekt; keine Console-Errors.
- **Fund:**

#### WLT-07 Fog of War (falls aktiv)
- **Ziel:** Aufdecken bleibt gespeichert; unterschiedliche Sichten.
- **Schritte:** 1) Als DM Bereiche aufdecken. 2) Als Spieler ansehen (2. Browser). 3) Reload.
- **Erwartet:** Spieler sieht nur Aufgedecktes; Persistenz nach Reload; DM-Sicht vollständig.
- **Fund:**

#### WLT-08 Map-Debug-Reste (Playtest-Fund Regression)
- **Ziel:** Kein Debug-Quadrat/Entwickler-Reste sichtbar.
- **Schritte:** 1) Karte in beiden Rollen ansehen. 2) Zoom-Grenzen prüfen.
- **Erwartet:** Keine Debug-Artefakte; saubere Ränder.
- **Fund:**

#### WLT-09 Wetter pro Region
- **Ziel:** Wetter setzen/ändern.
- **Schritte:** 1) Region öffnen → Wetter (Temperatur/Wind/Typ/Beschreibung). 2) Wechsel simulieren (falls Weather-Service-Button). 3) Reload.
- **Erwartet:** Werte sichtbar/persistiert; Spieler-Sicht konsistent.
- **Fund:**

#### WLT-10 Weltzeit-Fortschritt
- **Ziel:** Zeit vergeht/ist setzbar; Anzeige konsistent in Kopfzeile.
- **Schritte:** 1) Zeit setzen (+1 Tag). 2) Seitenwechsel.
- **Erwartet:** Alle Ansichten nutzen dieselbe Zeit; keine Timezone-Sprünge.
- **Fund:**

#### WLT-11 Rechte: Welt sichtbar/unsichtbar
- **Ziel:** PRIVATE-Welt: Fremder ohne Mitgliedschaft sieht nichts (kein Enumeration-Leak).
- **Schritte:** 1) Welt auf PRIVATE. 2) Fremd-Account: Dashboard + Direkt-URL `/worlds/{id}`.
- **Erwartet:** Keine Existenz-Hinweise (404/denied gleich); keine Einträge in Listen.
- **Fund:**

#### WLT-12 Welt-Mitglieder: Rollen
- **Ziel:** DM-Rolle vs. Spieler-Rolle wirkt (Aktionen aus KRG-Block vorbereiten).
- **Schritte:** 1) Spieler1 als DM, Spieler2 als Spieler. 2) Beide einloggen, Welt öffnen.
- **Erwartet:** DM sieht DM-Aktionen (Anlegen), Spieler nicht.
- **Fund:**

#### WLT-13 Welt-Quota
- **Ziel:** Quota-Fehler ist verständlich (falls Limit erreichbar; sonst überspringen).
- **Schritte:** 1) Viele Welten anlegen bis Limit. 2) Meldung ansehen.
- **Erwartet:** Klare Meldung „Limit erreicht", kein technischer Stacktrace.
- **Fund:**

#### WLT-14 Welt löschen (Soft-Delete)
- **Ziel:** Gelöschte Welt ist gesperrt, Daten bleiben.
- **Schritte:** 1) Test-Welt löschen. 2) Dashboard/Direkt-URL. 3) Als Fremder zugreifen.
- **Erwartet:** `WORLD_ACCESS_DENIED`/ausgeblendet; kein 500; Listen sauber.
- **Fund:**

#### WLT-15 Enrich/List-Performance
- **Ziel:** Große Listen (NPCs/Orte) fühlen sich flott an.
- **Schritte:** 1) ~50 NPCs anlegen (ich kann per API seeden). 2) Entity-Liste scrollen/filtern/suchen.
- **Erwartet:** Kein Ruckeln, Ladezustand sichtbar, Suche filtert sofort.
- **Fund:**

#### WLT-16 Ideen-Sweep Welt
- **Ziel:** Freie Beobachtung (Karte, Editor, Fraktionen).
- **Fund:**

---

## Block D — Kampagne, Rechte-Matrix, Fork

#### KRG-01 ★ Kampagne anlegen (System-Pin)
- **Ziel:** Kampagne wählt System + Welt, pinnt Snapshot.
- **Schritte:** 1) Kampagne „QA-Kampagne" auf QA-Welt mit QA-DSA5. 2) Details öffnen.
- **Erwartet:** gameSystemId + Version sichtbar; Snapshot gesetzt (API: `rulesJsonSnapshot` nicht leer per DB).
- **Fund:**

#### KRG-02 Kampagnen-Mitglieder & Rollen
- **Ziel:** Einladen/entfernen; DM-Rolle.
- **Schritte:** 1) Spieler1+2 einladen. 2) Spieler1 zum DM machen. 3) Spieler2 entfernen → wieder hinzufügen.
- **Erwartet:** Mitgliederliste korrekt; entfernter Spieler verliert Zugriff sofort (2. Browser prüfen).
- **Fund:**

#### KRG-03 Rechte-Matrix ★ (Kern-Tabelle)
- **Ziel:** Owner/DM/Mitglied/Fremd konsistent über Kernaktionen.
- **Vorgehen:** Für jede Zeile mit **Meister / Spieler1 (Owner von QA-Mira) / Spieler2 (Member, fremder Char) / Fremder** prüfen:

| Aktion | Erwartung |
|---|---|
| NPC anlegen/editieren | Owner+DM ja, Spieler nein |
| Abenteuer anlegen/editieren | DM ja, Spieler nein |
| Quest anlegen/Status ändern | DM ja, Spieler nein |
| DM-Queue approve/reject | DM ja, Spieler nein |
| Charakter-QA-Mira: Sheet editieren | Spieler1 ja, Spieler2 nein |
| Inventar/Trade von QA-Mira | Spieler1 ja, Spieler2 nein |
| Rest für QA-Mira | Spieler1 ja, Spieler2 nein |
| Probe für QA-Torben | nur Spieler2/DM |
| Welt-Chat schreiben | Mitglieder ja, Fremder nein |
| Kampf-Zug von QA-Mira | Spieler1/DM ja, Spieler2 nein |

- **Schritte:** Zeile für Zeile durchklicken; bei Verbot: Meldung notieren (Code? Klartext?).
- **Erwartet:** Keine stillen Fehlschläge, keine 500er, keine Rechte-Lücken.
- **Fund:**

#### KRG-04 Ownership setzen (DM)
- **Ziel:** DM kann PC an Spieler binden (`PATCH …/owner`).
- **Schritte:** 1) Als DM QA-Mira Besitzer Spieler1 zuweisen (UI/API). 2) Spieler1 sieht Bearbeiten-Button, Spieler2 nicht.
- **Erwartet:** Eigentümeranzeige korrekt; Legacy-Entities (owner null) bleiben für Mitglieder nutzbar.
- **Fund:**

#### KRG-05 Cross-World-Abwehr (Security-Stichprobe)
- **Ziel:** Fremde IDs liefern deny, nicht Daten.
- **Schritte:** 1) Request mit fremder `entityId` in `/rolls`, `/probe`, Inventar-PATCH (ich stelle curl bereit). 2) Direkt-URLs fremder Welten/Abenteuer/Quests.
- **Erwartet:** 403/404 ohne Daten-Leak; keine unterschiedlichen Fehlercodes für „existiert nicht" vs. „kein Zugriff" (Enumeration).
- **Fund:**

#### KRG-06 Kampagne: Systemwechsel/-nachziehen UI
- **Ziel:** Nachziehen ist auffindbar und verständlich.
- **Schritte:** 1) System ändern. 2) Kampagnen-Detail: Hinweis auf neuere Version? 3) Nachziehen klicken.
- **Erwartet:** Sichtbarer Versions-Hinweis; nach Nachziehen Bestätigung; Verhalten siehe SYS-12.
- **Fund:**

#### KRG-07 Kampagne löschen/verlassen
- **Ziel:** Verlassen/Löschen klärt Sichtbarkeit.
- **Schritte:** 1) Spieler2 verlässt Kampagne. 2) Dashboard/Detail prüfen. 3) Wieder joinen (falls Join-Flow).
- **Erwartet:** Kein Zugriff nach Verlassen; Join-Seite funktioniert mit Einladung/Code.
- **Fund:**

#### KRG-08 Fork: Basis
- **Ziel:** „Welt kopieren"/Kampagnen-Fork erzeugt vollständige Kopie.
- **Schritte:** 1) QA-Welt forken (Kopie-Button). 2) Beide Welten vergleichen: Regionen, Orte, Fraktionen, NPCs, Karte, Wetter.
- **Erwartet:** Alle Objekte da; keine Referenz auf Original-IDs (Stichprobe in DB/API).
- **Fund:**

#### KRG-09 Fork: Quests & Abenteuer-Referenzen
- **Ziel:** T6-Remap korrekt.
- **Schritte:** 1) Vor dem Fork: Quest mit Ziel-NPC/-Ort + Abenteuer mit Nodes/Choices anlegen. 2) Forken. 3) Im Fork Quest öffnen (Ziel zeigt auf Fork-NPC) und Abenteuer spielen.
- **Erwartet:** Keine dangling IDs; Quest-Ziele/Choices zeigen auf Fork-Objekte; Skillcheck-Zweige funktionieren.
- **Fund:**

#### KRG-10 Fork: Adventure-Progress & Beziehungen
- **Ziel:** Fortschritt/Beziehungen wandern mit (T6).
- **Schritte:** 1) Vor Fork: Abenteuer starten + 2 Knoten spielen; Beziehung QA-Mira↔QA-Alrik „freundlich". 2) Forken. 3) Im Fork prüfen (API: progress vorhanden? Beziehungs-Score in sozialer Probe sichtbar?).
- **Erwartet:** Progress (aktueller Knoten/Besucht/Status) im Fork vorhanden; Beziehung ebenso; Metadaten-`relationships` zeigen auf Fork-IDs.
- **Fund:**

#### KRG-11 Fork: Unabhängigkeit
- **Ziel:** Änderungen im Fork berühren Original nicht (und umgekehrt).
- **Schritte:** 1) Im Fork NPC umbenennen. 2) Original ansehen. 3) Original-Quest abschließen; Fork prüfen.
- **Erwartet:** Vollständig getrennt (inkl. Fortschritt/Chat? — Chat-Historie: Erwartung notieren).
- **Fund:**

#### KRG-12 Fork: Spielbarkeit mit neuem DM
- **Ziel:** Fork kann von eingeladenem DM weitergeführt werden.
- **Schritte:** 1) Spieler1 als DM in Fork einladen. 2) Er ändert System/nachziehen (falls Owner-Rechte? beobachten) und leitet Abenteuer.
- **Erwartet:** Rollen wirken wie dokumentiert; unklare Grenzen = Notiz.
- **Fund:**

#### KRG-13 Join-Flow (Einladungslink)
- **Ziel:** Eingeladener kann per Link/Kampagne joinen.
- **Schritte:** 1) Einladung erzeugen. 2) Mit Spieler2 öffnen → JoinPage.
- **Erwartet:** Beitritt klappt, Welt sichtbar, korrekte Rolle.
- **Fund:**

#### KRG-14 Ideen-Sweep Kampagne/Rechte
- **Fund:**

---

## Block E — Charaktere (Wizard, Sheet, Proben, Inventar, Rest, Markt)

> Zwei PCs (QA-Mira Spieler1, QA-Torben Spieler2) im QA-System. DSA-Fokus, Querverweise für DnD/CoC in Block XS.

#### CHR-01 ★ Charakter-Wizard: kompletter Durchlauf
- **Ziel:** PC mit Paketen, Traits, Attributen, Skills, Fate speichern.
- **Schritte:** 1) Wizard öffnen. 2) Spezies „Waldelf" (Mods/Choice/autoTrait), Kultur/Profession falls vorhanden. 3) Attribute verteilen (Budget/AP-Anzeige beobachten). 4) Traits (Vor-/Nachteil, Tier, Exklusion testen). 5) Skills mit FW, Basiswert-Chip (≥4 bei Klettern) beachten. 6) Endwerte/Kosten prüfen. 7) Speichern.
- **Erwartet:** Keine Blockade; Kosten matchen Rechnung; Endwerte enthalten Paket-Mods + Basiswerte; PC erscheint in Liste.
- **Fund:**

#### CHR-02 Wizard: Basiswert-Gratis-Regel (T5)
- **Ziel:** Kauf unter Basiswert kostet 0 AP; darüber nur Differenz.
- **Schritte:** 1) Klettern-Basiswert 4. 2) FW 2 kaufen → Kosten 0. 3) FW 6 kaufen → nur 2 Punkte Kosten. 4) Attribut-Basiswert (falls definiert) analog.
- **Erwartet:** Kostenanzeige/AP stimmen; End-FW zeigt mindestens den Basiswert.
- **Fund:**

#### CHR-03 Wizard: Skill-Cap & Fehlermeldungen
- **Ziel:** Cap `min(maxSkillValue, höchstes Attribut+2)` greift.
- **Schritte:** 1) Attribut niedrig halten, Skill weit hochkaufen. 2) Speichern.
- **Erwartet:** Cap blockiert mit konkretem Hinweis; kein stilles Speichern über Cap.
- **Fund:**

#### CHR-04 Wizard: Save-Gate + Fehlende Pflichtangaben
- **Ziel:** Verständliche Prüfung vor Speichern (leerer Name, Budget überschritten).
- **Schritte:** 1) Budget deutlich überschreiten. 2) Speichern.
- **Erwartet:** Konkrete Liste der Probleme; nichts wird halb gespeichert.
- **Fund:**

#### CHR-05 ★ Sheet: Attribute/Derived/Skills
- **Ziel:** Korrekte Anzeige inkl. Tabelle & Formel.
- **Schritte:** 1) QA-Mira öffnen. 2) Attribute prüfen (Mods eingerechnet?). 3) Derived-Tabelle sk/lep/asp. 4) Skill-FW mit Zauberer-Trait (asp sichtbar?).
- **Erwartet:** Werte konsistent zur Wizard-Rechnung; fehlende Traits verstecken Ressourcen (Bedingung).
- **Fund:**

#### CHR-06 HP-Nachzug bei Attributänderung
- **Ziel:** F5-Fix wirkt.
- **Schritte:** 1) Konstitution erhöhen (DM/Regel-UI). 2) LEP/HP beobachten. 3) Reload.
- **Erwartet:** Max-LEP angepasst (Formel), aktueller Wert sinnvoll (nicht überraschend voll).
- **Fund:**

#### CHR-07 ★ 3W20-Probe (Basis)
- **Ziel:** Proben-Details verständlich (3 Würfe, Schwelle, Ausgleich).
- **Schritte:** 1) Skill „Überreden" würfeln. 2) Details aufklappen.
- **Erwartet:** 3 Würfe mit Attribut/Schwelle; FW-Ausgleich erklärt; Ergebnis konsistent zur Anzeige.
- **Fund:**

#### CHR-08 Probe: Difficulty (erschwert/erleichtert)
- **Ziel:** Difficulty-Grade wirken korrekt.
- **Schritte:** 1) Grad „erschwert" wählen, würfeln. 2) „erleichtert". 3) Schwellen in Details vergleichen.
- **Erwartet:** Schwellen sinken/steigen um Delta; Anzeige benennt den Grad.
- **Fund:**

#### CHR-09 Probe: Fate-Bonus (+★)
- **Ziel:** Schicksalspunkt gibt Bonus vor dem Wurf.
- **Schritte:** 1) Notiere aktuelle Fate-Punkte. 2) „+★" aktivieren (gelb) → würfeln. 3) Punktestand prüfen.
- **Erwartet:** Genau 1 Punkt weg; Modifier +1 im Ergebnis; Toggle danach aus.
- **Fund:**

#### CHR-10 Probe: Schicksalspunkt „neu würfeln" (★ nach Wurf)
- **Ziel:** Altverhalten bleibt nutzbar.
- **Schritte:** 1) Probe mit Fehlschlag provozieren. 2) ★ klicken.
- **Erwartet:** Punkt weg + neuer Wurf; bei 0 Punkten klare Meldung.
- **Fund:**

#### CHR-11 Casting: Kosten/Trait/Restore ★
- **Ziel:** Zauber wirkt, Ressource sinkt, Rest füllt nach.
- **Schritte:** 1) Zauber (asp cost 3, restore long) wirken. 2) AsP beobachten. 3) Kurz-Rest → unverändert? 4) Lang-Rest → aufgefüllt.
- **Erwartet:** Kosten exakt; ohne Trait gesperrt; Restore je Konfiguration.
- **Fund:**

#### CHR-12 Cast ohne Ressource/Trait (Fehlerbilder)
- **Ziel:** Klare Fehlermeldungen (`CAST_INSUFFICIENT_RESOURCE`, `CAST_MISSING_TRAIT`).
- **Schritte:** 1) AsP auf 0 setzen (Rest/Fate/API). 2) Zaubern. 3) Trait entfernen (API) und erneut.
- **Erwartet:** Toast im Klartext, kein leerer Screen; Punkt bleibt 0.
- **Fund:**

#### CHR-13 Bedingungen: hinzufügen/entfernen/ticken
- **Ziel:** Zustände mit Runden korrekt.
- **Schritte:** 1) „Wunde" 2 Runden setzen. 2) Probe: Malus sichtbar? 3. Im Kampf 2× Zugwechsel (Block F) → Zustand fällt weg.
- **Erwartet:** Malus auf Proben; Runden zählen nur beim Zug des Trägers; Anzeige aktualisiert.
- **Fund:**

#### CHR-14 Bedingungen: Blocks (T3) im Kampf vorbereiten
- **Ziel:** „Betäubt" sperrt Aktionen (Ausführung in FGT-09).
- **Schritte:** 1) QA-Mira „Betäubt" geben. 2) Buttons im Kampf beobachten (falls deaktiviert) / beim Klick Meldung.
- **Erwartet:** Aktion wird mit `COMBAT_ACTION_BLOCKED`-Klartext verweigert.
- **Fund:**

#### CHR-15 Inventar: Items, Equip, Mengen
- **Ziel:** Inventar-Editoren robust.
- **Schritte:** 1) Waffe + Rüstung hinzufügen. 2) Equippen. 3) Menge +/−. 4) Reload.
- **Erwartet:** Persistiert; Rüstung/RS im Sheet sichtbar; falsche Menge wird verhindert.
- **Fund:**

#### CHR-16 Inventar: Schadenstyp wirkt
- **Ziel:** Waffe mit `damage_type: fire` gegen Resistenz (Ausführung in FGT-07).
- **Schritte:** 1) Feuerwaffe equippen. 2) Für FGT-07 vormerken.
- **Erwartet:** —
- **Fund:**

#### CHR-17 Abilities: aktiv/passiv
- **Ziel:** Fähigkeiten zuweisen; passive wirken (falls Effekte), aktive im Kampf nutzbar.
- **Schritte:** 1) Aktive Fähigkeit mit Heilung zuweisen. 2) Passive prüfen (Bonus in Probe?).
- **Erwartet:** Sichtbar/ausführbar; Kosten korrekt.
- **Fund:**

#### CHR-18 Rests: kurz/lang
- **Ziel:** Heilung/Ressourcen nach Konfiguration.
- **Schritte:** 1) HP/AsP verbrauchen. 2) Kurz-Rest. 3) Lang-Rest. 4) Reload.
- **Erwartet:** Werte/Prozente exakt zur System-Config; keine Überschreitung Max.
- **Fund:**

#### CHR-19 XP & Progression/Level-Up
- **Ziel:** XP vergeben → Level/Verbesserung.
- **Schritte:** 1) XP erhöhen (Sheet/DM). 2) Level-Up-Schwelle (falls konfiguriert) erreichen. 3) Attribut/Skill verbessern (Freipunkte?).
- **Erwartet:** Schwelle korrekt (generic levels); Fortschritt sichtbar; keine negative XP.
- **Fund:**

#### CHR-20 Markt: Kauf/Verkauf (falls aktiv)
- **Ziel:** Markt-Flow inkl. Preis-Modifikator des Händlers.
- **Schritte:** 1) QA-Alrik (price_modifier) öffnen. 2) Item kaufen. 3) Inventar/Geld prüfen. 4) Verkaufen.
- **Erwartet:** Preise korrekt gerundet; Inventar/Geld konsistent; Fehlermeldung bei zu wenig Geld.
- **Fund:**

#### CHR-21 Handel: zwei Spieler (Trades V104)
- **Ziel:** Sicherer Spieler-zu-Spieler-Handel.
- **Schritte:** 1) Spieler1 öffnet Trade mit Spieler2. 2) Beide legen Items, bestätigen. 3) Inventare prüfen. 4) Reload.
- **Erwartet:** Atomarer Tausch; Abbruch lässt alles unverändert; fremder Trade nicht sichtbar.
- **Fund:**

#### CHR-22 Trade: Race/Locking
- **Ziel:** Doppelhandel schützt vor Duplikation.
- **Schritte:** 1) Beide starten gleichzeitig Trade mit demselben Item. 2) Schnell bestätigen.
- **Erwartet:** Nur ein Tausch gewinnt; keine Item-Verdopplung (ich prüfe DB).
- **Fund:**

#### CHR-23 Social Probe vom NPC-Panel (T7)
- **Ziel:** Soziale Aktion wirkt mit Beziehungs-Score.
- **Schritte:** 1) Beziehung QA-Mira→QA-Alrik „freundlich" setzen (DM/API). 2) NPC-Seite: Panel „Soziale Probe", PC + Aktion wählen, würfeln. 3) NPC-Zustand prüfen (Beeindruckt/Verärgert).
- **Erwartet:** Modifier enthält +2; Erfolg/Fehlschlag setzt Zustand am NPC mit Runden.
- **Fund:**

#### CHR-24 Social: falscher Skill / fremdes Ziel (API)
- **Ziel:** Server lehnt inkonsistente Requests ab (Audit-Fix).
- **Schritte:** 1) Probe mit `socialAction` aber falschem `skillName` (curl). 2) Ziel aus fremder Welt.
- **Erwartet:** `SOCIAL_SKILL_MISMATCH` / `SOCIAL_TARGET_INVALID`, keine Zustandsänderung, kein Fate-Verbrauch.
- **Fund:**

#### CHR-25 Proben-Log & Chat-Kopplung
- **Ziel:** Probe erscheint im Chat/Log (je Einstellung).
- **Schritte:** 1) Probe würfeln. 2) Welt-Chat prüfen.
- **Erwartet:** Ergebnis mit Kontext (Skill, Erfolg/Misserfolg); keine Doppelposts.
- **Fund:**

#### CHR-26 Sheet für Fremd-PC
- **Ziel:** Lesen ja/nein je Rolle konsistent.
- **Schritte:** 1) Spieler2 öffnet QA-Mira-Sheet. 2) Würfelt Probe für QA-Mira.
- **Erwartet:** Entweder Read-only oder denied — konsistent, verständlich; Probe wird verweigert.
- **Fund:**

#### CHR-27 Ressourcen-Anzeige bei Trait-Verlust/Gewinn
- **Ziel:** requiresTrait versteckt/zeigt Ressourcen dynamisch.
- **Schritte:** 1) Trait „Zauberer" entfernen (API) → Sheet. 2) Wieder hinzufügen.
- **Erwartet:** asp-Anzeige verschwindet/erscheint ohne Reload-Fehler.
- **Fund:**

#### CHR-28 Attribut-Grenzen im Sheet (Run/Edit)
- **Ziel:** Direkte Attribut-Änderung respektiert min/max.
- **Schritte:** 1) Attribut über max setzen versuchen (UI/API).
- **Erwartet:** Validierungsfehler, kein Overflow.
- **Fund:**

#### CHR-29 Ideen-Sweep Charaktere
- **Fund:**

#### CHR-30 Sheet-Performance (viele Skills)
- **Ziel:** 50+ Skills handhabbar.
- **Schritte:** 1) System mit vielen Skills importieren (ich kann seeden). 2) Sheet öffnen, Probe würfeln.
- **Erwartet:** Flüssig, Suche/Filter falls vorhanden.
- **Fund:**

---

## Block F — Kampf

> Setup: QA-Mira (Spieler1) vs. QA-Räuber (DM). DnD/CoC-Varianten in Block XS.

#### FGT-01 ★ Kampf starten
- **Ziel:** Teilnehmer, Initiative, AP korrekt initialisiert.
- **Schritte:** 1) DM startet Kampf mit beiden. 2) Reihenfolge + Werte prüfen.
- **Erwartet:** Initiative absteigend; AP voll; erster Zug markiert; Chat/Log (je Toggle) informiert.
- **Fund:**

#### FGT-02 Zugwechsel & Runden
- **Ziel:** Reihenfolge, Runden, AP-Refresh.
- **Schritte:** 1) Zug beenden bis Runde 2. 2) AP/aktiver Kämpfer beobachten. 3) Bedingungen ticken (Wunde 2R).
- **Erwartet:** Nur aktueller Kämpfer handelt; AP-Refresh beim Zugbeginn; Rundenanzeige +1; Bedingungen ticken beim Träger.
- **Fund:**

#### FGT-03 Angriff DSA: Treffer
- **Ziel:** `value: at` vs `pa`, Vergleich `lte`.
- **Schritte:** 1) Angreifen (Angriff-Button). 2) Ergebnis/Log.
- **Erwartet:** Wurf ≤ at sichtbar; bei Treffer Schaden; AP des Angreifers sinkt wie konfiguriert.
- **Fund:**

#### FGT-04 Angriff: MISS verbraucht AP (P1)
- **Ziel:** Fehlschlag ohne Schaden, AP weg.
- **Schritte:** 1) Ziel mit hohem pa/schwerer Probe provozieren; MISS erzielen. 2) AP/HP prüfen.
- **Erwartet:** „MISS"-Feedback (Toast/Log), HP unverändert, AP reduziert.
- **Fund:**

#### FGT-05 Manöver mit attackMalus (T2)
- **Ziel:** Wuchtschlag erschwert Treffer, Bonus-Schaden wenn trifft.
- **Schritte:** 1) Wuchtschlag ausführen (mehrfach). 2) Treffer/Fehlschlag + Schaden vergleichen; AP prüfen.
- **Erwartet:** Fehlschläge anteilig hoch (Malus), bei Treffer +3 Schaden; AP-Kosten stimmen.
- **Fund:**

#### FGT-06 Manöver ohne Attack-Config (Fallback)
- **Ziel:** Systeme ohne Attack-Gate: Manöver wirken direkt.
- **Schritte:** 1) In QA-Schmiede (DnD ohne Konfig? prüfen) Manöver testen — falls keins, überspringen.
- **Erwartet:** Direkter Schaden wie vor T2; kein Regressionsbruch.
- **Fund:**

#### FGT-07 Schadenstyp & Resistenz
- **Ziel:** Feuer-Waffe gegen Feuer-Elementar (Resistenz halbiert).
- **Schritte:** 1) NPC mit `damage_resistances:["fire"]` (ich seede). 2) Mit Feuerwaffe angreifen. 3) Mit physischer Waffe.
- **Erwartet:** Feuer-Schaden halbiert (Log nennt Typ), physisch voll; Anzeige konsistent.
- **Fund:**

#### FGT-08 Abilities im Kampf
- **Ziel:** Aktive Fähigkeiten mit Kosten/Heilung.
- **Schritte:** 1) Heilfähigkeit auf verletzten Verbündeten. 2) Schadensfähigkeit auf Gegner. 3) AP/Ressourcen prüfen.
- **Erwartet:** Effekte korrekt, Kosten exakt, Ziel-Guards (kein Heilen von Besiegten-Ausnahme vs. kein Schaden an Besiegten).
- **Fund:**

#### FGT-09 Condition-Sperren (T3) ★
- **Ziel:** „Betäubt" blockiert Aktionen.
- **Schritte:** 1) QA-Mira „Betäubt" geben (Runden). 2) Angriff/Manöver/Bewegen/Verteidigen/Fähigkeit probieren.
- **Erwartet:** Klare Block-Meldung (`COMBAT_ACTION_BLOCKED`), keine AP/Effekte; Blutrausch sperrt nur Verteidigung (Content prüfen).
- **Fund:**

#### FGT-10 Fate: Tod abwenden (T4)
- **Ziel:** Ziel mit Schicksalspunkt überlebt tödlichen Treffer mit 1 HP.
- **Schritte:** 1) QA-Torben auf 2 HP bringen, `fate_points` auf 1. 2) Tödlichen Treffer landen. 3) HP/Punkte/Log prüfen.
- **Erwartet:** HP=1, Punkt weg, Log „wendet Tod ab"; ohne Punkt: besiegt.
- **Fund:**

#### FGT-11 Besiegt & Ende
- **Ziel:** Defeat-Zustand; Kampf beenden schreibt HP/AP zurück.
- **Schritte:** 1) QA-Räuber besiegen. 2) Kampf beenden. 3) NPC-Sheet/Entity prüfen.
- **Erwartet:** Kein Angriff auf Besiegte; Ende setzt Status; Werte persistent; Reload zeigt kein aktives Kampf-Panel.
- **Fund:**

#### FGT-12 Aktiver Kampf: Reload/Rehydrate
- **Ziel:** Seite neu laden mitten im Kampf.
- **Schritte:** 1) Nach 2 Zügen F5. 2) Panel/Turn-Anzeige vergleichen.
- **Erwartet:** Kampf wird wieder aufgenommen (`/combat/active`), aktuelle Runde/Actor korrekt.
- **Fund:**

#### FGT-13 Rechte im Kampf
- **Ziel:** Nur Kontrolleur/DM handelt.
- **Schritte:** 1) Spieler2 versucht QA-Mira-Zug (Button/API). 2) Fremder versucht Zug.
- **Erwartet:** Verweigert (`COMBAT_NOT_YOUR_TURN`/403), keine AP-Änderung.
- **Fund:**

#### FGT-14 Range/LoS-Fehlerbilder
- **Ziel:** Verständliche Meldungen.
- **Schritte:** 1) Fernkampf ohne Reichweite (falls Waffe konfiguriert). 2) Blockierte Sichtlinie (Fog/Wand) provozieren.
- **Erwartet:** Klartext-Fehler, kein Effekt; im Log nachvollziehbar.
- **Fund:**

#### FGT-15 Kampf-Chat-Toggle (WLT-02)
- **Ziel:** Log-Schalter wirkt sofort.
- **Schritte:** 1) Toggle aus → Aktion. 2) Toggle an → Aktion.
- **Erwartet:** Aus: keine Chat-Posts; An: mit Text.
- **Fund:**

#### FGT-16 Kampf mit 3+ Teilnehmern (WS)
- **Ziel:** Live-Aktualisierung aller Clients.
- **Schritte:** 1) Kampf mit QA-Mira, QA-Torben, QA-Räuber. 2) In beiden Browsern beobachten.
- **Erwartet:** Zug-Änderungen/HP live; keine verlorenen Events; Reihenfolge stabil nach Reload.
- **Fund:**

#### FGT-17 Gleichzeitige Aktionen (Race)
- **Ziel:** Doppelaktionen werden verhindert.
- **Schritte:** 1) Zwei schnelle Angriffe von Spieler1+Dummy gleichzeitig.
- **Erwartet:** Nur ein gültiger Zug; konsistente Endwerte.
- **Fund:**

#### FGT-18 Kampf-Neustart nach Ende
- **Ziel:** Zweiter Kampf funktioniert.
- **Schritte:** 1) Neuen Kampf starten. 2) HP/AP Startwerte prüfen.
- **Erwartet:** Frische Initiative; Werte aus Sheet; keine Reste vom Altkampf.
- **Fund:**

#### FGT-19 Ideen-Sweep Kampf
- **Fund:**

#### FGT-20 Kampf-Performance (6 Teilnehmer)
- **Ziel:** Flüssig bei mehr Teilnehmern (ich seede 4 weitere NPCs).
- **Fund:**

---

## Block G — Abenteuer & Quests

#### AQ-01 ★ Abenteuer anlegen (DM)
- **Ziel:** Editor vollständig.
- **Schritte:** 1) Abenteuer „QA-Höhle" am Ort mit Giver. 2) Start-Node, 2 Nodes, 2 Choices (eine mit Skillcheck onSuccess/onFailure), End-Node.
- **Erwartet:** Speichern ohne Fehler; Graph/Übersicht korrekt.
- **Fund:**

#### AQ-02 Abenteuer starten (Spieler)
- **Ziel:** Start nur für eigenen Charakter; Start-Node korrekt.
- **Schritte:** 1) QA-Mira startet. 2) Zweiter Start desselben Chars.
- **Erwartet:** Ein Fortschritt (Resume statt Duplikat); Start-Node sichtbar.
- **Fund:**

#### AQ-03 Abenteuer spielen: Erfolgspfad (3W20 via ProbeService, T1)
- **Ziel:** Skillcheck routet korrekt (3W20-Details!) und zweigt.
- **Schritte:** 1) Choice mit Skillcheck wählen, erfolgreich. 2) Details/Verlauf.
- **Erwartet:** 3W20-Probe (Status „gewürfelt"), Erfolgs-Node; Event/Verlauf nachvollziehbar.
- **Fund:**

#### AQ-04 Abenteuer: Fehlschlagspfad
- **Ziel:** onFailure-Zweig greift.
- **Schritte:** 1) Erschwernis/Malus provozieren (z. B. Modifier hoch). 2) Fehlschlagen.
- **Erwartet:** Fehl-Node; kein Hänger; Choice bleibt konsistent.
- **Fund:**

#### AQ-05 Abenteuer: Abschluss + Fortschritt
- **Ziel:** End-Node schließt ab; Fortschritt pro Charakter.
- **Schritte:** 1) Bis End-Node spielen. 2) Erneut starten. 3) QA-Torben startet parallel.
- **Erwartet:** Status COMPLETED; Neustart-Verhalten klar (Resume/neu?); Torben eigener Fortschritt.
- **Fund:**

#### AQ-06 Abenteuer: Manipulierte Choice (F2)
- **Ziel:** Fremde/ungültige Choice wird abgewiesen.
- **Schritte:** 1) API-Request mit Choice eines anderen Nodes/Abenteuers (curl).
- **Erwartet:** `ADVENTURE_CHOICE_INVALID`, kein Sprung, kein State-Change.
- **Fund:**

#### AQ-07 Live-Inject & Force-Node (DM) — Regression T33-09
- **Ziel:** DM kann Choice live einspielen; Force/Override funktionieren.
- **Schritte:** 1) Im laufenden Abenteuer Choice injizieren. 2) Spieler sieht sie (Reload/WS?). 3) Force-Node, Text-Override.
- **Erwartet:** DM-Tools nur DM; Spieler-Ansicht aktualisiert; injizierte Choice spielbar.
- **Fund:**

#### AQ-08 Abenteuer: Abbruch/Resume (falls vorhanden)
- **Ziel:** Abbrechen und später fortsetzen.
- **Fund:**

#### AQ-09 ★ Quest anlegen & Statuszyklus
- **Ziel:** pending→active→completed/cancelled inkl. Validierung.
- **Schritte:** 1) Quest mit Objectives (Kill-Ziel auf QA-Räuber, Ort). 2) Status durchschalten. 3) Ungültigen Status per API.
- **Erwartet:** Whitelist greift (`QUEST_STATUS_INVALID`); UI zeigt Labels lokalisiert; Filter stimmen.
- **Fund:**

#### AQ-10 Quest: Objectives & Abschluss-Erkennung
- **Ziel:** Ziel-Fortschritt (Kill/Ort besuchen) wird erkannt/aktualisiert.
- **Schritte:** 1) Räuber im Kampf besiegen. 2) Quest-Status/Objectives prüfen.
- **Erwartet:** Ziel als erledigt (falls Auto-Erkennung) oder DM-Abschluss möglich; keine Geister-Ziele.
- **Fund:**

#### AQ-11 Quest: Rewards & Giver/Location-Filter
- **Ziel:** Belohnung sichtbar/auszahlbar (falls Mechanik) und Listenfilter korrekt.
- **Schritte:** 1) Quest bei NPC öffnen. 2) Ort/NPC-Ansicht: Quests des Givers/Ortes.
- **Erwartet:** Nur passende Quests; Belohnung korrekt angezeigt.
- **Fund:**

#### AQ-12 Quest: DM-Gates
- **Ziel:** Spieler können keine Quests anlegen/Status ändern (KRG-03).
- **Erwartet:** 403/DM_REQUIRED.
- **Fund:**

#### AQ-13 Fork: Abenteuer/Quests spielbar (KRG-09/10 Kreu zverweis)
- **Ziel:** Im Fork weiterführen ohne dangling IDs.
- **Fund:**

#### AQ-14 Ideen-Sweep AQ
- **Fund:**

---

## Block H — Sozial (Intents/Beziehungen), Chat, Handel

#### SOC-01 Relationships setzen/lesen
- **Ziel:** Beziehungspflege + Anzeige.
- **Schritte:** 1) Beziehung QA-Mira→QA-Alrik ändern (API/UI). 2) NPC-View-Badges prüfen (Score-Chip).
- **Erwartet:** Wert + Score-Badge konsistent; Reload bleibt.
- **Fund:**

#### SOC-02 NPC-View: Stammdaten
- **Ziel:** Alle NPC-Felder editierbar (Persönlichkeit, Wissen, Ziele, Services, Begrüßung, Preis-Mod, Beruf).
- **Schritte:** 1) Alle Felder ändern. 2) Reload. 3) Markt-Preiswirkung (CHR-20) prüfen.
- **Erwartet:** Persistiert; Services-Empty-State ok; i18n sauber.
- **Fund:**

#### SOC-03 NPC-Intents: Erzeugung (Bot/LLM oder API)
- **Ziel:** Intents landen in DM-Queue.
- **Schritte:** 1) NPC-Aktion auslösen (Discord-Bot-Nachricht falls vorhanden, sonst API-Trigger). 2) DM-Queue öffnen (Spieler1 als DM? Meister).
- **Erwartet:** Panel erscheint (ggf. per WS ohne Reload); Intent-Typ/Params lesbar.
- **Fund:**

#### SOC-04 DM-Queue: Approve/Reject
- **Ziel:** Einzelentscheidungen wirken (SPEAK an Chat, MOVE, CHANGE_RELATION, ITEM).
- **Schritte:** 1) Je einen Intent jeder Kategorie approven/rejecten. 2) Effekte beobachten.
- **Erwartet:** Approve führt Aktion aus (Chat/Position/Beziehung); Reject ohne Seiteneffekt; Queue aktualisiert.
- **Fund:**

#### SOC-05 DM-Queue: Bulk (Teilerfolg, P34-T03)
- **Ziel:** Bulk mit einem ungültigen Eintrag.
- **Schritte:** 1) Mehrere Intents (einer bereits entschieden). 2) Bulk-Approve.
- **Erwartet:** Gültige approved, ungültiger übersprungen + Ergebnisliste; kein Rollback aller.
- **Fund:**

#### SOC-06 CHANGE_RELATION Wirkung
- **Ziel:** Genehmigter Intent setzt Beziehung (und nur diese Richtung/Paarung).
- **Schritte:** 1) Intent approven. 2) Beziehungsanzeige + Sozial-Probe-Modifier prüfen.
- **Erwartet:** Score wirkt in CHR-23-Kette.
- **Fund:**

#### SOC-07 Chat: Senden/Historie/Ladezeit
- **Ziel:** Nachrichten persistent, Verlauf paginiert/scrollbar.
- **Schritte:** 1) 30 Nachrichten senden (ich kann seeden). 2) Reload. 3) Scroll nach oben (ältere laden?).
- **Erwartet:** Reihenfolge/Timestamps korrekt; kein Duplikat; Performance ok.
- **Fund:**

#### SOC-08 Chat: /r Echo-Dedupe (Runde 2)
- **Ziel:** Roll-Echo erscheint genau einmal.
- **Schritte:** 1) Mehrfach Probe würfeln. 2) Chat beobachten.
- **Erwartet:** Kein Doppelpost; Fehlerfälle (Probe verweigert) ebenfalls korrekt.
- **Fund:**

#### SOC-09 Chat: Rechte & leere Nachricht
- **Ziel:** Fremder kann nicht schreiben/lesen; leere Nachricht wird verhindert.
- **Schritte:** 1) Fremd-Account → WS/REST. 2) Leere Nachricht + nur Leerzeichen.
- **Erwartet:** 403/nichts; keine Broadcasts.
- **Fund:**

#### SOC-10 Chat: WS-Reconnect
- **Ziel:** Nach Backend-Neustart oder Netz-Wackler reconnected der Client.
- **Schritte:** 1) `pm2 restart lwe-backend`. 2) Nachricht senden/empfangen.
- **Erwartet:** Nach kurzer Zeit wieder live (kein Dauer-Offline ohne Hinweis).
- **Fund:**

#### SOC-11 Handel: UI-Feinheiten (Runde 3/4)
- **Ziel:** TradeModal-Politur.
- **Schritte:** 1) Zwei Spieler, Item-Auswahl, Mengen, Max-Schnellwahl, Summenzeile. 2) Keyboard: Tab/Enter/Escape. 3) Fehlerfall (Item wegnehmen während Trade).
- **Erwartet:** Fokus gefangen, Escape schließt, Toasts verständlich, Zahlen konsistent.
- **Fund:**

#### SOC-12 Intents: unbekannter Typ/Parameter
- **Ziel:** Robustheit.
- **Schritte:** 1) Intent mit kaputtem JSON/Parametern (API). 2) Approve.
- **Erwartet:** Klarer Fehler, kein 500, Queue bleibt bedienbar.
- **Fund:**

#### SOC-13 Discord-Bot End-to-End (falls Server vorhanden)
- **Ziel:** Bot erzeugt Intent aus Chat.
- **Schritte:** 1) Im Discord-Kanal NPC ansprechen. 2) Antwort + DM-Queue prüfen.
- **Erwartet:** Antwort im Kanal; Intent erscheint; Freigabe wirkt.
- **Fund:**

#### SOC-14 Ideen-Sweep Sozial/Chat
- **Fund:**

---

## Block I — Querschnitt: i18n, UX, Performance, Robustheit

#### QX-01 i18n-Stichprobe (de→en)
- **Ziel:** Kernflows konsistent übersetzt.
- **Schritte:** 1) Sprache en. 2) Dashboard→Welt→Sheet→Kampf→Adventure→NPC→Chat durchklicken; sichtbare deutsche Reste/Platzhalter notieren.
- **Erwartet:** Wenige bekannte Lücken (GM-Tool); alles andere englisch.
- **Fund:**

#### QX-02 i18n-Stichproben fr/es/it/tr
- **Schritte:** Je Sprache 2 Screens (Sheet + Campaign).
- **Erwartet:** Keine abgeschnittenen Texte; keine rohen Keys.
- **Fund:**

#### QX-03 Fehlermeldungs-Qualität (Sweep)
- **Ziel:** Fehlercodes vs. Klartext.
- **Schritte:** Aus KRG-05/FGT-09/CHR-12 sammeln: Welche Meldung war unklar? Liste.
- **Erwartet:** Verbesserungsliste.
- **Fund:**

#### QX-04 Mobile (schmaler Viewport)
- **Ziel:** Kernflows auf Handy-Breite.
- **Schritte:** 1) DevTools 375px. 2) Login→Welt→GameView→Sheet→Kampf→Chat.
- **Erwartet:** Drawer/Tabs funktionieren; nichts überlappt; Touch-Ziele ≥ ~40px.
- **Fund:**

#### QX-05 Tastatur/Fokus-Sweep
- **Ziel:** Dialoge/Schließen/Fokus.
- **Schritte:** 1) Nur Tastatur: Dialoge (Trade, Wizard, Delete-Confirm). 2) Escape/Fokus-Rückgabe.
- **Erwartet:** Fokus gefangen, sinnvolle Reihenfolge; keine Fokus-Falle ohne Ausweg. (Offener P2-Punkt: Fokus-Trap in Dialogen!)
- **Fund:**

#### QX-06 Kontrast/Kleintext
- **Ziel:** Lesbarkeit (10px-Texte, Sekundärfarben).
- **Schritte:** Sheet/Kampf/Editor auf Lesbarkeit prüfen (Zoom 100 %).
- **Fund:**

#### QX-07 Doppelklick/Spam
- **Ziel:** Buttons idempotent.
- **Schritte:** 1) Speichern/Probe/Start 5× schnell klicken.
- **Erwartet:** Keine Duplikate (2. Welt, 2. Probe, 2. Start).
- **Fund:**

#### QX-08 Backend-Neustart mitten im Flow
- **Ziel:** Freundliche Degradierung.
- **Schritte:** 1) Sheet offen, `pm2 restart lwe-backend`. 2) Aktion ausführen (Probe) → Meldung? 3) Nach Reconnect erneut.
- **Erwartet:** Klarer Fehler statt Endlosspinner; danach normal.
- **Fund:**

#### QX-09 Große Datenmengen (Performance-Gefühl)
- **Schritte:** 1) Welt mit 100 Entities (ich seede). 2) Liste, Karte, Chat (500 Messages) öffnen.
- **Erwartet:** Interaktiv < ~2s; Scroll flüssig; kein Layout-Sprung.
- **Fund:**

#### QX-10 Konsolen-/Netzwerk-Sweep
- **Ziel:** Keine roten Fehler im Normalbetrieb.
- **Schritte:** 1) DevTools Console/Network während 10-Minuten-Tour offen lassen.
- **Erwartet:** Keine unerwarteten Errors/400er/500er (erwartete 403-Fälle dokumentieren).
- **Fund:**

#### QX-11 Uhrzeiten/Zeitzonen (Chat/Log/Events)
- **Ziel:** Zeitstempel plausibel (lokal).
- **Schritte:** Chat + Kampf-Log + Timeline vergleichen.
- **Erwartet:** Konsistent, keine „Invalid Date".
- **Fund:**

#### QX-12 Ideen-Sweep UX (Top-5-Wünsche)
- **Ziel:** Deine Top-5-Verbesserungen aus allen Blöcken sammeln.
- **Fund:**

---

## Block XS — Cross-System DnD5e / CoC7e (Generik-Beweis)

> Daten: QA-Schmiede (dnd5e), QA-Arkham (coc7e) aus 1.2.

#### XS-01 DnD: Charakter erstellen & Sheet
- **Ziel:** Budget, Skills, Slots, AC-Anzeige.
- **Schritte:** 1) PC im DnD-System bauen (falls Wizard-Daten vorhanden). 2) Sheet: AC/HP/Slots.
- **Erwartet:** Systemdaten treiben Anzeige; keine DSA-Reste (AsP etc.).
- **Fund:**

#### XS-02 DnD: Angriff gte gegen AC
- **Schritte:** 1) Kampf PC vs Ork. 2) Angriff mehrfach; AC-Variation (ich ändere AC).
- **Erwartet:** Treffer nur bei Wurf+Mod ≥ AC; MISS-Verhalten wie DSA (AP/Log).
- **Fund:**

#### XS-03 DnD: Probe & Slots-Cast
- **Schritte:** 1) Skillprobe d20. 2) Zauber mit slot_1 casten + Rest.
- **Erwartet:** Slot sinkt, Restore long füllt; Trait-Gate falls definiert.
- **Fund:**

#### XS-04 DnD: Damage-Type/Resistenz falls Daten
- **Fund:**

#### XS-05 CoC: Sheet & Probe d100
- **Schritte:** 1) Investigator: Fertigkeiten-Werte. 2) Probe d100 mit regular/hard/extreme-Grad.
- **Erwartet:** Schwellen multipliziert (hard 0.5 etc.); Erfolg/Misserfolg korrekt; Details lesbar.
- **Fund:**

#### XS-06 CoC: Bonus-/Penalty-Würfel
- **Schritte:** 1) Probe mit 1 Bonuswürfel. 2) Mit 2 Penalty. 3) Netto-Fall (1/1).
- **Erwartet:** Zehnerwürfe sichtbar; 00+0=100-Regel korrekt; Netto heben sich auf.
- **Fund:**

#### XS-07 CoC: Angriff skill-basiert (`Kampf (Raufen)`, lte)
- **Schritte:** 1) Kampf Investigator vs Kultist. 2) Mehrfach angreifen; Skill-Wert ändern.
- **Erwartet:** Treffer nur bei d100 ≤ Skill; Miss = kein Schaden.
- **Fund:**

#### XS-08 CoC: Sanity/MP (falls konfiguriert)
- **Fund:**

#### XS-09 Systemwechsel-Sichtbarkeit
- **Ziel:** Keine UI-Elemente aus Fremdsystemen.
- **Schritte:** DnD-Sheet vs. CoC-Sheet vs. DSA-Sheet vergleichen.
- **Erwartet:** Ressourcen/Begriffe systemgerecht; keine „asp" im DnD, keine Slots im DSA.
- **Fund:**

#### XS-10 Ideen-Sweep Cross-System
- **Fund:**

---

## Block API — Konsistenz & Security-Stichproben

> Ich bereite die curl-Befehle pro Fall vor; Du siehst Ergebnis + Meldung.

#### API-01 Roll-/Probe-/Cast-Fehlerkontrakt
- **Ziel:** ERROR-CODES.md stimmt mit API überein.
- **Fälle:** unbekannte Entity, fremde Welt, Cast ohne Ressource, Social-Fehler, Block-Aktion.
- **Erwartet:** Code + HTTP wie dokumentiert; kein 500.
- **Fund:**

#### API-02 Trade-Kontrakt
- **Fälle:** Doppel-Trade, fremder Participant, gelöschtes Item.
- **Erwartet:** TRADE_*-Codes; keine Teiländerung.
- **Fund:**

#### API-03 Fork-Kontrakt
- **Fälle:** Fork fremder Welt (403), Fork als Nicht-Owner.
- **Fund:**

#### API-04 Uploads/Bilder (Node-Bilder)
- **Ziel:** Bild-Upload/Anzeige robust.
- **Schritte:** 1) Node-Bild hochladen (groß>5MB, falscher Typ). 2) Anzeige.
- **Erwartet:** Limit-/Typfehler klar; gültige Bilder überall sichtbar (auch Fork).
- **Fund:**

#### API-05 IDs/Timestamps
- **Fälle:** UUIDs stabil; Zeitfelder ISO.
- **Fund:**

#### API-06 Pagination/Filter
- **Schritte:** Chat-Historie, Entity-Listen mit >100 Einträgen.
- **Erwartet:** Seitengrößen konsistent, keine Duplikate/Lücken.
- **Fund:**

#### API-07 Konkurrenz: gleiches Sheet gleichzeitig edieren
- **Schritte:** 1) Spieler1 + DM ändern QA-Mira-Attribute gleichzeitig.
- **Erwartet:** Definierter Gewinner (Last-Write) ohne Datenkorruption.
- **Fund:**

---

## Anhang A — Findings-Log

| ID | Block | Fall | Schwere | Beschreibung | Repro | Status | Fix-Commit |
|----|-------|------|---------|--------------|-------|--------|------------|
| QA-001 | | | | | | offen | |

**Triage-Regeln:**
- BLOCKER/HIGH → sofort fixen (mit Test), dann weiter.
- MEDIUM → sammeln, nach Block entscheiden (Fix oder Backlog).
- LOW/IDEE → Backlog Phase 39, am Ende priorisieren.

## Anhang B — Schnell-Kommandos

```bash
# Logs
pm2 logs lwe-backend --lines 80 --nostream
pm2 logs lwe-frontend --lines 20 --nostream

# Backend-Neustart (nach Fixes)
export JAVA_HOME=$HOME/.local/share/jdk21; export PATH=$JAVA_HOME/bin:$PATH
cd backend && mvn -q -DskipTests package && pm2 restart lwe-backend

# Regression
cd backend && mvn test
cd frontend && npx vitest run && E2E_EMAIL=devbe@test.de E2E_PASSWORD='Test123!' npx playwright test

# Aufräumen (E2E-Artefakte, Testsysteme)
E2E_EMAIL=devbe@test.de E2E_PASSWORD='Test123!' ./scripts/e2e-cleanup.sh --dry-run
```

## Anhang C — Abnahmekriterien

- [ ] Alle ★-Fälle bestanden (kritischer Pfad DSA).
- [ ] Kein offener BLOCKER/HIGH ohne Ticket.
- [ ] Fundliste in Phase 39 überführt + priorisiert.
- [ ] Backend/Frontend/E2E-Suiten nach der Testphase grün.
- [ ] Doku (USER-GUIDE/RULES-SCHEMA/API/ERROR-CODES) an gefundene Abweichungen angepasst.
