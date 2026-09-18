# ADR-014: Generische Kampfwert-Formeln und Kampagnen-Leiterrechte

- **Status:** Akzeptiert (Design, 2026-09-18)
- **Kontext:** AT/PA aus simplen Attributsmitteln (+4) sind zu hoch (AT/PA ~17), Parade wird gar nicht gewürfelt; Schaden ignoriert den Würfelteil von `combat.damage`; jeder DM konnte jeden zum Leiter befördern und Spieler sahen fremde Charaktere.

## Understanding Summary

- **Was:** Generische, pro System definierbare Kampfwert-Formeln (Angriff/Parade/Ausweichen aus Skills + Attribut-Schwellen; Schaden aus Würfel + Boni-Stufen), korrekt gewürfelt; Kampagnen-Rechte nach Ersteller-prägt-Leiter-Modell.
- **Warum:** DSA-Formeln (KTW + Schwellen über 8) nicht abbildbar, DnD rechnet anders (Mod + Übung), Rechte zu weit.
- **Wer:** Systemautoren (Regel-JSON), Spieler (nur eigene Charaktere), Leiter (alle Charaktere + Spielerverwaltung), Ersteller (Leiter-Rollen).
- **Constraints:** Alles im Regel-JSON pro System, kein System-Hardcode in der Engine; Kampfablauf unverändert.
- **Non-goals:** Kein neuer Kampfablauf/Parade-Wurf jetzt, keine UI-Neugestaltung, kein Balancing.

## Annahmen

- Schwellen zählen nur oberhalb (`max(0, floor((Wert-8)/3))` o. ä.), pro System formulierbar.
- Formelauswertung bleibt auf `FormulaEvaluator` beschränkt (kein Scripting).
- Ersteller = initialer Leiter; letzter Leiter bleibt geschützt.
- Alte Systeme ohne neue Felder laufen unverändert (NFR-Defaults bestätigt: <50 ms, keine N+1, Fehler statt falscher Werte).

## Decision Log

| Entscheidung | Alternativen | Warum |
|---|---|---|
| Nur Formeln + korrektes Würfeln, Ablauf später | Aktive Parade jetzt einbauen | Kampfablauf bewusst vertagt (Vorgabe) |
| Skills in Formeln referenzierbar | Nur Attribute | DSA-KTW/Ausweichen brauchen Skillwerte |
| Alles pro System im Regel-JSON | Globale Konstanten | Systeme bleiben reine Daten (ADR-012) |
| Schaden als Stufen pro System | Wie bisher | Waffen-/Zauberwürfel + Boni je System nötig |
| Ansatz A (Formel-Kontext erweitern) | Strukturiertes Schema (B), Hybrid (C) | Wenig Code, maximal generisch |
| Skill-`kind` als freier Content-String | Feste Enum / kein Kind | Trennt Kampf-/Handwerks-/Sozial-Skills, verhindert „Angriff mit Schmieden", offen für Ausrüstung |
| Rechte-Modell A (Ersteller vergibt Leiter) | Alle verwalten alles / Ersteller verwaltet alles | Beobachtetes Problem gezielt behoben |
| Fail-closed statt Fail-open bei Formel-/Schadensfehlern | Stille Defaults (Gate-aus = Treffer, 1d6-Fallback) | Review: stiller Treffer ist der schlimmste Fehler |
| Fehlend = Default, kaputt = Fehler | Alles Fehler / alles Default | Backwards-kompatibel ohne Lügen |
| Schadens-Attributbonus als System-Formel | Feste `floor((Attr-10)/2)`-Konstante | Review: Konstante verletzt Generik-Gebot |
| Eine Schadens-Pipeline für alle Quellen | Sonderwege je Quelle | Review: Manöver/Ability wichen ab |
| `creatorId` persistiert + Übergabe-Pfad, Legacy → Welt-Owner | Nur letzter-Leiter-Schutz | Review: Austritt/Account-Löschung sonst undefiniert |
| Kampfaufstellung für Teilnehmer + Leiter sichtbar | Alles filtern | Taktik braucht Roster; Bögen bleiben privat |
| Case-insensitiv + formeltaugliche Namen/`ref`-Alias | Case-sensitiv überall | Review: drei uneinheitliche Vergleiche im Code |
| Evaluator-Limits + Single-Load + seedbarer RNG | Wie bisher | Review: DoS-, N+1-, Testbarkeits-Einwände |

## Finales Design

