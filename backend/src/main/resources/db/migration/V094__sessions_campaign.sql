ALTER TABLE game_sessions ADD COLUMN campaign_id UUID REFERENCES campaigns(id) ON DELETE SET NULL;
CREATE INDEX idx_game_sessions_campaign ON game_sessions(campaign_id);
