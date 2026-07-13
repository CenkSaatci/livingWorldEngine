-- ============================================================
-- V004__combat.sql
-- Living World Engine — Turn-basiertes Kampf-System
-- ============================================================

CREATE TABLE combat_sessions (
    id              UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    world_id        UUID         NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    current_turn_entity_id UUID  NULL,
    round           INTEGER      NOT NULL DEFAULT 1,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE', 'ENDED')),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    ended_at        TIMESTAMPTZ  NULL
);
CREATE INDEX idx_combat_world ON combat_sessions(world_id);

CREATE TABLE combat_participants (
    id              UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    combat_id       UUID         NOT NULL REFERENCES combat_sessions(id) ON DELETE CASCADE,
    entity_id       UUID         NOT NULL REFERENCES entities(id) ON DELETE CASCADE,
    initiative      INTEGER      NOT NULL,
    ap_current      INTEGER      NOT NULL,
    ap_max          INTEGER      NOT NULL,
    side            VARCHAR(20)  NOT NULL DEFAULT 'A'
                    CHECK (side IN ('A', 'B', 'NEUTRAL')),
    UNIQUE (combat_id, entity_id)
);
CREATE INDEX idx_combat_participants_combat ON combat_participants(combat_id);