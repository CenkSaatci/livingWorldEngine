-- ============================================================
-- V061__entity_extended.sql
-- Living World Engine — Erweiterte Entity-Felder
-- ============================================================
-- F ür NPC-Bewusstsein: Hintergrundgeschichte, Alter,
-- Erfahrungsstufe, sozialer Stand.
--
-- Siehe docs/NPC-MEMORY.md

ALTER TABLE entities
  ADD COLUMN backstory         TEXT,
  ADD COLUMN age               INTEGER      NOT NULL DEFAULT 30,
  ADD COLUMN experience_level  VARCHAR(20)  NOT NULL DEFAULT 'green',
  ADD COLUMN social_standing   VARCHAR(20)  NOT NULL DEFAULT 'peasant';

COMMENT ON COLUMN entities.backstory IS 'Hintergrundgeschichte (z.B. "Verlor Eltern bei Goblin-Überfall")';
COMMENT ON COLUMN entities.age IS 'Alter in Jahren';
COMMENT ON COLUMN entities.experience_level IS 'green | veteran | elite';
COMMENT ON COLUMN entities.social_standing IS 'peasant | merchant | guard | noble | clergy | criminal';
