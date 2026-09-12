# Fehlercode-Katalog

> Stabile, maschinenlesbare Fehlercodes für die LWE API. Diese Codes sind **prechten Stable** — sie ändern sich nicht zwischen Releases. Das menschenlesbare `message`-Feld wird via `Accept-Language` lokalisiert (siehe [`ADR/007`](ADR/007-internationalization-strategy.md)).

---

## 1. Konzept

Jede Fehlerantwort der API folgt dem in [`API.md`](API.md) definierten Format:

```json
{
  "error": {
    "code": "AUTH_INVALID_CREDENTIALS",
    "message": "E-Mail oder Passwort ist falsch",
    "details": [
      { "field": "password", "issue": "incorrect" }
    ]
  }
}
```

- `code` — maschinenlesbar, konstant, **Großbuchstaben mit Unterstrich**, Gruppierung via Prefix
- `message` — lokalisiert gemäß `Accept-Language`
- `details` — optional, feldspezifische Details (z. B. Validierungsfehler pro Feld)

---

## 2. Konvention für Codes

- Format: `<DOMAIN>_<SPECIFIC>` in UPPER_SNAKE_CASE
- Domänen-Prefixes:
  - `AUTH_` — Authentifizierung, JWT, Tokens
  - `USER_` — User-Konto, Präferenzen
  - `WORLD_` — Welten, Mitglieder, Limits
  - `ENTITY_` — Charaktere, NPCs, Fraktionen
  - `FACTION_` — Fraktionen, Diplomatie
  - `GAME_SYSTEM_` — Regelwerke
  - `ABILITY_` — Fähigkeiten (inkl. `ALREADY_ASSIGNED`, `NOT_ASSIGNED`)
  - `ITEM_` / `SESSION_` — Items, Spiel-Sessions
  - `LOCATION_` / `REGION_` / `QUEST_` — Orte, Regionen, Quests
  - `INVITE_` / `ALREADY_MEMBER` — Welt-Einladungen
  - `LEVEL_` / `ATTRIBUTE_*` — Level-Ups, Attributpunkte
  - `CAMPAIGN_` / `MEMBER_*` / `DM_*` — Kampagnen, Mitgliedschaft, DM-Rechte
  - `INVENTORY_` — Inventar, Items, Equip
  - `ROLL_` — Würfelproben
  - `COMBAT_` — Kampf, Turns, Aktionen
  - `ADVENTURE_` — Abenteuer-Nodes, Choices, Progress (inkl. `NODE_*`)
  - `INTENT_` — NPC-Intent, Validierung
  - `EVENT_` — Welt-Events
  - `TIME_` — Weltzeit, Kalender, Ticks
  - `WEATHER_` — Regions-Wetter
  - `ADMIN_` — Admin-Endpunkte
  - `I18N_` — Lokalisierung
  - `SYSTEM_` — Systemfehler (5xx)
  - `RATE_*` / `ROUTE_*` — Rate-Limits, Routing
  - `WS_` — WebSocket / STOMP

---

## 3. Vollständige Code-Liste

### 3.1 Authentifizierung (`AUTH_*`)

| Code | HTTP | Bedeutung |
|---|---|---|
| `AUTH_INVALID_CREDENTIALS` | 401 | E-Mail oder Passwort falsch |
| `AUTH_TOKEN_EXPIRED` | 401 | Access-Token abgelaufen — Client soll Refresh verwenden |
| `AUTH_TOKEN_INVALID` | 401 | Token_signatur ungültig oder nicht parsbar |
| `AUTH_REFRESH_INVALID` | 401 | Refresh-Token ungültig oder widerrufen |
| `AUTH_REFRESH_EXPIRED` | 401 | Refresh-Token abgelaufen — User muss neu einloggen |
| `AUTH_ROLE_INSUFFICIENT` | 403 | Rolle reicht für Endpunkt nicht aus (z. B. Nicht-Admin) |
| `AUTH_USER_DISABLED` | 403 | User-Konto gesperrt |
| `AUTH_EMAIL_TAKEN` | 409 | E-Mail bei Registrierung bereits vergeben |
| `AUTH_USERNAME_TAKEN` | 409 | Username bereits vergeben |
| `AUTH_USER_NOT_FOUND` | 400 | User zu Token/Verify-Link nicht vorhanden |
| `AUTH_ALREADY_VERIFIED` | 400 | E-Mail wurde bereits verifiziert |
| `AUTH_VERIFICATION_INVALID` | 400 | Verify-Token ungültig |
| `AUTH_VERIFICATION_EXPIRED` | 400 | Verify-Token abgelaufen |
| `AUTH_RATE_LIMITED` | 429 | Zu viele Login-Fehlversuche (pro IP) |
| `AUTH_SERVICE_TOKEN_INVALID` | 401 | Bot-Service-Token ungültig |

