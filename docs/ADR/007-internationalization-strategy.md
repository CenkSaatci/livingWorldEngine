# ADR-007: Internationalisierung (i18n) als Cross-Cutting Concern

- **Status:** Accepted
- **Date:** 2025-07-12

## Kontext

LWE soll Spieler weltweit begeistern — nicht nur im deutschsprachigen Raum. Das bedeutet:

- UI-Strings in mehreren Sprachen
- Lokalisierte Backend-Fehlermeldungen
- LLM-Prompts und NPC-Sprache pro Welt konfigurierbar
- Datum/Uhrzeit/Zahlen-Formatierung je Locale
- Erweiterbarkeit: neue Sprachen ohne Code-Änderung

Optionen für die Strategie:

1. **i18n erst in Phase 5** — schnellere MVP, aber Tech-Debt
2. **i18n von Anfang an (Phase 1)** — Aufwand früh, aber sauberes Fundament
3. **Nur DE, später umstellung** — Risiko von hartkodierten Strings

## Entscheidung

**i18n wird ab Phase 1 als Cross-Cutting Concern etabliert.** Zwei Sprachen werden von Anfang an ausgeliefert: **Deutsch (de)** und **Englisch (en)**. Die Architektur unterstützt beliebige weitere Sprachen via Resource-Dateien ohne Code-Änderung.

## Strategie

### Backend (Spring Boot)
- `MessageSource` mit UTF-8-Resource-Bundles pro Modul
- `LocaleResolver` liest `Accept-Language`-Header (BCP 47)
- Validierungsfehler via `@Valid` + `MessageSource` übersetzt
- Resource-Pfade:
  ```
  backend/src/main/resources/i18n/
  ├── messages_de.properties
  ├── messages_en.properties
  └── validation_de.properties / validation_en.properties
  ```
- fallback-Locale: `en` (wenn angefragte Sprache nicht vorhanden)
- Default-Locale (kein Header): `de`

### Frontend (React + Vite)
- `react-i18next` + `i18next` als Backend-Loader
- JSON-Namespaces pro Feature:
  ```
  frontend/src/i18n/locales/
  ├── de/
  │   ├── common.json
  │   ├── auth.json
  │   ├── character.json
  │   ├── map.json
  │   ├── chat.json
  │   └── dm.json
  └── en/
      ├── common.json
      └── ...
  ```
- Locale-Quellen (Priorität):
  1. User-Setting in Datenbank (`users.locale`)
  2. `localStorage('lwe:locale')`
  3. `navigator.language`
  4. Fallback `de`
- Sprachumschalter in TopBar (Globe-Icon → Dropdown)
- Datum/Uhrzeit: `Intl.DateTimeFormat` mit User-Locale
- Zahlen: `Intl.NumberFormat`

### LLM-Prompts (AI-Bot)
- Welt-Setting `worlds.settings_json.language` (BCP 47 Tag, z. B. `de`, `en`, `fr`)
- Prompt-Templates enthalten Sprach-Anweisung: „Antworte in der Sprache des NPC: {{ language }}"
- NPC-Metadata JSON kann `speaks`-Array enthalten (Mehrsprachigkeit)

### Datenmodell
- `users.locale` (VARCHAR 10, BCP 47 Tag) — User-Präferenz
- `worlds.settings_json.language` — Weltsprache für LLM und Default-UI-Sprache in dieser Welt
- Siehe [`DATA-MODEL.md`](../DATA-MODEL.md)

### API
- `Accept-Language`-Header auf allen Endpunkten
- `POST /api/users/me/preferences` — ändert `locale`
- `GET /api/users/me/preferences` — liefert Locale-Optionen und User-Wahl

## Sprach-Auswahl (initial)

| Code | Sprache | Status |
|---|---|---|
| `de` | Deutsch | Phase 1+ |
| `en` | Englisch | Phase 1+ |
| `fr` | Französisch | später, Community-Beitrag möglich |
| `es` | Spanisch | später |
| `pl` | Polnisch | später |
| `it` | Italienisch | später |

Architektur erlaubt Community-Übersetzungen via PR auf die JSON-Dateien.

## Implementations-Hinweise

- **Keine hartkodierten UI-Strings** — jede Komponente nutzt `const { t } = useTranslation('namespace')`
- **Test-Abdeckung**: Ein Test prüft, dass alle angezeigten Keys in allen Sprachen vorhanden sind (`i18next-extract` optional, aber empfohlen)
- **Backend-Tests**: Parametrisierter Test ruft `/api/auth/login` mit `Accept-Language: de` und `en` und erwartet lokalisierte Fehlermeldung
- **CI-Gate**: Build fails, wenn Keys in `de` und `en` nicht übereinstimmen

## Konsequenzen

**Positiv:**
- Sauberes Fundament von Tag 1 an
- Weniger Tech-Debt als Retrofit
- Spieler weltweit ohne Sprachbarriere ansprechbar
- Community kann Übersetzungen beisteuern

**Negativ:**
- Jede UI-Änderung erfordert Update in 2 (oder mehr) JSON-Dateien
- Anfangs-Mehraufwand ~5 % pro UI-Feature
- Backend-Fehlermeldungen zuerst in `messages.properties` definieren, nicht inline

## Verweise

- [`UI-UX.md`](../UI-UX.md) — Frontend-i18n-Details
- [`API.md`](../API.md) — Accept-Language-Konvention
- [`DATA-MODEL.md`](../DATA-MODEL.md) — `users.locale`, `worlds.settings_json.language`
- [`AI-AGENT.md`](../AI-AGENT.md) — LLM-Prompt-Sprache
- [`TASKS.md`](../TASKS.md) — Tasks P1-T09, P3-T12