ALTER TABLE abilities DROP COLUMN world_id;
ALTER TABLE abilities ADD COLUMN game_system_id UUID REFERENCES game_systems(id) ON DELETE CASCADE;
CREATE INDEX idx_abilities_game_system_id ON abilities(game_system_id);
