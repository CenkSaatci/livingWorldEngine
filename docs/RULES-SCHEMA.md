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
| `derived_values` | array | Formeln, Tabellen, `requiresTrait` (P28). **ADR-014:** Formeln lesen zusätzlich Fertigkeitswerte aus `skills[]` (Charakter-`skillsJson` → Regel-`bonus`, explizite 0 gültig); Lookup case-insensitiv, Attribute gewinnen bei Kollisionen. Namen mit Leerzeichen/Klammern sind in Formeln nicht referenzierbar. Ergebnisse werden **aufgerundet (`ceil`)**, auch bei Tabellen-Lookups; Content nutzt explizites `floor` wo abgerundet werden soll. |
| `skills[].kind` | string | **ADR-014:** freie Skill-Art (`combat`/`craft`/`social`/…) für Gruppierung; Angriffs-Skill mit gesetzter Art ≠ `combat` erzeugt eine **Warnung** (validierbar über `/game-systems/{id}/validate`, kein Upload-Blocker) |
| `dice_mechanics.combat.damage` | string | **QA (T7-Nacharbeit):** Schadens-Ausdruck, z. B. `1d8+staerke`, `1d6`, `1d6+2`, `1d8+staerke+2` oder `1d6-staerke`. Syntax: `NdM[±Attribut][±Flat]…`, Leerzeichen erlaubt, max. ein Attribut-Term (negative Vorzeichen senken den Bonus). Der **Würfelteil wird wirklich gewürfelt**; ein angehängtes Attribut (`+staerke`, ohne Waffe) bzw. das Waffen-Attribut gibt den Bonus per `damage_attr_bonus`-Formel. Ohne Waffe/Attribut: reiner Würfel + Boni. **Fehlend = Default `1d6`, kaputt = Upload-Fehler (`COMBAT_ATTACK_UNRESOLVABLE` zur Laufzeit).** |
| `dice_mechanics.combat.damage_attr_bonus` | string | **ADR-014:** System-Formel mit Variable `attr` für den Attribut-Schadenbonus (Default `floor((attr-10)/2)`) |
| `creationBudget` | object | AP-Topf + Caps (P28) |
| `attributeCosts` | object | Attribut-Kostenkurven (P28) |
| `traits` | array | Vor-/Nachteile-Katalog (P28) |
| `advancement` | object | Steigerungs-Matrix + Max-Regel (P28) |
| `packages` | array | Pakete (P29): `name`, `kind` (species/culture/profession), `cost`, `attributeMods[]` (fest oder Choice `["MU","KK"]`/`"*"`), `autoTraits[]`, `baseValues[]` (T5: Untergrenze fuer Attribute/Skills, gratis — nur der Kauf darueber kostet AP), `recommended[]`, `restricted[]` |
| `conditions` | array | Zustands-Katalog (P29): `name`, optional `rounds`, `effects[]` (`target`/`op`/`value`), optional `blocks[]` (T3: gesperrte Aktionstypen wie `ATTACK`, `MOVE`, `DEFEND`, `MANEUVER`, `ABILITY` → `COMBAT_ACTION_BLOCKED`) |
| `dice_mechanics.combat.maneuvers` | array | Kampfmanöver (P29): `name`, `apCost` (≥1), optional `attackMalus`¹, `effects[]` |
| `items[].metadata_json` | object | **QA:** Waffen-Felder `damage` (Würfel, z. B. `"1d6+2"`, Default `"1d6"`), `damage_attr` (Attribut je Waffe, z. B. `"ge"` Rapier / `"kk"` Axt), `damage_bonus` (flat, Default 0), `damage_type` (Resistenzen). Treffer-Schaden = Waffenwürfel + Flat + Bonus + Attributbonus + Zustands-/Merkmal-Boni. Beispiel Langschwert: `{"damage":"1d6+2","damage_attr":"koerperkraft","damage_type":"cut"}`. |
| `traits[].effects` | array | Effekt `{"target":"damage","op":"add","value":N}` wirkt jetzt auch im Kampf (gewählte Merkmale, Tier-Suffix egal). |
| `dice_mechanics.combat.attack` | object | **P1/T2** optionales Angriffswurf-Gate. Quelle (genau eine): `attribute` (Attribut des Angreifers, Engine-Modifikator wie gehabt), `value` (Name eines **abgeleiteten Werts** des Angreifers, z. B. DSA `at`) oder `skill` (Per-Charakter-Fertigkeitswert, z. B. CoC `Kampf (Raufen)`); `value`/`skill` sind finale Werte (reiner Wurf, kein Engine-Modifikator). `target` (Name eines abgeleiteten Werts des Verteidigers) ist Pflicht fuer `attribute`/`gte`, bei `value`/`skill`+`lte` nur Doku. `dice` (Default `1d20`), `comparison` (`gte` = Wurf ≥ Ziel, Default; `lte` = Wurf ≤ Ziel fuer d100/CoC/DSA). Fehlt der Zielwert (attribute-Pfad), greift das Gate nicht. **ADR-014:** Konfigurierte, aber nicht ableitbare Angriffs-/Zielwerte blockieren den Angriff (`COMBAT_ATTACK_UNRESOLVABLE`, 422) statt stiller Treffer. Manöver mit `attackMalus` laufen durch dasselbe Gate (Malus erschwert: `lte` addiert, `gte` subtrahiert). |
| `dice_mechanics.difficulties` | array | **P1** benannte Schwierigkeitsgrade: `name`, optional `multiplier` (d100, z. B. 0.5) und/oder `delta` (Verschiebung; 3W20-Schwellen bzw. d20-Zielwert). Proben übergeben `difficultyKey`; `bonusDice`/`penaltyDice` gibt es als reine API-Option (d100-Zehnerwürfe). |
| `skills[].casting` | object | **P1** generisch: `resource` (frei, `^[a-z][a-z0-9_]{0,30}$` — z. B. `asp`, `kap`, `mp`, `slot_1`), `cost` (≥1), optional `requiresTrait`, optional `restore` (`short`\|`long`, Default `long`). Das Maximum kommt aus dem **abgeleiteten Wert gleichen Namens**; Rasten füllt die Zähler (`{resource}_current` in `metadataJson`) gemäß Rest-Config auf. |
| `currency` | object | **ADR-015:** Währung eines Systems: `name?`, `denominations[]` mit `{name, abbr?, factor}` (`factor` ≥ 1, Namen/Abkürzungen eindeutig). Geld ist am Charakter **ein Basiswert** (`metadataJson.money`, kleinste Sorte); die Anzeige rechnet in Sorten um. Ohne `currency` wird der Wert als nackte Zahl angezeigt. |
| `poi_actions` | array | **ADR-015:** Aktions-Katalog für Orte/NPCs: `name`, `description?`, `chat` (`none`\|`public`\|`actor`), `requiresTrait?`, `dmOnly?`, `probe?`, `trade?`, `effects[]`. Gebunden über `location.services[]` bzw. NPC-`services_offered[]`. Effekte: `money` (±), `item` (`name`, `qty` ±), `heal` (Zahl/Würfel/`full`), `condition` (`name`, `rounds?`, `remove?`), `fate` (±), `rest` (`mode` short\|long), `text`. Alle negativen Posten werden **vor** jeder Wirkung geprüft. |

