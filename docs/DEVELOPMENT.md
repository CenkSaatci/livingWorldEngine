# Entwicklung & Betrieb

## Tech-Stack

| Komponente | Technologie |
|---|---|
| Backend | Java 21, Spring Boot 3.3.5, Maven |
| Frontend | React 18, TypeScript, Vite 5, Tailwind CSS |
| Datenbank | PostgreSQL 18 (extern `192.168.31.151:5432`) |
| Migrationen | Flyway (30 Migrationen, V001–V088) |
| Caching | Redis (optional, via `compose.prod.yml`) |
| AI Bot | Python 3.12, FastAPI, httpx, Jinja2 |
| LLM | Ollama (`/api/generate`) oder vLLM (`/v1/chat/completions`) |
| Container | Podman + Podman-Compose |
| WebSocket | STOMP via Spring Messaging |

---

## 1. Entwickler-Setup

### Voraussetzungen

```bash
# JDK 21 (Temurin)
curl -sL "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.6%2B7/OpenJDK21U-jdk_x64_linux_hotspot_21.0.6_7.tar.gz" | tar xz
export JAVA_HOME=/pfad/zu/jdk-21.0.6+7
export PATH=$JAVA_HOME/bin:$PATH

# Node.js 22
# Podman
# PostgreSQL 18 (lokal oder via docker)
```

### Backend starten

```bash
# .env erstellen (siehe .env.example)
cp .env.example .env
# DB_HOST, DB_USER, DB_PASSWORD, JWT_SECRET setzen

# Kompilieren + Testen
mvn clean compile
mvn test

# Lokal starten
mvn spring-boot:run
# → http://localhost:8080
```

### Frontend starten

```bash
cd frontend
npm install
npm run dev
# → http://localhost:5173 (proxied nach :8080)

# Qualitätssicherung
npm run check          # ESLint + Prettier + TypeScript
npm t                  # Vitest
```

### AI Bot lokal

```bash
cd ai-bot
pip install -e .
# Siehe compose.bot.yml für Podman-Setup
```

### Wichtige Befehle

```bash
# Backend-Tests (alle Services)
mvn test -Dtest="AbilityServiceTest,EntityAbilityServiceTest,InventoryServiceTest,WorldServiceTest,RegionServiceTest,CombatServiceTest#shouldStartCombat+shouldStartCombatWithMapId,FileUploadControllerTest,GameSessionServiceTest,EventArchiveJobTest,QuestServiceTest,QuotaServiceTest,WorldMapServiceTest,LevelUpServiceTest,WorldInviteServiceTest,RestServiceTest" -DfailIfNoTests=false

# Frontend-Check
cd frontend && npm run check

# Alle Tests
cd frontend && npx vitest run
```

---

## 2. Architecture Overview

```
┌──────────┐     ┌──────────┐     ┌──────────┐
│ Frontend │────→│ Backend  │────→│ PostgreSQL│
│ :5173    │ WS  │ :8080    │     │ :5432    │
└──────────┘     ├──────────┤     └──────────┘
                 │ Redis    │
                 │ (Cache)  │
                 └──────────┘
                       ↑ poll
                 ┌──────────┐     ┌──────────┐
                 │ AI Bot   │────→│ LLM      │
                 │ :8000    │     │ (Ollama/ │
                 └──────────┘     │  vLLM)   │
                                  └──────────┘
```

### Packages

| Package | Verantwortung |
|---|---|
| `com.lwe.api` | 28 REST-Controller, 28 DTO-Records |
| `com.lwe.config` | Caching, CORS, JWT-Filter, Rate-Limit, Health |
| `com.lwe.core.domain` | 20 JPA-Entities |
| `com.lwe.core.repository` | 25 Spring Data Repositories |
| `com.lwe.core.service` | 28 Services, alle getestet |
| `com.lwe.rules` | D20/Fudge/Pool-RuleEngine + Schema-Validator |
| `com.lwe.security` | JWT-Service, LoginRateLimiter |
| `com.lwe.time` | WorldTimeService, WeatherService |
| `com.lwe.events` | EventArchiveJob, MemoryCleanupJob |

