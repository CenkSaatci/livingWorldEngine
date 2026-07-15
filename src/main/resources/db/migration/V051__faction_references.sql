-- ============================================================
-- V051__faction_references.sql
-- Living World Engine — Faction FK + Migration Helper
-- ============================================================
-- Ändert entities.faction_id von self-referencing (entities)
-- auf die neue factions-Tabelle (V050).
--
-- regions.factions und locations.factions bleiben als JSONB
-- (String-Array) erhalten — das ist Design-Intent für
-- Cross-System-Flexibility.

ALTER TABLE entities
  DROP CONSTRAINT IF EXISTS entities_faction_id_fkey;

ALTER TABLE entities
  ADD CONSTRAINT entities_faction_fk
  FOREIGN KEY (faction_id) REFERENCES factions(id)
  ON DELETE SET NULL;

COMMENT ON COLUMN entities.faction_id IS 'FK auf factions-Tabelle (statt entities-Self-Ref)';
