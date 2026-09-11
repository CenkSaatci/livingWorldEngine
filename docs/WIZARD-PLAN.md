# Generischer System-Wizard — Detail-Plan (P28/P29)

> Implementierungsplan zu [`ADR/012`](ADR/012-generic-wizard-engine.md). **Umsetzungsstand (2026-09-11):** P28, P29, P30 (Charakter-Wizard) und P31 (E2E) vollständig umgesetzt; Suiten 362/168/7; Audits abgearbeitet. Details/Teilstände siehe „Bemerkungen" und „Aktueller Projektstand" in [`TASKS.md`](TASKS.md).
> Konventionen: [UI-UX](UI-UX.md) (Dark-first, Keyboard-first, DE+EN), [TASKS](TASKS.md) (TDD, Tests grün), [TESTING](TESTING.md) (E2E-Abnahme).

---

## 1. Ist-Stand (verifiziert 2026-09-11)

- 11 Steps (`STEPS`-Array, Reihenfolge u. a. `…6,7,8,4,5`), ein `WizardData`-State, `update()`-Helper, Row-Editoren (Name/Typ/Min/Max/Default + Delete-X + Add-Button), Test-Würfel mit Toast-Resultat, Übersichts-Step mit Save.
- **Keine Step-Validierung:** Weiter immer möglich, Save prüft nur Name; Backend-Schema prüft Shape.
- i18n-Namespace `systemWizard` (`s0_*` … `s5_*`, `sdv_*`), Icons lucide-react, Toasts für Feedback.
- Wire-Format: `toRulesJson` / `fromRulesJson` in `frontend/src/types/gameSystem.ts`; Backend-Schema `additionalProperties: false`.

## 2. UI/UX-Konzept (gilt für alle neuen Steps)

- **Warnen statt blockieren:** Steps zeigen Probleme amber inline; **Save ist rot-gated** mit Prüfbericht in der Übersicht (Fehler → Klick springt zum Step). Begründung: Systembau ist explorativ (Template laden, hin-und-her) — Blockaden zerstören Flow.
- **Budget-Prinzip:** Wo AP fließen (Budget, Kostenkurven, Traits, Steigerung), steht oben eine `BudgetBar` (verbraucht/verfügbar) — gleiche Komponente überall.
- **Row-Editor-Muster** des Bestands übernehmen (Header-Zeile, Add-Button, Delete-X, `focus:border-accent`); Tabellen (Matrix, Lookup) werden auf Small-Screens zu gestapelten Cards.
- **Keyboard-first** ([UI-UX §8](UI-UX.md)): alle Icon-Buttons mit `aria-label`, Enter bestätigt, Escape bricht ab (Audit-Lehre aus Icon-Buttons ohne Labels).
- **i18n:** neue Keys `sb_*` (Budget), `st_*` (Traits), `sa_*` (Advancement), `sv_*` (Derived deluxe), je DE+EN (Pflicht per ADR-007).
- **Step-Integration:** bestehende Steps **nicht** umnummerieren (Deep-Links/Tests); neue Steps **vor** Übersicht einhängen; Step-Indikator scrollt horizontal (bestehendes `overflow-x-auto`).

## 3. P28-Detail — Engine-Bausteine

### P28-T01: Schema + Wire-Format öffnen
- **Datenmodell:** Neue optionale Top-Level-Keys (alle optional → abwärtskompatibel): `creationBudget`, `attribute.costs[]`, `traits[]`, `advancement{}`, erweiterte `derived_values`-Einträge (`table[]`, `requiresTrait`, `input`). Alles andere unverändert.
- **Backend:** `RuleSchemaValidator.DEFAULT_SCHEMA` erweitern; Konsumenten lesen defensiv (`path().isMissingNode()` → Defaults wie heute). Keine neuen Endpunkte.
- **UI:** keine (reine Grundlage).
- **Tests (TDD):** Altes D20Lite-JSON validiert weiter (Regression); Minimalbeispiel mit allen neuen Keys validiert; `toRulesJson`/`fromRulesJson`-Roundtrips inkl. alter Dateien.
- **Risiko:** niedrig. **Abhängigkeit:** keine (Einstieg).

### P28-T02: AP-Budget + Caps + Attribut-Kostenkurven
- **Datenmodell:**
  ```json
  "creationBudget": { "ap": 1100, "apCarryoverMax": 10, "fatePoints": 3,
    "maxAttrTotal": 100, "maxAttrValue": 14, "maxSkillValue": 10,
    "maxCombatValue": 12, "maxSpells": 12, "maxAdvantageAp": 80 }
  ```
  Pro Attribut optional: `"costs": [{"upTo": 14, "cost": 15}, {"upTo": 15, "cost": 30}, {"upTo": 99, "cost": 60}]` (Default-Kurve global konfigurierbar, DSA-Referenz: 15/30/45/60/75/90).
