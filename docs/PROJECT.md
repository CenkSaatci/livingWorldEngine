# Living World Engine (LWE) — Projektdefinition

> **Status:** Living Document. Diese Datei beschreibt das *Was* und *Warum*. Für das *Wie* siehe `ARCHITECTURE.md`, für das *Wann* siehe `ROADMAP.md` und `TASKS.md`.

---

## 1. Executive Summary

Die **Living World Engine (LWE)** ist eine modulare Web-Plattform für Pen-&-Paper-Rollenspiele, die über klassische Virtual Tabletops (VTT) hinausgeht. Statt einer statischen Karte, auf der nur der Spielleiter Veränderungen herbeiführt, **lebt die Welt**:

- NPCs haben Persönlichkeit, Wissen und Ziele und **reagieren autonom** auf Spieleraktionen und Welt-Events.
- Die Simulation läuft **dauerhaft im Hintergrund** — auch ohne aktive Sitzung.
- Sitzungen sind **Ausschnittspunkte** der durchgehend simulierten Welt; Spieler treten in eine Welt ein, die sich bereits entwickelt hat, und tragen Folgen aus vergangenen (und zwischen-sessionlichen) Ereignissen mit sich.

Kernidee der Architektur: **Trennung von Spiel-Logik** (validierender Game Server mit Regel-Engine) **und Welt-Intelligenz** (KI-Agent oder menschlicher DM), die beide über dasselbe **Event-System** kommunizieren.

---

## 2. Vision Statement

> *„Statt einer statischen Karte, in der nur der DM Veränderungen herbeiführt, reagieren NPCs autonom auf Spieleraktionen. Die Welt lebt — mit oder ohne DM."*

Eine Welt, die nach den Spielregeln tickt, aber ohne menschliche Regie auskommt, wenn der DM das will. Der DM wird vomAllein-Regisseur zum **Dirigenten**, der Prozesse begleitet statt sie komplett herbeizuführen.

---

## 3. Stakeholder

| Stakeholder | Rolle | Wichtigste Interessen |
|---|---|---|
| **Spielleiter (DM)** | Weltbauer, Session-Leiter, AI-Kontrolle | Schneller Weltbau, Überblick über Welt-Status, AI-Modus übersteuerbar |
| **Spieler** | Charakter-Steuerung, Interaktion mit Welt | Fluide Karteninteraktion, klare Wurf-Logs, IMMERSION (Welt reagiert scheinbar intelligent) |
| **Operator / SaaS-Host** | Betreiber der Plattform | Multi-Tenancy, Isolation, Monetarisierung, Skalierbarkeit |

---

## 4. Kernkonzept: Die zwei Modi

Die LWE unterscheidet konzeptionell zwei Modi, die nicht gegeneinander austauschbar sind, sondern **gleichzeitig** aktiv sein können:

### 4.1 Lebende Welt (Hintergrundsimulation)

- Läuft kontinuierlich, ob Spieler aktiv sind oder nicht.
- Welt-Events werden durch Spieleraktionen, Zeit-Ticks oder autonome NPC-Entscheidungen erzeugt.
- Ein separater **AI-Bot-Prozess** beobachtet die Event-Queue.
- Für jeden NPC werden **Persönlichkeit, Wissen, Ziele** in `entities.metadata_json` gespeichert.
- Der Bot baut aus Event-Kontext + NPC-State einen **LLM-Prompt**, empfängt eine vorgeschlagene Aktion und legt sie als `npc_intent` an.
- Der Game Server **validiert** jeden Intent gegen Spielregeln (Reichweite, Sicht, Ressourcen) und führt ihn ggf. aus oder verwirft ihn.
- Konfigurierbar via `worlds.settings_json.ai_mode`:
  - `autonom` — KI darf direkt handeln, sofern Regel-konform
  - `suggest` — KI schlägt vor, DM gibt frei
  - `off` — KI-Bot pausiert für diese Welt

### 4.2 Sitzungs-Modus (DM + Spieler)

- Ein DM lädt Spieler in eine Welt ein und wählt eine **aktive Karte** aus (Dungeon, Stadt, Region).
- Spieler sehen Token auf dieser Karte, bewegen sich, würfeln, chatten.
- Alle Spieleraktionen erzeugen **Welt-Events** → speisen in die Hintergrundsimulation ein.
- Der DM kann während der Session **AI-Modus umschalten**.
- Konsequenzen der Sitzung (getötete NPCs, versiegte Verträge) kehren in die Welt-Simulation zurück und **wirken über die Session hinaus**.

### 4.3 Verhältnis beider Modi

```
   ┌──────────────────────────────────────────────────────────┐
   │          LeBENDE WELT (läuft immer)                     │
   │  ┌──────────────────────────────────────────────────┐    │
   │  │   SITZUNG (tempor<>rer Ausschnitt)               │    │
   │  │   Spieler <-> DM <-> Karte <-> Token           │    │
   │  └─────────────────────┬────────────────────────────┘    │
   │                        │ Events                          │
   │                        ▼                                 │
   │           [world_events Queue]  ◄──── AI-Bot pollt       │
   │                        │                                 │
   │                        ▼                                 │
   │           [npc_intents Queue]  ◄──── AI-Bot               │
   │                    │                                      │
   │                    ▼ validator                            │
   │             AusfÜhrung -> neue Events                    │
   └──────────────────────────────────────────────────────────┘
```

---

## 5. Projektziele (SMART)

