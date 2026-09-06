# Living World Engine (LWE)

> Diese Datei ist ein Einstiegspunkt. Die vollständige Projektdokumentation liegt unter [`docs/`](docs/).

## Schnelleinstieg

| Was | Wo |
|---|---|
| Projektvision, Ziele, Scope | [`docs/PROJECT.md`](docs/PROJECT.md) |
| Phasen & Meilensteine | [`docs/ROADMAP.md`](docs/ROADMAP.md) |
| Granulare Aufgabenliste (mit IDs) | [`docs/TASKS.md`](docs/TASKS.md) |
| Development Workflow (TDD-Zyklus) | [`docs/WORKFLOW.md`](docs/WORKFLOW.md) |
| Technische Architektur | [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) |
| Datenmodell & Migrationen | [`docs/DATA-MODEL.md`](docs/DATA-MODEL.md) |
| API-Referenz (REST + WS) | [`docs/API.md`](docs/API.md) |
| Regelwerk-JSON-Schema | [`docs/RULES-SCHEMA.md`](docs/RULES-SCHEMA.md) |
| AI-Bot-Architektur | [`docs/AI-AGENT.md`](docs/AI-AGENT.md) |
| UI/UX-Design & Komponenten | [`docs/UI-UX.md`](docs/UI-UX.md) |
| Testing-Strategie | [`docs/TESTING.md`](docs/TESTING.md) |
| Fehlercode-Katalog | [`docs/ERROR-CODES.md`](docs/ERROR-CODES.md) |
| Architektur-Entscheidungen | [`docs/ADR/`](docs/ADR/) |

## Setup (vorbereitet in Phase 1)

1. `.env` aus `.env.example` kopieren und anpassen (PostgreSQL-Zugangsdaten)
2. Backend (Spring Boot): `cd backend && mvn spring-boot:run` (Java 21, übernimmt `.env` automatisch via
   `spring-dotenv`)
3. Frontend (Phase 3): `cd frontend && pnpm install && pnpm dev` (liest ebenfalls root `.env`
   via `envDir: ..`)
4. AI-Bot (Phase 4): `cd ai-bot && uvicorn ai_bot.main:app --reload`
5. Container (optional): `podman compose up -d` (für pgAdmin etc., siehe
   [`docs/ADR/006`](docs/ADR/006-container-runtime-podman.md))

## Status

- **Phase:** Entwicklungs-Grundlage gelegt
- **Dokumentation:** Vollständig (Projects, Roadmap, Tasks, Architecture, Data-Model, API,
  Rules-Schema, AI-Agent, UI-UX, Testing, Error-Codes, 9 ADRs)
- **Skelette:** Backend (Spring Boot, Java 21, Maven, Package-Layout, application.yml,
  i18n-Bundles DE/EN) + Frontend (Vite + React + TS + Tailwind, i18next DE/EN für 7 Namespaces,
  Zustand-Stores `authStore`/`worldStore`, Axios-Client mit `Accept-Language`)
- **DB-Host:** Externer Portainer-Postgres unter `192.168.31.151:5432` (siehe `.env`)
- **Erste Code-Tasks:** `P1-T02` (Podman-Compose) + `P1-T03` (Flyway-Migration V001), siehe
  [`docs/TASKS.md`](docs/TASKS.md)

## Weiterführende Dokumentation

Siehe [`docs/`](docs/).