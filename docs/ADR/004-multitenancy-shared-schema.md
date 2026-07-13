# ADR-004: Multi-Tenancy via Shared-Schema + Row-Level Security

- **Status:** Accepted
- **Date:** 2025-07-12

## Kontext

LWE ist auf SaaS-Betrieb ausgerichtet. Mehrere Welten sollen isoliert betrieben werden, d. h. die Daten eines Tenants dürfen für keinen anderen Tenant einsehbar oder beeinflussbar sein.

Optionen:
1. **Database-per-Tenant** — Jeder Tenant hat eigene PostgreSQL-Instanz
2. **Schema-per-Tenant** — Geteilte PostgreSQL, aber ein Schema pro Tenant
3. **Shared Schema + Row-Level Security** — Alle in einer DB, RLS filtert implizit
4. **Shared Schema + Application-Level Filter** — WHERE-Bedingungen in jedem Repository

## Entscheidung

**Shared Schema + Postgres Row-Level Security (RLS)** für die meisten Tabellen.

## Begründung

### Gegen Database-per-Tenant
- Operationeller Overhead (ógico pro Tenant eine DB aufsetzen, migrieren, backuppen)
- Höchste Isolation, aber für LWE-Use-Case überdimensioniert
- Migrationskosten bei Schema-Updates sehr hoch

### Gegen Schema-per-Tenant
- Geteilter Code muss zwischen Tenants vermitteln (jeder Query muss SET SEARCH_PATH)
- Flyway wandert durch viele Schemas (langsam)
- Mittelweg, aber nicht nötig für LWE mit few-heavy-Antenanteil

### Gegen Application-Level Filter
- Entwickler muss jeden Repository-Query an owner_id filtern (40+ Queries)
- Ein vergessener Filter → Daten-Leck
- Spring Data JPA lässt sich das schwer zentralisiert erzwingen

### Für RLS
- Postgres RLS policies sind_APPEND-Filters, die automatisch an jeden Query angehängt werden — auch beim direkten psql-Login
- Spring Boot: JPA Repository-Methode bezieht sich implizit auf `current_setting('app.tenant_id')`
- Setzen via Transaction-Kontext pro Request (`SET LOCAL app.tenant_id`)
- Migrations/Index-Verwaltung zentral
- Schützt auch vor Application-Fehlern (z.B. vergessen `world_id`-Filter)

## Implementations-Strategie

### 1. Tenant-Schlüssel
- Tenant-Schlüssel = `worlds.owner_id`
- Tabellen, die an Welt hängen, inherit Tenant implizit über `world_id` → `worlds.owner_id`

### 2. RLS-Policies
```sql
ALTER TABLE worlds ENABLE ROW LEVEL SECURITY;
CREATE POLICY worlds_isolation ON worlds
    USING (owner_id = current_setting('app.tenant_id')::uuid);

ALTER TABLE entities ENABLE ROW LEVEL SECURITY;
CREATE POLICY entities_isolation ON entities
    USING (world_id IN (
        SELECT id FROM worlds WHERE owner_id = current_setting('app.tenant_id')::uuid
    ));
-- Equivalent für world_events, npc_intents, maps, usw.
```

### 3. Tenant-Context setzen (pro Request)
```java
@Component
public class TenantInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest req, ...) {
        UUID userId = ((Authentication) req.getUserPrincipal()).getUserId();
        try (Connection c = dataSource.getConnection();
             PreparedStatement s = c.prepareStatement("SET LOCAL app.tenant_id = ?")) {
            s.setString(1, userId.toString());
            s.execute();
        }
        return true;
    }
}
```

Bei transaktionsbasierter Connection kann `SET LOCAL` in der ersten Query ausgelöscht werden. Alternative via Hibernate `StatementInspector` für Sitzungsweite.

### 4. Cross-Tenant-Queries (Admin)
- Rolle `ADMIN` überspringt RLS via `BYPASSRLS` (Postgres-Role-Eigenschaft) oder via `ALTER TABLE ... FORCE ROW LEVEL SECURITY OFF` per Context-Flag.

## Konsequenzen

**Positiv:**
- Isolation explizit auf Datenbankebene, nicht nur App-Ebene
- Geringer Operationeller Overhead
- Single Migrations-Pfad

**Negativ:**
- Performance-Einbuße durch RLS-Policy-Filter (mit passenden Indizes minimal)
- Nicht trivial zu debuggen (Policy-Logik schwer sichtbar)
- Cross-Tenant-Admin-Queries brauchen Bypass

## Migrationskonzept

- `V010__tenant_rls.sql` in Phase 5
- Passende Indizes via `CREATE INDEX ... ON worlds(owner_id)` etc.
- Tests mit zwei Usern: User A darf seine Welten sehen, User B's Welten sind unsichtbar; beide dürfen Admin-Endpunkte nur, wenn Role `ADMIN`.

## Referenzen

- https://www.postgresql.org/docs/current/ddl-rowsecurity.html
- [`DATA-MODEL.md`](../DATA-MODEL.md) — Tabellen und Indizes