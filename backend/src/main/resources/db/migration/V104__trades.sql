-- B4: Handel zwischen Charakteren (Angebot/Gegenangebot + Annahme).
CREATE TABLE trades (
    id UUID PRIMARY KEY,
    world_id UUID NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    proposer_entity_id UUID NOT NULL REFERENCES entities(id) ON DELETE CASCADE,
    partner_entity_id UUID NOT NULL REFERENCES entities(id) ON DELETE CASCADE,
    offer_json TEXT NOT NULL DEFAULT '[]',
    request_json TEXT NOT NULL DEFAULT '[]',
    status VARCHAR(20) NOT NULL DEFAULT 'proposed',
    last_editor_entity_id UUID NOT NULL REFERENCES entities(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_trades_world ON trades(world_id, status);
CREATE INDEX idx_trades_participants ON trades(proposer_entity_id, partner_entity_id);
