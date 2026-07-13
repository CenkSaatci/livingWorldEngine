-- ============================================================
-- V011__events_archive.sql
-- Living World Engine — Event-Archivierung (Retention 30 Tage)
-- ============================================================

CREATE TABLE world_events_archive (
    id                  BIGSERIAL    PRIMARY KEY,
    world_id            UUID         NOT NULL,
    event_type          VARCHAR(50)  NOT NULL,
    source_entity_id    UUID,
    target_entity_id    UUID,
    payload_json        JSONB        NOT NULL DEFAULT '{}',
    event_hash          VARCHAR(64),
    archived_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_events_archive_world ON world_events_archive(world_id, created_at DESC);

COMMENT ON TABLE world_events_archive IS 'Archivierte Events, älter als 30 Tage (from world_events). Retention Policy.';
