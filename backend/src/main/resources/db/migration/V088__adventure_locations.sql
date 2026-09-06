ALTER TABLE adventures ADD COLUMN location_id UUID REFERENCES locations(id) ON DELETE SET NULL;
ALTER TABLE adventures ADD COLUMN giver_entity_id UUID REFERENCES entities(id) ON DELETE SET NULL;