---

## 3. API

Alle Endpunkte unter `/api/v1/...`

### Auth
| Methode | Pfad | Beschreibung |
|---|---|---|
| POST | `/auth/register` | Registrierung (Email, Username, Passwort) |
| POST | `/auth/login` | Login |
| POST | `/auth/refresh` | Refresh-Token |
| POST | `/auth/verify-email` | Email-Verifikation |
| POST | `/auth/forgot-password` | Passwort-Reset (Dev: Token inline) |

### Worlds
| Methode | Pfad | Beschreibung |
|---|---|---|
| GET | `/worlds/accessible?page=&size=` | Welten (paginiert) |
| POST | `/worlds` | Welt erstellen |
| PATCH | `/worlds/{id}` | Welt bearbeiten |
| POST | `/worlds/{id}/clone` | Welt kopieren (inkl. Orte/NPCs/Fraktionen) |
| POST | `/worlds/{id}/invites` | Einladungslink erzeugen |
| POST | `/worlds/join?token=` | Einladung annehmen |

### Entities
| Methode | Pfad | Beschreibung |
|---|---|---|
| GET | `/worlds/{id}/entities` | Alle NPCs/PCs |
| POST | `/entities/{id}/xp` | XP vergeben (DM) |
| POST | `/entities/{id}/rest` | HP/AP regenerieren |
| POST | `/entities/{id}/levelup` | Attributspunkte verteilen |
| POST | `/entities/{id}/reset-points` | Punkte zurücksetzen |

### Combat
| Methode | Pfad | Beschreibung |
|---|---|---|
| POST | `/combat/start` | Kampf starten |
| POST | `/combat/{id}/action` | Aktion (ATTACK/DEFEND/MOVE) |
| POST | `/combat/{id}/ability` | ACTIVE Ability einsetzen |
| POST | `/combat/{id}/next-turn` | Nächster Zug |
| POST | `/combat/{id}/end` | Kampf beenden |

### Adventures
| Methode | Pfad | Beschreibung |
|---|---|---|
| GET | `/adventures?worldId=` | Alle Adventures einer Welt |
| GET | `/adventures/by-location/{id}` | Adventures an einem Ort |
| GET | `/adventures/by-giver/{id}` | Adventures eines NPC |
| POST | `/adventures/{id}/override-text` | DM: Text überschreiben |
| POST | `/adventures/{id}/force-node/{nodeId}` | DM: Node forcen |

### Error-Codes
Alle Fehler folgen dem Schema:
```json
{"error": {"code": "ENTITY_NOT_FOUND", "message": "Entity not found"}}
```
~80 stabile Error-Codes in `ERROR-CODES.md`

---

## 4. Datenbank

### Migrationen

30 Flyway-Migrationen in `src/main/resources/db/migration/`:

```bash
# Status prüfen
mvn flyway:info

# Manuelle Migration
mvn flyway:migrate

# Bei Konflikten: baseline
mvn flyway:baseline -Dflyway.baselineVersion=0
```

### Wichtige Tabellen

| Tabelle | Beschreibung |
|---|---|
| `users` | Benutzer (+ email_verified_at, verification_token) |
| `worlds` | Welten (+ settings_json mit AI-Mode, Time-Config) |
| `entities` | NPCs/PCs (+ HP/AP, XP, Attribute, Fraktion) |
| `items` | Gegenstände (+ bonuses_json, metadata_json für Consumables) |
| `abilities` | ACTIVE/PASSIVE Fähigkeiten |
| `entity_abilities` | Many-to-Many: Entity ↔ Ability |
| `combat_sessions` | Kampf-Sessions |
| `world_events` | Event-Queue (alter als 30 Tage → archiviert) |
| `world_invites` | Einladungs-Tokens |
| `game_systems` | Regelwerke (rules_json + schema_json) |

