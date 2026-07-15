ALTER TABLE combat_sessions ADD COLUMN map_id UUID REFERENCES world_maps(id) ON DELETE SET NULL;
