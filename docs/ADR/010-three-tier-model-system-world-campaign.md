# ADR-010: 3-Ebenen-Modell — System, Welt, Kampagne

- **Status:** Akzeptiert (2026-07-25)
- **Entscheidung:** Einführung einer dritten Ebene „Kampagne" — Systeme und Welten werden entkoppelt; Items und Abilities wandern vom Welt- in den System-Kontext.

---

## 1. Kontext

Das aktuelle Datenmodell bindet Regelwerks-Komponenten an die Welt:

- `items.world_id` — Items gehören zur Welt
- `abilities.world_id` — Abilities gehören zur Welt
- `worlds.game_system_id` — eine Welt ist fest an ein System gebunden

Dies widerspricht dem fachlichen Modell: **Ein System beschreibt Regeln, Spielmechaniken und deren Bausteine (Items, Abilities, Attribute). Eine Welt beschreibt Geschichte, Karte, Völker und NPCs. Eine Kampagne kombiniert eine Welt mit einem System** — z. B. „Aventurien" (Welt) mit dem DSA-System, oder dieselbe Welt mit einem D&D-System für eine andere Kampagne.

Dynamische Welt-Zustände (Krieg, Eroberungen, Fraktionsverschiebungen) gehören zur **Kampagne**, nicht zur statischen Welt.

## 2. Entscheidung

Einführung des 3-Ebenen-Modells:

```
SYSTEM (game_systems)             WELT (worlds)
├── rules_json                    ├── name, owner_id
├── items  → game_system_id       ├── regionen, orte, karten
└── abilities → game_system_id    └── entities (NPCs, PCs)

KAMPAGNE (campaigns — neu)
├── world_id + game_system_id     ← Kombination Welt × System
├── name, settings_json
└── state_json                    ← dynamische Zustände
```

Konkret:
1. `items.game_system_id` statt `items.world_id`
2. `abilities.game_system_id` statt `abilities.world_id`
3. Neue Tabelle `campaigns(id, world_id, game_system_id, name, settings_json, state_json)`
4. `worlds.game_system_id` wird entfernt — Welten sind systemunabhängig
5. Alle System-Logik (RulesLoader, Combat, LevelUp, CharacterSheet) liest das System über den **Kampagnen-Kontext**
6. Sessions werden an Kampagnen gebunden (`sessions.campaign_id`)

## 3. Konsequenzen

### Positiv
- Systeme sind **wiederverwendbar** über beliebige Welten (DSA-System × Aventurien, D&D-System × Aventurien)
- Items/Abilities sind **konzeptionell korrekt** dem Regelwerk zugeordnet
- Welt bleibt **statisch** — Geschichte und Völker unabhängig von Kampagnen-Zuständen
- Klare Trennung: System = Regeln, Welt = Orte/Geschichte, Kampagne = Zustand

### Negativ / Aufwand
- DB-Migrationen: items, abilities, worlds, neue campaigns-Tabelle, sessions
- Umbau von 8+ Stellen, die `world.getGameSystemId()` nutzen (RulesLoader, CombatService, LevelUpService, CharacterSheetService, ProbeService)
- Frontend: Kampagnen-CRUD + Welt-Erstellung ohne System + Combat/Session auf Kampagnen-Kontext
- Bestandsdaten: bestehende `items`/`abilities` an `world_id` können keiner `game_system_id` zugeordnet werden → werden verworfen (Entwicklungsumgebung)

## 4. Alternativen

- **Modell A (Status quo):** Welt bleibt an System gebunden, Items/Abilities bleiben weltweit — schneller, aber konzeptionell falsch und verhindert System-Wiederverwendung.
- **Items/Abilities in rulesJson:** Keine Tabellen, aber Items wären nicht pro Instanz verwaltbar (Mengen, Zustände) und der Wizard würde unübersichtlich. Für einfache Fälle ok, nicht für die langfristige Item-Ökonomie.

## 5. Umsetzung

Siehe `docs/TASKS.md`:
- **Phase 24:** Datenmodell-Umbau (Migrationen, Domain, Campaign-CRUD)
- **Phase 25:** Kampagnen-Integration (RulesLoader, Combat, LevelUp, Sheet, Sessions, Item-CRUD)
- **Phase 26:** Frontend 3-Ebenen (Kampagnen-UI, Welt-Wizard, Combat-Kontext)
