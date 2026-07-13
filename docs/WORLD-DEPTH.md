# Regionen, Orte, NPCs & Ereignis-Logs

> Erweiterungskonzept für Phase 6 — eine lebendige Welt braucht Tiefe und Geschichte.

---

## 1. Architektur

```
Welt → Regionen → Orte → NPCs
                  ↓
            Entity-Ereignis-Log (chronik)
```

Jede Region, jeder Ort und jeder NPC bekommt eine **eigene Chronik** von Ereignissen.
Diese Chronik ist die **Gedächtnisgrundlage für den KI-Bot** — er bekommt beim Prompt-Bau
die letzten Ereignisse der Entity mitgeliefert.

---

## 2. Entity-Ereignis-Log (`entity_events`)

### Tabellendefinition

```sql
CREATE TABLE entity_events (
    id              BIGSERIAL    PRIMARY KEY,
    entity_type     VARCHAR(50)  NOT NULL,   -- 'region', 'location', 'npc'
    entity_id       UUID         NOT NULL,   -- FK zu regions/locations/entities
    event_type      VARCHAR(100) NOT NULL,   -- 'BANDIT_RAID', 'ROYAL_VISIT', 'NPC_EVENT'
    title           VARCHAR(200) NOT NULL,   -- "Goblin-Überfall auf Düsterburg"
    description     TEXT,                    -- "Am Morgen des 15. Tages..."
    importance      INT          NOT NULL DEFAULT 1,  -- 1 (Alltag) bis 5 (epochal)
    source_entity_id UUID,                   -- Wer hat das Event ausgelöst?
    metadata_json   JSONB,                   -- Zusatzdaten
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_entity_events_entity ON entity_events(entity_type, entity_id, created_at DESC);
```

### Event-Typen (Beispiele)

| Entity | Event-Typ | Bedeutung |
|---|---|---|
| **Region** | `MONSTER_INFESTATION` | Monsterplage in der Region |
| | `NATURAL_DISASTER` | Erdbeben, Überschwemmung |
| | `TRADE_AGREEMENT` | Handelsabkommen mit Nachbarregion |
| | `WAR_DECLARATION` | Krieg zwischen Fraktionen |
| **Ort** | `BANDIT_RAID` | Banditenüberfall |
| | `ROYAL_VISIT` | Königlicher Besuch |
| | `FESTIVAL` | Jahrmarkt, Erntedank |
| | `SIEGE` | Belagerung |
| | `MARKET_OPEN` | Neuer Markt eröffnet |
| **NPC** | `QUEST_COMPLETED` | NPC hat Auftrag erledigt |
| | `ARRIVED` | NPC in der Stadt angekommen |
| | `PROMOTION` | NPC wurde befördert |
| | `MARRIAGE` | NPC hat geheiratet |
| | `DEATH` | NPC gestorben |

---

## 3. Datenflüsse

### Wie entstehen Entity-Events?

```
[DM]           → POST /api/v1/entity-events
  └─ "Goblin-Überfall auf Düsterburg"

[KI-Bot]       → POST /api/v1/entity-events (via Service-Token)
  └─ "Schmied Karl hat königlichen Auftrag erhalten"

[Spieleraktion] → Game-Server → auto-entity-event
  └─ "Spieler besiegte Goblin-Anführer" → Region-Event

[Time Engine]  → Scheduled → auto-entity-event
  └─ "Jahrmarkt in Düsterburg beginnt" (wiederkehrend)
```

### KI-Nutzung

Beim Prompt-Bau für einen NPC lädt der Bot die letzten Ereignisse:

```python
events = api.get_entity_events(npc.location_id)  # Events für den Ort
events += api.get_entity_events(npc.location.region_id)  # Events für die Region
events += api.get_entity_events(npc.id)  # Events für den NPC selbst
```

→ LLM bekommt: *„In Düsterburg gab es letzte Woche einen Goblin-Überfall.
  Der Schmied hat einen königlichen Auftrag. Was tust du?"*

---

## 4. Regions-Detail

```sql
regions (
    id              UUID PK,
    world_id        UUID FK → worlds,
    name            VARCHAR(200),
    description     TEXT,
    history         TEXT,          -- Langtext: Gründung, wichtige Ereignisse
    danger_level    INT DEFAULT 1, -- 1 (friedlich) bis 10 (tödlich)
    climate         VARCHAR(50),   -- forest, desert, mountains, plains, swamp, coast, tundra
    resources       JSONB,         -- [{ type: "iron_ore", abundance: 3 }]
    factions        JSONB,         -- ["crown", "goblin_tribes"]
    population      INT,
    capital_id      UUID,          -- Hauptort (FK → locations)
    position_json   JSONB,         -- { bounds: [[x1,y1],[x2,y2]] }
    created_at, updated_at
)
```

---

## 5. Locations-Detail

