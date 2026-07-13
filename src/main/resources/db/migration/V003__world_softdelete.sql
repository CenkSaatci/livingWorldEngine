-- ============================================================
-- V003__world_softdelete.sql
-- Living World Engine — Soft-Delete-Spalte für worlds + entities
-- ============================================================

ALTER TABLE worlds ADD COLUMN active BOOLEAN NOT NULL DEFAULT true;
COMMENT ON COLUMN worlds.active IS 'Soft-Delete: false = Welt ist gelöscht (unsichtbar für User).';

ALTER TABLE entities ADD COLUMN active BOOLEAN NOT NULL DEFAULT true;
COMMENT ON COLUMN entities.active IS 'Soft-Delete: false = Entity ist gelöscht.';