# API-Referenz

> REST- und WebSocket-Endpunkte. Base URL: `http://localhost:8080/api/v1` (dev), `https://{host}/api/v1` (prod). Siehe [`ADR/008`](ADR/008-api-versioning.md) zur Versionierungs-Strategie.

---

## Konventionen

- Alle POST/PUT/Payloads sind `application/json`
- Authentifizierte Endpunkte benötigen `Authorization: Bearer {jwt}`
- **Versionierung:** Alle REST-Endpunkte liegen unter `/api/v1/...`. Bei Breaking Changes wird eine neue Major-Version parallel unterstützt (mind. 6 Monate alte bleibt). Siehe [`ADR/008`](ADR/008-api-versioning.md).
- **Internationalisierung:** Alle Endpunkte akzeptieren `Accept-Language`-Header (BCP 47, z. B. `de`, `en`). Fehlermeldungen und lokalisierte Inhalte werden in dieser Sprache zurückgegeben. Fallback-Locale: `en`. Siehe [`ADR/007`](ADR/007-internationalization-strategy.md).
- **Fehlercodes:** `code` ist maschinenlesbar und stabil — Katalog in [`ERROR-CODES.md`](ERROR-CODES.md). `message` ist lokalisiert via `Accept-Language`.
- Fehlerformat einheitlich:
  ```json
  {
    "error": {
      "code": "AUTH_INVALID_CREDENTIALS",
      "message": "E-Mail oder Passwort ist falsch",
      "details": [ ... ]
    }
  }
  ```
- Zeitstempel als ISO-8601 UTC

> **Hinweis zu Pfaden:** Sämtliche Endpunkt-Übersichten verwenden den vollständigen Pfad inkl. `/api/v1`-Prefix.

---

## 1. Auth (`/api/v1/auth`)

### `POST /api/v1/auth/register`
Öffentlicher Endpunkt. Erstellt User.

**Request:**
```json
{ "email": "user@example.com", "username": "hero", "password": "****" }
```

**Response 201:**
```json
{ "id": "uuid", "email": "user@example.com", "username": "hero", "role": "USER", "locale": "de" }
```

**Fehlercodes:** `AUTH_EMAIL_TAKEN`, `AUTH_USERNAME_TAKEN`, `USER_PROFILE_INVALID`

### `POST /api/v1/auth/login`
**Request:**
```json
{ "email": "user@example.com", "password": "****" }
```

**Response 200:**
```json
{ "accessToken": "...", "refreshToken": "...", "expiresIn": 86400 }
```

**Fehlercodes:** `AUTH_INVALID_CREDENTIALS`, `AUTH_USER_DISABLED`

### `POST /api/v1/auth/refresh`
**Request:** `{ "refreshToken": "..." }`
**Response 200:** wie `POST /api/v1/auth/login`
**Fehlercodes:** `AUTH_REFRESH_INVALID`, `AUTH_REFRESH_EXPIRED`

### `POST /api/v1/auth/service-login`
Bot-Service-Login. Siehe Sektion 15 und [`ADR/005`](ADR/005-ki-validation-layer.md).

**Request:** `{ "service_user": "lwe-bot", "service_password": "****" }`
**Response 200:** wie `/api/v1/auth/login`, aber Token hat Role `BOT`
**Fehlercodes:** `AUTH_SERVICE_TOKEN_INVALID`

---

## 2. User Preferences (`/api/v1/users/me/preferences`)

### `GET /api/v1/users/me/preferences` (auth)
Liefert User-Präferenzen, insb. `locale`.

**Response 200:**
```json
{
  "locale": "de",
  "available_locales": ["de", "en"]
}
```

**Fehlercodes:** `USER_NOT_FOUND`

### `POST /api/v1/users/me/preferences` (auth)
Aktualisiert User-Präferenzen. `locale` muss ein unterstützter BCP 47-Tag sein.

**Request:**
```json
{ "locale": "en" }
```

**Response 200:** aktualisierte Präferenzen.

**Fehlercodes:** `USER_LOCALE_UNAVAILABLE`, `I18N_LOCALE_UNAVAILABLE`, `USER_PROFILE_INVALID`

---

## 3. Game-Systems (`/api/v1/game-systems`)

### `POST /api/v1/game-systems` (auth)
Lädt ein neues Regelwerk hoch. Wird gegen JSON-Schema validiert (siehe [`RULES-SCHEMA.md`](RULES-SCHEMA.md)).

