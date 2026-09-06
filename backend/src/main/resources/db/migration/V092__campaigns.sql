CREATE TABLE campaigns (
    id              UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    world_id        UUID        NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    game_system_id  UUID        NOT NULL REFERENCES game_systems(id) ON DELETE RESTRICT,
    name            VARCHAR(200) NOT NULL,
    settings_json   JSONB       NOT NULL DEFAULT '{}',
    state_json      JSONB       NOT NULL DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_campaigns_world_id ON campaigns(world_id);
CREATE INDEX idx_campaigns_game_system_id ON campaigns(game_system_id);