```sql
locations (
    id              UUID PK,
    region_id       UUID FK → regions,
    type            VARCHAR(50),   -- village, town, city, castle, dungeon, ruin,
                                   -- temple, camp, tower, cave, port, mine
    name            VARCHAR(200),
    description     TEXT,
    history         TEXT,
    population      INT,
    wealth          INT DEFAULT 5, -- 1 (arm) bis 10 (reich)
    services        JSONB,         -- ["inn", "blacksmith", "alchemist", "trainer",
                                   --  "temple", "market", "stable", "library", "guild"]
    factions        JSONB,         -- ["crown", "merchant_guild"]
    is_capital      BOOLEAN,
    position_json   JSONB,         -- { x, y, map_id }
    created_at, updated_at
)
```

---

## 6. NPC-Erweiterung (via `entities.metadata_json`)

Bestehende `entities`-Tabelle wird um Metadaten erweitert:

```json
{
  "occupation": "blacksmith",
  "location_id": "uuid-der-schmiede",
  "schedule": {
    "active_during": ["day"],
    "location_id": "uuid"
  },
  "services_offered": ["sell_weapons", "repair", "buy_ore"],
  "price_modifier": 1.0,
  "faction_id": "uuid",
  "relationships": {
    "npc_uuid_a": "likes",
    "npc_uuid_b": "hates",
    "faction_uuid_c": "loyal"
  },
  "greeting": "Willkommen in meiner Schmiede!",
  "quests_available": ["quest_uuid_1"],
  "biography": "Karl ist seit 30 Jahren Schmied in Düsterburg..."
}
```

---

## 7. Services & Wirtschaft

| Ortstyp | NPC-Rollen | Verfügbare Dienste | Wohlstand |
|---|---|---|---|
| Dorf | Händler, Schmied, Gastwirt | Essen, einfache Waffen, Rast, Gerüchte | 2–5 |
| Stadt | Lehrer, Alchemist, Gildenmeister | Skills trainieren, Tränke, Fraktionsquests | 5–8 |
| Burg | Hauptmann, Hofmagier | Hauptquests, Rüstungen, Aufträge | 7–10 |
| Dungeon | — | Loot, XP | — |
| Tempel | Priester, Heiler | Heilen, Segen, Identifizieren | 3–6 |
| Lager | Nomadenhändler | Graumarkt, Informationen | 2–4 |

**Preisformel:**
```python
preis = basispreis × (1 + (wealth - 5) × 0.1) × npc.price_modifier
```

---

## 8. KI-Integration

Beim Prompt-Bau für NPC-Intents lädt der Bot:

1. NPC-Stammdaten (Name, Beruf, Persönlichkeit)
2. NPC-Ereignis-Log (letzte 5 Ereignisse)
3. Standort-Details (Location-Name, -Typ, -Wohlstand)
4. Standort-Ereignis-Log (letzte 5 Ereignisse)
5. Regions-Details (Gefahrenlevel, Fraktionen)
6. Regions-Ereignis-Log (letzte 5 Ereignisse)
7. Tageszeit + Wetter (aus Time Engine)

→ Der LLM-Prompt wird deutlich reichhaltiger: *„Du bist Schmied Karl in Düsterburg.
   Letzte Woche gab es einen Goblin-Überfall. Ein Fremder betritt deine Schmiede..."*

---

## 9. Tasks

| ID | Task | Beschreibung | Aufwand |
|---|---|---|---|
| **P6-T01** | Entity-Event-Log | `entity_events`-Tabelle, Migration, Repository, `EntityEventService`, REST-Endpunkte (POST/GET) | 2 Tage |
| **P6-T02** | Regionen-Modell | Migration `regions`, JPA-Entity, Repository, `RegionService` + `RegionController` (CRUD) | 2 Tage |
| **P6-T03** | Orte-Modell | Migration `locations`, JPA-Entity, Repository, `LocationService` + `LocationController` (CRUD) | 2 Tage |
| **P6-T04** | NPC-Erweiterung (Ort + Beruf) | `PATCH /entities/{id}` erlaubt `location_id`, `occupation`; Service-Endpunkt `GET /locations/{id}/npcs` | 2 Tage |
| **P6-T05** | Wirtschaft & Services | Preiskalkulation, Service-Abfrage pro Ort/NPC | 2 Tage |
| **P6-T06** | KI-Kontextaufbau Regionen | Bot lädt Entity-Events + Region/Location-Kontext in Prompt | 1 Tag |
| **P6-T07** | Quest-Generierung (einfach) | KI generiert Quests basierend auf Regionen-Zustand + Events | 3 Tage |

---

## 10. Verweise

- [`docs/WORLD-DEPTH.md`](WORLD-DEPTH.md) — dieses Dokument
- [`docs/DATA-MODEL.md`](DATA-MODEL.md) — Schema-Erweiterungen
- [`docs/AI-AGENT.md`](AI-AGENT.md) — Bot-Prompt-Kontext
- [`docs/ERROR-CODES.md`](ERROR-CODES.md) — Fehlercodes für neue Services
