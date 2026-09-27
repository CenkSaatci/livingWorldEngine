# Audit 2026-09-27 — Findings-Batch + ADR-016

Scope: `67bfacf..HEAD` (4 Commits)
- `20861fe` fix(map/ws): Karten-Assets anonym, WS-Origin LAN-tauglich
- `dea5611` fix(ui): Login lokalisiert + System-Filter
- `6b8a02d` feat(wizard/ux): Manöver-Editor, Header, Hinweise, Karte im Spiel
- `5b291ef` feat(combat): Regel-Fähigkeiten im Kampf (ADR-016)

Strategie: FOCUSED (mittelgroßer Diff, 3 HIGH-Risk-Flächen: Auth/Uploads, CORS/WS, neue Eingabe-Pfade).

---

## H-1 — CORS-Origins aus zwei Quellen (Prod lief offen) — **BEHOBEN**

**Evidenz:** `SecurityConfig.corsConfigurationSource()` las `System.getenv("CORS_ALLOWED_ORIGINS")`
**direkt** und ignorierte die Property `lwe.cors.allowed-origins`. `application-prod.yml` setzt aber
`lwe.cors.allowed-origins: ${CORS_ALLOWED_ORIGINS:https://example.com}`. Wurde nur die Property
(oder der Prod-Default) genutzt, ohne die Env zu setzen, fiel der Code auf `setAllowedOriginPatterns("*")`
zurück → **jede Origin erlaubt**, auch im Prod-Profil. `WebSocketConfig` erbte dasselbe Muster.

**Angriffsszenario:** Beliebige Website öffnet Cross-Origin-Requests/WS gegen die API; CSRF-artige
Aktionen bzw. Cross-Site-WebSocket-Hijacking werden möglich (durch JWT im STOMP-CONNECT entschärft,
aber die Origin-Schranke fiel weg).

**Fix:** Beide lesen jetzt die Property `lwe.cors.allowed-origins` (eine Quelle der Wahrheit).
Dev-Default `*` (LAN-tauglich), Prod-Default explizit (`https://example.com`), via
`CORS_ALLOWED_ORIGINS` überschreibbar. Regressionstest `CorsOriginConfigTest`.

**Live-Verifikation:** WS-Handshake localhost + LAN → `101`; HTTP-Preflight LAN →
`Access-Control-Allow-Origin: http://192.168.31.151:5173`.

---

## H-2 — Karten-Assets anonym ausliefern — **BEWUSST AKZEPTIERT**

**Evidenz:** `FileUploadController.serveFile` verlangt keinen Principal mehr; Security gibt
`GET /api/v1/uploads/**` frei. Grund: `<img>`/PIXI können keinen `Authorization`-Header senden.

**Blast-Radius (begrenzt):**
- Nur `map.(png|jpg|jpeg|webp)` (`SERVE_FILENAME`), kein Directory-Listing, kein SVG (XSS-frei).
- Path-Traversal abgewehrt (Regex ohne `/`/`..` **und** `startsWith(base)`); Tests decken
  `..%2f`, `%00`, Backslash, `/etc/passwd` ab.
- Upload validiert Magic Bytes, ≤ 10 MB (`MAX_BYTES`).
- Zugriff erfordert die Welt-UUID (122 Bit, nicht erratbar).

**Restrisiko:** Wer eine Welt-UUID kennt (Einladungslink, geteilte URL), kann die Karte lesen.
**Mitigation-Pfad (offen):** signierte, kurzlebige URLs oder authentifizierter Blob-Fetch im Frontend.

---

## M-1 — `findRulesAbility` castete ungeprüft (500 statt fail-closed) — **BEHOBEN**

**Evidenz:** `(List<Map<String,Object>>) rules.get("abilities")` mit `@SuppressWarnings`. Enthielt ein
Kampagnen-Snapshot `abilities` als Nicht-Liste, warf das eine `ClassCastException` → HTTP 500.

**Fix:** Typprüfung via `instanceof List<?>` / `instanceof Map<?,?>`, fremde Einträge werden
übersprungen, sonst `ABILITY_NOT_FOUND`. Test `malformedRulesAbilitiesFailClosed`.

---

## M-2 — MP-Fähigkeiten kosten 0 AP — **AKZEPTIERT (dokumentiert)**

Im Kampfmodell existiert nur AP/HP, kein Mana-Vorrat. `costType: "MP"` kostet vorerst 0 AP und ist
damit beliebig oft einsetzbar. Als `ponytail:`-Kommentar und Nicht-Ziel in ADR-016 vermerkt.
**Empfehlung:** Mana-Pool einführen, dann hier abbuchen.

---

## L-1 — Fähigkeiten-/Manöver-Liste aus Live-System, Server aus Snapshot — **OFFEN (Empfehlung)**

Die ActionBar liest `rules.abilities`/`maneuvers` aus `GET /game-systems/{id}` (Live-System), der
Server löst aus dem **Kampagnen-Snapshot** auf (`RulesLoader`). Nach einer Regeländerung ohne
`pull-system` zeigt die Leiste Einträge, die der Server nicht kennt → Klick liefert
`ABILITY_NOT_FOUND`. Bestehendes Muster (galt schon für Manöver).
**Empfehlung:** ActionBar aus dem Kampagnen-Snapshot speisen (eigener Endpunkt).

