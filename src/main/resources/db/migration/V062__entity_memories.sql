-- ============================================================
-- V062__entity_memories.sql
-- Living World Engine — NPC-Gedächtnis + Beziehungen
-- ============================================================
-- Ermöglicht NPCs, sich an Interaktionen mit bestimmten
-- Spielern/NPCs zu erinnern und individuelle Beziehungen zu pflegen.
--
-- Siehe docs/NPC-MEMORY.md

CREATE TABLE entity_memories (
    id              BIGSERIAL    PRIMARY KEY,
    entity_id       UUID         NOT NULL REFERENCES entities(id) ON DELETE CASCADE,
    subject_id      UUID         NOT NULL REFERENCES entities(id) ON DELETE CASCADE,
    memory_type     VARCHAR(50)  NOT NULL,
    sentiment       INTEGER      NOT NULL DEFAULT 0 CHECK (sentiment BETWEEN -5 AND 5),
    summary         TEXT         NOT NULL,
    source_event_id BIGINT       REFERENCES world_events(id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_memories_entity ON entity_memories(entity_id, created_at DESC);
CREATE INDEX idx_memories_subject ON entity_memories(subject_id, created_at DESC);

COMMENT ON TABLE entity_memories IS 'NPC-Erinnerungen an Interaktionen mit bestimmten Entitäten.';
COMMENT ON COLUMN entity_memories.memory_type IS 'combat | disrespect | helped | trade | gossip';
COMMENT ON COLUMN entity_memories.sentiment IS '-5 (Todfeind) bis +5 (Verbündeter)';
COMMENT ON COLUMN entity_memories.source_event_id IS 'Optional: Verweis auf das auslösende World-Event';

CREATE TABLE entity_relationships (
    id              BIGSERIAL    PRIMARY KEY,
    entity_a_id     UUID         NOT NULL REFERENCES entities(id) ON DELETE CASCADE,
    entity_b_id     UUID         NOT NULL REFERENCES entities(id) ON DELETE CASCADE,
    relationship    VARCHAR(50)  NOT NULL DEFAULT 'neutral',
    modified_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE(entity_a_id, entity_b_id)
);

CREATE INDEX idx_relationships_a ON entity_relationships(entity_a_id);
CREATE INDEX idx_relationships_b ON entity_relationships(entity_b_id);

COMMENT ON TABLE entity_relationships IS 'Individuelle Beziehungen zwischen NPCs/Spielern.';
COMMENT ON COLUMN entity_relationships.relationship IS 'ally | friend | neutral | dislike | enemy | fear | love';
