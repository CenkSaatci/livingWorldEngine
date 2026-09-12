-- Runde 1 (Audit F1/F3): Entity-Ownership fuer Charakterkontrolle.
-- NULL = Legacy (member-level), wird per DM-Endpoint zugeordnet.
ALTER TABLE entities ADD COLUMN owner_user_id UUID REFERENCES users(id) ON DELETE SET NULL;
CREATE INDEX idx_entities_owner ON entities(owner_user_id);
