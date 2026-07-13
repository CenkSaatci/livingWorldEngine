# ADR-009: Weltzeit & Kalender-System (Time Engine)

- **Status:** Accepted
- **Date:** 2025-07-12

## Kontext

Eine „lebende Welt" lebt nicht nur über NPC-Aktionen, sondern auch über die **Zeit**. Aktuell hat LWE keine Vorstellung von „in-game time": ein NPC kann nicht „nachts schlafen", Händler können nicht „am Vormittag öffnen", eine Belagerung kann nicht „drei Tage dauern". Die Lebensähnlichkeit der Welt ist unvollständig.

Anforderungen vom Stakeholder (Projektinhaber):
- **Automatische Zeit soll laufen** — konfigurierbar, wie schnell In-Game-Zeit vergeht
- **DM soll manuell „ein Tag ist vorbei" erklären können** — Override der automatischen Zeit
- **Kombimodus** — Automatisch UND DM kann eingreifen

## Entscheidung

LWE bekommt eine **Time Engine** als eigener Service-Bereich im Backend, mit **drei Modi**:

### `automatic`
- Zeit advanced automatisch im Hintergrund, basierend auf `tick_interval_real_seconds` → `tick_advance_game_minutes`
- DM-Eingriff nicht nötig

### `manual`
- Zeit steht still, bis DM `POST /api/v1/worlds/{id}/time/advance` oder `/time/set` aufruft
- DM erklärt „Tag ist vorbei" → `advance { by: "1 day" }`

### `hybrid` (Default)
- Zeit läuft automatisch, DM kann aber jederzeit:
  - **Pause** (`/time/pause`) — friert aktuelle Zeit ein
  - **Resume** (`/time/resume`)
  - **Advance** (`/time/advance`) — zusätzlich zur normalen Tick-Logik
  - **Set** (`/time/set`) — springt zu bestimmtem Zeitpunkt
- DM-Override hat Vorrang; nach Set oder Advance läuft die normale Tick-Logik weiter (außer pausiert)

## Schema

### `worlds.current_game_time` (neue dedizierte Spalte)
- Typ `TIMESTAMPTZ`
- In-Game-Zeit der Welt (in Welt-eigenem Kalender-System interpretiert)
- NULL = Welt hat noch nicht begonnen; erstes Set/Nächstes-Tick setzt initialen Wert
- Dedizierte Spalte statt JSONB, da häufige Updates und Indexing-Relevanz für zeitbasierte NPC-Logik

### `worlds.settings_json.time` (Konfiguration)
```json
{
  "ai_mode": "hybrid",
  "visibility": "fog_of_war",
  "grid": "hex",
  "language": "de",
  "time": {
    "mode": "automatic | manual | hybrid",
    "tick_interval_real_seconds": 60,
    "tick_advance_game_minutes": 60,
    "calendar_system": "gregorian",
    "start_time": "2025-03-15T06:00:00",
    "paused": false,
    "day_starts_at_hour": 6
  }
}
```

| Feld | Typ | Default | Beschreibung |
|---|---|---|---|
| `mode` | enum | `hybrid` | einer der drei Modi |
| `tick_interval_real_seconds` | int | 60 | Wie oft (Echtsekunden) ein Tick passiert, wenn nicht pausiert. `0` disable |
| `tick_advance_game_minutes` | int | 60 | Wieviel In-Game-Minuten pro Tick |
| `calendar_system` | string | `gregorian` | `gregorian` supported; `custom` kommt später (Fantasy-Kalender) |
| `start_time` | ISO-8601 | verpflichtend | Initiale In-Game-Zeit |
| `paused` | bool | `false` | Pausiert automatische Ticks |
| `day_starts_at_hour` | int | 6 | Stunde, ab der `is_daytime` gilt (für NPC-Verhalten) |

## API-Endpunkte

Alle unter `/api/v1/worlds/{id}/time/...`:

| Endpunkt | Methode | Bedeutung |
|---|---|---|
| `/time` | GET | Liefert `{ current_game_time, mode, paused, is_daytime, day_phase }` |
| `/time/advance` | POST | `{ by: "1 day" | "6 hours" | "30 minutes" | "PT8H" (ISO-8601 Duration) }` |
| `/time/set` | POST | `{ to: "2025-03-16T08:00:00" }` |
| `/time/pause` | POST | Setzt `paused: true` |
| `/time/resume` | POST | Setzt `paused: false` |
| `/time/mode` | PATCH | `{ mode: "automatic" | "manual" | "hybrid" }` —_owner-only |

`advance` und `set` sind nur Owner/DM zulässig (siehe [`ADR/004`](ADR/004-multitenancy-shared-schema.md)).

### Dauer-Syntax
- ISO-8601 Duration: `P1D` (1 Tag), `PT8H` (8 Stunden), `PT30M` (30 Minuten)
- oder vereinfacht: `"1 day"`, `"6 hours"`, `"30 minutes"`, `"dawn"`, `"noon"`, `"dusk"`, `"midnight"` (Roundings auf Tag-Phasen)

## Berechnung `day_phase`

`day_phase` ist ein **derived value** (nicht gespeichert):
- `dawn` (Morgendämmerung): `day_starts_at_hour − 1` bis `day_starts_at_hour`
- `day` (Tag): `day_starts_at_hour` bis `day_starts_at_hour + 11`
- `dusk` (Abenddämmerung): `day_starts_at_hour + 11` bis `day_starts_at_hour + 12`
- `night` (Nacht): sonst
- `is_daytime` = `(day_phase == "day" || day_phase == "dawn")`

