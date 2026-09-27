# ADR-016: Regel-Fähigkeiten im Kampf (systemweit aus dem Regel-JSON)

Status: Angenommen (2026-09-27)

## Kontext

Es existierten zwei getrennte Fähigkeits-Welten:

1. **Regel-Fähigkeiten** — `rulesJson.abilities[]`, im System-Wizard definiert. Wurden nur
   im Charakterbogen angezeigt (`CharacterSheetService.parseAbilities`), ohne Wirkung.
2. **Kampf-Fähigkeiten** — DB-Tabellen `abilities` + `entity_abilities`. Die ActionBar
   (`GET /entities/{id}/abilities`) und `CombatService.useAbility` nutzen nur diese.
   Es gab Endpunkte (`POST /game-systems/{id}/abilities`, `POST /entities/{id}/abilities/{abilityId}`),
   aber **keine UI** und **kein Seeding** aus den Regeln.

Folge: Im Wizard definierte Fähigkeiten waren im Kampf nicht nutzbar.

## Entscheidung

Regel-Fähigkeiten werden **systemweit direkt aus dem Regel-JSON** im Kampf nutzbar. Kein
DB-Sync, keine Zuordnung pro Charakter. Das passt zur Projektlinie „alles über Regel-JSON".

- `POST /combat/{session}/ability` akzeptiert zusätzlich `abilityName`. Ist `abilityId` gesetzt,
  gewinnt diese (DB-Pfad bleibt für Bestand). Sonst wird die Fähigkeit im Kampagnen-Regel-Snapshot
  (`RulesLoader.loadRules(campaignId, worldId)`) per Name (case-insensitiv) gesucht.
- Wirkung: `cost` → AP-Abzug, `diceExpression` → Schaden über `computeDamage` (inkl.
  Attributsbonus), `damageType` → Rüstung/Resistenz. Passive Einträge (`type != active`)
  werden abgelehnt (`ABILITY_NOT_ACTIVE`).
- Die ActionBar liest `rules.abilities`, filtert aktive Einträge und postet `abilityName`.

## Nicht-Ziele / bewusste Grenzen

- **MP-Kosten:** Im Kampf existiert nur AP/HP, kein Mana-Vorrat. `costType: "MP"` kostet
  vorerst 0 AP (`ponytail:`-Kommentar im Code). Nachrüstbar, sobald ein Mana-Pool existiert.
- **Passive Fähigkeiten:** bleiben Anzeige im Bogen; ihr `bonus` ist Freitext. Strukturierte
  passive Boni wären ein eigener Schritt.
- **Kein Angriffswurf** für Fähigkeiten (wie bisher): sie treffen direkt bzw. heilen.
  Manöver mit Angriffsmalus (z. B. Wuchtschlag) bleiben im Manöver-Katalog.
- Kein freies Effekt-Skripting, keine Buffs/Debuffs über Runden.

## Konsequenzen

- Eine Quelle der Wahrheit für System-Fähigkeiten (Regel-JSON). Der DB-`Ability`-Pfad bleibt
  für Sonderfälle bestehen, wird aber nicht mehr vorausgesetzt.
- Fehlerfälle wie bisher fail-closed: Fähigkeit fehlt, AP zu wenig, Schaden ohne Ziel,
  kaputter Würfelausdruck → Fehler **vor** dem AP-Abzug.

## Entscheidungs-Log

| Entscheidung | Alternativen | Grund |
|---|---|---|
| Systemweit aus Regel-JSON | (B) Seeding in DB + Zuordnung pro Charakter; (C) DB-Verwaltungs-UI | Projektlinie „alles über Regel-JSON"; kein Sync/Zuordnen nötig |
| Name statt UUID im Request | Eigene Endpunkt-Route | Minimaler Eingriff, Bestand bleibt kompatibel |
| MP-Kosten = 0 AP (vorerst) | Mana-Pool einführen | Kein Mana-Pool im Kampfmodell; Scope klein halten |
