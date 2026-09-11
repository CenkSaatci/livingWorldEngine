# ADR-012: Generischer System-Wizard — Engine statt Inhalt

- **Status:** Akzeptiert (2026-09-11)
- **Entscheidung:** Der System-Wizard modelliert **generische Mechanik-Bausteine** (Verben, Währungen, Zwänge), keinen DSA-Klon. Referenz: DSA-Heldenerschaffung + Grundregeln-Liste (Regelwiki, abgerufen 2026-09-11). Umsetzung in zwei Phasen: **P28 Engine-Bausteine**, **P29 Spielgefühl + Pakete**.

---

## 1. Kontext

Ziel: Unterschiedliche Systeme (D&D, DSA, CoC, …) sollen sich mit dem Wizard nachbilden lassen, ohne dass das Tool zum aufgeblähten „alles geht ein bisschen" wird. DSA dient als **Komplexitäts-Referenz**: Was DSA braucht, braucht fast jedes crunchy System — aber als generische Bausteine, nicht als DSA-Code.

Ausgewertete DSA-Quellen: Heldenerschaffung Übersicht + Schritte 2 (Erfahrungsgrad), 3 (Spezies), 5 (Eigenschaftspunkte), 7 (Vor-/Nachteile), 8 (Steigerungen), 12a (Basiswerte), Fertigkeitsproben (3W20), Grundregeln-Index.

Stand Wizard (verifiziert 2026-09-11): Attribute (min/max/default), Skills (Attribute + Bonus), Derived als reine Formeln, Level/XP/Improvement-Progression, Combat (Initiative/Damage/AP), Abilities, Conditionals, Probe-Typen (`d20_target`, `d100_threshold`, `d20_3attr` — letzterer korrekt mit FW-Ausgleich implementiert).

## 2. Entscheidung: Leitprinzip „Engine, nicht Inhalt"

> **Mechanik kommt in die Engine. Inhalt kommt in JSON-Beispieldateien.**

Die Grundregeln-Liste (~70 Einträge) zerfällt damit objektiv: Bestiarium, Herbarium, Gifte, Liturgien, Rüstkammer, Sphären, Vertrautentiere, Preise, Kosmologie u. v. m. sind **Content** und werden `docs/examples/*.json` + Templates — niemals Engine-Code. Übrig bleiben Bausteine, die **jede Session** betreffen oder **generische Muster** sind.

### Phase P28 — Engine-Bausteine (T01–T06)
1. **Schema/Wire öffnen:** neue Top-Level-Keys (abwärtskompatibel), `toRulesJson`/`fromRulesJson`, Backend-Konsumenten — Voraussetzung für alles Folgende.
2. **AP-Budget + Caps + Attribut-Kostenkurven** (`creationBudget`: AP-Topf, Maxima pro Kategorie, Startwerte; Kostenstaffel pro Attribut wie DSA 15/30/45/…; Wizard-Step mit Live-Kosten; Backend-Validierung).
3. **Traits-Katalog** (Vor-/Nachteile: feste/gestaffelte Kosten wie Glück I–III, Prerequisites, Exklusionen wie „nicht: Nachtblind", Effekt-Hooks für Basiswerte; 80-AP-Cap als konfigurierbare Schranke).
4. **Steigerung** (Kosten-Spalten A–D, Kostenmatrix pro Stufe, Aktivierungskosten, Max-Regel höchstes Attribut +2; Backend-Enforcement, Sheet-Kostenvorschau).
5. **Derived deluxe** (Tabellen-Lookup wie SK/ZK-Summe→Wert, `requiresTrait`-Bedingungen wie AsP-nur-mit-Zauberer, Spezies-Basis als Variable; FormulaEvaluator-Erweiterung per TDD).
6. **Abnahme:** Beispiel-System nutzt alle Bausteine, Doku.

### Phase P29 — Spielgefühl + Pakete (T01–T06)
1. **Zustände/Status-Engine** (Conditions mit mechanischen Effekten — generisch für DSA- wie D&D-Zustände).
2. **Schicksalspunkte** (Meta-Währung: neu würfeln, +1, Tod abwenden; inkl. Refresh-Regel).
3. **Kampfmanöver-Framework** (Angriffsmalus gegen Effekt tauschen — Wuchtschlag/Finte-Prinzip; baut auf Kampfsonderfertigkeiten-Idee).
4. **Rüstung + Schadenstypen** (Rüstungswerte, Resistenzen/Vulnerabilitäten; verzahnt mit P23-`damageType`).
5. **Pakete** (Spezies/Kultur/Profession: Kosten, Attribut-Mods mit Choice-Gruppen wie „MU *oder* KK −1", Basiswerte, Auto-Traits, Kultur-Restriktionen).
6. **DSA-Referenzcontent + Abnahme** (`dsa5.json` auf neues Format heben, E2E-Heldenbau nach DSA-Regeln: 100-AP-Paket, Probe mit FW-Ausgleich, Basiswerte).

### Bewusst vertagt (mit Begründung)
- **Patzer-/Krit-Tabellen:** Deluxe über Basis-Crit (später).
- **Heilung/Wunden-Ausbau:** baut auf Resting auf (später).
- **Tragkraft, Sozialer Stand:** geht heute schon (Formel bzw. Feld).
- **Sichtstörungen:** Fog existiert (später als Modifikator-Framework).
- **Optionale Regeln:** prinzipiell später (kein Mechanik-Blocker).
- **Content-Kataloge:** nie Engine (s. Leitprinzip).

## 3. Konsequenzen

### Positiv
- Jedes Feature ist entweder **jede Session spürbar** oder **Voraussetzung** dafür — kein „ein bisschen alles".
- DSA-, D&D- und CoC-artige Systeme werden aus denselben Bausteinen gebaut (kein System-Sondercode).
- Schema bleibt abwärtskompatibel; alte Systeme validieren weiter.

### Negativ / Aufwand
- Schema-Erweiterung + alle Backend-Konsumenten (RulesLoader, ProbeService, Sheet, LevelUp, Validator) + Wizard-UI + Beispiel-Content: ~10–12 Tage über zwei Phasen.
- FormulaEvaluator wächst (Tabellen, Bedingungen) — braucht disziplinierte TDD-Tests.
- P23 (`damageType`) muss vor R4 fertig sein (Abhängigkeit dokumentieren).

## 4. Alternativen (verworfen)
- **Alles aus der Grundregeln-Liste aufnehmen:** führt zum befürchteten Bloated-Tool („alles ein bisschen, System wirkt langweilig") — verworfen per Leitprinzip.
- **DSA-spezifische Implementierung** (Spezies-/Zauber-Hardcode): schneller für genau ein System, tötet aber die Generizität — verworfen.
- **Nur Content, keine Engine-Blöcke:** reicht nicht — Kostenkurven, Tabellen und Caps lassen sich nicht als reine JSON-Daten ohne Engine-Semantik abbilden.

## 5. Umsetzung

Siehe `docs/TASKS.md`:
- **Phase 28:** Generische Engine-Bausteine (P28-T01 … T06)
- **Phase 29:** Spielgefühl + Pakete (P29-T01 … T06)
