# UI/UX Vision — Living World Engine

> Design-Konzept für die Darstellung der Welt-Tiefe (Regionen, Orte, NPCs, Chroniken, Quests, Wirtschaft).
> Alle UI-Komponenten folgen React Patterns: Komposition, Custom Hooks, Zustand, Tailwind.

---

## 1. Layout-Architektur

Das Haupt-Spielfenster bekommt eine **flexiblere Struktur**, die sich je nach Kontext anpasst:

```
┌─────────────────────────────────────────────────────────────────────┐
│  Top Bar │ LWE │ Weltname │ ⏱ 14:30 Tag │ 🌐 DE │ 👤 User │
├──────────┴────────────────────────────────────┬──────────────────────┤
│  Sidebar                                     │                      │
│  ┌──────────────┐   Main Area                │   Right Panel        │
│  │ 🌍 Regionen   │────────────────────────── │                      │
│  │ 🏘️ Orte      │  [Karte / Detail /         │   ┌──────────────┐  │
│  │ 👥 NPCs      │   Quest-Log / Markt]       │   │ Event-Chronik  │  │
│  │ 📜 Quests     │                           │   │               │  │
│  │ 🛒 Markt     │                           │   │               │  │
│  │              │                           │   ├──────────────┤  │
│  │              │                           │   │ NPC-Profil     │  │
│  │              │                           │   │ bei Klick      │  │
│  └──────────────┘                           │   └──────────────┘  │
├──────────┬──────────────────────────────────┴──────────────────────┤
│ Status   │ FPS: 60 │ KI: suggest │ Session aktiv │ Turn: — │
└──────────┴─────────────────────────────────────────────────────────┘
```

### Seiten-Struktur (React-Komponentenbaum)

```
<App>
  <WorldLayout>
    <TopBar />
    <div class="flex flex-1 overflow-hidden">

      {/* Linke Sidebar — kontextabhängig */}
      <Sidebar>
        <RegionTree />           {/* Regionen → Orte strukturiert */}
        <QuestLog />             {/* Aktive Quests */}
      </Sidebar>

      {/* Hauptbereich */}
      <main class="flex-1">
        {/* Wechselt je nach Ansicht */}
        <WorldMapView />         {/* Karte mit Regionen + Orten */}
        <LocationDetail />       /* Ort angeklickt → Detail-Ansicht */
        <NpcProfile />           /* NPC angeklickt → Profil-Ansicht */
        <MarketView />           /* Markt angeklickt → Preise */}
        <QuestDetail />          /* Quest angeklickt → Details */}
      </main>

      {/* Rechtes Panel — Kontext-Panel */}
      <ContextPanel>
        <EntityTimeline />       {/* Ereignis-Chronik der aktiven Entity */}
        <NpcQuickInfo />         {/* Schnellinfo zum ausgewählten NPC */}
      </ContextPanel>

    </div>
    <StatusBar />
  </WorldLayout>
</App>
```

---

## 2. Komponenten-Detail

### 2.1 RegionTree (Sidebar)

```tsx
// Strukturierte Baumansicht: Welt → Regionen → Orte
<RegionTree>
  ▸ Schattental (Region)
    ▸ Düsterburg (Stadt, 💰 7)
    ▸ Waldhain (Dorf, 💰 4)
    ▸ Alte Ruinen (Dungeon)
  ▸ Eisgipfel (Region)
    ▸ Frostheim (Burg, 💰 8)
```

- Jeder Ort zeigt sein **Typ-Icon** (🏘️ Dorf, 🏰 Burg, ⛪ Tempel, 🏚️ Ruine)
- **Wealth** als 💰-Indikator
- **Klick** → Main Area zeigt LocationDetail
- **Drag** → kann auf Karte positioniert werden

### 2.2 LocationDetail (Main Area)

