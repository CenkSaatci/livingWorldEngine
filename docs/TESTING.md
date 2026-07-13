# Testing-Strategie

> Wie LWE auf allen Ebenen getestet wird. Verbindlich für alle Phasen.

---

## 1. Test-Pyramide

```
            ┌───────────┐
            │    E2E     │   ← Playwright (Browser)
            │   (~5%)    │
            ├───────────┤
            │ Integration│   ← Spring Boot @SpringBootTest, Testcontainers
            │   (~25%)   │
            ├───────────┤
            │    Unit    │   ← JUnit 5 / pytest / vitest
            │   (~70%)   │
            └───────────┘
```

Anteile sind Richtwerte, keine harte Vorgabe. Wichtig: **jede Ebene hat ihren Zweck** — Unit-Tests ohne Dependencies für schnelles Feedback, Integration-Tests für Zusammenspiel realer Komponenten, E2E-Tests für kritische User-Flows.

---

## 2. Backend (Java / Spring Boot)

### 2.1 Unit-Tests
- **Framework:** JUnit 5, Mockito, AssertJ
- **Bereich:** Services, Rule-Engine, Expression-Parser, Validatoren, Utility-Klassen
- **Keine** Spring-Context, keine Datenbank — gemockte Dependencies
- **Naming:** `ClassNameTest` mit `@TestMethodName`-Konvention: `shouldReturnXWhenY`
- **Zeit:** < 10 s gesamte Suite

### 2.2 Integration-Tests
- **Framework:** `@SpringBootTest`, Testcontainers (PostgreSQL)
- **Bereich:** Repository-Methoden, End-to-End durch Controller → Service → Repository → DB
- **Datenbank:** Echte PostgreSQL-Instanz via Testcontainers (nicht H2 — JSONB-Verhalten soll realistisch sein)
- **Migrationen:** Flyway läuft automatisch vor Tests
- **Naming:** `*IT.java` (Suffix für Maven Failsafe-Plugin)
- **Zeit:** < 60 s gesamte Suite

### 2.3 Slicing-Tests
- `@WebMvcTest` für Controller-Layer (Service gemockt)
- `@DataJpaTest` für Repository-Layer (Testcontainers)
- Schneller als volle Integrationstests, isoliert Schicht

### 2.4 WebSocket-Tests
- `WebSocketStompClient` in Integrationstests
- STOMP-Connect mit JWT, Subscription und Receive prüfen

### 2.5 i18n-Tests
- Parametrisierter Test: gleicher Endpunkt mit `Accept-Language: de` bzw. `en`
- Assert: Fehlermeldung korrekt übersetzt
- Test für "missing translation key" (Fallback-Locale)

---

## 3. Frontend (React / TypeScript)

### 3.1 Unit-Tests
- **Framework:** Vitest + React Testing Library
- **Bereich:** Komponenten, Hooks, Utility-Funktionen
- **Mocking:** MSW (Mock Service Worker) für HTTP-Interceptor, keine echten Backend-Calls
- **Naming:** `*.test.tsx` / `*.test.ts` direkt neben Source-Datei
- **Zeit:** < 15 s gesamte Suite

### 3.2 Komponenten-Tests
- Rendering mit varying props, Store-State
- Benutzerinteraktion via `@testing-library/user-event`
- A11y-Prüfung: `jest-axe` für WCAG-Verletzungen pro Komponente

### 3.3 i18n-Tests
- Test, dass alle Keys in `de` und `en` vorhanden sind
- Vergleich der Key-Sets der Sprachen
- Render einer Beispiel-Komponente mit beiden Sprachen (snapshot optional)

### 3.4 E2E-Tests (Playwright)
- **Bereich:** Kritische User-Flows (Login, Welt erstellen, Würfel werfen, Spielbetreten)
- **Gegen:** Echtes Backend via `podman compose` (Test-Setup startet kompletten Stack)
- **Parallelität:** Worker-basiert, Tests isoliert per Test-User
- **Zeit:** < 5 min gesamte Suite
- **Ausführung:** In CI bei jedem PR (Phase 5+)

---

## 4. AI-Bot (Python)

### 4.1 Unit-Tests
- **Framework:** pytest, pytest-asyncio, httpx-Mock
- **Bereich:** Kontext-Loader, Prompt-Renderer, Parser für LLM-Output, Validierung
- **LLM:** Gemockt (`MockLLMClient`) — keine echten API-Calls in CI
- **Fixtures:** `tests/fixtures/npc_contexts/*.json`