**Request:**
```json
{
  "name": "D20Lite",
  "rules_json": {
    "version": 1,
    "attributes": [ { "name": "stärke", "type": "INT", "min": 1, "max": 20, "default": 10 } ],
    "dice_mechanics": {
      "probe": "1d20 + mod",
      "combat": {
        "initiative": "1d20 + geschicklichkeit",
        "damage": "1d8 + stärke"
      }
    }
  }
}
```

**Response 201:**
```json
{ "id": "uuid", "name": "D20Lite", "version": 1, "active": true }
```

**Fehlercodes:** `GAME_SYSTEM_SCHEMA_INVALID`, `GAME_SYSTEM_ATTRIBUTE_REF_INVALID`, `GAME_SYSTEM_EXPRESSION_INVALID`, `GAME_SYSTEM_VERSION_CONFLICT`

### `GET /api/v1/game-systems` (auth)
Listet alle aktiven Regelwerke.

**Response 200:**
```json
[
  { "id": "uuid", "name": "D20Lite", "version": 1 },
  { "id": "uuid", "name": "TwoDicePool", "version": 1 }
]
```

### `GET /api/v1/game-systems/{id}` (auth)
Liefert vollständiges Regelwerk.

### `POST /api/v1/game-systems/{id}/validate` (auth)
Validiert ein geladenes Regelwerk erneut gegen das Schema.

**Response 200:** `{ "valid": true }` bzw. `{ "valid": false, "errors": [...] }`

---

## 4. Worlds (`/api/v1/worlds`)

Alle Endpunkte auth und user-scoped (Tenant-Isolation via JWT `user_id`, siehe [`ADR/004`](ADR/004-multitenancy-shared-schema.md)).

### `POST /api/v1/worlds`
**Request:**
```json
{
  "name": "Schattental",
  "game_system_id": "uuid",
  "settings_json": {
    "ai_mode": "suggest",
    "visibility": "fog_of_war",
    "grid": "hex",
    "language": "de",
    "time": {
      "mode": "hybrid",
      "tick_interval_real_seconds": 60,
      "tick_advance_game_minutes": 60,
      "calendar_system": "gregorian",
      "start_time": "2025-03-15T06:00:00",
      "paused": false,
      "day_starts_at_hour": 6
    }
  }
}
```

**Response 201:**
```json
{
  "id": "uuid",
  "name": "Schattental",
  "owner_id": "uuid",
  "game_system_id": "uuid",
  "settings_json": { ... },
  "current_game_time": "2025-03-15T06:00:00",
  "last_tick_at": "2025-03-15T06:00:00",
  "created_at": "2025-..."
}
```

**Fehlercodes:** `WORLD_NAME_REQUIRED`, `WORLD_SETTINGS_INVALID`, `WORLD_GAME_SYSTEM_INACTIVE`, `GAME_SYSTEM_NOT_FOUND`

### `GET /api/v1/worlds` → Liste eigener Welten
### `GET /api/v1/worlds/{id}` → Details (**Fehlercodes:** `WORLD_NOT_FOUND`, `WORLD_ACCESS_DENIED`)
### `PATCH /api/v1/worlds/{id}` → Update settings_json (nur Owner)
### `DELETE /api/v1/worlds/{id}` → Soft-delete (Phase 5)

### `POST /api/v1/worlds/{id}/members` (Owner)
**Request:** `{ "user_id": "uuid", "role": "PLAYER" }`
**Fehlercodes:** `WORLD_MEMBER_ALREADY`, `USER_NOT_FOUND`

### `GET /api/v1/worlds/{id}/members` → Liste der Mitglieder

---

## 5. Entities (`/api/v1/worlds/{worldId}/entities`)

### `POST /api/v1/worlds/{worldId}/entities`
**Request:**
```json
{
  "entity_type": "NPC",
  "name": "Goblin-Anführer Grishnak",
  "attributes_json": { "stärke": 14, "geschicklichkeit": 16, "intelligenz": 8 },
  "position_json": { "map_id": "uuid", "x": 15, "y": 22 },
  "metadata_json": {
    "personality": "aggressiv",
    "knowledge": [ "kennt Stärken des Stammes" ],
    "goals": [ "Stamm beschützen" ],
    "schedule": { "active_during": ["day", "dawn"], "sleeps_at_dusk": true }
  }
}
```

**Response 201:** komplettes Entity-Objekt.

