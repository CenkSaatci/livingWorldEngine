# ADR-015: POI-Aktionen, Währung und Händler

- **Status:** Umgesetzt (2026-09-21, entwickeln)
- **Kontext:** Orte/NPCs haben Content (`services_offered`, `services`, `price_modifier`, Wohlstand), aber keine Mechanik: kein Handeln, kein Geld, keine auslösbaren Aktionen. „Händler" existieren nur als Service-Kürzel (`sell_weapons`) ohne Sortiment.

## Understanding Summary

- **Was:** Ein deklarativer Regel-Katalog `poi_actions[]` (Handeln, Wirtshaus, Medicus, Ruine untersuchen, narrative Anlaufstelle), Währung als System-Config `currency`, Händler als NPCs mit Sortiment. Auslösung in der Ortsansicht und per Karten-POI.
- **Warum:** Aktionen sollen von Systemautoren per Regel-JSON beschrieben werden (systemagnostisch), nicht hartkodiert; Geld/Händler gehören zum Spiel.
- **Wer:** Systemautoren (Regel-JSON), Spieler (eigene Charaktere), Leiter (auch fremde Charaktere).
- **Constraints:** Keine Systemsonderlogik im Code (ADR-014); nur serverseitige, atomare Effekte („erst validieren, dann anwenden"); Effekte laufen durch bestehende Services.
- **Non-goals v1:** Keine Bestände/Lager, kein Feilschen, keine Preise pro Charakter, keine persistierten Flüster-Nachrichten, kein Crafting, keine Cooldowns (Tischsache). Bereits vertagt: aktiver Parade-Wurf, Waffen→Skill-Verknüpfung.

## Annahmen

- `money` ist **eine ganze Zahl in der kleinsten Sorte** am Charakter (`metadataJson.money`), Default 0, nie negativ. Anzeige rechnet in Sorten um — dadurch kein Wechselgeld-Randfall.
- Aktionen ohne `currency`-Config zeigen `money` als nackte Zahl (rückwärtskompatibel).
- `item`-Effekte referenzieren Items des Charakter-Systems per **Name** (case-insensitiv); unbekannter Name = Fehler (kein stiller No-op).
- Fehlende Felder = dokumentierter Default; vorhandene, aber kaputte Felder = Fehler (ADR-014-Linie).
- Leiter dürfen Aktionen für fremde Charaktere ausführen; Nähe ist Tischsache.
- Händler-Sortiment v1 unbegrenzt; Verkauf an Händler 50 % (`sellRate`), per Aktion/NPC konfigurierbar.

## Decision Log

| Entscheidung | Alternativen | Warum |
|---|---|---|
| Generischer `PoiActionService` + Regel-Katalog (Ansatz A) | Eigene Tabellen/CRUD (B), Effekte inline im Content (C) | Nutzt bestehende Infrastruktur, systemagnostisch, wenig Migrationen; B dupliziert das Regel-JSON, C verletzt ADR-014 |
| Kein `kind`-Enum — Form ergibt sich aus Feldern (`probe`, `trade`, sonst Effekte) | `kind`-Enum | Weniger Schema, keine Doppelpflege; YAGNI |
| Effekte tragen signierte Deltas, Executor validiert **alle** negativen Posten vorab | Getrennte `costs` + `effects` | Eine Quelle, keine Duplikation; H-2-Lehre (keine halben Zustände) |
| Geld als ein Basiswert, Sorten nur Darstellung | Stückzahlen je Sorte | Kein Wechselgeld-Algorithmus |
| Händler = NPC-Metadaten (`is_merchant`, `shop_inventory`) | Eigene Merchant-Tabelle | Nutzt NPC-/Orts-/Preis-Infrastruktur; Sortiment im NPC-Editor |
| `EconomyService`-Formel bleibt einzige Preisquelle | Zweite Formel im MerchantService | Keine zwei Wahrheiten |
| Aktion `chat: public` persistiert, `actor` transient per User-Queue + im Ergebnis | Immer öffentlich / persistierte Flüster | Private Belege ohne Migration; Welt-Chatverlauf bleibt sauber |
| „Handeln" öffnet Händlerliste, Kauf/Verkauf eigene Endpunkte | Ein überladener Aktions-Endpunkt | Sauberere Validierung/Atomarität |
| Fail-closed: unbekannte Aktion/Item/Zustand = Fehler | Stille Defaults | Wie ADR-014 |

## Finales Design

**1. Regel-Schema.** Neue Top-Level-Keys (beide optional, `additionalProperties: false` bleibt):
- `currency { name?, denominations[{name, abbr?, factor}] }` — `factor ≥ 1`, eindeutige Namen/Abkürzungen.
- `poi_actions[] { name, description?, chat?, requiresTrait?, dmOnly?, probe?, trade?, effects[] }`.
  - `probe { skill, difficulty?, onSuccess[], onFailure[] }`
  - `trade { buy?, sell?, sellRate? }`
  - `effects[]`: `money` (±), `item` (name, qty ±), `heal` (Zahl/Würfel/`full`), `condition` (name, rounds?, remove?), `fate` (±), `rest` (`short`/`long`), `text` (text).

**2. Währung.** `CurrencyService` liest `currency` aus den Regeln: `format(base, rules)` → `"3 G, 1 S, 2 K"` (greedy, fehlende Abkürzung = Name, ohne Config = Zahl); `payable(entity)` / `pay` / `credit` schreiben `metadataJson.money` mit Schreib-Lock (`findByIdForUpdate`).

**3. Executor.** `PoiActionService.list(locationId, actorId, userId)` und `execute(locationId, actionName, actorId, userId)`:
1. Ort + Welt laden (`worldAccess.requireRead`), Aktion im Katalog (case-insensitiv, `RuleNames`), sonst `POI_ACTION_UNKNOWN`.
2. Verfügbarkeit: an Ort (`location.services[]`) oder NPC (`services_offered[]`) gebunden, sonst `POI_ACTION_NOT_AVAILABLE`; `dmOnly` → Welt-/Kampagnen-DM; `requiresTrait` → Merkmal.
3. Kontrolle: `entityAccess.checkControl(actor, userId)`.
4. Vorvalidierung ohne Wirkung: Geld, Items, Zustandsnamen, Heil-Ausdruck.
5. Optionale Probe über `ProbeService` → `onSuccess`/`onFailure`.
6. Effekte anwenden (transaktional, eine gesperrte Entity, ein Save): Geld, Items, HP, Zustände, Fate, Rast (über `RestService`-Logik via Effekt `rest`), Text.
7. Ergebnis: Erfolg, Text, Geld vorher/nachher formatiert, angewandte Effekte, Probe-Aufstellung.

**4. Händler.** NPC-Metadaten `is_merchant`, `shop_inventory[{item, price?}]`, `sell_rate?`. `MerchantService.list(locationId, userId)` liefert Händler + Sortiment mit fertigen Kauf-/Verkaufspreisen; `buy`/`sell` sind transaktional (Geld + `InventoryService`), Menge ≥ 1, Preis aus Sortiment sonst `EconomyService` (Basispreis × Wohlstandsfaktor × `price_modifier`), Verkauf `floor(Preis × sellRate)`.

**5. Chat.** `public` → persistieren + `/topic/world/{id}` (wie bestehender Chat); `actor` → transient an `/user/queue/poi` + im Ergebnis (Broker um `/queue` erweitert); `none` → nur Ergebnis.

**6. API/UI.** `GET /locations/{id}/actions?actorId=&campaignId=`, `POST /locations/{id}/actions/{name}`, `GET /locations/{id}/merchants`, `POST /merchants/{npcId}/buy|sell`. Frontend: `LocationActionsPanel` (Ortsansicht + Karten-POI-Sheet), `MerchantPanel` (Markt), Geldzeile im Bogen (`moneyText` im `SheetResponse`), NPC-Editor um Händler/Sortiment erweitert.

## Risiken

- WS-Erweiterung `/queue` berührt die Broker-Config → bestehende Topics mitprüfen.
- `money`-Races → Schreib-Lock wie `fate_points`.
- „Weiche" Händler-Metadaten → Validator-Warnung bei unbekannten Sortiments-Items.
- Content-Verantwortung für Balance → Beispiele + Doku, kein Balancing im Code.

## Teststrategie

- `CurrencyService`: Format/Breakdown (mehrere Sorten, ohne Config), `payable` bei zu wenig, `pay`/`credit`, nie negativ.
- `PoiActionService`: unbekannt/nicht verfügbar/`dmOnly`/fehlendes Merkmal, Vorvalidierung blockt alle Effekte, Probe-Erfolg/-Fehlschlag, jeder Effekt-Typ, `chat`-Sichtbarkeit.
- `MerchantService`: Kauf/Verkauf, Preis-Fallback, `sellRate` mit `floor`, nicht im Sortiment, Item nicht im Besitz, ungültige Menge.
- Validator: `currency`-Sorten (leer, `factor < 1`, Duplikate), kaputte `poi_actions` → Fehler, unbekanntes Sortiments-Item → Warnung.
- Frontend: Panel-Rendering, Kauf/Verkauf, Geldanzeige, Fehlercode-Mapping.