- **Backend:** Shape-Validierung; **keine** Durchsetzung von Budgets beim System-Save (Durchsetzung passiert beim Heldenbau — außerhalb P28, explizit dokumentieren).
- **UI:** Neuer Step „Budget & Rahmen" nach Step 0: AP-Topf (Zahl), Caps-Tabelle (je Kategorie Max-Wert), Startwerte. Attribut-Step: neue Spalte „Kosten" (Button öffnet Kurven-Editor: Staffel-Zeilen upTo/cost + Live-Vorschau „8→14 kostet X AP"). `BudgetBar` oben in Budget-, Attribut- und Trait-Steps (liest prognostizierte Kosten aus gleichem Helper `calcCosts(data)` in `types/gameSystem.ts` — eine Quelle, UI + spätere Validierung teilen sie).
- **Tests:** Kosten-Helper (DSA-Kurve: 8→14 = 90 AP … wait: 6 Punkte × 15 = 90 ✓; 14→15 = 30; Cap-Verletzung erkannt); Wizard rendert Step + Save blockiert bei Budget-Überschreitung nur, wenn Kurven aktiv sind (sonst Warnung).
- **Risiko:** mittel (Kostenlogik muss exakt der Referenz entsprechen — DSA-Tabellen als Fixture in Tests).

### P28-T03: Traits-Katalog (Vor-/Nachteile)
- **Datenmodell:**
  ```json
  "traits": [
    {"name": "Glück", "kind": "advantage",
     "costs": [{"tier": "I", "cost": 30}, {"tier": "II", "cost": 60}, {"tier": "III", "cost": 90}],
     "requires": [], "excludes": ["Pech"], "effects": []},
    {"name": "Hohe Lebenskraft", "kind": "advantage",
     "costs": [{"tier": "I", "cost": 6}],
     "requires": [], "excludes": ["Niedrige Lebenskraft"],
     "effects": [{"target": "derived:hp", "op": "add", "value": 3}]}
  ]
  ```
  Effekt-Semantik P28: nur `derived:<name>` + `attribute:<name>` mit `add`; Rest ignorieren (später erweiterbar, dokumentiert).
- **Backend:** Shape-Validierung; Sheet wertet `effects` auf Derived aus (kleine Erweiterung in `DerivedValueService`, TDD); Schranke `maxAdvantageAp` aus Budget prüfen (Warnung, kein Block — Heldenbau-Enforcement später).
- **UI:** Neuer Step „Merkmale": Katalog-Tabelle (Name, Art-Toggle, Kosten-Staffel-Editor, Voraussetzungen/Exklusionen als Tag-Inputs), Konflikt-Warnung live („schließt X aus"), Kosten-Summe + `BudgetBar`-Anbindung.
- **Tests:** Exklusions-Erkennung, Staffel-Kosten, Effekt auf Derived (hp 20→23-Analogon), Roundtrip.
- **Abhängigkeit:** T01. **Risiko:** mittel (Effekt-Semantik sauber begrenzen — kein Mini-Skriptsprache!).

### P28-T04: Steigerung (Spalten, Matrix, Aktivierung)
- **Datenmodell:**
  ```json
  "advancement": {
    "columns": ["A", "B", "C", "D"],
    "table": [{"from": 1, "to": 12, "costs": {"A": 1, "B": 2, "C": 3, "D": 4}}, {"from": 13, "to": 13, "costs": {"A": 2, "B": 4, "C": 6, "D": 8}}],
    "activationCosts": {"spell": 3, "liturgy": 3, "combat": 0, "talent": 0},
    "maxRule": "highestAttributePlus2"
  }
  ```
  Pro Skill: `"costColumn": "C"` (ersetzt implizite Annahmen; Default-Heuristik dokumentiert).
- **Backend:** Max-Regel-Enforcement (Verstoß → 422 mit Code, analog COMBAT_AP_INSUFFICIENT); Sheet liefert Kosten-Vorschau-Daten (nächste Stufe + Kosten) — reine Leselogik, keine neuen Endpunkte.
- **UI:** Skill-Step: Spalten Spalte (A–D-Dropdown) + Aktivierungskosten-Anzeige; neuer Matrix-Editor (Zeilen from/to + 4 Kosten-Spalten, Add/Delete); Sheet-Vorschau „nächste Stufe: X AP".
- **Tests:** Matrix-Lookup (Grenzen!), Max-Regel 422, Vorschau-Math, Roundtrip.
- **Abhängigkeit:** T01. **Risiko:** niedrig-mittel.