## Events

| Event-Type | Wann | `payload_json` |
|---|---|---|
| `TIME_ADVANCED` | Jedes Mal, wenn `current_game_time` sich signifikant ändert (>= 1 Tick oder manuelles Advance) | `{ from, to, by, trigger: "tick" | "dm_advance" | "dm_set", day_phase }` |
| `TIME_PAUSED` | Übergang `paused: true` | `{ at }` |
| `TIME_RESUMED` | Übergang `paused: false` | `{ at }` |
| `TIME_MODE_CHANGED` | Mode-Wechsel | `{ from, to }` |

`TIME_ADVANCED` ist ein reguläres `WorldEvent` — Bot kann es polllen und NPCs darauf reagieren lassen (z. B. „Es wird Nacht — NPC geht schlafen").

## Throttling der Events

Automatische Ticks erzeugen schnell viele `TIME_ADVANCED` Events. Strategie:
- Tick-Intervalle unter 10 s → Bot aggregiert (nicht jeder Tick einzeln gepublished)
- Bot-Filter: ignoriert `TIME_ADVANCED` mit `trigger=="tick" && by_minutes < 30`
- Owner setzt Tick-Intervall pragmatisch (60 s real → 60 min in-game ist typisch)

## NPC-Integration

### NPC-Kontext (AI-Bot)
Prompt bekommt zusätzlich:
- `current_game_time` (ISO-8601, formatiert per Lokalisierung)
- `day_phase` (`dawn`/`day`/`dusk`/`night`)
- `is_daytime` (bool)

### NPC-Metadaten können Zeit-Präferenz enthalten
`entities.metadata_json` kann enthalten:
```json
{
  "schedule": {
    "active_during": ["day", "dawn"],
    "sleeps_at_dusk": true
  }
}
```

NPC-Template-Prompts können dies referenzieren:
```text
Tageszeit: {{ time.day_phase }} ({{ time.current_game_time }})
NPC ist aktiv während: {{ npc.metadata.schedule.active_during | join(", ") }}
```

## DM-UI

- In Status Bar: **Weltzeit-Anzeige** (z.B. `14:30 — Tag`, lokalisiert formatiert)
- DM-Only-Buttons in Toolbar:
  - ⏸ Pause / ▶ Resume
  - ⏩ Advance (Dropdown: +1 Stunde / +6 Stunden / +1 Tag / bis Morgens / bis Abends)
  - 📅 Set (Modal mit Time-Picker)
- Spieler sehen die Zeit, können sie nicht steuern

## Implementations-Hinweise

### Time-Service (Spring)
```java
@Service
public class WorldTimeService {
    @Scheduled(fixedDelayString = "${lwe.time.tick_check_interval_ms:1000}")
    public void tickAllWorlds() {
        for (World w : worlds.findActiveWithAutoTick()) {
            if (w.settings().time().paused()) continue;
            if (now - w.lastTickAt() >= w.settings().time().tick_interval_real_seconds()) {
                advance(w, byMinutes(w.settings().time().tick_advance_game_minutes()), trigger="tick");
            }
        }
    }
    
    public void advance(World w, Duration d, String trigger) { ... }
    public void set(World w, Instant to) { ... }
    public DayPhase dayPhase(World w) { ... }
}
```

- `@Scheduled`-Task läuft jede Sekunde, prüft alle Welten, elkelt needed Ticks
- World-spezifisch persistiert `last_tick_at` und `current_game_time`

### Persistenz
- Neue Spalte `current_game_time TIMESTAMPTZ` in `worlds` (siehe [`DATA-MODEL.md`](../DATA-MODEL.md))
- Neue Spalte `last_tick_at TIMESTAMPTZ` in `worlds`
- Migration `V007__world_time.sql`

## Konsequenzen

**Positiv:**
- Welt tickt realistisch — NPCs können tageszeitabhängig agieren
- DM-Override möglich, aber nicht zwingend
- Generische Time-Engine — Fantasy-Kalender später möglich
- NPCs können langfristige Pläne haben („in 3 Tagen ist der Markt")

**Negativ:**
- zusätzliche Komplexität im Backend (Scheduled Tasks, Event-Throttling)
- `current_game_time` ist pro Welt eine那张 hochfrequent-updated Spalte — separate Cache-Strategie nötig (Redis in Phase 5)
- LLM-Prompts werden etwas länger (Zeit-Infos)

## Phasen-Zuordnung

- **Phase 2 (P2-T09):** Time-Service (Automatic + Manual), `current_game_time`-Spalte, `TIME_ADVANCED`-Event
- **Phase 3:** Status Bar Weltzeit-Anzeige, DM-Toolbar-Buttons
- **Phase 4:** Zeit-Kontext in NPC-Prompt integrieren
- **Phase 5:** Custom-Kalender-System (Fantasy)

## Verweise

- [`DATA-MODEL.md`](../DATA-MODEL.md) — `worlds.current_game_time` + `settings_json.time`
- [`API.md`](../API.md) — `/api/v1/worlds/{id}/time/*` Endpunkte
- [`AI-AGENT.md`](../AI-AGENT.md) — NPC-Kontext um Tageszeit erweitert
- [`UI-UX.md`](../UI-UX.md) — DM-Toolbar und Status-Bar
- [`TASKS.md`](../TASKS.md) — P2-T09 (Time Engine)