```
┌──────────────────────────────────────────────┐
│  ← Zurück     🏘️ Düsterburg                  │
│  Region: Schattental | Typ: Dorf | 💰 5      │
├──────────────────────────────────────────────┤
│  Geschichte                                   │
│  "Gegründet vor 200 Jahren als Handelsposten…│
│  Vor 3 Tagen: Goblin-Überfall"               │
├──────────────────────────────────────────────┤
│  Dienste                          NPCs (3)   │
│  ┌──────────┐ ┌──────────┐     ┌──────────┐ │
│  │ ⚔️ Waffen │ │ 🔧 Rep.  │     │ Karl     │ │
│  │   25 G    │ │   15 G   │     │ Schmied   │ │
│  └──────────┘ └──────────┘     │ ⭐ Meister │ │
│  ┌──────────┐ ┌──────────┐     └──────────┘ │
│  │ 🍺 Herberge│ │ 🧪 Tränke │   ┌──────────┐ │
│  │    5 G    │ │   30 G   │     │ Lena     │ │
│  └──────────┘ └──────────┘     │ Händlerin │ │
│                                 └──────────┘ │
├──────────────────────────────────────────────┤
│  Ereignis-Chronik (letzte 7 Tage)             │
│  📅 Gestern — Goblin-Überfall                 │
│  📅 Vor 3d — Jahrmarkt eröffnet               │
│  📅 Vor 5d — Schmied Karl angekommen          │
└──────────────────────────────────────────────┘
```

### 2.3 NpcProfile (Right Panel oder Modal)

```
┌──────────────────────┐
│ 👤 Karl, Schmied      │
│ ⚒️ Beruf: Schmied     │
│ ⭐ Meister-Schmied    │
├──────────────────────┤
│ 📍 Düsterburg         │
│ 🕐 Tagsüber in Schmiede│
│ ❤️ Mag Lena           │
│ 💔 Hasst Rudi         │
├──────────────────────┤
│ 📜 Chronik             │
│ • 🟢 Königlicher Auftrag│
│ • ⚪ 10 Schwerter     │
│ • 🔴 Erzvorrat leer   │
├──────────────────────┤
│ 💰 Preis-Mod: ×1.0    │
│ 🛒 Dienste:           │
│   ⚔️ Waffen kaufen 25G│
│   🔧 Reparieren   15G │
└──────────────────────┘
```

### 2.4 EntityTimeline (Context Panel)

```
┌─────────────────────────────┐
│ 📜 Chronik: Düsterburg       │
│                             │
│ 🔴 Gestern                  │
│    Goblin-Überfall           │
│    "Eine Gruppe Goblins…"   │
│                             │
│ 🟢 Vor 3 Tagen              │
│    Jahrmarkt eröffnet        │
│                             │
│ 🟢 Vor 5 Tagen              │
│    Karl (Schmied) angekommen│
│                             │
│ ⭐ Wichtigkeit: ●●○○○       │
└─────────────────────────────┘
```

Farbcodierung der Ereignisse:
- 🔴 **Negativ** (Überfall, Naturkatastrophe) → `bg-danger/20`
- 🟢 **Positiv** (Markt eröffnet, Quest abgeschlossen) → `bg-success/20`
- 🔵 **Neutral** (NPC angekommen, Erkundung) → `bg-accent/20`

### 2.5 QuestLog (Sidebar + Detail)

```
Sidebar:
┌──────────────────────────────────┐
│ 📜 Quests                       │
│                                  │
│ ◉ Töte 5 Goblins ⏳ aktiv       │
│   Belohnung: 100 XP, 25 G       │
│   ⏱ in Schattental              │
│                                  │
│ ○ Bringe Heilkraut ✅ erledigt  │
│ ─────────────────────────────   │
│ ⊕ Schmiede-Auftrag (KI)         │
│   DM: [✔️ Approve] [✖️ Reject] │
└──────────────────────────────────┘
```

**KI-Quest-Vorschläge** haben Button zum Approve/Reject (DM-only).

### 2.6 MarketView (Main Area)

```
┌──────────────────────────────────────────────┐
│ 🛒 Markt — Düsterburg                        │
│ Wohlstand: 💰💰💰💰○ (5)                      │
├──────────────────────────────────────────────┤
│ Item                 | NPC     | Basis | Preis│
│──────────────────────|─────────|───────|──────│
│ ⚔️ Kurzschwert       | Karl    | 25 G  | 25 G │
│ 🔧 Reparatur (einfach)| Karl    | 15 G  | 15 G │
│ 🧪 Heiltrank         | Lena    | 30 G  | 33 G │ ← +10 % wegen wealth
│ 🍺 Bier              | Lena    |  3 G  |  3 G │
│ 🛡️ Lederrüstung      | Karl    | 50 G  | 50 G │
└──────────────────────────────────────────────┘
```

---

## 3. Datenaustausch (Custom Hooks)