**Fehlercodes:** `ENTITY_TYPE_INVALID`, `ENTITY_ATTRIBUTES_INVALID`, `ENTITY_POSITION_INVALID`, `ENTITY_FACTION_NOT_FOUND`

### `GET /api/v1/worlds/{worldId}/entities?type=NPC` → Filter
### `GET /api/v1/worlds/{worldId}/entities/{id}` (**Fehlercodes:** `ENTITY_NOT_FOUND`)
### `PATCH /api/v1/worlds/{worldId}/entities/{id}` → Update attributes/inventory/metadata

### `PATCH /api/v1/worlds/{worldId}/entities/{entityId}/attributes`
**Request:** `{ "staerke": 16 }` — merged in attributesJson
**Fehlercodes:** `ENTITY_NOT_FOUND`, `WORLD_ACCESS_DENIED`

### `PATCH /api/v1/worlds/{worldId}/entities/{entityId}/progression`
**Request:** `{ "experience_points": 1500 }`
**Fehlercodes:** `ENTITY_NOT_FOUND`, `WORLD_ACCESS_DENIED`

### `PATCH /api/v1/worlds/{worldId}/entities/{entityId}/skills`
**Request:** `{ "Athletik": 5, "Wahrnehmung": 3 }` — merged in skillsJson, überschreibt globale Skill-Boni
**Fehlercodes:** `ENTITY_NOT_FOUND`, `WORLD_ACCESS_DENIED`

### `PATCH /api/v1/worlds/{worldId}/entities/{entityId}/override`
**Request:** `{ "hp": 2, "ac": 1 }` — Formel-Overrides, additiv zu derivedValues
**Fehlercodes:** `ENTITY_NOT_FOUND`, `WORLD_ACCESS_DENIED`

### `POST /api/v1/worlds/{worldId}/entities/import`
**Request:** Wie `POST /api/v1/worlds/{worldId}/entities` (Create), erzeugt neue Entity aus Export-JSON
**Response 201:** Entity-Objekt
**Fehlercodes:** `ENTITY_TYPE_INVALID`, `WORLD_NOT_FOUND`

---

## 6. Inventory (`/api/v1/entities/{entityId}/inventory`)

### `POST /api/v1/entities/{entityId}/inventory/add`
**Request:** `{ "item_id": "uuid", "quantity": 3 }`
**Fehlercodes:** `INVENTORY_ITEM_NOT_FOUND`

### `POST /api/v1/entities/{entityId}/inventory/remove`
**Request:** `{ "item_id": "uuid", "quantity": 1 }`
**Fehlercodes:** `INVENTORY_INSUFFICIENT_QUANTITY`

### `POST /api/v1/entities/{entityId}/inventory/equip`
**Request:** `{ "item_id": "uuid", "slot": "weapon" }`
**Fehlercodes:** `INVENTORY_SLOT_OCCUPIED`, `INVENTORY_ITEM_TYPE_INVALID`, `INVENTORY_EQUIP_NOT_IN_INVENTORY`

### `POST /api/v1/entities/{entityId}/inventory/unequip`
**Request:** `{ "itemId": "uuid" }`

### `GET /api/v1/entities/{entityId}/inventory`
**Response:**
```json
{
  "items": [
    { "item": {...}, "quantity": 2, "equipped": false },
    { "item": {...}, "quantity": 1, "equipped": true, "slot": "weapon" }
  ],
  "computed_bonuses": { "armor_class": 16, "initiative": 1 }
}
```

### `POST /api/v1/entities/{entityId}/rest/short`
**Beschreibung:** Kurze Rast — HP-Heilung laut `rulesJson.dice_mechanics.combat.resting.short_rest`.
Unterstützt: `"full"`, `"50%"`, `"1d8+konstitution"`, `"5"` (flach) für HP; `"full"`/`"half"` für AP.
**Request:** `{}` (kein Body)
**Response 200:** `{ "message": "Short rest completed" }`
**Fehlercodes:** `ENTITY_NOT_FOUND`, `WORLD_ACCESS_DENIED`, `INVALID_HP_EXPR`

### `POST /api/v1/entities/{entityId}/rest/long`
**Beschreibung:** Lange Rast — volle Heilung laut `rulesJson.dice_mechanics.combat.resting.long_rest`.
**Request:** `{}` (kein Body)
**Response 200:** `{ "message": "Long rest completed" }`
**Fehlercodes:** `ENTITY_NOT_FOUND`, `WORLD_ACCESS_DENIED`, `INVALID_HP_EXPR`