### 3.2 User (`USER_*`)

| Code | HTTP | Bedeutung |
|---|---|---|
| `USER_NOT_FOUND` | 404 | User mit ID nicht vorhanden |
| `USER_PROFILE_INVALID` | 400 | Profil-Payload ungültig |
| `USER_LOCALE_UNAVAILABLE` | 400 | Angeforderte Locale wird nicht unterstützt |

### 3.3 Welten & Mitglieder (`WORLD_*`)

| Code | HTTP | Bedeutung |
|---|---|---|
| `WORLD_NOT_FOUND` | 404 | Welt existiert nicht oder nicht für User sichtbar |
| `WORLD_NAME_REQUIRED` | 400 | Name fehlt |
| `WORLD_SETTINGS_INVALID` | 400 | `settings_json` invalide (z. B. unbekannter `ai_mode`) |
| `WORLD_ACCESS_DENIED` | 403 | User ist nicht Mitglied der Welt |
| `WORLD_OWNER_REQUIRED` | 403 | Nur Owner darf diese Aktion (z. B. löschen) |
| `WORLD_MEMBER_ALREADY` | 409 | User ist bereits Mitglied |
| `INVALID_VISIBILITY` | 400 | Sichtbarkeit nicht PRIVATE/INVITE_ONLY/PUBLIC |
| `METHOD_NOT_ALLOWED` | 405 | HTTP-Methode für die Route nicht erlaubt |
| `WORLD_MEMBER_LIMIT` | 403 | Mitglieder-Limit erreicht (Phase 5 Konfigurierbar) |
| `WORLD_LIMIT_REACHED` | 403 | Welten-Limit des Plans erreicht |
| `WORLD_GAME_SYSTEM_INACTIVE` | 422 | Referenziertes Regelwerk ist deaktiviert |
| `MAP_NOT_FOUND` | 404 | Karte zu dieser Welt nicht vorhanden |

### 3.4 Entities (`ENTITY_*`)

| Code | HTTP | Bedeutung |
|---|---|---|
| `ENTITY_NOT_FOUND` | 404 | Entity (PC/NPC/Fraktion) nicht vorhanden |
| `ENTITY_TYPE_INVALID` | 400 | `entity_type` nicht in (`PC`, `NPC`, `FACTION`) |
| `ENTITY_ATTRIBUTES_INVALID` | 400 | `attributes_json` passt nicht zum Welt-Regelwerk |
| `ENTITY_POSITION_INVALID` | 400 | Position außerhalb der Karte oder belegt |
| `ENTITY_FACTION_NOT_FOUND` | 400 | Referenzierter `faction_id` existiert nicht |
| `UNKNOWN_CONDITION` | 422 | Zustand nicht im Katalog des Systems (`rules.conditions`) |
| `FATE_NONE_LEFT` | 422 | Keine Schicksalspunkte mehr zum Ausgeben |

### 3.5 Game Systems (`GAME_SYSTEM_*`)

| Code | HTTP | Bedeutung |
|---|---|---|
| `GAME_SYSTEM_ACCESS_DENIED` | 403 | Kein Schreib-/Lesezugriff (Owner/Admin/Public-/Share-Regeln, F8/T33-05) |
| `GAME_SYSTEM_SHARE_EXISTS` | 409 | System ist fuer den Nutzer bereits freigegeben |
| `GAME_SYSTEM_SHARE_NOT_FOUND` | 404 | Freigabe existiert nicht |
| `GAME_SYSTEM_SHARE_SELF` | 422 | Owner kann sich nicht selbst freigeben |
| `GAME_SYSTEM_PUBLIC_NO_SHARE_NEEDED` | 422 | PUBLIC-Systeme brauchen keine Freigabe |

