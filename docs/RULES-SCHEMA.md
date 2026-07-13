# Rules-Schema

> JSON-Schema-Definition für austauschbare Regelwerke. Server validiert jedes hochgeladene `rules_json` gegen dieses Schema (Implementierung via `com.networknt:json-schema-validator`).

---

## 1.顶层 Schema (JSON Schema 2020-12)

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "GameSystem",
  "type": "object",
  "required": ["version", "attributes", "dice_mechanics"],
  "additionalProperties": false,
  "properties": {
    "version": { "type": "integer", "minimum": 1 },
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
        "probe":        { "type": "string", "$ref": "#/$defs/diceExpression" },
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
      "required": ["name", "attribute"],
      "properties": {
        "name":      { "type": "string" },
        "attribute": { "type": "string", "description": "Referenz auf Attribut #/properties/attributes/items/properties/name" },
        "bonus":     { "type": "integer", "default": 0 }
      }
    },
    "combat": {
      "type": "object",
      "required": ["initiative", "damage"],
      "properties": {
        "initiative":     { "$ref": "#/$defs/diceExpression" },
        "damage":         { "$ref": "#/$defs/diceExpression" },
        "action_points":  { "$ref": "#/$defs/actionPoints" }
      }
    },
    "actionPoints": {
      "type": "object",
      "properties": {
        "standard": { "type": "integer", "default": 2 },
        "max":      { "type": "integer", "default": 4 }
      }
    },
    "diceExpression": {
      "type": "string",
      "pattern": "^[0-9]+d[0-9]+([+-][a-z_0-9]+)([+-][0-9]+)?$",
      "description": "Ausdrücke wie '1d20+mod', '2d6+intelligenz', '1d8+stärke'"
    }
  }
}
```

---

## 2. Dice-Expression-Syntax

| Ausdruck   | Bedeutung |
|------------|-----------|
| `1d20`     | Einmal 20-seitiger Würfel |
| `2d6`      | Zweimal 6-seitiger Würfel (Ergebnis ist Summe) |
| `1d20+mod` | 1W20 mit Additions-Operator |
| `2d6+intelligenz` | 2W6 + Wert des Attributs `intelligenz` des Charakters |
| `1d8+stärke+2` | 1W8 + Stärke-Attribut + fixer Modifikator 2 |

### Parser-BNF (vereinfacht)
```
expression := dice ( modifier )*
modifier   := ("+" | "-") operand
operand    := number | attribute
attribute  := [a-z_][a-z0-9_]*
dice       := count "d" sides
count      := [0-9]+
sides      := [0-9]+
number     := [0-9]+
```

### Fehlerfälle
- Invalides Token:  `1d20+@foo` → `VALIDATION_ERROR` mit `{ token: "@foo" }`
- Attribut nicht im Regelwerk: `1d20+wahrheit` (kein Attribut `wahrheit`) → `INVALID_ATTRIBUTE_REF`
- Division/Modulo werden nicht unterstützt

---

## 3. Beispiel 1 — D20Lite (klassisch, D&D-artig)

```json
{
  "version": 1,
  "attributes": [
    { "name": "stärke",            "type": "INT", "min": 1, "max": 20, "default": 10 },
    { "name": "geschicklichkeit",  "type": "INT", "min": 1, "max": 20, "default": 10 },
    { "name": "konstitution",     "type": "INT", "min": 1, "max": 20, "default": 10 },
    { "name": "intelligenz",       "type": "INT", "min": 1, "max": 20, "default": 10 },
    { "name": "weisheit",          "type": "INT", "min": 1, "max": 20, "default": 10 },
    { "name": "charisma",         "type": "INT", "min": 1, "max": 20, "default": 10 }
  ],
  "skills": [
    { "name": "athletik",     "attribute": "stärke" },
    { "name": "schleichen",   "attribute": "geschicklichkeit" },
    { "name": "wahrnehmung",  "attribute": "weisheit" },
    { "name": "überzeugen",   "attribute": "charisma" }
  ],
  "dice_mechanics": {
    "probe": "1d20+mod",
    "combat": {
      "initiative": "1d20+geschicklichkeit",
      "damage": "1d8+stärke",
      "action_points": { "standard": 1, "max": 2 }
    }
  }
}
```

### Probe-Auswertung
- Würfel 1W20 → E ∈ [1, 20]
- Modifikator `mod` = floor((Attribut − 10) / 2)
- Total = E + mod
- Probe erfolgreich bei Total ≥ Target (vorgegeben im `WorldEvent` oder Aufrufer)

---

## 4. Beispiel 2 — TwoDicePool (eigene Mechanik)

```json
{
  "version": 1,
  "attributes": [
    { "name": "kraft",        "type": "INT", "min": 1, "max": 6, "default": 3 },
    { "name": "intelligenz",  "type": "INT", "min": 1, "max": 6, "default": 3 },
    { "name": "geschick",     "type": "INT", "min": 1, "max": 6, "default": 3 }
  ],
  "skills": [
    { "name": "schlagen",     "attribute": "kraft" },
    { "name": "zaubern",      "attribute": "intelligenz" }
  ],
  "dice_mechanics": {
    "probe": "2d6+mod",
    "combat": {
      "initiative": "2d6+geschick",
      "damage": "2d6+kraft",
      "action_points": { "standard": 2, "max": 3 }
    }
  }
}
```

### Probe-Auswertung (Pool-System)
- Würfel 2W6 → Summe ∈ [2, 12]
- Plus Attributswert (1–6)
- Target-Schwellen:
  - **≥ 6** → Erfolg mit Komplikation
  - **≥ 8** → Erfolg
  - **≥ 11** → Großer Erfolg

Die Schwellen sind nicht im `rules_json` selbst gespeichert — sie gehören zum Implementierungsverhalten der `PoolRuleEngine`-Klasse. Zukünftige Erweiterung: `dice_mechanics.success_tiers` als optionales Array.

---

## 5. Probe Request / Response (API-Ebene)

### Request (POST `/api/rolls`)
```json
{
  "world_id": "uuid",
  "entity_id": "uuid",
  "skill_id": "athletik",
  "modifier": 2,
  "target": 14
}
```

### Response
```json
{
  "roll_id": "uuid",
  "roll_expressions": ["1d20+mod"],
  "dice": [ { "count": 1, "sides": 20, "rolls": [14] } ],
  "modifier": 2,
  "total": 16,
  "target": 14,
  "success": true,
  "world_event_id": 789012
}
```

---

## 6. Validationsschritte beim Upload

1. JSON-Schema-Validierung (strukturell)
2. Semantische Validierung:
   - Jeder `skill.attribute`-Ref verweist auf existierendes Attribut
   - Jede Attributreferenz in `dice_mechanics.*` existiert
   - Attributnamen sind case-sensitiv lower-snake-case
3. Bei Erfolg: Persistenz in `game_systems.rules_json` und Versionierung
4. Bei Fehler: 400 mit strukturierten Errors

---

## 7. Erweiterungs-Strategie

Das Schema ist absichtlich schlank gehalten. Zukünftige Erweiterungen sind via zusätzliche optionale Properties möglich:

- `magic` — Magie-Subsystem (Fokus, Kosten, Schule)
- `advantages` — Vor- und Nachteile als Attribut-Modifikatoren
- `dungeons` — Dungeon-Generierung
- `resting` — Kurze/Lange-Rast-Mechaniken
- `leveling` — XP/Level-Tabelle

Alle Erweiterungen müssen vor Usage dokumentiert + versioniert werden.