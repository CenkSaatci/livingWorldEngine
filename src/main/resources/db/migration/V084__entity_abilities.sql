CREATE TABLE entity_abilities (
    entity_id UUID NOT NULL REFERENCES entities(id) ON DELETE CASCADE,
    ability_id UUID NOT NULL REFERENCES abilities(id) ON DELETE CASCADE,
    unlocked BOOLEAN NOT NULL DEFAULT true,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (entity_id, ability_id)
);

CREATE INDEX idx_entity_abilities_entity ON entity_abilities(entity_id);
CREATE INDEX idx_entity_abilities_ability ON entity_abilities(ability_id);