| Code | HTTP | Bedeutung |
|---|---|---|
| `GAME_SYSTEM_NOT_FOUND` | 404 | Regelwerk nicht vorhanden |
| `GAME_SYSTEM_SCHEMA_INVALID` | 400 | JSON nicht schema-konform |
| `GAME_SYSTEM_ATTRIBUTE_REF_INVALID` | 400 | Skill/Dice-Expression referenziert unbekanntes Attribut |
| `GAME_SYSTEM_EXPRESSION_INVALID` | 400 | Dice-Expression syntaktisch falsch (z. B. `1d20+@foo`) |
| `GAME_SYSTEM_VERSION_CONFLICT` | 409 | Versionsnummer kollidiert mit bestehendem Regelwerk |
| `FORMULA_EXPRESSION_INVALID` | 400 | Formel-Ausdruck syntaktisch falsch oder unbekannte Variable (`FormulaEvaluator`) |

### 3.6 Inventar (`INVENTORY_*`)

| Code | HTTP | Bedeutung |
|---|---|---|
| `INVENTORY_ITEM_NOT_FOUND` | 404 | Item nicht vorhanden |
| `INVENTORY_INSUFFICIENT_QUANTITY` | 422 | Versucht mehr zu entfernen als vorhanden |
| `INVENTORY_SLOT_OCCUPIED` | 409 | Equip-Slot bereits belegt |
| `INVENTORY_ITEM_TYPE_INVALID` | 422 | Item-Typ passt nicht zum Slot (z. B. Waffe in Rüstung-Slot) |
| `INVENTORY_WEIGHT_EXCEEDED` | 422 | Gewichtslimit überschritten |
| `INVENTORY_EQUIP_NOT_IN_INVENTORY` | 422 | Item nicht im Inventar des Charakters |
| `INVENTORY_NOT_CONSUMABLE` | 400 | `use` auf nicht-verbrauchbarem Item |
| `INVENTORY_NO_EFFECT` | 400 | Consumable hat keinen definierten Use-Effekt |

### 3.7 Proben / Würfel (`ROLL_*`)

| Code | HTTP | Bedeutung |
|---|---|---|
| `ROLL_EXPRESSION_INVALID` | 400 | Ausdruck kann nicht geparsed werden |
| `ROLL_ATTRIBUTE_NOT_FOUND` | 422 | Referenziertes Attribut existiert am Charakter nicht |
| `ROLL_ENTITY_NOT_CHARACTER` | 422 | Entity ist keine Probe-fähige Figur (z. B. Fraktion) |
| `ROLL_TARGET_REQUIRED` | 400 | Probe benötigt ein Target-Wert, aber keins übergeben |
| `INVALID_INPUT` | 400 | Ungültiges Argument/Parameter (z. B. nicht parsebare Dice-Expression, malformed UUID) |

### 3.8 Combat (`COMBAT_*`)

| Code | HTTP | Bedeutung |
|---|---|---|
| `COMBAT_NOT_FOUND` | 404 | Kampf-Session existiert nicht |
| `COMBAT_NOT_ACTIVE` | 422 | Kampf ist bereits beendet |
| `COMBAT_NOT_YOUR_TURN` | 422 | Actor ist nicht der aktuelle Turn-Inhaber |
| `COMBAT_AP_INSUFFICIENT` | 422 | Nicht genug Action Points für Aktion |
| `COMBAT_RANGE_INVALID` | 422 | Ziel außerhalb der Waffenreichweite |
| `COMBAT_LINE_OF_SIGHT_BLOCKED` | 422 | Sichtlinie durch Fog of War / Wand blockiert |
| `COMBAT_TARGET_INVALID` | 422 | Ziel-Entity existiert oder ist verbündet |
| `COMBAT_TARGET_DEFEATED` | 422 | Ziel bereits besiegt (Heilung weiter erlaubt) |
| `COMBAT_ACTOR_DEFEATED` | 422 | Actor besiegt — kann nicht handeln |
| `COMBAT_ACTION_TYPE_INVALID` | 400 | `action_type` nicht bekannt |
| `COMBAT_INSUFFICIENT_PARTICIPANTS` | 400 | Weniger als 2 Teilnehmer beim Kampfstart |
| `COMBAT_MANEUVER_UNKNOWN` | 422 | Manöver nicht in `dice_mechanics.combat.maneuvers` konfiguriert |

### 3.9 Adventures (`ADVENTURE_*`)

