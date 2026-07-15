-- ============================================================
-- V050__factions.sql
-- Living World Engine — Fraktionen + Diplomatie
-- ============================================================
-- Fraktionen sind organisierte Gruppen innerhalb einer Welt.
-- Jede Fraktion hat eine eigene Identität, Territorien (über JSON-Arrays
-- in regions/locations) und diplomatische Beziehungen zu anderen Fraktionen.
--
-- Siehe docs/WORLD-DEPTH.md

CREATE TABLE factions (
    id              UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    world_id        UUID         NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    color           VARCHAR(7)   DEFAULT '#888888',
    leader_entity_id UUID        NULL,
    founded_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE(world_id, name)
);

CREATE INDEX idx_factions_world ON factions(world_id);

COMMENT ON TABLE factions IS 'Fraktionen in einer Welt. Jede Fraktion hat einen Namen, eine Beschreibung und optionale Führungs-Entity.';
COMMENT ON COLUMN factions.color IS 'Hex-Farbe für UI (z.B. #ff4444 für rot)';
COMMENT ON COLUMN factions.leader_entity_id IS 'Optional: Entity (NPC), die diese Fraktion anführt';

-- ============================================================
-- Faction Relations — Diplomatie zwischen Fraktionen
-- ============================================================

CREATE TABLE faction_relations (
    id                  UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    faction_a_id        UUID        NOT NULL REFERENCES factions(id) ON DELETE CASCADE,
    faction_b_id        UUID        NOT NULL REFERENCES factions(id) ON DELETE CASCADE,
    relation_status     VARCHAR(20) NOT NULL DEFAULT 'NEUTRAL'
                        CHECK (relation_status IN ('ALLIANCE', 'FRIENDLY', 'NEUTRAL', 'UNFRIENDLY', 'WAR')),
    changed_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(faction_a_id, faction_b_id)
);

CREATE INDEX idx_faction_relations_a ON faction_relations(faction_a_id);
CREATE INDEX idx_faction_relations_b ON faction_relations(faction_b_id);

COMMENT ON TABLE faction_relations IS 'Diplomatische Beziehungen zwischen Fraktionen. Immer bidirektional gespeichert (nur ein Eintrag pro Paar).';
COMMENT ON COLUMN faction_relations.relation_status IS 'ALLIANCE | FRIENDLY | NEUTRAL | UNFRIENDLY | WAR';
