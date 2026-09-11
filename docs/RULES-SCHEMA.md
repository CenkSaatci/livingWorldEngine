# Rules-Schema

> JSON-Schema-Definition für austauschbare Regelwerke. Server validiert jedes hochgeladene `rules_json` gegen dieses Schema (Implementierung via `com.networknt:json-schema-validator`).

---

## 1. Schema (JSON Schema 2020-12)

> **Single Source of Truth:** `RuleSchemaValidator.DEFAULT_SCHEMA` (Backend-Code).
> Das Schema wird hier nicht mehr dupliziert — die frühere Kopie ist bereits
> einmal vom Code abgedriftet (u. a. `description` existierte nie im Code).

**Top-Level-Keys** (`additionalProperties: false`):

| Key | Typ | Zweck |
|---|---|---|
| `version` | integer ≥ 1 | Pflicht |
| `attributes` | array (min 1) | Pflicht — Attribut-Definitionen |
| `dice_mechanics` | object (`probe` Pflicht) | Pflicht — Probe/Kampf-Ausdrücke |
| `probeType` | enum | `d20_target` \| `d100_threshold` \| `d20_3attr` |
| `progressionType` | string | `level` \| `xp` \| `improvement` |
| `features` / `magic` / `psionics` / `conditionals` / `abilities` / `progression` | object/array | Bestehende Blöcke |
| `derived_values` | array | Formeln, Tabellen, `requiresTrait` (P28) |
| `creationBudget` | object | AP-Topf + Caps (P28) |
| `attributeCosts` | object | Attribut-Kostenkurven (P28) |
| `traits` | array | Vor-/Nachteile-Katalog (P28) |
| `advancement` | object | Steigerungs-Matrix + Max-Regel (P28) |
| `packages` | array | Pakete (P29): `name`, `kind` (species/culture/profession), `cost`, `attributeMods[]` (fest oder Choice `["MU","KK"]`/`"*"`), `autoTraits[]`, `baseValues[]` (schema-only), `recommended[]`, `restricted[]` |
| `conditions` | array | Zustands-Katalog (P29): `name`, optional `rounds`, `effects[]` (`target`/`op`/`value`) |
| `dice_mechanics.combat.maneuvers` | array | Kampfmanöver (P29): `name`, `apCost` (≥1), optional `attackMalus`¹, `effects[]` |

---

## 2. Probe-Typen

| `probeType` | Beschreibung | Beispiel-System |
|---|---|---|
| `d20_target` | 1W20 + Modifikator ≥ Zielwert (default) | D&D 5e |
| `d100_threshold` | 1W100 ≤ Fertigkeitswert | CoC 7e |
| `d20_3attr` | 3W20, je ≤ Attribut, Fehlschläge kompensieren | DSA 5 |

**Auto-Detect:** Ist `probeType` nicht gesetzt, erkennt das System den Typ aus `dice_mechanics.probe`:
- `1d100` → `d100_threshold`
- `3d20` → `d20_3attr`
- Sonst → `d20_target`

---

## 3. Modifier-Formel

Die optionale `modifierFormula` definiert, wie aus Attributswerten Modifikatoren berechnet werden. Syntax: `FormulaEvaluator` mit Attributnamen als Variablen.

| System | Formel | Effekt |
|---|---|---|
| D&D 5e | `floor((attr-10)/2)` | Staerke 16 → +3 |
| DSA 5 | (nicht benötigt) | — |
| CoC 7e | (nicht benötigt) | — |

Beispiel: `"modifierFormula": "floor((attr-10)/2)"`

---

## 4. Dice-Expression-Syntax

Wird in `dice_mechanics.probe` sowie `combat.initiative/damage` verwendet:

| Ausdruck   | Bedeutung |
|------------|-----------|
| `1d20`     | Einmal 20-seitiger Würfel |
| `2d6`      | Zweimal 6-seitiger Würfel (Summe) |
| `1d20+mod` | 1W20 + System-Modifikator (nur D20RuleEngine) |
| `2d6+intelligenz` | 2W6 + Wert des Attributs `intelligenz` |
| `1d8+staerke+2` | 1W8 + Stärke-Attribut + fixer Modifikator 2 |

---

## 5. Probe-Endpunkt

### `POST /rolls/probe` (systembewusst, mit Conditionals)

```json
{
  "entityId": "uuid",
  "skillName": "Athletik",
  "target": 14,
  "advantage": false
}
```

Response enthält: Würfelergebnis, Modifikator, Erfolg, Detail-Infos (z.B. bei 3er-Proben), aktive Conditionals.

---

