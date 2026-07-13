# ADR-001: Frontend = React + Vite + PixiJS

- **Status:** Accepted
- **Date:** 2025-07-12
- **Decision Owner:** Projekt
- **Consulted:** Benutzer

## Kontext

Das LWE-Frontend muss eine interaktive 2D-Karte (VTT) mit Token-Management, Fog of War, Chat und einem dynamischen Charakterbogen rendern. Cross-Plattform-Web ist primäres Ziel, mobile PWA als Nice-to-have. Backend ist Spring Boot; Entwickler ist in Java/Spring erfahren, im Frontend-Bereich offen für Empfehlungen.

In Frage kommende Optionen:

1. **Flutter Web** — Cross-Plattform (Web, iOS, Android) aus einer Codebasis
2. **Next.js (React SSR)** — Industry-Standard React-Framework mit SSR/SSG/App Router
3. **React + Vite** — Schlankes React-Setup ohne SSR-Overhead
4. **Angular** — Meinungsstarkes Enterprise-Framework

## Entscheidung

**React + Vite + TypeScript + PixiJS** für das Frontend.

## Begründung

### Gegen Flutter Web
- CanvasKit-WASM-Download ~3 MB gzipped, spürbar mobiler
- Kein Web Worker-Support — schwere Berechnungen (FOV, Pathfinding) blockieren Render-Thread
- Dünneres Web-Ökosystem, Google-Risiko bezüglich langfristigem Commitment zum Web-Target
- Dart ist Java-ähnlich und wäre für den Entwickler attraktiv, aber der Performance-Nachteil überwiegt für ein produktorientiertes VTT

### Gegen Next.js
- LWE ist ein authentifizierter, interaktiver Fat Client — kein Blog mit SEO-Anspruch
- Next.js' App Router, RSC, Server-Client-Boundary-Komplexität bringt **null Vorteil** für VTT, aber deutlichen Overhead
- Vite ist nah am Build-System, schneller Start, simple Konfiguration

### Gegen Angular
- Meinungsstarke Struktur hilft Enterprise-Teams, aber für kleines Team zu viel Boilerplate
- Weniger flexibel für Game-artige Canvas-Anwendung

### Für React + Vite
- Größtes Web-Ökosystem — keine Library-Probleme
- TypeScript hat密度l-Dizplip lin wie Java-Entwickler gewohnt
- PixiJS als 2D-Renderer ist der De-facto-Standard für Web-Spiel-anwendungen
- Zustand als minimaler State-Manager, kein Redux-Boilerplate
- @pixi/react bridge für deklarative PixiJS-Steueurung

## Konsequenzen

**Positiv:**
- Volle Flexibilität bei Build-Tooling, State-Management, API-Library-Wahl
- Performance-optimal für den Karten-Canvas
- Größte Auswahl an Frontend-Developern später

**Negativ:**
- Entwickler muss TypeScript/React lernen (uneiungsgemäß im Java- Stack)
- Keine native Mobile-App aus derselben Codebasis — PWA reicht aber laut [`UI-UX.md`](../UI-UX.md) aus
- Mehrere Libraries müssen selbst gewählt werden (state, http, ws) statt eines Batteries-included-Frameworks

## Referenzen

- [`UI-UX.md`](../UI-UX.md) — Frontend-Stack-Details
- Research-Vergleich in Sitzung (Reakt im Vergleich zu Flutter, Vue-Vergleich siehe Sessionverlauf)
- https://pixijs.com/
- https://vitejs.dev/