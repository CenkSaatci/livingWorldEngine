# QA-Agent-Log — Playwright-Sweep Runde 1 (Golden Path + Kern-Blöcke)

> **Zweck:** Agenten-Befundlog als Abgleichsbasis für die gemeinsamen manuellen Tests.
> Meine Funde heißen `QA-A-###` (Agent), deine `QA-###` (manuell, s. `MANUAL-TESTPLAN.md` Anhang A).
> Beim Abgleich: Duplikate streichen, Deltas (nur menschlich fühlbar: Verwirrung, Geschmack,
> Reibung) priorisieren → daraus `IMPLEMENTATION-PLAYBOOK.md` ableiten.
>
> **Stand:** 2026-09-13. Backend 505 / Frontend 190 / E2E 11 grün. Commit-Stand s. unten.

## Methode

- Specs: `frontend/e2e/qa-helpers.ts`, `qa-golden.spec.ts` (GP-04…GP-15), `qa-ui-states.spec.ts`
  (Seiten-Matrix Desktop+375px, Admin-Deny, Register, Login). Lauf: `npx playwright test qa-golden qa-ui-states`
  (serielle Chromium-Runs, je Test frische 3-Account-Kontexte: DM, Spieler1, Spieler2).
- Daten: frische QA-Welt (System `QA-DSA5` = `docs/examples/dsa5-playtest.json` v6+),
  Kampagne `QA-Runde` (Fork), PCs QA-Mira/QA-Torben, NPCs (Händler/Räuber/Wache),
  Items, Abenteuer, Quest, Beziehung. Setup-Skript: `/tmp/qa-setup.py` (API-seitig).
- Pro Schritt: Assertion + Screenshot (`frontend/e2e/qa-out/shots/`, 48 Dateien, **nicht** committed)
  + Console-Errors + HTTP-4xx/5xx + DOM-Heuristiken (namenlose Buttons/Inputs, Alt-Texte,
  Overflow, `undefined`/UUID-Leaks). Ergebnisse: `frontend/e2e/qa-out/*-results.json`.
- Determinismus-Hilfen in den Specs: Fate-Punkte auffüllen, Schwert nachlegen, HP-Reset per SQL
  vor Läufen (`update entities set hp_current = hp_max where world_id='<fork>'`), frisches
  Abenteuer pro GP-12-Lauf, Cleanup (Kampf beenden, UI-PCs löschen, Fortschritte aufgeben).
- Screenshots kann ich sehen und habe die Schlüsselbilder alle geprüft; Geschmack/Verständnis
  bleiben menschliche Domäne (s. Abgleich).

## Behobene Befunde (mit Tests, verifiziert)