## 6. Validierungsschritte beim Upload

1. JSON-Schema-Validierung (strukturell)
2. Semantische Validierung:
   - Jeder `skill.attribute`/`attributes`-Ref verweist auf existierendes Attribut
   - Attributnamen sind case-sensitiv lower-snake-case
3. Bei Erfolg: Persistenz in `game_systems.rules_json`
4. Bei Fehler: 400 mit strukturierten Errors

---

## 7. Beispiel-Dateien

Siehe [`docs/examples/`](examples/) für drei vollständige Beispielsysteme:
- [`dnd5e.json`](examples/dnd5e.json) — D&D 5th Edition
- [`coc7e.json`](examples/coc7e.json) — Call of Cthulhu 7th Edition
- [`dsa5.json`](examples/dsa5.json) — Das Schwarze Auge 5. Edition (P29-T06-Referenz: Budget, Staffeln, Traits, Matrix, Tabellen-Derived, Pakete, Zustände, Manöver, Schadensarten)

---

## 8. Schema-Erweiterungen

Neue optionale Properties können jederzeit ergänzt werden. Aktuell nutzbar:
- `conditions` — Zustands-Katalog: Effekte (`probe`/`damage`) wirken summiert; Katalog-`rounds` gilt, wenn beim Anwenden keine Runden mitgegeben werden; Tick beim Zugbeginn
- `dice_mechanics.combat.maneuvers` — AP-Kosten + Schadens-Effekte; ActionBar zeigt Katalog-Buttons
  - ¹ `attackMalus` wird als Feld akzeptiert/dokumentiert, aber noch nicht angewandt — es gibt (noch) kein Attack-Roll-Modell im Kampf (P29-T03-Teilstand, siehe TASKS.md)
- `abilities[].damageType` — Sheet-Anzeige (P23-T01); Kampf nutzt `effects_json.damageType` der Entity-Fähigkeit
- `items.metadata_json.damage_type` — Waffenschaden (ActionBar sendet ausgerüstete Waffe mit)
- `entities.metadata_json` — `damage_armor` (flache Reduktion), `damage_resistances`/`damage_vulnerabilities` (Listen, halbiert/verdoppelt, Case-insensitiv)
- `conditionals` — Bedingte Boni/Mali (via `ConditionEvaluator`)
- `derived_values` — Abgeleitete Werte; drei Formen: `formula`, `input`+`table` (Lookup, Lücken/Überlappungen = Fehler), `requiresTrait` (Eintrag fehlt ohne Trait)
- `abilities` — Charakter-Fähigkeiten für den Kampf
- `features` — System-Feature-Flags (magic, psionics, armorPenalty)
- `progression` — Level/XP-Tabellen und Verbesserungen
- `magic` / `psionics` — Magie- und Psionik-Subsystem

### P28: Engine-Bausteine (Beispiel: [`p28-reference.json`](https://github.com/CenkSaatci/livingWorldEngine/blob/main/backend/src/test/resources/rules/p28-reference.json))

```json
{
  "creationBudget": { "ap": 1100, "attrBase": 8, "maxAttrTotal": 100, "maxAdvantageAp": 80, "fatePoints": 3 },
  "attributeCosts": { "default": [{ "upTo": 14, "cost": 15 }, { "upTo": 15, "cost": 30 }] },
  "traits": [
    { "name": "Hohe Lebenskraft", "kind": "advantage",
      "costs": [{ "tier": "I", "cost": 6 }],
      "excludes": ["Niedrige Lebenskraft"],
      "effects": [{ "target": "derived:hp", "op": "add", "value": 3 }] }
  ],
  "advancement": {
    "columns": ["A", "B", "C", "D"],
    "maxRule": "highestAttributePlus2",
    "table": [{ "from": 1, "to": 12, "costs": { "A": 1, "B": 2, "C": 3, "D": 4 } }]
  },
  "skills": [{ "name": "Zauber", "attributes": ["klugheit"], "costColumn": "C", "activationCost": 3 }]
}
```

- **Traits:** Effekte nur `derived:<name>` / `attribute:<name>` mit `add`; gewählte Traits stehen am Charakter in `entities.metadata_json.traits` (Tier-Suffix wie `"Glück II"` wird ignoriert).
- **Advancement:** `maxRule: "highestAttributePlus2"` erzwingt beim Skill-PATCH mit `campaignId` ein 422 (`SKILL_MAX_EXCEEDED`).
- **Budget-Durchsetzung:** Schema prüft Shapes; AP-Durchsetzung erfolgt im Wizard (Save-Gate), nicht am System-Save.