### 4.2 Prompt-Tests
- Golden-File-Tests: gerenderter Prompt verglichen mit `tests/prompts/expected/*.txt`
- Schützt vor unbeabsichtigter Prompt-Drift durch Refactoring

### 4.3 Integration-Tests
- Starre den Bot mit gemocktem Backend auf
- Polling-Loop simuliern, Event aus_fixture_einspeisen, Intent-Output prüfen

---

## 5. Property-Based Tests (optional)

Für ressourcenkritische Logik wie Dice-Expression-Parser und Rule-Engine:
- **jqwik** (Java) oder **hypothesis** (Python)
- Generiert tausende zufällige Eingaben
- Testet Invarianten (z. B.: „Probeergebnis ist immer im Wertebereich [Roll_min, Roll_max] + mod")

---

## 6. Last- und Performance-Tests

- **Framework:** k6 (skriptbar in JS)
- **Scope in Phase 5:** "100 gleichzeitige Spieler in einer Welt würfeln"
- **Metriken:** Latenz p95 < 200 ms, Fehlerquote < 0,1 %
- **Häufigkeit:** Manuell vor Release, später optional in CI-Nightly

---

## 7. Test-Abdeckung

- **Gemessen via:** JaCoCo (Backend), Vitest-Coverage (Frontend), pytest-cov (Bot)
- **Ziel (Richtwert, nicht hart):**
  - Backend: ≥ 80 % Line Coverage, ≥ 70 % Branch Coverage
  - Frontend: ≥ 75 % Line Coverage
  - Bot: ≥ 85 % Line Coverage (kritische Validierungslogik)
- **Ausnahmen:** DTO-Entities, reine Konfiguration, `main`-Methoden — nicht mitgezählt

---

## 8. CI-Anbindung (Phase 5)

```yaml
# .github/workflows/ci.yml (vereinfacht)
jobs:
  backend:
    runs-on: ubuntu-latest
    services:
      postgres: ... # oder testcontainers
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
      - run: ./mvnw verify   #_unit + integration via failsafe
  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: pnpm/action-setup
      - run: pnpm install
      - run: pnpm check       # lint + typecheck
      - run: pnpm test        # vitest
  bot:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/setup-python@v5
      - run: pip install -e ".[test]"
      - run: pytest
```

**Gates (PR-merge-blocking):**
- Alle Test-Suiten grün
- Coverage hinter Minimum
- i18n-Keys konsistent zwischen `de` und `en`

---

## 9. Samen-Daten (Seed)

- `db/migration/afterMigrate__seed.sql` (Flyway Callback) lädt Beispieldaten in Dev-Profil:
  - 1 Admin-User, 2 reguläre User
  - Beide Beispiel-Regelwerke (D20Lite, TwoDicePool)
  - 1 Demo-Welt mit NPCs und Adventure-Start
- In Produktion inaktiv (`profile != dev`)
- Erlaubt schnelles Ausprobieren ohne händisches Anlegen

---

## 10. Manuelle Tests

Für jeden Meilenstein wird ein **Smoke-Test-Skript** gepflegt:
- `docs/SMOKE-TEST.md` (Phase 1)
- `docs/DEMO.md` (Phase 3)
- `docs/LIVING-WORLD-DEMO.md` (Phase 4)
- `docs/USER-GUIDE.md` (Phase 5)

Diese werden vor jedem Meilenstein-Release per Hand durchgespielt.

---

## 11. Testlisten pro Phase

| Phase | Neue Tests |
|---|---|
| 1 | Unit für `JwtService`, `AuthService`, `RuleSchemaValidator`; Integration für `/api/auth`, `/api/worlds`, WebSocket |
| 2 | Unit für Rule-Engine, Dice-Parser; Integration für `/api/rolls`, Combat-Flow, Adventure |
| 3 | Vitest für alle Komponenten; `jest-axe` für A11y; E2E für Login-Flow |
| 4 | pytest für ContextLoader, LLM-Client (gemockt), Validator; Golden-File-Prompt-Tests |
| 5 | Last-Tests mit k6; E2E Suite mit echtem Container-Stack |

---

## 12. Verweise

- Integration in TASKS.md: jede("{ acceptance criteria")-Liste enthält Testanforderungen
- [`ADR/007`](ADR/007-internationalization-strategy.md) — i18n-Test-Prüfung
- [`ADR/005`](ADR/005-ki-validation-layer.md) — Validator-Schicht hat eigenenTestsatz