| Code | HTTP | Bedeutung |
|---|---|---|
| `ADVENTURE_NOT_FOUND` | 404 | Abenteuer nicht vorhanden |
| `ADVENTURE_NODE_NOT_FOUND` | 404 | Node nicht vorhanden |
| `ADVENTURE_CHOICE_NOT_FOUND` | 404 | Choice nicht vorhanden |
| `ADVENTURE_PROGRESS_NOT_FOUND` | 404 | Kein aktiver Fortschritt für diesen Charakter |
| `ADVENTURE_ALREADY_COMPLETED` | 422 | Abenteuer bereits abgeschlossen |
| `ADVENTURE_NODE_TERMINAL` | 422 | Node hat keine Choices mehr (`is_end`) |
| `NODE_NOT_FOUND` | 400 | Adventure-Node existiert nicht |
| `NODE_NOT_IN_ADVENTURE` | 422 | Node gehört nicht zu diesem Abenteuer |

### 3.10 NPC Intents (`INTENT_*`)

| Code | HTTP | Bedeutung |
|---|---|---|
| `INTENT_NOT_FOUND` | 404 | Intent existiert nicht |
| `NPC_NOT_FOUND` | 404 | NPC existiert nicht |
| `INTENT_WORLD_MISMATCH` | 422 | NPC/Kampagne gehören nicht zur angegebenen Welt |
| `QUEST_TYPE_INVALID` | 400 | Quest-Typ nicht kill/fetch/escort/deliver/explore/talk |
| `CAMPAIGN_NOT_FOUND` | 404 | Kampagne existiert nicht |

| Code | HTTP | Bedeutung |
|---|---|---|
| `INTENT_NOT_FOUND` | 404 | Intent nicht vorhanden |
| `INTENT_TYPE_INVALID` | 400 | `intent_type` nicht in (`ATTACK`, `MOVE`, `SPEAK`, `USE_ITEM`, `IDLE`) |
| `INTENT_PARAMS_INVALID` | 400 | `params_json` fehlerhaft (z. B. `target_id` fehlt bei `ATTACK`) |
| `INTENT_VALIDATION_RULE` | 422 | Validator Stage 1: Regelverletzung (AP, Reichweite) |
| `INTENT_VALIDATION_VISIBILITY` | 422 | Validator Stage 2: Sicht verdeckt |
| `INTENT_VALIDATION_RANGE` | 422 | Validator Stage 3: Außerhalb Reichweite |
| `INTENT_VALIDATION_RESOURCE` | 422 | Validator Stage 4: Ressource fehlt (Item/HP) |
| `INTENT_NOT_PENDING` | 409 | Intent wurde bereits approved/rejected/executed |
| `INTENT_APPROVAL_REQUIRED` | 422 | Welt ist im `suggest`-Modus, DM-Approval fehlt |
| `INTENT_REJECTED_BY_DM` | 422 | DM hat Intent abgelehnt (in `rejection_reason` begründet) |

### 3.11 Events (`EVENT_*`)

| Code | HTTP | Bedeutung |
|---|---|---|
| `EVENT_NOT_FOUND` | 404 | Event-ID nicht vorhanden |
| `EVENT_SINCE_INVALID` | 400 | `since`-Parameter negativ oder nicht numerisch |
| `EVENT_RATE_LIMIT` | 429 | Bot pollt zu schnell (Rate-Limit pro Welt) |

### 3.12 Weltzeit / Kalender (`TIME_*`)

| Code | HTTP | Bedeutung |
|---|---|---|
| `TIME_MODE_INVALID` | 400 | `time.mode` nicht in (`automatic`, `manual`, `hybrid`) |
| `TIME_ADVANCE_INVALID` | 400 | Advance-Wert negativ oder nicht interpretierbar |
| `TIME_SET_INVALID` | 400 | `to`-Wert liegt vor Welt-Start oder ist nicht parsebar |
| `TIME_CALENDAR_INVALID` | 400 | `calendar_system` nicht unterstützt |
| `TIME_PAUSED` | 422 | Operation nicht möglich, weil Zeit pausiert ist |
| `TIME_NOT_PAUSED` | 422 | Resume aufgerufen, aber Zeit läuft bereits |
| `TIME_DM_OVERRIDE_REQUIRED` | 422 | Im `manual`-Modus ist automatisches Advance verboten |
| `TIME_NOT_FOUND` | 404 | Welt für Zeit-Operation nicht vorhanden |
| `TIME_ACCESS_DENIED` | 403 | Nur Owner darf Zeit steuern |
| `TIME_CONFIG_MISSING` | 404 | Keine Zeit-Konfiguration in `settings_json` |
| `TIME_CONFIG_INVALID` | 500 | Zeit-Konfiguration konnte nicht gelesen/geschrieben werden |

