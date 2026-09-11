# ADR-011: Geteilte Templates, eigene Universen — Visibility, Fork & Bot pro Kampagne

- **Status:** Akzeptiert (2026-09-11)
- **Entscheidung:** Systeme und Welten sind **teilbare Templates** mit Sichtbarkeit (privat / Einladungsliste / öffentlich); nur Ersteller ändern. Kampagnen erzeugen per **Clone-on-Create einen Fork** der Welt als eigenes Universum und pinnen die System-Version (manuelles Nachziehen). Der Bot ist **pro Kampagne** konfigurierbar.

---

## 1. Kontext

Aufbauend auf [ADR-010](010-three-tier-model-system-world-campaign.md) (System ∥ Welt → Kampagne) soll die Plattform wie ein Marktplatz für Spielmaterial funktionieren:

- User erstellen **Spielsysteme** (z. B. DSA-Regeln) und **Welten** (z. B. Aventurien, Schwertküste) und entscheiden, wer sie sieht/nutzt: **nur sie selbst, eine Einladungsliste oder alle (öffentlich)**.
- **Nur Ersteller** dürfen Templates verändern.
- Spieler erstellen **Kampagnen** aus System + Welt und laden **Spieler wie Spielleiter** ein.
- Alle Ereignisse und **Veränderungen der Welt** (toter Fraktionsführer, vernichtete Fraktion, Krieg, …) werden **in der Kampagne** gespeichert. Die Welt dient als **Sandbox/Template** (Karte, Regionen, Fraktionen); der SL erzeugt darauf **seine eigene Version, ohne das Template zu verändern**.
- Beispiel: Viele Gruppen nutzen „DSA + Aventurien" als Basis, spielen aber in ihrem eigenen Universum.
- Aktionen sollen **wahlweise vom Bot oder vom menschlichen SL** abgearbeitet werden (Aktionsliste mit Bearbeiten/Freigeben) — der Bot ist dabei **pro Kampagne** konfigurierbar (an/aus/Modus); eine Koppelung an Abo-Modelle kommt später.
- Eigene Welten (die auf einem System basieren) sollen **später ebenfalls teilbar** sein.

Heute-Zustand (verifiziert 2026-09-11): Systeme sind global und ohne Owner (jeder kann ändern), Welten nur Owner+Member, Entities rein welt-scoped, Bot-Konfiguration nur auf Welt-Ebene, Kampagnen-Rollen als freier String.

## 2. Entscheidung

### 2.1 Sichtbarkeit (Templates)
- `game_systems` und `worlds` erhalten `visibility`: `PRIVATE` (nur Ersteller) / `INVITE_ONLY` (Ersteller + explizit Eingeladene) / `PUBLIC` (alle, lesend/nutzend).
- Einladungsliste nutzt die bestehenden Member-Mechanismen (`world_members`, Welt-Einladungslinks).
- Ändern/Löschen: **nur Ersteller** (`owner_id` auf `game_systems` nachrüsten; Bestandsrows → System-Admin bzw. definierter Migrations-Owner).
- Teilen gilt unterschiedslos für Template-Welten **und** aus Kampagnen entstandene Welten (späteres Teilen der eigenen Welt funktioniert über denselben Mechanismus).

### 2.2 Universum-Fork per Clone-on-Create (Option A)
- Kampagne anlegen = **Welt wird tief kopiert** (bestehender `WorldService.clone`: Regionen, Orte, NPCs, Fraktionen, Entities, Karten). Die Kampagne zeigt auf den **Fork**, nie aufs Template.
- Template-Updates fließen **nicht automatisch** nach (kein Tracking-Overhead, keine Überraschungen laufender Kampagnen).
- Alternative Copy-on-Write (`campaign_id`-Overlay auf Entities, Template-Fallback bei Reads) wurde **verworfen**: jede Query bräuchte Overlay-Logik — zu großer Umbau für den jetzigen Stand; kann bei echtem Speicher-/Sync-Bedarf neu bewertet werden.

### 2.3 System-Versionierung: Pin + manuelles Nachziehen
- Kampagnen **pinnen** die System-Version zum Erstellungszeitpunkt und bleiben darauf (stabile Regeln während der Kampagne).
- UI zeigt „Update verfügbar", sobald eine neuere Version desselben Systems existiert; **Nachziehen manuell** (Button), **automatisiert später** (Migration/Diff der Regelwerke ist eigenes Thema).
- Kein Zwangs-Update laufender Kampagnen.

### 2.4 Bot pro Kampagne (+ DM-Queue für Menschen)
- Bot-Konfiguration wandert auf Kampagnen-Ebene (`campaigns.settings_json → {"bot": {"mode": "autonom|suggest|off"}}`, keine neue Spalte nötig): pro Kampagne an/aus/Modus.
- Menschliche SLs arbeiten dieselbe Pipeline über die **Aktionsliste ab** (Pending-Intents mit Bearbeiten/Freigeben/Ablehnen — `DmQueuePanel`, Approve/Reject-Endpoints existieren).
- Abo-Modell-Koppelung („Bot nur mit Abo X") wird als **Stub/Flag** vorbereitet, aber **nicht** implementiert (aktuell unwichtig).

## 3. Konsequenzen

### Positiv
- Geteilte Templates (Aventurien, Schwertküste) + isolierte Gruppen-Universen ohne Zusatzaufwand pro Gruppe.
- Klare Verantwortlichkeit: Template-Ersteller pflegt, Gruppen spielen ungestört.
- Bot- und Menschen-Entscheidungen laufen über **eine** Pipeline (Intents), nur der Entscheider wechselt.
- Keine Migration laufender Kampagnen bei Template-Updates nötig.

### Negativ / Aufwand
- Migrationen: `visibility` + `owner_id` (game_systems), Bestandsdaten-Zuordnung klären.
- `WorldService.clone` in den Kampagnen-Create-Flow verdrahten + UI-Hinweis („erstellt eigene Kopie").
- Kampagnen-Rollen härten (Enum DM/PLAYER, Befördern/Degradieren, mindestens-1-DM-Invariante).
- Bot liest Konfiguration pro Kampagne statt pro Welt; UI-Schalter pro Kampagne.
- Speicher-Duplikate pro Kampagne (akzeptiert; siehe verworfene Alternative).

## 4. Alternativen (verworfen, s. 2.2)
- **Copy-on-Write:** speichereffizient, Template-Updates propagierbar — aber Overlay-Logik in allen Reads, hohes Refactoring-Risiko.
- **Gemeinsame Live-Welt pro Template:** kein Fork — widerspricht der Anforderung isolierter Gruppen-Universen.

## 5. Umsetzung

Siehe `docs/TASKS.md`:
- **Phase 27:** Shared Universes & DM-Workflow (P27-T01 Visibility+Ownership, T02 Rollen, T03 Fork-Flow, T04 Bot-pro-Kampagne, T05 Versionierung, T06 Queue+Doku-Abnahme)
