CREATE TABLE game_sessions (
    id         UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    world_id   UUID        NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ended_at   TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_game_sessions_world ON game_sessions(world_id);