---

## L-2 — i18next-Reserviertheit von `count` — **BEHOBEN**

`systems.count` interpolierte `{{count}}`; `count` ist in i18next für Pluralisierung reserviert.
Auf `{{shown}}` umgestellt (6 Sprachen + Komponente).

## L-3 — Dezimalwerte in Integer-Feldern — **BEHOBEN**

`attackMalus`/`apCost` sind Schema-`integer`; die Manöver-Inputs hatten kein `step`. `step={1}` ergänzt.

## L-4 — Doppelter Kommentar in ActionBar — **OFFEN (kosmetisch)**

Zwei identische Zeilen `// Best-effort Config-Ladung …` (vorbestehend).

---

---

# Nachtrag — Welterstellung/-anpassung & Karten-Flow (2026-09-27)

**Verifikation (live, E2E `e2e/map-editor.spec.ts`):**
- **Bild hochladen:** ✅ `POST /worlds/{id}/map/upload` → 200.
- **Anzeige:** ✅ anonymes `GET /uploads/{id}/map.png` → 200 `image/png`; `<img alt="Map">`
  in der Spielansicht lädt (`naturalWidth > 0`), kein 4xx auf `/uploads/`.
- **Regionen einzeichnen:** ✅ Editor zeichnet + speichert (`PATCH …/regions/{id}`),
  Polygon persistiert (3 Punkte).

## F-1 — Regionen im Spiel nicht sichtbar — **BEHOBEN**

**Evidenz:** Polygone wurden nur in `MapEditorPage` (PIXI) gerendert; `WorldMapView`
(Spielansicht) zeigte Bild + Ortsmarker + Legende, aber **keine** Regionen. Wer Regionen
einzeichnete, sah sie im Spiel nie.
**Fix:** SVG-Overlay in `WorldMapView` (Bildkoordinaten, `regionColorHex` wie im Editor);
Legenden-Punkt nutzt jetzt dieselbe Regionfarbe. E2E-Assertion `svg polygon` ergänzt.

## F-2 — Legende hartkodiert englisch — **BEHOBEN**

`Regions ({n})` → `t('map.regions')` (6 Sprachen).

## F-3/F-6 — WorldEditorPage hartkodiert englisch — **BEHOBEN**

Member-Bereich (`Members (n)`, Copy Link, Suche-Platzhalter, Add, No members yet, Remove,
Rollen), Archivierung (Label + Beschreibung), Karte- und Klon-Bereich → `worldEditor.*`
(6 Sprachen).

## F-8 — Member-Liste zeigte rohe UUIDs — **BEHOBEN**

`WorldMemberResponse` trägt jetzt `username`/`email`; `WorldController` reichert per
`UserRepository` an. Frontend zeigt den Namen (Fallback UUID-Kürzel) und übersetzte Rollen.
Vorher stand dort `a1b2c3d4…`.

## F-7 — Kein Karten-Löschen — **OFFEN (Empfehlung)**

Es gibt nur Upload (ersetzt `map.<ext>`), keinen DELETE-Endpunkt. Ein falsch hochgeladenes
Bild lässt sich überschreiben, aber nicht entfernen. Empfehlung: `DELETE /worlds/{id}/map`.

## F-9 — Modus „Zeichnen" ohne Region — **OFFEN (UX)**

Der Modus-Umschalter erlaubt Zeichnen ohne ausgewählte Region; Punkte sind möglich, das
Speichern scheitert mit „Region auswählen". Empfehlung: Modus nur mit Region starten oder
die Regionsauswahl im Zeichenmodus erzwingen.

## F-5 — Native `window.prompt`/`confirm` im Karten-Editor — **OFFEN (UX)**

Region anlegen/umbenennen/löschen nutzt Browser-Dialoge (unstyled, in manchen Umgebungen
blockiert). Empfehlung: Modal wie im Rest der App.

---

## Abdeckung / Grenzen

- **Getestet:** Backend-Unit (Fähigkeit: Schaden/AP, unbekannt, passiv, MP, kaputtes JSON),
  CORS-Quelle, Upload-Serve-Security; Frontend ActionBar (Regel-Fähigkeit per Name), Manöver-Roundtrip.
- **Nicht end-to-end getestet:** ein echter Klick auf eine Regel-Fähigkeit im laufenden Kampf
  (Playwright deckt A11y + POI ab, nicht Kampf-Fähigkeiten). Empfehlung: ein E2E-Fall.
- **Nicht geprüft:** Produktionsprofil gegen echte Infrastruktur (CORS-Quelle per Unit-Test belegt).
- **Confidence:** HIGH für die behobenen Punkte (Test + Live-Check), MEDIUM für die Breite des
  übrigen UX-Diffs (i18n/Texte, visuell nicht neu gescannt außer dem A11y-Gate).
