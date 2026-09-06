ALTER TABLE items DROP COLUMN world_id;
ALTER TABLE items ADD COLUMN game_system_id UUID REFERENCES game_systems(id) ON DELETE CASCADE;
CREATE INDEX idx_items_game_system_id ON items(game_system_id);