| ID | Schwere | Befund | Fix |
|----|---------|--------|-----|
| QA-A-001 | HIGH | Trade-Annahme als Partner lieferte 403 („DM access required") — Spieler-zu-Spieler-Handel war ohne DM unmöglich. Ursache: `moveItems` lief mit User-Rechten durch `requireControl` beider Entities. | `InventoryService.transferItem` als Engine-Aktion (Trade-Vertrag = Autorisierung) + Tests (TradeService/InventoryService). Live verifiziert. |
| QA-A-002 | HIGH | Chat-Doppelpost: eigene Nachricht 2× (Echo-nach-POST-Race — WS gewinnt). Runde-2-Regression. | Echo-vor-POST in `ChatPanel` + Unit-Test (Broadcast-vor-Response → genau 1×). Sweep zählt Duplikate. |
| QA-A-003 | CRITICAL (FE) | `NpcViewPage` crashte komplett („Rendered more hooks…") — mein SM-03-`useEffect` stand hinter dem Early-Return. | Hook vor Early-Return verschoben. |
| QA-A-004 | HIGH | Adventure-Start 500 bei Doppel-Start (StrictMode feuert Auto-Start 2×) — Unique-Verstoß, Postgres-Tx danach abortet (Catch+Retry unmöglich). | Zeilen-Lock (`findByIdForUpdate`) serialisiert Starts + Resume; Frontend-Guard (Ref) war falsch und ist revertiert (hätte Seite im Lader hängen lassen). Doppel-Start live: 200+200. |
| QA-A-005 | MEDIUM | Spieler pollten DM-Endpunkte (`/sessions`, `/npc-intents`) → 403-Spam in Konsole (10× pro Seitenaufruf). | Neuer Endpoint `GET /worlds/{id}/membership` (OWNER/DM/PLAYER/VIEWER) + `GameView` gatet DM-Panels danach. |
| QA-A-006 | MEDIUM | Angriffs-Button zeigte Roh-Typ „action" (i18n-Key `combat.attack` ungenutzt). | Typ `action` → Label „Angreifen". |
| QA-A-007 | MEDIUM | Location-Typ „Dorf" (deutsch) → 500 `SYSTEM_INTERNAL_ERROR` (DB-Check-Constraint). | `@Pattern` auf `CreateRequest.type` (12 Enum-Werte) → sauberes 400. UI nutzt bereits Select. |
| QA-A-008 | MEDIUM (Content) | DSA-Manöver (Wuchtschlag/Finte, apCost 2) bei AP-Max 1 **nie nutzbar** (Buttons permanent disabled). | apCost 1 in `dsa5.json` + `dsa5-playtest.json` + QA-DSA5-System + Kampagnen-Pull. |
| QA-006b | MEDIUM | Rename-Konflikt-Härtung war Überkorrektur (User will entscheiden: Version leicht anheben, keine Kopie-Flut). Speichern erstellt nie Kopien (nur Clone-Button). | Umgesetzt: Warn-Dialog + forceRename, Versions-Checkbox (Bump default an), Hinweistexte; Tests + Live-Verifikation (409/force/bump) |
| QA-A-023 | MEDIUM (Infra) | App von anderem Rechner im LAN nicht erreichbar: Vite band nur `127.0.0.1`; API-/WS-URLs hart auf `localhost` (fremder Browser hätte ins Leere gezielt). | Frontend auf `0.0.0.0` (pm2 neu angelegt + `pm2 save`); API-/WS-Host dynamisch aus `window.location.hostname` (`client.ts`/`useWorldSocket.ts`, `VITE_*`-Override bleibt). Zusatz: Root-`.env` setzte `VITE_API_URL/WS_URL` hart auf localhost und hat das Dynamic-Host-Override wirkungslos gemacht → aus `.env` entfernt (nur noch opt-in in `.env.example`). |

## Offene / akzeptierte Befunde (für gemeinsame Tests + Playbook)

| ID | Schwere | Befund | Notiz |
|----|---------|--------|-------|
| QA-A-009 | LOW | Admin-Seite als USER: korrekter „Zugriff verweigert"-Screen, aber 2 rohe englische „Forbidden"-Toasts (API-Fire-and-forget). | Toast-Texte lokalisieren/unterdrücken wenn Seite Deny zeigt. |
| QA-A-010 | LOW (a11y) | Kampagnen-Titel ist kein Heading-Element (Role-Selektor findet ihn nicht). | Titel als `<h1>` rendern. |
| QA-A-011 | LOW (a11y, Batch) | Pro Seite 1–2 Buttons ohne Namen, 1–4 Inputs ohne Label (Matrix: systems/campaign/world/entities/sheet/npc/quest/settings; Details in `ui-states-results.json`). | Batch fixen (aria-label/title). |
| QA-A-012 | LOW | Sheet: horizontaler Overflow bei 375px. | Responsiv fixen (2026-09-13 bestätigt). |
| QA-A-013 | INFO | Frisches Browser-Profil → englische UI (Browser-Locale schlägt Default), native Validierungs-Bubbles englisch („Please fill out this field"). | Verhalten dokumentieren oder Default `de` erzwingen. |
| QA-A-014 | MEDIUM (Content) | DSA ohne `resting`-Config: Rasten ist wirkungslos (still). | Wizard-Hinweis/Default + Doku; manuell prüfen (CHR-18). |
| QA-A-015 | MEDIUM (UX) | Kampfstart mit besiegten Teilnehmern möglich → erst bei Aktion 422 (`COMBAT_ACTOR_DEFEATED`). | Beim Start warnen/filtern (FGT-Flow). |
| QA-A-016 | MEDIUM (UX) | Adventure-Seite bei Start-Fehler: leere Seite + flüchtiger Toast (kein Retry/Hinweis). | Fehlerzustand mit Aktion rendern (AQ-Flow). |
| QA-A-017 | INFO (korrigiert) | GP-15: Owner-Löschung ist legitim (kein Bug); P2-Deny (403 + Entity bleibt) verifiziert. | Rechte-Matrix (KRG-03) manuell vertiefen. |
| QA-A-018 | LOW (UX) | „Angebot senden" wirkt ohne Partner klickbar (technisch disabled, Affordance schwach). | Disabled-Stil schärfen. |
| QA-A-019 | INFO | Trade-Modal listet wiederholte identische Proposals mehrfach (Rauschen bei Retries). | Optional gruppieren. |
| QA-A-020 | LOW (Doku) | Member-Rollen heißen PLAYER/DM (nicht MEMBER); Location-Typen englisch — Setup stolperte je mit 400 (korrekt), Doku/API-Beispiele sollten sie nennen. | API.md ergänzen. |
| QA-A-021 | INFO | StrictMode feuert Auto-Start doppelt (zwei POSTs im Log) — durch Lock harmlos, aber Rauschen. | Akzeptiert. |
| QA-A-022 | INFO | Meister-Quota 1/1 (QA-Welt läuft auf Spieler1-Account); Sweep-Artefakte (Adventures/PCs/Trades) sammeln sich in QA-Welt — Cleanup dokumentiert (afterAll), Soft-Deletes bleiben in DB. | Vor Manuell-Runden ggf. frische QA-Welt. |

## Abgleich-Hinweise (für dich)

- **Von mir NICHT geprüft (dein Part):** Wizard-Tiefe (alle Steps/Inhalte), Quest-UI-Flows, Markt Kauf/Verkauf, Wetter/Fog/Map-Gefühl, Fork-Unabhängigkeit im Detail, Mobile-Gefühl, Texte/Tonalität, DnD/CoC (Runde 2), alles Geschmackliche.
- **Meine blinden Flecken:** StrictMode-/Timing-Artefakte in der Harness (durch `waitVisible` + Isolation-Runs minimiert; als „flaky in Suite" markiert, nie als App-Bug gewertet ohne Screenshot-/API-Beweis).
- **Screenshots:** 48 in `frontend/e2e/qa-out/shots/` (lokal) — Schlüsselmotive: `gp*-chat/trade-open/combat-open/after-action`, `ui-*-desktop/mobile`, `gp12-*`, `gp13-social-result`. Beim Abgleich gemeinsam ansehen.

## Nächste Schritte

1. Gemeinsam: Block GP + Kern-Blöcke nach `MANUAL-TESTPLAN.md`, Funde als `QA-###`.
2. Abgleich QA-A ↔ QA → Duplikate raus, Deltas priorisieren.
3. `IMPLEMENTATION-PLAYBOOK.md` schreiben (Checkliste + automatisierbare Gates für E2E).
4. Runde 2: XS-Systeme (DnD/CoC) + Wiederholung nach Fixes.
