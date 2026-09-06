ALTER TABLE combat_sessions ADD COLUMN campaign_id UUID REFERENCES campaigns(id) ON DELETE SET NULL;
CREATE INDEX idx_combat_sessions_campaign ON combat_sessions(campaign_id);
