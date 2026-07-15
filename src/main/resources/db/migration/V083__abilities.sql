CREATE TABLE abilities (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    world_id UUID NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    ability_type VARCHAR(20) NOT NULL CHECK (ability_type IN ('ACTIVE', 'PASSIVE')),
    description TEXT,
    effects_json JSONB NOT NULL DEFAULT '{}',
    stat_bonuses_json JSONB NOT NULL DEFAULT '{}',
    ap_cost INT NOT NULL DEFAULT 1,
    cooldown_rounds INT NOT NULL DEFAULT 0,
    target_type VARCHAR(20) NOT NULL DEFAULT 'enemy',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_abilities_world_id ON abilities(world_id);