---

## 1a. Generische Mechaniken (P1)

- **Angriffswurf:** über `dice_mechanics.combat.attack` konfigurierbar (siehe Tabelle); Systeme ohne diesen Block behalten das direkte Schadensmodell. `comparison` deckt Roll-High (D&D) und Roll-Under (d100) ab — keine systemspezifische Sonderlogik im Code.
- **Adventure-`skillCheck` (T1):** `{"skill","modifier","target"}`. Bei `probeType: d20_3attr` (3W20) laeuft die Probe ueber den ProbeService; `modifier` ist dann die Difficulty (positiv = erschwert). Alle anderen Systeme nutzen den RollService mit Kampagnen-Kontext (`modifier` = Wurfmodifikator, `target` = Zielwert).
- **Schwierigkeitsgrade:** über `dice_mechanics.difficulties`; das Charakterblatt liefert sie als `difficultyLevels`, der Probenroller bietet ein Dropdown (bei Zaubern ausgeblendet).
- **Casting-Ressourcen:** frei wählbar; Maxima sind abgeleitete Werte (z. B. `asp`, `mp`, `slot_1`). Wiederherstellung beim Rasten steuert `casting.restore` zusammen mit `recover_resources`/`recover_all`.

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
   - **Namensregel (ADR-014):** Namen (Attribute, Skills, Zustände, Merkmale) werden beim Lookup **case-insensitiv** verglichen; kollidierende Namen (auch case-insensitiv) lehnt der Validator ab. Kanonische Schreibweise für Attribute ist lower-snake-case
3. Bei Erfolg: Persistenz in `game_systems.rules_json`
4. Bei Fehler: 400 mit strukturierten Errors; Warnungen (z. B. Skill-`kind`-Fehlgriff) blockieren nicht

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
  - ¹ `attackMalus` wird angewandt, sobald `dice_mechanics.combat.attack` konfiguriert ist (T2); ohne Gate bleibt es wirkungslos (Fallback: direkter Schaden).
