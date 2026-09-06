-- ============================================================
-- V021__regions.sql
-- Living World Engine — Regionen einer Welt
-- ============================================================
-- Jede Welt kann in mehrere Regionen aufgeteilt werden.
-- Regionen haben Geschichte, Gefahrenlevel, Klima und Fraktionen.
--
-- Siehe docs/WORLD-DEPTH.md

CREATE TABLE regions (
    id              UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    world_id        UUID         NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    history         TEXT,
    danger_level    INTEGER      NOT NULL DEFAULT 1
                    CHECK (danger_level BETWEEN 1 AND 10),
    climate         VARCHAR(50)  NOT NULL DEFAULT 'temperate',
    resources       JSONB        NOT NULL DEFAULT '[]',
    factions        JSONB        NOT NULL DEFAULT '[]',
    population      INTEGER      NOT NULL DEFAULT 0,
    capital_id      UUID         NULL,
    position_json   JSONB,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_regions_world ON regions(world_id);

COMMENT ON TABLE regions IS 'Regionen einer Welt. Jede Region hat eigenes Klima, Gefahrenlevel und Ressourcen.';
COMMENT ON COLUMN regions.danger_level IS '1 = friedlich, 5 = gefährlich, 10 = tödlich';
COMMENT ON COLUMN regions.climate IS 'temperate, forest, desert, mountains, plains, swamp, coast, tundra, jungle';
COMMENT ON COLUMN regions.resources IS 'z.B. [{ "type": "iron_ore", "abundance": 3 }]';
COMMENT ON COLUMN regions.factions IS 'z.B. ["crown", "goblin_tribes", "merchant_guild"]';