| Kategorie | Ziel | KPI |
|---|---|---|
| Funktionalität | Vollständiges System zur Erstellung von Welten, Charakteren, Abenteuern und zur Durchführung von Sitzungen. | 100 % Kern-Features (Map, Char, Rule-Engine, AI-Validierung) implementiert. |
| Modularität | Regelwerke per JSON-Konfiguration austauschbar. | Mindestens 2 Regelwerke (D20Lite, TwoDicePool) ohne Code-Änderung lauffähig. |
| KI-Integration | KI-gesteuerter NPC kann auf Spieleraktion reagieren. | KI-Triggered Intent < 5 s nach auslösendem Event, validiert durch Server. |
| Benutzerfreundlichkeit | Responsive UI für DMs und Spieler, Web + PWA. | Wurf-Aktion < 200 ms Frontend-WS-Latenz. |
| Skalierbarkeit | Multi-Tenancy für SaaS. | 10 gleichzeitige Welten pro Instanz bei < 30 % CPU. |
| Internationalisierung | Worldwide Spielerschaft — UI + Backend + LLM in mehreren Sprachen. | Phase-1-Ship mit DE + EN; neue Sprache via JSON-Datei ergänzbar. |
| Immersion | Spieler spüren, dass die Welt „lebt". | Pro Session ≥ 3 nicht vom DM initiierte NPC-Aktionen sichtbar. |

---

## 6. Out-of-Scope (Phase 1–5)

- 3D-Karten
- Sprach-/Video-Chat (Voice over WebRTC)
- Native Mobile-Apps (PWA reicht)
- Dynamic Lighting auf Pixel-Ebene (stattdessen Fog of War)
- Charakter portrait-Generator (kommt ggf. später als eigener Service)
- Quest-Editor mit visuellem Flow (Phase 1 reicht Node-JSON)

---

## 7. Definition of Done (per Phase)

- Code in `main`, alle Tests grün
- Erforderliche Docs (`TASKS.md`, `ARCHITECTURE.md`, `API.md`) aktualisiert
- Breaking Changes dokumentiert in `CHANGELOG.md`
- Meilenstein im Task-Tracker erreicht
- Code-Review (mind. 1 Reviewer) für nicht-triviale Änderungen

---

## 8. Glossar

| Begriff | Bedeutung |
|---|---|
| **DM** | Dungeon Master, Spielleiter |
| **PC** | Player Character (vom Spieler gesteuerter Charakter) |
| **NPC** | Non-Player Character (vom System oder DM gesteuerter Charakter) |
| **Welt-Event** | Diskretes Ereignis in der Welt (`FIRE_CREATED`, `PROBE_ROLLED`, …), geloggt in `world_events` |
| **NPC-Intent** | Vorschlag einer Aktion durch die KI (`ATTACK`, `MOVE`, `SPEAK`), wartet in `npc_intents` auf Validierung |
| **Rule-Engine** | Komponente, die Würfelproben, Schadensberechnung etc. basierend auf einem JSON-Regelwerk ausführt |
| **Game System** | Sammlung aller Regel-Definitionen (Attribute, Würfel, Kampfregeln) für eine bestimmte Spielart |
| **Karte** | 2D-Repräsentation einer Region einer Welt, mit Grid, Token, Fog of War |
| **Session** | Aktiver Zeitraum, in dem DM + Spieler gleichzeitig mit derselben Welt interagieren |
| **Tenant** | Isolierte Organisationseinheit (typischerweise ein Welten-Besitzer) im SaaS-Betrieb |
| **Template** | Geteiltes, unveränderliches Ausgangsmaterial: ein Spielsystem oder eine Welt mit Sichtbarkeit (privat / Einladungsliste / öffentlich) |
| **Universum** | Geforkte, bespielte Kopie eines Templates im Rahmen einer Kampagne — Änderungen (tote NPCs, Kriege, …) landen hier, nie im Template |
| **Sichtbarkeit** | Wer ein Template sieht/nutzt: `PRIVATE` (nur Ersteller), `INVITE_ONLY` (Ersteller + Eingeladene), `PUBLIC` (alle, lesend/nutzend); ändern darf nur der Ersteller |

---

## 9. Verweise auf Documentation

| Dokument | Zweck |
|---|---|
| [`ROADMAP.md`](ROADMAP.md) | Phasen und Meilensteine auf oberster Ebene |
| [`TASKS.md`](TASKS.md) | Granulare, abarbeitbare Tasks mit IDs und Akzeptanzkriterien |
| [`ARCHITECTURE.md`](ARCHITECTURE.md) | Komponenten, Datenfluss, Sicherheit, Deployment |
| [`DATA-MODEL.md`](DATA-MODEL.md) | Vollständiges DB-Schema + Flyway-Migrationen |
| [`API.md`](API.md) | REST- und WebSocket Endpunkte-Referenz |
| [`RULES-SCHEMA.md`](RULES-SCHEMA.md) | JSON-Schema für austauschbare Regelwerke |
| [`AI-AGENT.md`](AI-AGENT.md) | Bot-Architektur, Event-Protokoll, Prompt-Pattern |
| [`UI-UX.md`](UI-UX.md) | Design-Prinzipien, Komponenten-Struktur, Benutzerflows |
| [`TESTING.md`](TESTING.md) | Test-Pyramide, Frameworks, CI-Gates |
| [`ADR/`](ADR/) | Architecture Decision Records |