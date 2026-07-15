-- ============================================================
-- V080__world_maps.sql
-- Living World Engine — Karten-Hintergrund + Regions-Polygone
-- ============================================================

CREATE TABLE world_maps (
    id         UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    world_id   UUID         NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    name       VARCHAR(200) NOT NULL DEFAULT 'default',
    image_url  TEXT,
    width      INTEGER      NOT NULL DEFAULT 1000,
    height     INTEGER      NOT NULL DEFAULT 750,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_world_maps_world ON world_maps(world_id);

COMMENT ON TABLE world_maps IS 'Hintergrundkarten für Welten. Optional — Welt funktioniert auch ohne.';
COMMENT ON COLUMN world_maps.image_url IS 'Pfad zur hochgeladenen Bilddatei (NULL = nur Grid)';

ALTER TABLE regions
  ADD COLUMN polygon_points JSONB;

COMMENT ON COLUMN regions.polygon_points IS 'Array von {x,y}-Punkten für das Regions-Polygon auf der Karte. NULL = keine Polygon-Grenze.';