---

## 5. Deployment

### Production (Docker/Podman)

```bash
# Bauen + Starten
podman-compose -f compose.prod.yml build
podman-compose -f compose.prod.yml up -d

# Migration läuft automatisch via Flyway
# Frontend: nginx serving build
# Backend: Spring Boot + Redis
```

### AI Bot (lokal)

```bash
# compose.bot.yml für Zuhause
# Verbindet sich OUTBOUND mit dem LWE-Server
podman-compose -f compose.bot.yml up -d
```

### Umgebungsvariablen

| Variable | Default | Beschreibung |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL Host |
| `DB_PORT` | `5432` | PostgreSQL Port |
| `DB_NAME` | `lwe` | Datenbankname |
| `DB_USER` | `admin` | Datenbank-User |
| `DB_PASSWORD` | `admin` | Datenbank-Passwort |
| `JWT_SECRET` | `dev-only...` | JWT Secret (min 32 Zeichen) |
| `AI_BOT_LLM_TYPE` | `ollama` | `ollama` oder `vllm` |
| `AI_BOT_LLM_URL` | `http://localhost:11434` | LLM-Server-URL |
| `AI_BOT_LLM_MODEL` | `llama3.1` | LLM-Modell |
| `APP_URL` | `http://localhost:5173` | öffentliche App-URL (für Invite-Links) |

---

## 6. Wartung

### Regelmäßige Tasks

| Task | Frequenz | Automatisch? |
|---|---|---|
| Event-Archivierung | Täglich 03:00 UTC | ✅ Scheduled |
| Memory-Drift | Täglich 01:00 UTC | ✅ Scheduled |
| Session-Cleanup | Manuell | ❌ |
| Log-Rotation | Via Docker | ❌ |

### Monitoring

```bash
# Health-Check
curl https://dein-server.com/actuator/health
# → {"status":"UP","components":{"db":{"status":"UP"},"redis":{"status":"UP"},"bot":{"status":"UP"}}}

# Metrics
curl https://dein-server.com/actuator/metrics
```

### Backup

```bash
# PostgreSQL Dump
pg_dump -h $DB_HOST -U $DB_USER lwe > backup_$(date +%Y%m%d).sql

# Uploads (Map-Bilder)
tar czf uploads_$(date +%Y%m%d).tar.gz /pfad/zu/uploads/
```

### Bekannte Fehler

| Problem | Lösung |
|---|---|
| `InventoryServiceTest.shouldUnequipItem` | Mock `itemRepo.findById` fehlt — siehe Git-History für Fix |
| Rate-Limit zu aggressiv | `lwe.rate-limit.default` in application.yml anpassen |
| Event-Tabelle wächst | `event_archive_days` pro Welt im WorldEditor einstellen |
| Redis nicht verfügbar | Cache fällt auf `spring.cache.type=none` zurück |

---

## 7. Test-Strategie

| Ebene | Framework | Anzahl |
|---|---|---|
| Backend Unit-Tests | JUnit 5 + Mockito | 67 |
| Frontend Unit-Tests | Vitest + Testing Library | 63 |
| **Gesamt** | | **130** |

Neue Services müssen einen `*ServiceTest.java` haben (Mockito + JUnit 5).
Neue Komponenten müssen einen `.test.tsx` haben (Vitest + Testing Library).

---

## 8. Fehlerbehandlung

Jeder Service wirft eine domänenspezifische Exception:

```java
throw new CombatException("COMBAT_NOT_FOUND", "Combat session not found");
```

Der `GlobalExceptionHandler` fängt alle ab und gibt einheitliches JSON zurück:

```json
{"error": {"code": "COMBAT_NOT_FOUND", "message": "Combat session not found"}}
```

Error-Codes sind in `ERROR-CODES.md` dokumentiert.