**1. Skills als Formel-Variablen.** `derived_values`-Formeln lesen jeden Namen aus `skills[]` desselben Regel-JSON (z. B. `Schwerter`, `Dolche`). Wertquelle: Charakter-`skillsJson`, Fallback Regel-`bonus` (explizite 0 ist gültig), sonst klarer Formelfehler. Beispiel (Content): `at_schwerter = Schwerter + max(0, floor((mut-8)/3))`, `ausweichen = floor(gewandtheit/2)`.
- Namensregeln (Review): Groß-/Kleinschreibung wird überall einheitlich **case-insensitiv** behandelt; Kollisionen (auch case-insensitiv) meldet der Validator als Fehler. Namen mit Leerzeichen/Klammern (z. B. `Kampf (Raufen)`) sind in Formeln **nicht** referenzierbar — der Validator verlangt formeltaugliche Namen (`[A-Za-z_][\w]*`, Umlaute ok) oder ein optionales `ref`-Alias-Feld; sonst Fehler statt stiller Bruch.
- Skills werden dem Formelkontext explizit beigemischt (heute fehlen sie dort — Kern-Umsetzung). Korruptes `skillsJson` wird geloggt und fällt aufs Regel-`bonus` zurück (kein stiller Erfolg).
- `DerivedValueService` rundet weiter auf (`ceil`); Content nutzt explizites `floor` wo abgerundet werden soll (Doku + Beispiele). Schwellen-Muster (`max(0, …)`) ist Content-Verantwortung, dokumentiert.
- `FormulaEvaluator` bekommt Längen-/Tiefen-Limits (Validator + Evaluator, gegen `StackOverflow`/DoS).

**2. Skill-Arten.** Optionales freies `kind`-Feld pro Skill (`combat`, `craft`, `social`, …). UI gruppiert danach; Angriffs-/Waffen-Configs verlangen passende Art (Validator-**Warnung** bei Fehlgriff, kein harter Fehler). Waffen verweisen später per Item-Feld auf den Skillnamen. (YAGNI-Einwand verworfen: `kind` hat sofort Konsumenten in Validator + UI + Angriffsprüfung.)

**3. Schadens-Stufen.** Fixe, dokumentierte Reihenfolge: Waffen-/Aktionswürfel → flat Bonus → Attribut-Schwellenbonus → Sonderfertigkeits-/Merkmalsboni → Ziel-Schutz (erst Rüstung, dann Multiplikator; Integer-Halbierung dokumentiert).
- **Fehleregeln:** Feld *fehlend* → dokumentierter Default (z. B. `1d6`, Boni 0, alte Systeme laufen weiter). Feld *vorhanden, aber kaputt* → klarer Fehler, kein stiller Ersatz.
- Der fest verdrahtete `floor((Attr-10)/2)`-Bonus wird eine **systemdefinierte Formel** (Default dokumentiert, DnD-kompatibel) statt Engine-Konstante.
- Manöver- und Fähigkeits-Schaden laufen durch **dieselbe** Stufen-Pipeline (Waffe des Actors bzw. Aktions-/Zauberwerte als Eingabe), statt drei Sonderwege.
- Regeln werden pro Kampfaktion **einmal** geladen und durchgereicht (kein N+1); Würfel-RNG seedbar für Tests.

**4. Kampagnen-Rechte.** Nur der Ersteller vergibt/entzieht Leiter (persistiertes `creatorId`; Beförderungsversuche anderer → `DM_REQUIRED` mit Ersteller-Nennung). Leiter verwalten Spieler (einladen/entfernen), keine Beförderungen. Spieler sehen/steuern nur eigene Charaktere (serverseitig gefiltert); Leiter alle. Kampfaufstellung sehen Kampfteilnehmer + Leiter (taktisch nötig), Charakterbögen bleiben owner/DM-only.
- Kanten: Ersteller-Rolle braucht einen **Übergabe-Pfad** (Nachfolger bestimmen, dann erst Austritt/Degradierung); Legacy-Kampagnen ohne Ersteller: Welt-Owner tritt ein. Letzter Leiter bleibt geschützt, kann aber nach Übergabe gehen. Welt-Owner-Rechte gelten auf Welt-Ebene, Kampagnen-Rollen auf Kampagnen-Ebene (Präzedenz dokumentiert).
- Folge-UI (später): lesbare Rechte-Fehler, Übergabe-Dialog, Formel-Autocomplete/Beispiele im Wizard, Fehlerdarstellung auf dem Bogen (Wert rot + Hinweis statt Zahl, Angriff mit kaputter Formel blockiert mit Meldung).

## Risiken

- Formelkorrektheit liegt beim Systemautor → Validator + Content-Tests als Netz.
- Namensraum-Kollisionen Skill/Attribut → Validator-Fehler statt stiller Schatten.
- Rechteänderung kann bestehende Runden überraschen → dokumentieren, kein stiller Datenumbau.

## Teststrategie

- Formel-Eval mit Skills/Schwellen (inkl. Fallback-Reihenfolge, unbekannte Namen, Kollisionen).
- Angriffs-/Schadens-Pipeline je Quelle (`attribute`/`value`/`skill`), DnD- und DSA-Beispiele.
- Validator: Art-Fehlgriff-Warnung, Namens-Kollision.
- Rechte: Ersteller-only-Beförderung, Leiter-verwaltet-Spieler, Listen-Filterung, Letzter-Leiter-Schutz.