### P28-T05: Derived deluxe (Tabellen + Bedingungen)
- **Datenmodell:**
  ```json
  "derived_values": [
    {"name": "sk", "input": "mut+klugheit+intuition",
     "table": [{"min": 24, "max": 26, "value": 4}, {"min": 27, "max": 32, "value": 5}]},
    {"name": "asp", "formula": "20+mut", "requiresTrait": "Zauberer"}
  ]
  ```
  Semantik: `input`-Ausdruck auswerten → Tabellen-Lookup (Lücken → Fehler statt Raten!); `requiresTrait` ohne Trait → Eintrag als „nicht vorhanden" (nicht „(Fehler)").
- **Backend:** `FormulaEvaluator`-Erweiterung strikt per TDD (Parser-Tests je Syntax); `DerivedValueService` wertet Tabelle/Bedingung aus; „(Fehler)"-Marker bleibt, bekommt aber Grund (`unknownVariable` vs. `tableGap` vs. `missingTrait`) für bessere UI-Texte.
- **UI:** Derived-Editor: Modus-Umschalter Formel/Tabelle (Tabellen-Zeilen min/max/value + Live-Test mit Beispiel-Attributen); Trait-Bedingungs-Dropdown (aus Traits-Katalog, T03-Abhängigkeit!); Fehler-Gründe als lesbare Hinweise.
- **Tests:** Parser (Tabellen-Syntax), Lookup (Grenzen, Lücken), requiresTrait beide Fälle, Sheet-Integration (DSA-SK-Beispiel: MU14+KL12+IN14=40 → 7).
- **Abhängigkeit:** T01, T03 (Trait-Namen). **Risiko:** mittel (Parser-Disziplin nötig).

### P28-T06: Abnahme Engine-Bausteine
- Referenz-System nutzt alle P28-Blöcke; E2E-Stichprobe (Wizard→Save→Sheet rechnet korrekt); alte Systeme (D20Lite/TwoDicePool/Fudge) unverändert grün; RULES-SCHEMA + Wizard-Hilfe-Doku aktuell.

## 4. P29-Detail (Plan-Ebene, Umsetzung später)

- **T01 Zustände:** `conditions[]` (Name, Effekte als Modifikator-Sperr-Liste, Dauer/Tick-Auflösung); UI Badges + DM-Vergabe; Backend wendet in Probe/Schaden an.
- **T02 Schicksalspunkte:** aus `creationBudget` (Start/Max/Refresh); Sheet-Button + Probe-Modal-Integration (Re-Roll/+1); Backend-Endpunkt zum Ausgeben.
- **T03 Manöver:** System-Config (Voraussetzung, Angriffsmalus, Effekt-Formel); ActionBar-Integration; E2E.
- **T04 Rüstung/Schaden:** hängt an **P23**; Zonen optional; Resistenz/Vulnerabilität; Log-Aufschlüsselung.
- **T05 Pakete:** `packages[]` (Typ, Kosten, Mods mit `oneOf`-Choice-Gruppen, Basiswerte, Auto-Traits, Kultur-Restriktionen); Wizard-Schritt mit Live-Vorschau + Untypisch-Warnung.
- **T06 DSA-Content:** `dsa5.json` heben; E2E-Heldenbau (100-AP-Paket, 3W20, Basiswerte); TESTING-Checkpoints.

## 5. Reihenfolge & Meilensteine

T01 → (T02, T04 parallel möglich) → T03 → T05 (braucht Trait-Namen) → T06. T03 vor T05 ist hart (requiresTrait-Dropdown). Meilenstein P28: „System mit Budget, Traits, Matrix und Tabellen-Derived speicherbar + Sheet rechnet". P29 erst nach P28-Abnahme.

## 6. Offene Detailfragen (vor jeweiligem Task klären)

- Effekt-Semantik über `derived:`/`attribute:` hinaus? (Nein in P28 — dokumentiert.)
- Max-Regel-Varianten außer `highestAttributePlus2`? (Feld ist String-enum, erweiterbar.)
- Tabellen-Lookup bei Überlappung/Lücken: Fehler (gewählt) vs. runden? (Fehler — Überraschungen vermeiden.)
