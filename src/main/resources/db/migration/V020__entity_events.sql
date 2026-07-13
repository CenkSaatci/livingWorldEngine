-- ============================================================
-- V020__entity_events.sql
-- Living World Engine — Entity-spezifisches Ereignis-Log
-- ============================================================
-- Jede Region, jeder Ort und jeder NPC bekommt eine eigene Chronik.
-- Der entity_type-Discriminator erlaubt Abfragen wie:
--   "Alle Events für Location X, sortiert nach Datum"
--
-- Siehe docs/WORLD-DEPTH.md

CREATE TABLE entity_events (
    id                BIGSERIAL    PRIMARY KEY,
    entity_type       VARCHAR(50)  NOT NULL,
    entity_id         UUID         NOT NULL,
    event_type        VARCHAR(100) NOT NULL,
    title             VARCHAR(200) NOT NULL,
    description       TEXT,
    importance        INTEGER      NOT NULL DEFAULT 1
                      CHECK (importance BETWEEN 1 AND 5),
    source_entity_id  UUID,
    metadata_json     JSONB        NOT NULL DEFAULT '{}',
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_entity_events_lookup
    ON entity_events(entity_type, entity_id, created_at DESC);

COMMENT ON TABLE entity_events IS 'Entity-spezifische Ereignis-Chronik für Regionen, Orte und NPCs. Siehe docs/WORLD-DEPTH.md';
COMMENT ON COLUMN entity_events.entity_type IS 'entity_type-Discriminator: region | location | npc';
COMMENT ON COLUMN entity_events.importance IS '1 = Alltag, 2 = Erwähnenswert, 3 = Wichtig, 4 = Bedeutend, 5 = Epochemachend';