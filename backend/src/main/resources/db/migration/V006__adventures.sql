-- ============================================================
-- V006__adventures.sql
-- Living World Engine — Node-basierte Abenteuer-Struktur
-- ============================================================

CREATE TABLE adventures (
    id              UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    world_id        UUID         NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    name            VARCHAR(200) NOT NULL,
    description     TEXT         NULL,
    start_node_id   UUID         NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_adventures_world ON adventures(world_id);

CREATE TABLE adventure_nodes (
    id              UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    adventure_id    UUID         NOT NULL REFERENCES adventures(id) ON DELETE CASCADE,
    text            TEXT         NOT NULL,
    image_url       VARCHAR(500) NULL,
    is_end          BOOLEAN      NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_adventure_nodes_adventure ON adventure_nodes(adventure_id);

CREATE TABLE node_choices (
    id                  UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    node_id             UUID         NOT NULL REFERENCES adventure_nodes(id) ON DELETE CASCADE,
    label               VARCHAR(200) NOT NULL,
    target_node_id      UUID         REFERENCES adventure_nodes(id),
    skill_check         JSONB        NULL,
    on_success_node_id  UUID         REFERENCES adventure_nodes(id),
    on_failure_node_id  UUID         REFERENCES adventure_nodes(id),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_node_choices_node ON node_choices(node_id);

COMMENT ON COLUMN node_choices.skill_check IS 'Optional: { "skill": "staerke", "modifier": 5 }. Wird via Rule-Engine ausgewertet.';

CREATE TABLE adventure_progress (
    id              UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    adventure_id    UUID         NOT NULL REFERENCES adventures(id) ON DELETE CASCADE,
    entity_id       UUID         NOT NULL REFERENCES entities(id) ON DELETE CASCADE,
    current_node_id UUID         NOT NULL REFERENCES adventure_nodes(id),
    visited_nodes   TEXT         NOT NULL DEFAULT '{}',
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE', 'COMPLETED', 'ABANDONED')),
    started_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (adventure_id, entity_id)
);
CREATE INDEX idx_adventure_progress_entity ON adventure_progress(entity_id);