- `abilities[].damageType` — Sheet-Anzeige (P23-T01); Kampf nutzt `effects_json.damageType` der Entity-Fähigkeit
- `items.metadata_json.damage_type` — Waffenschaden (ActionBar sendet ausgerüstete Waffe mit)
- `entities.metadata_json` — `damage_armor` (flache Reduktion), `damage_resistances`/`damage_vulnerabilities` (Listen, halbiert/verdoppelt, Case-insensitiv)
- `conditionals` — Bedingte Boni/Mali (via `ConditionEvaluator`)
- `derived_values` — Abgeleitete Werte; drei Formen: `formula`, `input`+`table` (Lookup, Lücken/Überlappungen = Fehler), `requiresTrait` (Eintrag fehlt ohne Trait)
- `abilities` — Charakter-Fähigkeiten für den Kampf
- `features` — System-Feature-Flags (magic, psionics, armorPenalty)
- `progression` — Level/XP-Tabellen und Verbesserungen
- `magic` / `psionics` — Magie- und Psionik-Subsystem

### P28: Engine-Bausteine (Beispiel: [`p28-reference.json`](https://github.com/CenkSaatci/livingWorldEngine/blob/main/backend/src/test/resources/rules/p28-reference.json))```json
{
  "creationBudget": { "ap": 1100, "attrBase": 8, "maxAttrTotal": 100, "maxAdvantageAp": 80, "fatePoints": 3 },
  "fate": { "probeBonusPerPoint": 1, "avoidDeathCost": 1 },
  "social": { "relationshipScores": { "freundlich": 2, "feindselig": -3 }, "maxModifier": 3 },
  "social_actions": [
    { "name": "Freundlich bitten", "skill": "Überreden", "relationshipWeight": 1,
      "onSuccess": [ { "condition": "Beeindruckt", "rounds": 3 } ],
      "onFailure": [ { "condition": "Verärgert", "rounds": 3 } ] }
  ],
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

### ADR-015: POI-Aktionen, Währung, Händler

```json
{
  "currency": {
    "name": "Dukaten",
    "denominations": [
      { "name": "Kupfer", "abbr": "K", "factor": 1 },
      { "name": "Silber", "abbr": "S", "factor": 10 },
      { "name": "Gold",   "abbr": "G", "factor": 100 }
    ]
  },
  "poi_actions": [
    { "name": "Medicus", "description": "Ein Wundarzt verbindet deine Verletzungen.",
      "chat": "actor",
      "effects": [
        { "type": "money", "amount": -15 },
        { "type": "heal", "amount": "2d6" },
        { "type": "condition", "name": "Wunde", "remove": true } ] },

    { "name": "Wirtshaus", "description": "Ein warmes Bett und eine Mahlzeit.",
      "chat": "public",
      "effects": [ { "type": "money", "amount": -5 }, { "type": "rest", "mode": "long" } ] },

    { "name": "Ruine untersuchen", "description": "Zwischen den Trümmern glitzert etwas.",
      "chat": "actor",
      "probe": { "skill": "Sinnesschärfe", "difficulty": 0,
        "onSuccess": [ { "type": "item", "name": "Alter Schlüssel", "qty": 1 } ],
        "onFailure": [ { "type": "text", "text": "Du findest nur Schutt." } ] } },

    { "name": "Aussichtspunkt", "description": "Von hier sieht man über das ganze Tal.",
      "chat": "public", "effects": [] },

    { "name": "Handeln", "description": "Kaufen und verkaufen beim Händler.",
      "chat": "actor",
      "trade": { "buy": true, "sell": true, "sellRate": 0.5 } }
  ]
}
```

- **Bindung:** Eine Aktion ist an einem Ort verfügbar, wenn ihr Name in `location.services[]` oder in `services_offered[]` eines NPCs am Ort steht (case-insensitiv).
- **`chat`:** `public` persistiert im Welt-Chat, `actor` geht transient an den Ausführenden (User-Queue) und steht im Ergebnis; fehlt die Angabe, ist eine reine Text-Aktion öffentlich, eine mechanische privat.
- **Händler:** NPC-Metadaten `is_merchant`, `shop_inventory[{item, price?}]`, optional `sell_rate`, `price_modifier`. Ohne expliziten `price` gilt `Item-Wert × Wohlstandsfaktor × price_modifier`; Verkaufserlös ist `floor(Preis × sellRate)`. Die Preisformel kommt aus `EconomyService`.
- **Fehlend = Default, kaputt = Fehler:** unbekannte Aktion/Item/Zustand und nicht leistbare Kosten brechen ab, bevor irgendein Effekt wirkt.