### 3.13 Admin (`ADMIN_*`)

| Code | HTTP | Bedeutung |
|---|---|---|
| `ADMIN_TARGET_SELF` | 422 | Admin darf sich nicht selbst sperren |
| `ADMIN_DEMOTE_LAST` | 422 | Letzter Admin kann nicht degrade werden |
| `ADMIN_BOT_NOT_FOUND` | 404 | Bot-Instanz für diese Welt nicht gefunden |

### 3.14 Internationalisierung (`I18N_*`)

| Code | HTTP | Bedeutung |
|---|---|---|
| `I18N_LOCALE_UNAVAILABLE` | 400 | Angeforderte Locale existiert nicht (nicht in `available_locales`) |

### 3.15 Systemfehler (`SYSTEM_*`)

| Code | HTTP | Bedeutung |
|---|---|---|
| `SYSTEM_INTERNAL_ERROR` | 500 | Unerwarteter Fehler im Server (mit `request_id` für Logs) |
| `SYSTEM_DATABASE_UNAVAILABLE` | 503 | Datenbank nicht erreichbar |
| `SYSTEM_LLM_UNAVAILABLE` | 503 | AI-Bot kann LLM nicht erreichen (delegiert an Backend) |
| `SYSTEM_DEPENDENT_SERVICE_DOWN` | 503 | Redis / andere abhängige Services nicht erreichbar |

### 3.16 WebSocket / STOMP (`WS_*`)

| Code | HTTP/sichtung | Bedeutung |
|---|---|---|
| `WS_CONNECT_FAILED` | 1006/1008 | Verbindung fehlgeschlagen |
| `WS_AUTH_REQUIRED` | 1008 | STOMP CONNECT ohne `Authorization`-Header |
| `WS_TOPIC_FORBIDDEN` | 1008 | User darf dieses Topic nicht abonnieren (z. B. fremde Welt) |
| `WS_PAYLOAD_INVALID` | 1003 | STOMP-Frame konnte nicht geparsed werden |

### 3.17 Fähigkeiten (`ABILITY_*`)

| Code | HTTP | Bedeutung |
|---|---|---|
| `ABILITY_NOT_FOUND` | 404 | Ability existiert nicht |
| `ABILITY_NOT_ACTIVE` | 400 | Ability ist keine aktive (nutzbare) Ability |
| `ALREADY_ASSIGNED` | 409 | Ability bereits an Entity vergeben |
| `NOT_ASSIGNED` | 400 | Ability ist nicht an Entity vergeben |

### 3.18 Kampagnen (`CAMPAIGN_*`, `MEMBER_*`, `DM_*`)

| Code | HTTP | Bedeutung |
|---|---|---|
| `CAMPAIGN_NOT_FOUND` | 404 | Kampagne existiert nicht |
| `MEMBER_NOT_FOUND` | 404 | User ist kein Kampagnen-Mitglied |
| `MEMBER_ALREADY` | 409 | User ist bereits Kampagnen-Mitglied |
| `INVALID_AI_MODE` | 400 | `botMode` nicht autonom/suggest/off |
| `DM_REQUIRED` | 403 | Nur der DM darf diese Aktion |
| `DM_REMOVAL_DENIED` | 403 | DM kann nicht entfernt/degradiert werden |

### 3.19 Fraktionen (`FACTION_*`)

| Code | HTTP | Bedeutung |
|---|---|---|
| `FACTION_NOT_FOUND` | 400 | Fraktion existiert nicht (Default-Mapping, siehe Hinweis unten) |
| `FACTION_WORLD_MISMATCH` | 400 | Fraktionen liegen in verschiedenen Welten |

### 3.20 Items & Sessions (`ITEM_*`, `SESSION_*`)

| Code | HTTP | Bedeutung |
|---|---|---|
| `ITEM_NOT_FOUND` | 404 | Item existiert nicht |
| `INVALID_ITEM_TYPE` | 400 | `type` nicht in der erlaubten Typ-Liste |
| `SESSION_NOT_FOUND` | 404 | Spiel-Session existiert nicht |

### 3.21 Orte, Regionen, Quests (`LOCATION_*`, `REGION_*`, `QUEST_*`)

