# ADR-005: KI-Aktionen müssen durch den Game Server validiert werden

- **Status:** Accepted
- **Date:** 2025-07-12

## Kontext

LWE hat einen separaten KI-Bot, der NPC-Aktionen vorschlägt. Frage: Soll der KI-Bot Aktionen **direkt in die Welt schreiben** (volle Vertrauensstellung) oder **vorschlagen** und der Game Server entscheidet über die Ausführung?

Mitwirkende Risiken:
- KI-Halluzinationen (LLM verletzt Spielregeln)
- KI kann falsche IDs, invalide Items oder unsinnige Positionen referenzieren
- Manipulierter Bot (compromised) könnte die Welt korrumpieren
- Schlechte Spielerfahrung, wenn unsinnige Aktionen landen

## Entscheidung

**Jede KI-Aktion muss vom Game Server validiert werden, bevor sie ausgeführt wird.** Der Bot darf ausschließlich `npc_intents` mit Status `pending` anlegen — niemals direkt eine `world_event` oder Zustandsänderung verursachen.

## Begründung

### Gegen „KI darf direkt schreiben"
- KI kann Regeln falsch interpretieren, die nicht im Prompt standen (z. B. „Waffenlos" trotz bewaffnet)
- KI kann Ressourcen ignorieren (AP, Inventar)
- KI kann abstrakte IDs halluzinieren (`target_id` existiert nicht)
- Undo unmöglich, wenn einmal ausgeführt
- Compromised Bot = kaputte Welt

### Für Validierung durch Game Server
- Game Server hat **alle** Regeln zentral (Rule-Engine)
- Game Server hat Access zu Datenbank ohne Rate/Limit
- Validation ist explizit pro Aktionstyp (ATTACK, MOVE, SPEAK, ...)
- Bei Ablehnung wird `rejection_reason` persistiert — Trainings-Feedback-Material
- DM kann jederzeit `ai_mode` umschalten (autonom/suggest/off)

### Validation Pipeline
Siehe [`AI-AGENT.md`](../AI-AGENT.md) Abschnitt 7:
1. Regel-Check (Range, AP, Sicht)
2. Visibility-Check (Fog of War)
3. Range-Check (Tile-Distanz)
4. Resource-Check (Inventar, HP)
5. (Conditional) DM-Approval bei `ai_mode=suggest`

Schlägt ein Check fehl → Intent `rejected`, `rejection_reason` gesetzt, Event `NPC_INTENT_REJECTED` gepublished.

## Konsequenzen

**Positiv:**
- Spielregeln sind Single-Source-of-Truth
- Compromised Bot kann keine Spielintegrität verletzen
- Auto-Trainingsdaten: zurückgewiesene Intents sind wertvoll für Prompt-Tuning
- DM kann „Drift" der KI überwachen

**Negativ:**
- Latenz: Bot → Server → Validator → Execution → Events → Bot. Kritischer Pfad etwas länger
- Zusätzliche Komplexität in `IntentValidator`

## Sicherheitsbetrachtung

- Bot-Service-Auth via Service-Token, Rolle `BOT`
- Token hat nur Berechtigungen für `POST /api/npc-intents` und `GET /api/worlds/{id}/events`
- Token hat **keine** Schreibrechte auf `worlds`, `entities`, `world_events` direkt
- Compromised Bot kann.Intent-Flood erzeugen → Rate-Limit pro Welt (z. B. 10 Intents / Minute)

## Implementations-Hinweise

- `IntentValidator`-Klasse (+ Chain von `IntentValidationStep`-Interfaces)
- Pro Intent-Typ eigene Strategie (z. B. `AttackValidator`, `MoveValidator`)
- Ergebnis `ValidationResult` mit `approved: bool`, `reason: str`
- Bei `approved=true` und `ai_mode=autonom`: sofortige Execution via `IntentExecutor`
- Bei `approved=true` und `ai_mode=suggest`: Wartet auf DM-Approval

## Referenzen

- [`AI-AGENT.md`](../AI-AGENT.md) Abschnitt 7 — Validator-Pipeline-Diagramm
- Projekt-`PROJECT.md` Risikoanalyse KI-Halluzinationen