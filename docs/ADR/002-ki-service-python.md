# ADR-002: KI-Service in Python (nicht Rust)

- **Status:** Accepted
- **Date:** 2025-07-12
- **Decision Owner:** Projekt

## Kontext

Der KI-Bot-Service muss:
- Welt-Events polllen
- NPC-Kontext aufbauen (HTTP-Calls zum Backend)
- LLM-Prompts templaten
- LLM-Antwort validieren
- Intents ans Backend senden

Optionen: **Python (FastAPI)** vs **Rust (axum)** vs **Java (spring-boot)**

## Entscheidung

**Python 3.12+ mit FastAPI**.

## Begründung

### Gegen Rust
- LLM-Ökosystem (LangChain, LlamaIndex, Transformers, OpenAI-Client) ist Python-dominated
- LLM-Calls sind Orchestrierungslogik, nicht Performance-kritisch — Rusts Vorteile (Memory-Safety ohne GC, C-grade Performance) bringen **null Vorteil** hier
- Schnelleres MVP und einfachere Iteration in Python (Jinja2, Pydantic, httpx)
- Weniger Schreibarbeit für Prompt-Templating

### Gegen Java
- Würde технологischen Stack vereinfachen (nur ein Sprache für Backend + Bot)
- Aber: Python ist der Standard für LLM-Orchestrierung; jede neue LLM-Bibliothek ist zuerst in Python verfügbar
- Pydantic für Validierung von LLM-Output ist bequemer als javax.validation
- Jinja2-Templating reifer als Thymeleaf für System-Prompt-Manipulation

### Für Python
- Defacto-Standard für LLM-Integration
- FastAPI bietet async out-of-the-box, ideal für I/O-bound Tasks (LLM-Calls sind langsame Network-Calls)
- Pydantic-Settings für strikte .env-Validierung
- Optional: später LangChain für fortgeschrittene RAG-Pattern einsetzbar

## Konsequenzen

**Positiv:**
- Schnelle Iteration, geringe Ceremony
- Zugang zu modernsten LLM-Bibliotheken
- Klare Trennung Backend (Java) vs KI-Logik (Python)

**Negativ:**
- Drei Sprachen im Repo (Java, TS, Python) → CI muss Tooling für alle drei vorhalten
- Bot-Performance bei 1000+ Events/sec limitiert (für LWE nicht kritisch — Polling ist leichgewichtig)
- Entwickler muss minimal ein Python-Setup lernen

## Referenzen

- [`AI-AGENT.md`](../AI-AGENT.md) — Bot-Architektur-Details
- https://fastapi.tiangolo.com/
- https://docs.pydantic.dev/