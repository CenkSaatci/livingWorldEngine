# Rules-Schema

> JSON-Schema-Definition für austauschbare Regelwerke. Server validiert jedes hochgeladene `rules_json` gegen dieses Schema (Implementierung via `com.networknt:json-schema-validator`).

---

## 1. Schema (JSON Schema 2020-12)

Das aktuelle Schema wird von `RuleSchemaValidator.DEFAULT_SCHEMA` definiert:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "GameSystem",
  "type": "object",
  "required": ["version", "attributes", "dice_mechanics"],
  "additionalProperties": false,
  "properties": {
    "version":          { "type": "integer", "minimum": 1 },
    "description":      { "type": "string" },
    "probeType":        { "type": "string", "enum": ["d20_target", "d100_threshold", "d20_3attr"] },
    "progressionType":  { "type": "string" },
    "modifierFormula":  { "type": "string" },
    "features":         { "type": "object" },
    "derived_values":   { "type": "array", "items": { "type": "object" } },
    "abilities":        { "type": "array", "items": { "type": "object" } },
    "progression":      { "type": "object" },
    "magic":            { "type": "object" },
    "psionics":         { "type": "object" },
    "conditionals":     { "type": "array", "items": { "type": "object" } },
    "attributes": {
      "type": "array",
      "minItems": 1,
      "items": { "$ref": "#/$defs/attribute" }
    },
    "skills": {
      "type": "array",
      "items": { "$ref": "#/$defs/skill" }
    },
    "dice_mechanics": {
      "type": "object",
      "required": ["probe"],
      "properties": {
        "probe":        { "type": "string" },
        "combat":       { "$ref": "#/$defs/combat" }
      }
    }
  },
  "$defs": {
    "attribute": {
      "type": "object",
      "required": ["name", "type", "default"],
      "properties": {
        "name":    { "type": "string", "minLength": 1, "maxLength": 50 },
        "type":    { "enum": ["INT", "FLOAT", "STRING", "BOOL"] },
        "min":     { "type": "number" },
        "max":     { "type": "number" },
        "default": { }
      }
    },
    "skill": {
      "type": "object",
      "required": ["name"],
      "anyOf": [
        { "required": ["attribute"] },
        { "required": ["attributes"] }
      ],
      "properties": {
        "name":       { "type": "string" },
        "attribute":  { "type": "string", "description": "Legacy single attribute reference" },
        "attributes": { "type": "array", "items": { "type": "string" }, "description": "Multi-attribute reference (z.B. DSA 3er-Proben)" },
        "bonus":      { "type": "integer", "default": 0 }
      }
    },
    "combat": {
      "type": "object",
      "required": ["initiative", "damage"],
      "properties": {
        "initiative":       { "type": "string" },
        "damage":           { "type": "string" },
        "action_points":    { "$ref": "#/$defs/actionPoints" },
        "action_types":     { "type": "array", "items": { "type": "string" } },
        "actions_per_turn": { "type": "object" }
      }
    },
    "actionPoints": {
      "type": "object",
      "properties": {
        "standard": { "type": "integer", "default": 2 },
        "max":      { "type": "integer", "default": 4 }
      }
    }
  }
}
```

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
- [`dsa5.json`](examples/dsa5.json) — Das Schwarze Auge 5. Edition

---

## 8. Schema-Erweiterungen

Neue optionale Properties können jederzeit ergänzt werden. Aktuell geplant/nutzbar:
- `conditionals` — Bedingte Boni/Mali (via `ConditionEvaluator`)
- `derived_values` — Abgeleitete Werte (HP, AC, Ini, etc.)
- `abilities` — Charakter-Fähigkeiten für den Kampf
- `features` — System-Feature-Flags (magic, psionics, armorPenalty)
- `progression` — Level/XP-Tabellen und Verbesserungen
- `magic` / `psionics` — Magie- und Psionik-Subsystem
