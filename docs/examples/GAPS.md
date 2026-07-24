# Lücken-Report: Wizard vs. D&D 5e / CoC 7e / DSA 5

Basierend auf den drei Beispielkonfigurationen in `docs/examples/`.

## Gefundene Lücken (priorisiert)

### P1: Probentyp (betrifft T02)
Der Würfelausdruck `probe` ist aktuell ein freier String (`1d20+mod`). 
Drei konkrete Systeme brauchen drei verschiedene Mechaniken:

| System | Mechanik | Benötigt |
|---|---|---|
| D&D 5e | `1d20 + Mod ≥ DC` | `probe_type: "d20_target"` |
| CoC 7e | `1d100 ≤ Fertigkeit%` | `probe_type: "d100_threshold"` |
| DSA 5 | `3d20, je ≤ Attribut` | `probe_type: "d20_3attr"` |

### P2: Skill-Attribute (betrifft T02)
Aktuell: jedes Skill hat genau **ein** Attribut (`attributes: ["staerke"]`).
DSA 5 braucht **drei** Attribute pro Skill: `attributes: ["mut", "klugheit", "intuition"]`.
CoC hat viele Skills, Wizard muss `attributes` als Array unterstützen (aktuell tut es das schon im Interface, aber UI zeigt nur 1 Select).

### P3: Modifier-Formel (betrifft T05)
D&D 5e: `modifier = (value - 10) / 2` — wird automatisch auf alle Attributs-Proben angewendet.
Aktuell kein Feld im Datenmodell. Vorschlag: `modifierFormula: "(value-10)/2"` in `rulesJson`.

### P4: Action Economy (betrifft T03)
Kein System hat aktuell Action-Typen konfigurierbar.
D&D braucht: Action, Bonus Action, Reaction.
DSA braucht: 1 Aktion (SF können Kosten reduzieren).
CoC braucht: 1 Aktion, Ausweichen als Reaktion.

### P5: Tags + Kategorie (betrifft T04)
`tags` und `category` sind in den Beispiel-JSONs verwendet, aber der Wizard hat kein UI dafür.

### P6: Skill-Bonus als Startwert
CoC: Fertigkeiten beginnen bei 1–50% (nicht 0).
DSA: Talentwerte (FW) von 1–20.
Aktuell: `bonus` ist int. Reicht für D&D (0 oder +2 etc.), aber CoC bräuchte `baseValue` als separaten Skill-Basiswert.

### P7: Sanity / LeP / ASP als Derived Values
Die Formeln funktionieren (T05 FormulaEvaluator), müssen nur noch in der Sheet-API ausgewertet werden.

## Was bereits funktioniert

| Feature | D&D | CoC | DSA |
|---|---|---|---|
| Attribute mit Range | ✅ | ✅ | ✅ |
| Skills mit Attribut | ✅ (1) | ✅ (1) | ❌ (braucht 3) |
| Derived Values | ✅ (HP, AC) | ✅ (HP, San) | ✅ (LeP, AsP) |
| Abilities aktiv/passiv | ✅ | ✅ | ✅ |
| Conditionals | ✅ | ✅ | ✅ |
| Progression | ✅ (Level) | ✅ (Improve) | ✅ (XP) |
| Magic config | ✅ | ✅ | ✅ |
| Psionics config | — | — | ✅ |
