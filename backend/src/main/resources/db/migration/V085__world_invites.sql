CREATE TABLE world_invites (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    world_id UUID NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    token VARCHAR(64) NOT NULL UNIQUE,
    created_by UUID NOT NULL REFERENCES users(id),
    max_uses INT NOT NULL DEFAULT 1,
    use_count INT NOT NULL DEFAULT 0,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_world_invites_token ON world_invites(token);
CREATE INDEX idx_world_invites_world ON world_invites(world_id);
