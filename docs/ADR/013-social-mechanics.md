# ADR-013: Social Mechanics — generische Bausteine statt System-Hardcode

> **Hinweis:** Task-Nummern als P35-SM-01…04 geführt (Kollision mit
den Security-Tickets P34-T01…T03 vermieden, s. TASKS.md).

- **Status:** Akzeptiert (Konzept, 2026-09-12)
- **Kontext:** P22 war als Konzeptphase offen. Vorhanden sind: `RelationshipService` (Beziehungen zwischen Entities), `MemoryService` (Erinnerungen mit Sentiment), `NpcIntentService` (SPEAK/CHANGE_RELATION über LLM+Validator), Conditions (P29) und Skill-Proben mit FW-Ausgleich (P16/P19).

---

## 1. Leitprinzip (ADR-012 gilt weiter)

> **Mechanik in die Engine, Inhalte in `rulesJson`/Content.**

Soziale Interaktionen dürfen **keine** DSA-/D&D-spezifischen Talente im Engine-Code verankern. Die Engine stellt generische Bausteine bereit, Systeme liefern Kataloge.

## 2. Entscheidung

1. **Soziale Proben sind Skill-Proben mit Beziehungs-Modifikator.**
   Ein `social_action` ist eine normale Probe auf einen Skill aus `rulesJson.skills`, deren Zielwert um den Beziehungswert (RelationshipService) und aktive Conditions modifiziert wird. Kein eigener Probe-Typ.
2. **Furcht/Moral sind Conditions.**
   `conditions[]` (P29) erhält Content-Einträge wie „Verängstigt" (probe −2) oder „Ermutigt" (probe +1); Aktions-/Flucht-Sperren bleiben bewusst zurückgestellt (P29-Teilstand).
3. **Beziehungsänderungen laufen über `CHANGE_RELATION`-Intents** (bereits vorhanden) — DM/Bot entscheidet, die Engine validiert und persistiert. Kein Auto-Persuade.
4. **Fraktions-Standing bleibt Content/abgeleitet**, kein neues Engine-Konzept: Faction-Relations + Formeln genügen heute.
5. **Verbale Inhalte bleiben LLM/DM-Sache.** Die Engine liefert nur Zahlen, Zustände und Ereignisse (WS), nie Dialogtexte.

## 3. Konkrete Folge-Tasks (P34-Vorschlag, nicht Teil von P33)

- **P35-SM-01:** Beziehungs-Modifikator in `ProbeService` verdrahten (`social: true`-Flag + `relationshipModifier`), Tests mit RelationshipService-Stub.
- **P35-SM-02:** `social_actions[]` im Schema (Skill-Referenz, Beziehungs-Gewicht, Erfolgs-/Fehlschlag-Conditions) + Wizard-Editor (kleiner Block im Specials-Step).
- **P35-SM-03:** UI: Relationship-Badges im NPC-View + sozialer Proben-Button; E2E (Probe/Beziehung/Reaktion).
- **P35-SM-04 (optional):** DM-Queue-Typ „SOCIAL_REACTION" für automatische NPC-Reaktionen auf soziale Aktionen.

## 4. Alternativen (verworfen)

- **DSA-Talentliste im Code:** verletzt ADR-012 (System-Hardcode), verworfen.
- **Auto-Erfolg über LLM:** nicht deterministisch/auditierbar, verworfen — LLM bleibt Vorschlagsgeber (P4).
- **Eigenes „Social-Engine"-Subsystem mit Regeln:** Overkill; Skills + Conditions + Beziehungen decken alle DSA/D&D-Muster ab.

## 5. Konsequenzen

- Kein neuer Probe-Typ, keine Migration nötig; Erweiterung ist rein additiv (Schema-Feld + Service-Modifikator).
- Content-Autoren definieren soziale Talente selbst (z. B. „Überreden" existiert bereits im DSA-Beispiel).
- Risiko: Beziehungs-Modifikator muss klar dokumentiert werden (Cap/Formel), sonst „+50 auf alles"-Systeme durch Content.