---

## 7. Rolls (`/api/v1/rolls`)

### `POST /api/v1/rolls`
Führt eine Probe aus. Erzeugt `PROBE_ROLLED` Event + WS-Broadcast.

**Request:**
```json
{
  "world_id": "uuid",
  "entity_id": "uuid",
  "skill_id": "stärke",
  "modifier": 2,
  "target": 15
}
```

**Response 200:**
```json
{
  "roll_id": "uuid",
  "dice": [ 14 ],
  "modifier": 2,
  "total": 16,
  "target": 15,
  "success": true,
  "event_id": 123456
}
```

**Fehlercodes:** `ROLL_EXPRESSION_INVALID`, `ROLL_ATTRIBUTE_NOT_FOUND`, `ROLL_ENTITY_NOT_CHARACTER`, `ROLL_TARGET_REQUIRED`

---

## 8. Combat (`/api/v1/combat`)

### `POST /api/v1/combat/start`
**Request:**
```json
{ "world_id": "uuid", "participant_ids": ["uuid", "uuid", "uuid"] }
```

**Response 201:** Combat-Session mit Initiative-Reihenfolge.

### `POST /api/v1/combat/{sessionId}/next-turn`
**Fehlercodes:** `COMBAT_NOT_YOUR_TURN`, `COMBAT_NOT_ACTIVE`

### `POST /api/v1/combat/{sessionId}/action`
**Request:**
```json
{ "actor_id": "uuid", "action_type": "ATTACK", "target_id": "uuid", "item_id": "uuid" }
```

**Response 200:** Ergebnis + WS-Broadcast an `/topic/combat/{sessionId}`.

**Fehlercodes:** `COMBAT_AP_INSUFFICIENT`, `COMBAT_RANGE_INVALID`, `COMBAT_LINE_OF_SIGHT_BLOCKED`, `COMBAT_TARGET_INVALID`, `COMBAT_ACTION_TYPE_INVALID`, `COMBAT_NOT_ACTIVE`, `COMBAT_NOT_YOUR_TURN`

### `POST /api/v1/combat/{sessionId}/end`

---

## 9. Adventures (`/api/v1/adventures`)

### `POST /api/v1/adventures`
**Request:** `{ "world_id": "uuid", "name": "Die Höhle des Schreckens", "description": "..." }`

### `POST /api/v1/adventures/{id}/nodes`
**Request:** `{ "text": "...", "image_url": null, "is_end": false }`

### `POST /api/v1/adventures/{id}/nodes/{nodeId}/choices`
**Request:**
```json
{
  "label": "Tür eintreten",
  "target_node_id": "uuid",
  "skill_check": { "skill": "stärke", "modifier": 0 },
  "on_success_node_id": "uuid",
  "on_failure_node_id": "uuid"
}
```

### `POST /api/v1/adventures/{id}/start`
Startet den Adventure-Flow für einen Charakter.

**Request:** `{ "entity_id": "uuid" }`
**Response 200:** Erste Node + Status.
**Fehlercodes:** `ADVENTURE_NOT_FOUND`, `ADVENTURE_ALREADY_COMPLETED`

### `POST /api/v1/adventures/{id}/advance`
Wählt eine Choice und wertet ggf. Skill-Check aus.

**Request:** `{ "entity_id": "uuid", "choice_id": "uuid" }`

**Response 200:**
```json
{
  "next_node_id": "uuid",
  "skill_check_result": { "roll_total": 18, "success": true },
  "adventure_status": "ACTIVE"
}
```

**Fehlercodes:** `ADVENTURE_CHOICE_NOT_FOUND`, `ADVENTURE_PROGRESS_NOT_FOUND`, `ADVENTURE_NODE_TERMINAL`

---

## 10. World Time & Calendar (`/api/v1/worlds/{id}/time`)

> Steuert die In-Game-Zeit der Welt. Siehe [`ADR/009`](ADR/009-world-time-calendar-system.md) für Modi und Schema. DM kann jederzeit manuell „Tag ist vorbei" erklären, Owner kann Modus wechseln.

### `GET /api/v1/worlds/{id}/time` (auth, world member)
Liefert aktuelle Zeit und Status.

