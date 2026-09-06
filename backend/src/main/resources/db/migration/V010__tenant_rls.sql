-- ============================================================
-- V010__tenant_rls.sql
-- Living World Engine — Row-Level Security für Multi-Tenancy
-- ============================================================
-- Aktiviert RLS auf allen Tenant-Tabellen. Jede Policy prüft,
-- ob der aktuelle User (via app.tenant_id) der Owner der Welt ist.
--
-- Siehe docs/ADR/004-multitenancy-shared-schema.md

-- Tenant-ID wird pro Request gesetzt (siehe TenantInterceptor.java)
-- Der Admin (Role ADMIN) hat BYPASSRLS und sieht alle Daten.

ALTER TABLE worlds ENABLE ROW LEVEL SECURITY;
CREATE POLICY worlds_isolation ON worlds
    USING (owner_id = current_setting('app.tenant_id')::uuid);

ALTER TABLE world_members ENABLE ROW LEVEL SECURITY;
CREATE POLICY world_members_isolation ON world_members
    USING (world_id IN (
        SELECT id FROM worlds WHERE owner_id = current_setting('app.tenant_id')::uuid
    ));

ALTER TABLE entities ENABLE ROW LEVEL SECURITY;
CREATE POLICY entities_isolation ON entities
    USING (world_id IN (
        SELECT id FROM worlds WHERE owner_id = current_setting('app.tenant_id')::uuid
    ));

ALTER TABLE world_events ENABLE ROW LEVEL SECURITY;
CREATE POLICY world_events_isolation ON world_events
    USING (world_id IN (
        SELECT id FROM worlds WHERE owner_id = current_setting('app.tenant_id')::uuid
    ));

ALTER TABLE npc_intents ENABLE ROW LEVEL SECURITY;
CREATE POLICY npc_intents_isolation ON npc_intents
    USING (world_id IN (
        SELECT id FROM worlds WHERE owner_id = current_setting('app.tenant_id')::uuid
    ));

ALTER TABLE adventures ENABLE ROW LEVEL SECURITY;
CREATE POLICY adventures_isolation ON adventures
    USING (world_id IN (
        SELECT id FROM worlds WHERE owner_id = current_setting('app.tenant_id')::uuid
    ));

ALTER TABLE combat_sessions ENABLE ROW LEVEL SECURITY;
CREATE POLICY combat_sessions_isolation ON combat_sessions
    USING (world_id IN (
        SELECT id FROM worlds WHERE owner_id = current_setting('app.tenant_id')::uuid
    ));

COMMENT ON POLICY worlds_isolation ON worlds IS 'RLS: User sieht nur eigene Welten';
COMMENT ON POLICY world_members_isolation ON world_members IS 'RLS: User sieht nur Mitglieder eigener Welten';
COMMENT ON POLICY entities_isolation ON entities IS 'RLS: User sieht nur Entitäten eigener Welten';
COMMENT ON POLICY world_events_isolation ON world_events IS 'RLS: User sieht nur Events eigener Welten';
COMMENT ON POLICY npc_intents_isolation ON npc_intents IS 'RLS: User sieht nur Intents eigener Welten';
COMMENT ON POLICY adventures_isolation ON adventures IS 'RLS: User sieht nur Adventures eigener Welten';
COMMENT ON POLICY combat_sessions_isolation ON combat_sessions IS 'RLS: User sieht nur Combat eigener Welten';