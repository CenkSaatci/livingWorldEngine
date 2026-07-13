# ADR-006: Container-Runtime = Podman (statt Docker)

- **Status:** Accepted
- **Date:** 2025-07-12

## Kontext

LWE benötigt eine Container-Runtime für lokale Entwicklung (pgAdmin, optional Redis, AI-Bot, später Production-Builds). Bisheriger Standard wäre Docker + Docker Compose. Optionen:

1. **Docker + Docker Compose** — De-facto-Standard, benötigt Docker Daemon (root)
2. **Podman + podman compose** — Daemon-los, rootless, OCI-kompatibel, Drop-in-Replacement für Docker-CLI
3. **Nerdctl + containerd** — OCI-kompatibel, weniger verbreitet

## Entscheidung

**Podman + `podman compose`** für lokale Entwicklung und Production-Builds.

## Begründung

### Gegen Docker
- Docker Daemon läuft als root-rooted Service — Sicherheitsrisiko auf Entwicklermaschinen
- Auf Linux ohne Docker Desktoplizenz für kommerzielle Nutzung nötig
- Weniger daemon-lose Architektur

### Für Podman
- **Daemon-los**: Prozesse starten direkt, kein `dockerd`-Hintergrundprozess
- **Rootless by default**: Container laufen unter User-Namespace — Sicherheitsvorteil
- **OCI-kompatibel**: Baut und läuft Standard-Container-Images
- **CLI-kompatibel**: `podman build`, `podman run`, `podman compose` sind Drop-in für `docker build`/`docker run`/`docker compose`
- **Compose-Untersützung**: Eingebauter `podman compose`-Subcommand (seit Podman 4.7+) liest `compose.yml` / `docker-compose.yml`
- **Keine Lizenzfragen**: Vollständig Open Source, keine kommerziellen Einschränkungen
- **Containerfile**: Podman liest sowohl `Dockerfile` als auch `Containerfile` — wir nutzen `Containerfile` als geschlechtsneutraleren Namen

## Dateinamen-Konventionen

| Datei | Zweck |
|---|---|
| `compose.yml` | Entwicklung (statt `docker-compose.yml` — Compose-V2/Podman-Konvention) |
| `compose.override.yml` | Persönliche lokale Overrides (in `.gitignore`) |
| `compose.prod.yml` | Production-Setup |
| `Containerfile` | Build-Definition (statt `Dockerfile`) |

`Dockerfile` wird als Alias weiterhin von Podman gelesen — falls externe Tools ihn erwarten, kann das als Fallback dienen.

## Befehle

| Docker | Podman |
|---|---|
| `docker compose up -d` | `podman compose up -d` |
| `docker build -t lwe-backend .` | `podman build -t lwe-backend .` |
| `docker run -p 8080:8080 lwe-backend` | `podman run -p 8080:8080 lwe-backend` |
| `docker compose -f docker-compose.prod.yml up` | `podman compose -f compose.prod.yml up` |
| `docker exec -it <c> bash` | `podman exec -it <c> bash` |

## Konsequenzen

**Positiv:**
- Kein root-Daemon nötig
- Sicherheitsvorteil durch rootless
- Keine kommerziellen Lizenzfragen
- Standard-Compose-Files weiterhin kompatibel

**Negativ:**
- `podman compose` ist minimal anders verhalten als `docker compose` in Edge-Cases (z. B. `depends_on` healthcheck-Syntax, build context)
- Podman Machine auf macOS/Windows notwendig (VMLite/CRC), Docker Desktop nutzt ebenfalls eine VM — kein echter Nachteil
- CI muss `podman`-Image verfügbar haben (GitHub Actions: `redhat-actions/podman-login` etc.)

## Kompatibilitäts-Notiz

Compose-Files werden so geschrieben, dass sowohl `docker compose` als auch `podman compose` sie lesen können. Edge-Constructs wie `depends_on: condition: service_healthy` funktionieren unter beiden. In der Doku verwenden wir konsequent `podman`-Befehle.

## Referenzen

- https://podman.io/
- https://docs.podman.io/en/latest/markdown/podman-compose.1.html
- Podman 5.x-Dokumentation