**Response 200:**
```json
{
  "current_game_time": "2025-03-15T14:30:00",
  "mode": "hybrid",
  "paused": false,
  "day_phase": "day",
  "is_daytime": true,
  "calendar_system": "gregorian"
}
```

### `POST /api/v1/worlds/{id}/time/advance` (auth, owner/DM)
Advance der Spielzeit. Interpretiert ISO-8601-Duration oder vereinfachte Strings.

**Request:**
```json
{ "by": "1 day" }
```

Alternative Werte: `"6 hours"`, `"30 minutes"`, `"PT8H"` (ISO-8601), `"dawn"`, `"noon"`, `"dusk"`, `"midnight"`.

**Response 200:** wie `GET` mit aktualisierter `current_game_time`. Publisht `TIME_ADVANCED` Event.

**Fehlercodes:** `TIME_ADVANCE_INVALID`, `TIME_PAUSED` (im reinen manual-Modus), `TIME_DM_OVERRIDE_REQUIRED`

### `POST /api/v1/worlds/{id}/time/set` (auth, owner/DM)
Springt zu absolutem Zeitpunkt.

**Request:**
```json
{ "to": "2025-03-16T08:00:00" }
```

**Response 200:** wie `GET`. Publish `TIME_ADVANCED` Event mit `trigger: "dm_set"`.

**Fehlercodes:** `TIME_SET_INVALID`

### `POST /api/v1/worlds/{id}/time/pause` (auth, owner/DM)
Pausiert automatische Ticks. Idempotent — kein Fehler, wenn schon pausiert.

**Response 200:** `{ "paused": true, "at": "..." }`. Publisht `TIME_PAUSED`.

### `POST /api/v1/worlds/{id}/time/resume` (auth, owner/DM)
Setzt automatische Ticks fort. Idempotent.

**Response 200:** `{ "paused": false, "at": "..." }`. Publisht `TIME_RESUMED`.

### `PATCH /api/v1/worlds/{id}/time/mode` (auth, owner)
Wechselt Zeit-Modus. Akzeptierte Werte: `"automatic"`, `"manual"`, `"hybrid"`. Publisht `TIME_MODE_CHANGED`.

**Request:**
```json
{ "mode": "automatic" }
```

**Fehlercodes:** `TIME_MODE_INVALID`

---

## 11. NPC Intents (`/api/v1/npc-intents`)

### `POST /api/v1/npc-intents` (auth, meist vom Bot via Service-Token)
**Request:**
```json
{
  "world_id": "uuid",
  "npc_id": "uuid",
  "intent_type": "ATTACK",
  "params_json": { "target_id": "uuid", "weapon_id": "uuid" },
  "reasoning": "NPC sieht Spielerfeuer, fühlt sich bedroht"
}
```

**Response 201:** Intent mit Status `pending`.

**Fehlercodes:** `INTENT_TYPE_INVALID`, `INTENT_PARAMS_INVALID`, `INTENT_VALIDATION_RULE`, `INTENT_VALIDATION_VISIBILITY`, `INTENT_VALIDATION_RANGE`, `INTENT_VALIDATION_RESOURCE`

### `GET /api/v1/npc-intents?worldId={id}&status=pending` (DM)
Listet pendente Intents für DM-Review.

### `POST /api/v1/npc-intents/{id}/approve` (DM, nur wenn `world.settings.ai_mode=suggest`)
Führt Intent aus.

**Fehlercodes:** `INTENT_ALREADY_PROCESSED`, `INTENT_APPROVAL_REQUIRED`

### `POST /api/v1/npc-intents/{id}/reject` (DM)
**Request:** `{ "reason": "Passt nicht zur Storyline" }`

**Fehlercodes:** `INTENT_ALREADY_PROCESSED`

---

## 12. World-Events (`/api/v1/worlds/{id}/events`)

### `GET /api/v1/worlds/{id}/events?since={eventId}&limit=100`
Gibt neue Events seit `eventId` zurück. Wird vom Bot gepollt.

**Response 200:**
```json
{
  "events": [
    {
      "id": 123456,
      "event_type": "FIRE_CREATED",
      "source_entity_id": "uuid",
      "target_entity_id": null,
      "payload_json": { "position": { "x": 15, "y": 22 }, "intensity": 1 },
      "created_at": "2025-..."
    }
  ],
  "last_event_id": 123456
}
```

**Fehlercodes:** `EVENT_NOT_FOUND`, `EVENT_SINCE_INVALID`, `EVENT_RATE_LIMIT`

