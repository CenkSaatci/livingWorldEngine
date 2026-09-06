# Smoke-Test — Phase 1 (Meilenstein M1)

> Vollständiger manueller End-to-End-Test aller Phase-1-Komponenten. Ziel: Nachweis, dass das komplette Backend-Grundgerüst (REST API, Datenbank, WebSocket, Authentifizierung, Regelwerk-Persistenz, Weltenverwaltung) funktioniert.

---

## Voraussetzungen

- **Server läuft:** `cd backend && JAVA_HOME=~/.local/share/jdk21 mvn spring-boot:run`
- **Datenbank:** PostgreSQL auf `192.168.31.151:5432/lwe` erreichbar (Credentials in `.env`)
- **Tooling:** `curl` installiert, `jq` optional für JSON-Better-Reading

---

## 1. Health-Check

```bash
curl -s http://localhost:8080/actuator/health
```

**Erwartet:** `{"status":"UP"}`

---

## 2. Benutzer registrieren

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -H "Accept-Language: en" \
  -d '{"email":"smoke@lwe.de","username":"smoker","password":"Test123!"}' | jq .
```

**Erwartet:** `201 Created`, JSON mit `accessToken`, `refreshToken`, `role: "USER"`, `locale: "en"`.

---

## 3. Login

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -H "Accept-Language: de" \
  -d '{"email":"smoke@lwe.de","password":"Test123!"}' | jq .
```

**Erwartet:** `200 OK`, JSON mit `accessToken` + `refreshToken`. Token für Folgeschritte in Variable speichern:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"smoke@lwe.de","password":"Test123!"}' | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
```

---

## 4. Regelwerk anlegen (Game System)

```bash
curl -s -X POST http://localhost:8080/api/v1/game-systems \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "$(cat src/test/resources/rules/d20lite.json | jq --arg schema '{}' '. | {name: "D20Lite", version: 1, rulesJson: ., schemaJson: $schema}')" | jq .
```

> Alternativ Request mit explizitem JSON:

```bash
GS_ID=$(curl -s -X POST http://localhost:8080/api/v1/game-systems \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"D20Lite","version":1,"rulesJson":"{\"version\":1,\"attributes\":[{\"name\":\"staerke\",\"type\":\"INT\",\"min\":1,\"max\":20,\"default\":10}],\"dice_mechanics\":{\"probe\":\"1d20+mod\"}}","schemaJson":"{\"type\":\"object\",\"properties\":{}}"}' | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
echo "GS_ID=$GS_ID"
```

**Erwartet:** `201 Created`, JSON mit `id`, `name: "D20Lite"`, `version: 1`.

---

## 5. Regelwerk validieren

```bash
curl -s -X POST "http://localhost:8080/api/v1/game-systems/$GS_ID/validate" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

**Erwartet:** `{"valid": true}`

---

## 6. Welt erstellen

```bash
WORLD_ID=$(curl -s -X POST http://localhost:8080/api/v1/worlds \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"SmokeWorld","gameSystemId":"'$GS_ID'","settingsJson":"{\"ai_mode\":\"suggest\",\"grid\":\"hex\"}"}' | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
echo "WORLD_ID=$WORLD_ID"
```

**Erwartet:** `201 Created`, JSON mit `id`, `name: "SmokeWorld"`, `owner_id` (entspricht User-ID).

---

## 7. Eigene Welten auflisten

```bash
curl -s http://localhost:8080/api/v1/worlds \
  -H "Authorization: Bearer $TOKEN" | jq .
```

**Erwartet:** Array mit mindestens einem Element (SmokeWorld).

---

## 8. Zugängliche Welten auflisten (Owner + Member)

```bash
curl -s http://localhost:8080/api/v1/worlds/accessible \
  -H "Authorization: Bearer $TOKEN" | jq .
```

**Erwartet:** Array, das SmokeWorld enthält.

---

## 9. Zweiten User registrieren und einladen

```bash
# Zweiten User registrieren
MEMBER_ID=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"member@lwe.de","username":"member","password":"Test123!"}' | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
echo "MEMBER_ID=$MEMBER_ID"

# Mitglied einladen (Owner)
curl -s -X POST "http://localhost:8080/api/v1/worlds/$WORLD_ID/members" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"userId":"'$MEMBER_ID'","role":"PLAYER"}' | jq .
```

**Erwartet:** `201 Created`, JSON mit `user_id` = MEMBER_ID, `role: "PLAYER"`.

---

## 10. Zugriff mit falschem Token verweigern

```bash
curl -s http://localhost:8080/api/v1/worlds \
  -H "Authorization: Bearer invalid-token" | jq .
```

**Erwartet:** `401`, JSON mit `error.code: "AUTH_TOKEN_INVALID"`.

---

## 11. Fremde Welt aufrufen (Error)

```bash
curl -s "http://localhost:8080/api/v1/worlds/00000000-0000-0000-0000-000000000000" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

**Erwartet:** `404`, JSON mit `error.code: "WORLD_NOT_FOUND"`.

---

## 12. WebSocket Test-Event

Der WebSocket-Test erfordert einen STOMP-Client. Mit einem Browser-Devtool oder Node-Script:

```javascript
// Browser-Code (DevTools Console):
const ws = new WebSocket('ws://localhost:8080/ws');
ws.onopen = () => {
  ws.send('CONNECT\nAuthorization: Bearer ' + TOKEN + '\n\n\x00');
  // Nach CONNECT-Response:
  ws.send('SUBSCRIBE\nid:sub-0\ndestination:/topic/world/' + WORLD_ID + '\n\n\x00');
};
ws.onmessage = (e) => console.log('WS Event:', e.data);
```

Dann REST-Event triggern:
```bash
curl -s -X POST "http://localhost:8080/api/v1/test/ws/$WORLD_ID" \
  -H "Authorization: Bearer $TOKEN"
```

**Erwartet:** `{"sent": true, "topic": "/topic/world/..."}`. WS-Client empfängt `{"event_type":"TEST_EVENT",...}`.

---

## 13. Welt Soft-Delete

```bash
curl -s -X DELETE "http://localhost:8080/api/v1/worlds/$WORLD_ID" \
  -H "Authorization: Bearer $TOKEN"
echo ""

# Prüfen, ob Welt verschwunden
curl -s http://localhost:8080/api/v1/worlds \
  -H "Authorization: Bearer $TOKEN" | jq '. | length'
```

**Erwartet:** `204 No Content` auf Delete. Liste nach Delete hat 0 Welten (oder weniger).

---

## Ergebnis-Dokumentation

Nach erfolgreichem Durchlauf:
- Alle 13 Schritte sind grün → **Meilenstein M1 erreicht**
- Bei Fehlern: Logs in `backend/target/` prüfen, ggf. Schritt wiederholen
- Bekannte Nicht-Probleme:
  - Flyway-Warnung `PostgreSQL 18.3 is newer than this version` ist harmlos
  - `hibernate.dialect`-Deprecation-Warnung ist harmlos (Hibernate wählt automatisch)

---

## Verweise

- [`TASKS.md`](TASKS.md) — P1-T08
- [`docs/API.md`](API.md) — Endpunkt-Referenz
- [`docs/ADR/`](ADR/) — Architektur-Entscheidungen