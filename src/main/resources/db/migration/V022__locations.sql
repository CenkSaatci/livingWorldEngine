-- ============================================================
-- V022__locations.sql
-- Living World Engine — Orte in Regionen
-- ============================================================
-- Jede Region kann mehrere Orte enthalten (Dörfer, Städte, Burgen, …).
-- Orte haben Typ, Geschichte, Wohlstand und Dienstleistungen.
--
-- Siehe docs/WORLD-DEPTH.md

CREATE TABLE locations (
    id              UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    region_id       UUID         NOT NULL REFERENCES regions(id) ON DELETE CASCADE,
    type            VARCHAR(50)  NOT NULL DEFAULT 'village'
                    CHECK (type IN ('village','town','city','castle','dungeon','ruin',
                                    'temple','camp','tower','cave','port','mine')),
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    history         TEXT,
    population      INTEGER      NOT NULL DEFAULT 0,
    wealth          INTEGER      NOT NULL DEFAULT 5
                    CHECK (wealth BETWEEN 1 AND 10),
    services        JSONB        NOT NULL DEFAULT '[]',
    factions        JSONB        NOT NULL DEFAULT '[]',
    is_capital      BOOLEAN      NOT NULL DEFAULT false,
    position_json   JSONB,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_locations_region ON locations(region_id);

COMMENT ON TABLE locations IS 'Orte innerhalb einer Region. Jeder Ort hat einen Typ, Wohlstand und verfügbare Dienste.';
COMMENT ON COLUMN locations.type IS 'village, town, city, castle, dungeon, ruin, temple, camp, tower, cave, port, mine';
COMMENT ON COLUMN locations.wealth IS '1 (arm) bis 10 (reich) — beeinflusst Preise';
COMMENT ON COLUMN locations.services IS 'Verfügbare Dienste: ["inn","blacksmith","alchemist","trainer","temple","market","stable","guild"]';