---

## 13. Admin (`/api/v1/admin/*` — Phase 5, Rolle `ADMIN`)

- `GET /api/v1/admin/users` — User-Übersicht
- `GET /api/v1/admin/users/{id}` — User-Detail
- `PATCH /api/v1/admin/users/{id}` — Sperren/Rollen ändern (**Fehlercodes:** `ADMIN_TARGET_SELF`, `ADMIN_DEMOTE_LAST`)
- `GET /api/v1/admin/worlds/stats` — Heatmap der Welten je Activity
- `GET /api/v1/admin/bots/status` — Bot-Status pro Welt
- `POST /api/v1/admin/bots/{worldId}/restart` (**Fehlercodes:** `ADMIN_BOT_NOT_FOUND`)

---

## 14. WebSocket (STOMP)

### Verbindung
- Endpoint: `ws://localhost:8080/ws` (dev), `wss://{host}/ws` (prod)
- Subprotocol: `v11.stomp` (STOMP 1.1). STOMP 1.2 mit `ACCEPT-VERSION` für zukünftige Inkompatibilitäten — siehe [`ADR/008`](ADR/008-api-versioning.md).
- Auth: Header `Authorization: Bearer {jwt}` beim CONNECT

### Client → Server Channels (`/app/...`)

| Channel | Richtung | Payload | Zweck |
|---|---|---|---|
| `/app/v1/rolls/{worldId}` | Client → Server | `RollRequest` | Online Würfelwurf (gleichwertig zu `POST /api/v1/rolls`) |
| `/app/v1/chat/{worldId}` | Client → Server | `{ message: "..." }` | Chat-Nachricht |
| `/app/v1/token/move/{mapId}` | Client → Server | `{ token_id, x, y }` | Token-Bewegung |
| `/app/v1/fog/update/{mapId}` | DM → Server | `{ polygon: [...] }` | Fog of War-Anpassung |

### Server → Client Topics (`/topic/...`)

| Topic | Empfänger | Payload |
|---|---|---|
| `/topic/world/{worldId}` | Alle in Welt | `WorldEvent` (Proben, Aktion, NPC-Intent-Ausführung) |
| `/topic/combat/{sessionId}` | Alle in Kampf | `CombatUpdate` (aktueller Turn, Teilnehmer) |
| `/topic/chat/{worldId}` | Alle in Welt | `ChatMessage` |
| `/topic/map/{mapId}` | Alle auf Karte | `TokenMove` / `FogUpdate` |
| `/topic/dm/intents/{worldId}` | Nur DM | `NpcIntent` (pending) |
| `/topic/world/{worldId}/time` | Alle in Welt | `TimeUpdate` (`TIME_ADVANCED`, `TIME_PAUSED`, `TIME_RESUMED`, `TIME_MODE_CHANGED`) |

### Beispiel-STOMP-Frame (Subscription)
```
SUBSCRIBE
id:sub-0
destination:/topic/world/abc-123
Authorization: Bearer {jwt}
```

---

## 15. Bot-Service-Token

Der AI-Bot benötigt **Service-Authentifizierung**:

- Eigenes Service-Token via `POST /api/v1/auth/service-login` mit Krankenpaar aus `BOT_SERVICE_USER` + `BOT_SERVICE_PASSWORD` (nur vom Admin anlegbar)
- Token wird im Bot gecacht und pro Request mitgeliefert
- Service-Token hat Role `BOT`, darf nur `/api/v1/npc-intents` + `/api/v1/worlds/{id}/events` aufrufen
- Siehe [`ADR/005`](ADR/005-ki-validation-layer.md)

---

## 16. Offene Punkte (spätere Phasen)

- Statistik-Endpunkte für Metriken
- S3-kompatibles Asset-Storage für Map-Hintergründe
- File-Upload für Charakter-Portraits
- WebHook-Registrierung für Dritt-Systeme
- Custom-Kalender-Systeme (Fantasy) — Phase 5+

---

## 17. Verweise

- [`ADR/008`](ADR/008-api-versioning.md) — API-Versionierungs-Strategie
- [`ADR/007`](ADR/007-internationalization-strategy.md) — i18n via `Accept-Language`
- [`ADR/009`](ADR/009-world-time-calendar-system.md) — Weltzeit & Kalender-System
- [`ERROR-CODES.md`](ERROR-CODES.md) — Vollständiger Fehlercode-Katalog