# User Guide — Living World Engine

> Schritt-für-Schritt-Anleitung für Spielleiter (DM) und Spieler.

---

## 1. Registrierung & Anmeldung

1. Rufe die LWE-Webseite auf (`http://localhost:5173` in Entwicklung, `https://deinedomain` in Produktion)
2. Klicke auf **Registrieren**
3. Gib E-Mail, Benutzername und Passwort ein
4. Nach erfolgreicher Registrierung landest du im **Dashboard**

---

## 2. Dashboard (Welten verwalten)

Im Dashboard siehst du alle deine Welten. Hier kannst du:

- **Neue Welt erstellen:** Klick auf „Erstellen", gib Namen ein, fertig
- **Welt betreten:** Klick auf eine Welt-Karte → gelangst zum **Spielfenster**
- **Sprache wechseln:** Klick auf DE/EN in der oberen Leiste
- **Abmelden:** Klick auf „Abmelden"

---

## 3. Spielfenster (Karte, Chat, Charakter)

Nachdem du eine Welt betreten hast, siehst du:

### Karte (Canvas)
- **Grid** (quadratisch) mit Zoom (Mausrad) und Pan (Ziehen)
- **Token** stellen Charaktere und NPCs dar
- **Fog of War** (DM-only) blendet Bereiche für Spieler aus

### Chat (rechtes Panel)
- Nachricht eingeben und mit Enter absenden
- `/r 1d20+5` für Inline-Würfel (rolled via Rule-Engine)
- Wurf-Log zeigt vergangene Proben

### Charakterbogen
- Attribute anzeigen/bearbeiten (INT = Range, BOOL = Switch)
- Skill-Liste mit Würfel-Button pro Skill
- Klick auf Würfel → API-Call → Ergebnis in der UI
- Zustands-Badges (z. B. „Wunde"), Schicksalspunkte ★ (Re-Roll), Rüstungswert im Kopfbereich
- „Formel-Overrides" überschreiben abgeleitete Werte pro Charakter

### Charakter-Wizard (P30)
- Voraussetzung: aktive Kampagne, deren Game-System ein Erstellungs-Budget (`creationBudget`) oder Pakete (`packages`) definiert
- Welt → Charaktere → **Charakter-Wizard**: Pakete wählen (Spezies/Kultur/Profession, inkl. Auswahl-Gruppen wie „MU oder KK −1"), Attribute kaufen, Merkmale wählen, speichern
- Live-Budget zeigt verbrauchte/verfügbare AP; Hinweise (Empfehlungen) und Probleme (Budget, Bereiche, Exklusionen) erscheinen vor dem Speichern
- Auto-Merkmale aus Paketen werden übernommen (z. B. „Nachtsicht"); der Charakter erhält Endwerte + Traits + Schicksalspunkte

### Game-System bauen (System-Wizard, P28/P29)
- Game Systems → **New System**/Bearbeiten öffnet den Wizard (Steps u. a. Budget, Merkmale, Pakete, Bedingungen)
- Vorlagen laden (D20Lite/TwoDicePool/Fudge) oder Referenz nutzen: `docs/examples/dsa5.json`
- Speichern ist gated: fehlende Attribute/Paketnamen o. Ä. blockieren mit Prüfbericht

---

## 4. DM-Funktionen

### KI-Intent-Queue
- Der AI-Bot schlägt NPC-Aktionen vor
- DM sieht pendente Intents im **DM-Queue Panel**
- Approve = Aktion wird ausgeführt
- Reject = Aktion wird verworfen

### Welt-Zeit steuern
über `POST /api/v1/worlds/{id}/time/advance` etc.
Im Frontend (Phase 5): Buttons in der Status-Leiste

### Mitglieder einladen
1. Welt öffnen → Invite-Link kopieren
2. Link an Spieler senden
3. Spieler klickt auf Link → wird Mitglied

---

## 5. Regelwerke (Game Systems)

Administratoren können Regelwerke hochladen:

```bash
curl -X POST http://localhost:8080/api/v1/game-systems \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"D20Lite","version":1,"rulesJson":"{\"version\":1,\"attributes\":[...]}","schemaJson":"..."}'
```

**Unterstützte Systeme:**
- **D20Lite**: 1W20 + Modifikator vs. Zielwert
- **TwoDicePool**: 2W6 + Attribut vs. Erfolgsstufen (6/8/11)

Jedes System wird via `DiceExpressionParser` automatisch erkannt.

---

## 6. API-Übersicht (für Entwickler)

Siehe [`docs/API.md`](API.md) für die vollständige Referenz.

| Bereich | Endpunkt |
|---|---|
| Auth | `POST /api/v1/auth/login\|register\|refresh` |
| Welten | `GET/POST/PATCH /api/v1/worlds` |
| Charaktere | `POST /api/v1/worlds/{id}/entities` |
| Würfeln | `POST /api/v1/rolls` |
| Kampf | `POST /api/v1/combat/start\|action\|next-turn\|end` |
| Inventar | `POST /api/v1/entities/{id}/inventory/equip\|unequip` |
| WebSocket | `ws://host/ws` (STOMP) |

---

## 7. Fehlerbehebung

| Problem | Lösung |
|---|---|
| „Token invalid" | Neu einloggen (Token nach 24 h abgelaufen) |
| „World not found" | Prüfen ob Welt aktiv (nicht soft-deleted) |
| Keine WS-Verbindung | Browser-Konsole prüfen, STOMP-Header mit JWT |
| Bot reagiert nicht | `GET /health` am Bot-Endpunkt prüfen |
| LLM-Fehler | Prüfe Ollama/vLLM-URL und Modell-Name |

---

## 8. Glossar

| Begriff | Bedeutung |
|---|---|
| **DM** | Dungeon Master — Spielleiter, verwaltet die Welt |
| **PC** | Player Character — vom Spieler gesteuerter Charakter |
| **NPC** | Non-Player Character — vom System/DM gesteuerter Charakter |
| **Intent** | KI-Vorschlag für eine NPC-Aktion (z. B. ATTACK, MOVE) |
| **Token** | Grafischer Marker für einen Charakter auf der Karte |
| **Fog of War** | Sichtbarkeitsmaske — DM legt fest, was Spieler sehen |
| **Game System** | JSON-Regelwerk (Attribute, Würfel, Kampfregeln) |
| **Rule-Engine** | Backend-Komponente, die Würfelproben berechnet |
| **Time Engine** | Automatische/manuelle In-Game-Zeit (Tag/Nacht) |