| Code | HTTP | Bedeutung |
|---|---|---|
| `LOCATION_NOT_FOUND` | 400 | Ort existiert nicht (Default-Mapping, siehe Hinweis unten) |
| `REGION_NOT_FOUND` | 400 | Region existiert nicht (Default-Mapping) |
| `QUEST_NOT_FOUND` | 400 | Quest existiert nicht (Default-Mapping) |

### 3.22 Einladungen (`INVITE_*`, `ALREADY_MEMBER`)

| Code | HTTP | Bedeutung |
|---|---|---|
| `INVITE_NOT_FOUND` | 404 | Einladung existiert nicht |
| `INVITE_EXPIRED` | 410 | Einladung ist abgelaufen |
| `INVITE_EXHAUSTED` | 409 | Einladung wurde bereits maximal oft genutzt |
| `ALREADY_MEMBER` | 409 | User ist bereits Welt-Mitglied |

### 3.23 Level & Rast (`LEVEL_*`, `ATTRIBUTE_*`, `INVALID_HP_EXPR`)

| Code | HTTP | Bedeutung |
|---|---|---|
| `INSUFFICIENT_POINTS` | 400 | Nicht genug unverteilte Attributpunkte |
| `ATTRIBUTE_PARSE_ERROR` | 400 | `attributes_json` konnte nicht geparsed werden |
| `INVALID_HP_EXPR` | 400 | HP-Ausdruck der Rest-Konfiguration ungültig |

### 3.24 Wetter (`WEATHER_*`)

| Code | HTTP | Bedeutung |
|---|---|---|
| `WEATHER_NOT_FOUND` | 404 | Kein Wetter für diese Region vorhanden |

### 3.25 Rate-Limit & Routing (`RATE_*`, `ROUTE_*`)

| Code | HTTP | Bedeutung |
|---|---|---|
| `RATE_LIMIT_EXCEEDED` | 429 | Generelles Request-Limit überschritten (`Retry-After`-Header beachten) |
| `ROUTE_NOT_FOUND` | 404 | Endpunkt existiert nicht |

> **Hinweis Default-Mapping:** Codes ohne explizites Mapping im `GlobalExceptionHandler`
> antworten derzeit mit 400 (`default -> BAD_REQUEST`). Betroffen: `FACTION_*`,
> `LOCATION_*`, `REGION_*`, `QUEST_*`, `NODE_*`. Das ist bewusst dokumentiert —
> ein Wechsel auf 404 wäre client-seitig beobachtbar und erfolgt ggf. separat.

---

## 4. Beispielhafte Fehlerantworten

### 4.1 Validierungsfehler (Form)
```json
{
  "error": {
    "code": "USER_PROFILE_INVALID",
    "message": "Das Profil ist ungültig",
    "details": [
      { "field": "locale", "issue": "must be a valid BCP 47 tag" }
    ]
  }
}
```

### 4.2 KI-Intent-Validator-Fehler
```json
{
  "error": {
    "code": "INTENT_VALIDATION_RANGE",
    "message": "Ziel außerhalb der Waffenreichweite",
    "details": [
      { "stage": "range", "actual_distance": 8, "max_range": 5, "weapon_id": "uuid" }
    ]
  }
}
```

### 4.3 Interner Fehler
```json
{
  "error": {
    "code": "SYSTEM_INTERNAL_ERROR",
    "message": "Ein unerwarteter Fehler ist aufgetreten",
    "request_id": "abc-123-def"
  }
}
```

---

## 5. Pflegekonvention

- **Katalog ist Source of Truth** — neue Codes müssen hier eingetragen werden
- **Codes werden NIE umbenannt** — wenn eine Semantik sich ändert, neuen Code hinzufügen und alten als deprecated markieren
- **Codes werden NIE gelöscht** — Deprecation-Hinweis reicht, Client-Kompatibilität bleibt erhalten
- **Tests prüfen**: Jede Controller-Antwort nutzt Codes aus diesem Katalog (eigener Test in CI)
- **Beim Hinzufügen**: Task muss此 Code-Datei updaten, sonst CI failt

---

## 6. Verweise

- [`API.md`](API.md) — Endpunkt-Referenz, Fehlerformat
- [`ADR/007`](ADR/007-internationalization-strategy.md) — Lokalisierung von `message`
- [`TESTING.md`](TESTING.md) — i18n- und Code-Stabilitäts-Tests