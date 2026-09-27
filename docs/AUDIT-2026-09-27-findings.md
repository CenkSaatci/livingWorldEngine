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

## H-3 — Jedes Welt-Mitglied konnte die Karte überschreiben — **BEHOBEN**

**Evidenz:** `FileUploadController.uploadMap` prüfte nur `worldAccess.requireAccess`
(Owner **oder beliebiges Mitglied**), während das Frontend den Karten-Editor auf DM/Owner
gated und `WorldMapService.update` nur den Owner zulässt. Ein Spieler konnte per API ein
neues Kartenbild hochladen (und damit das Kartenbild der Gruppe ersetzen).
**Fix:** Upload **und** der neue Delete nutzen `worldAccess.requireDm` (Owner oder DM).
Live verifiziert: Upload als `playtest-spieler1` → **403**.

## F-7 — Kein Karten-Löschen — **BEHOBEN**

`DELETE /worlds/{id}/map` (DM/Owner) entfernt Datei(en) `map.*` und leert `imageUrl`;
Editor-Button „Karte entfernen" mit Bestätigungs-Modal. Live: Owner-Delete → 204,
`imageUrl` null. E2E deckt Entfernen + Wiederherstellen ab.

## F-9 — Modus „Zeichnen" ohne Region — **BEHOBEN**

Der Modus-Umschalter „Zeichnen" ist gesperrt, solange keine Region gewählt ist
(Titel-Hinweis); Zeichnen startet über die Regionszeile („Zeichnen"/„Neu zeichnen").
Unit-Test + E2E-Assertion.

## F-5 — Native `window.prompt`/`confirm` im Karten-Editor — **BEHOBEN**

Region anlegen/umbenennen sowie alle Lösch-Bestätigungen (Region, Ort, Karte) laufen jetzt
über echte Modals im App-Stil (`role="dialog"`, `aria-modal`, Escape schließt).

---

## Abdeckung / Grenzen

- **Getestet:** Backend-Unit (Fähigkeit: Schaden/AP, unbekannt, passiv, MP, kaputtes JSON),
  CORS-Quelle, Upload-Serve-Security; Frontend ActionBar (Regel-Fähigkeit per Name), Manöver-Roundtrip.
- **Nicht end-to-end getestet:** ein echter Klick auf eine Regel-Fähigkeit im laufenden Kampf
  (Playwright deckt A11y + POI ab, nicht Kampf-Fähigkeiten). Empfehlung: ein E2E-Fall.
- **Nicht geprüft:** Produktionsprofil gegen echte Infrastruktur (CORS-Quelle per Unit-Test belegt).
- **Confidence:** HIGH für die behobenen Punkte (Test + Live-Check), MEDIUM für die Breite des
  übrigen UX-Diffs (i18n/Texte, visuell nicht neu gescannt außer dem A11y-Gate).