```tsx
// hooks/useRegion.ts
function useRegion(regionId: string) {
  const [region, setRegion] = useState<Region | null>(null);
  const [locations, setLocations] = useState<Location[]>([]);
  const [events, setEvents] = useState<EntityEvent[]>([]);

  useEffect(() => {
    apiClient.get(`/regions/${regionId}`).then(r => setRegion(r.data));
    apiClient.get(`/regions/${regionId}/locations`).then(r => setLocations(r.data));
    apiClient.get(`/entity-events`, {
      params: { entityType: 'region', entityId: regionId }
    }).then(r => setEvents(r.data));
  }, [regionId]);

  return { region, locations, events };
}

// hooks/useMarket.ts
function useMarket(locationId: string) {
  const [prices, setPrices] = useState<MarketItem[]>([]);
  const [npcs, setNpcs] = useState<NpcSummary[]>([]);

  useEffect(() => {
    apiClient.get(`/locations/${locationId}/market`).then(r => setPrices(r.data));
    apiClient.get(`/locations/${locationId}/npcs`).then(r => setNpcs(r.data));
  }, [locationId]);

  return { prices, npcs };
}
```

---

## 4. Neue Seiten & Routes

| Route | Page | Beschreibung |
|---|---|---|
| `/worlds/:id` | `GameView` | Hauptspielfenster mit Sidebar + Karte + Panel |
| `/worlds/:id/regions/:rid` | `RegionView` | Region + Orte + Chronik |
| `/worlds/:id/locations/:lid` | `LocationView` | Ort + Dienste + NPCs + Markt |
| `/worlds/:id/npcs/:nid` | `NpcView` | NPC-Profil + Chronik + Quests |
| `/worlds/:id/quests` | `QuestListView` | Alle Quests der Welt |
| `/worlds/:id/quests/:qid` | `QuestDetailView` | Einzelne Quest |

---

## 5. Neue Komponenten (Dateien)

### Sidebar
| Datei | Beschreibung |
|---|---|
| `components/world/RegionTree.tsx` | Baumansicht Regionen → Orte |
| `components/world/QuestLog.tsx` | Aktive Quests + KI-Vorschläge |

### Main Content
| Datei | Beschreibung |
|---|---|
| `components/world/WorldMapView.tsx` | PixiJS-Karte mit Regions-Grenzen + Orts-Markern |
| `components/world/LocationDetail.tsx` | Orts-Detail (Dienste, NPCs, Chronik) |
| `components/world/NpcProfile.tsx` | NPC-Profil (Biographie, Beziehungen, Preis) |
| `components/world/MarketView.tsx` | Markt-Preise mit Kauf-Button |
| `components/world/QuestDetail.tsx` | Quest-Detail + Fortschritt |

### Context Panel
| Datei | Beschreibung |
|---|---|
| `components/world/EntityTimeline.tsx` | Ereignis-Chronik der aktiven Entity |
| `components/world/NpcQuickInfo.tsx` | Minimierte NPC-Karte (Drag aus Sidebar) |

### UI Primitives
| Datei | Beschreibung |
|---|---|
| `components/ui/Badge.tsx` | Typ/Status-Badge (💰, ⏳, ✅) |
| `components/ui/EventCard.tsx` | Ereignis-Karte mit Farbcodierung |
| `components/ui/PriceTag.tsx` | Preis-Anzeige mit Rabatt/Aufschlag |

---

## 6. Design-Prinzipien

| Prinzip | Umsetzung |
|---|---|
| **Konsistenz** | Alle Entity-Details nutzen dasselbe Layout (Header → Info → Actions → Timeline) |
| **Kontext-Panel** | Rechtes Panel zeigt Chronik + Schnellinfo zur aktiven Entity |
| **Farbcodierung** | 🔴 Negativ / 🟢 Positiv / 🔵 Neutral für Events |
| **Progressive Disclosure** | Sidebar zeigt Struktur, Klick öffnet Detail, Panel zeigt Kontext |
| **DM-only Controls** | Approve/Reject-Buttons nur sichtbar für DM-Rolle |
| **Lazy Loading** | Regionen/Orte/NPCs laden beim Aufklappen, nicht beim Seitenstart |

---

## 7. Umsetzungs-Reihenfolge

| Schritt | Komponenten | Aufwand |
|---|---|---|
| 1 | `RegionTree` + `LocationDetail` + Grundgerüst GameView | 3 Tage |
| 2 | `EntityTimeline` + `EventCard` (mit Farbcodierung) | 1 Tag |
| 3 | `NpcProfile` + `NpcQuickInfo` | 2 Tage |
| 4 | `WorldMapView` (PixiJS Regions-Grenzen + Orts-Marker) | 3 Tage |
| 5 | `MarketView` + `PriceTag` | 1 Tag |
| 6 | `QuestLog` + `QuestDetail` + KI-Vorschläge | 2 Tage |
